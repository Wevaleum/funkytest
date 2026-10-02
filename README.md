# funkytest

Full-stack monorepo: a Spring Boot API and an Angular single-page app.

| Folder      | Stack                                            | Dev port |
| ----------- | ------------------------------------------------ | -------- |
| `backend/`  | Spring Boot 4.1.1, Java 25 target, JPA + Flyway   | 8080     |
| `frontend/` | Angular 21.2, PrimeNG 21, zoneless, Vitest        | 4200     |
| `docker/`   | Postgres 18 image with baked-in init scripts     | 5432     |

## Prerequisites

- JDK 25 or newer (built and tested on JDK 26)
- Node.js 20.19+ / 22.12+ / 24+ (built and tested on Node 26)
- Angular is pinned to **21.2** because PrimeNG 21 declares `@angular/core: ^21.0.0`
- Docker, for the Postgres container and for the Testcontainers-based tests

Maven comes from `backend/mvnw`; the Angular CLI comes from `frontend/node_modules`.

## Run it

```bash
# 0 — database on localhost:5432 (not yet required by the API, see Database)
docker compose up -d

# 1 — API on http://localhost:8080
cd backend && ./mvnw spring-boot:run

# 2 — app on http://localhost:4200
cd frontend && npm start
```

Open http://localhost:4200. The page lists the whole catalogue, so a table full of
products on screen means both halves are talking.

## How the two are wired

In development the Angular dev server proxies `/api/**` to port 8080
(`frontend/proxy.conf.json`, referenced from `angular.json`), so the browser sees a
single origin and CORS never comes into play.

For any deployment where the app is served from a *different* origin than the API, the
backend allows browser origins listed in `app.cors.allowed-origins`
(`backend/src/main/resources/application.yml`, default `http://localhost:4200`).

## Database

`docker compose up -d` builds `docker/postgres/Dockerfile` (Postgres 18 Alpine) and
starts it with database/user/password all defaulting to `funkytest`. Override them by
copying `.env.example` to `.env` — these are dev-only credentials, never production ones.

Everything in `docker/postgres/init/` is copied into `/docker-entrypoint-initdb.d` and
runs **once, in lexical order, only when the data directory is empty**:

| Script                 | What it does                                          |
| ---------------------- | ----------------------------------------------------- |
| `01-extensions.sql`    | `pgcrypto` (UUIDs, hashing), `pg_trgm` (fuzzy search) |
| `02-search-path.sql`   | Puts `funkytest` first on `search_path` for psql convenience |

The scripts deliberately do **not** create the `funkytest` schema or any table — that
belongs to Flyway (see *Migrations*). They only set up what has to exist before the
application connects.

So editing a script does nothing to a volume that already holds data. To re-run them:

```bash
docker compose down -v && docker compose up -d --build   # destroys the data
```

```bash
docker compose ps                    # status + health
docker compose logs -f postgres      # logs
docker compose exec postgres psql -U funkytest -d funkytest
docker compose down                  # stop, keep the data
docker compose down -v               # stop and delete the volume
```

Note: Postgres 18 moved `PGDATA` to `/var/lib/postgresql/18/docker` and declares the
volume at `/var/lib/postgresql`, which is what `compose.yaml` mounts. Mounting
`/var/lib/postgresql/data` (correct for Postgres 17 and earlier) would look fine but
lose the data when the container is recreated.

### How the backend connects

`backend/` uses Spring Data JPA over the PostgreSQL driver, with Flyway migrating the
schema at startup. The datasource is declared
in `application.yml` and defaults to the same values as `compose.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:funkytest}
    username: ${POSTGRES_USER:funkytest}
    password: ${POSTGRES_PASSWORD:funkytest}
```

So one `.env` drives both compose and the app, and any value can be overridden per
environment (`SPRING_DATASOURCE_URL`, `POSTGRES_HOST`, …).

Two deliberate choices:

- `spring.jpa.hibernate.ddl-auto: validate` — Hibernate never writes DDL. The schema
  belongs to `docker/postgres/init/`, and later to a migration tool.
- `spring.jpa.properties.hibernate.default_schema: funkytest` — entities resolve to
  the `funkytest` schema created by the init scripts, not to `public`.

**The API now needs Postgres to start.** `./mvnw spring-boot:run` fails fast with a
connection error if `docker compose up -d` has not been run. If you would rather have
the container start automatically, add `spring-boot-docker-compose` to `backend/pom.xml`
and Boot will drive `compose.yaml` itself for you.

### Tests do not need the container

`./mvnw test` starts its own throwaway Postgres through Testcontainers
(`TestcontainersConfiguration`), so it passes whether or not `docker compose up` has run
and never touches the development data. It does need the Docker daemon to be running.

The container needs no init script: Flyway creates the schema and runs the same
migrations it runs in development, so test and dev schemas cannot drift apart.

Tests that do not touch the database stay slice tests — `GreetingControllerTests` is a
`@WebMvcTest` and starts no container at all.

### Convention: no `EntityManager`

Persistence goes through Spring Data repositories, never through `EntityManager` — in
tests as much as in production code. `ProductCatalogTests` therefore runs with
`@Transactional(propagation = NOT_SUPPORTED)`, which switches off the transaction
`@DataJpaTest` would otherwise wrap each test in. Every repository call then gets its own
transaction and persistence context, so a read genuinely goes back to the database rather
than handing back the instance a previous write left in the first-level cache — the job
`entityManager.clear()` used to do.

Two consequences worth knowing:

- Nothing rolls back, so the class cleans up in `@AfterEach` with `deleteAll()`
  (products before categories, to respect the foreign key).
- Reads happen outside a transaction, so a LAZY association cannot be walked. Add an
  `@EntityGraph` finder instead of relying on lazy loading — that is what
  `findWithVariantsBySlug` and `findWithCategoryBySlug` are for.

Raw SQL uses `JdbcClient`. Remember to schema-qualify (`funkytest.product_variant`): a
plain JDBC connection has no `search_path`, only Hibernate knows about `default_schema`.

## Migrations

Flyway owns the `funkytest` schema. Migrations live in
`backend/src/main/resources/db/migration/` and run automatically on every startup,
before Hibernate looks at the schema.

| Migration                 | What it does                                      |
| ------------------------- | ------------------------------------------------- |
| `V1__initial_schema.sql`  | `pgcrypto` and `pg_trgm` extensions in `public`   |
| `V2__product_catalog.sql` | `category`, `product`, `product_variant` tables   |

`spring.flyway.schemas: funkytest` means Flyway creates the schema itself if it is
missing and keeps `flyway_schema_history` inside it. A completely empty database is
enough — nothing has to pre-create the schema.

To add a change, drop a new file in and restart:

```
V2__add_customer_table.sql
```

Two rules Flyway enforces for you:

- **Never edit an applied migration.** Flyway stores a checksum per migration and
  validates them all at startup; an edited file fails the boot rather than drifting
  silently. Fix forward with a new `V<n>` instead.
- **Hibernate never writes DDL** (`ddl-auto: validate`). If an entity and the migrated
  schema disagree, startup fails instead of quietly altering tables.

Inspect the state at any time:

```bash
docker compose exec postgres psql -U funkytest -d funkytest \
  -c 'TABLE funkytest.flyway_schema_history'
```

## Domain: the product catalogue

`com.funkytest.backend.catalog` holds three entities:

```
Category ---< Product ---< ProductVariant
```

- **`Category`** groups products. It has no `List<Product>` field on purpose — listing a
  category's products is a repository query, not a lazily loaded collection.
- **`Product`** is what a shopper browses: name, slug, description, active flag. It is the
  **aggregate root** for its variants, which cascade and are orphan-removed, so
  `productRepository.save(product)` writes the whole thing and `product.removeVariant(v)`
  deletes the row. Use `addVariant` / `removeVariant` — `getVariants()` is unmodifiable so
  both sides of the association cannot drift.
- **`ProductVariant`** is the sellable unit: one `sku`, one `price`, one `stock_quantity`.
  "T-shirt" is a product; "T-shirt / red / L" is a variant.

Keys are `uuid` (`GenerationType.UUID`), money is `numeric(12,2)` mapped to `BigDecimal`,
and timestamps are `Instant` filled by Hibernate's `@CreationTimestamp` /
`@UpdateTimestamp`.

Constraints are enforced in the database, not only in Java: `sku` is globally unique, a
variant name is unique within its product, and `price`/`stock_quantity` carry `CHECK
(>= 0)`. Bean Validation mirrors them so bad input fails before a round trip.

Repositories: `CategoryRepository`, `ProductRepository` (including
`findWithVariantsBySlug` and `findWithCategoryBySlug`, which fetch the association in one
query via an entity graph) and `ProductVariantRepository` for direct SKU lookups. All
persistence goes through these — see *Convention: no `EntityManager`*.

Because `ddl-auto` is `validate`, an entity that disagrees with the migration fails
startup — the entities and `V2__product_catalog.sql` are checked against each other on
every boot and every test run.

## API

| Endpoint                  | Returns                                             |
| ------------------------- | --------------------------------------------------- |
| `GET /api/greeting?name=` | `{message, timestamp}` — the connectivity smoke test |
| `GET /api/products`       | Every product, with its category and its variants    |

### `GET /api/products` is unpaged on purpose

It returns the **whole** catalogue in one response. Measured against the 10k-product
fixture set:

| | |
| --- | --- |
| Response size | **4.4 MB** (4,562,818 bytes) |
| Response time | ~0.17–0.27 s warm |
| SQL statements | **1** |
| Rows materialised | 10,000 products + 20,074 variants |

The single statement is not luck: `findAllWithCategoryAndVariantsBy()` carries
`@EntityGraph(attributePaths = {"category", "variants"})`, so category and variants come
back in the same join rather than one query per product. Verified by counting statements
with `SPRING_JPA_SHOW_SQL=true` — without the graph this would be 10,001 queries.

What is still true: every row is held in memory and serialised into one payload. That is
fine for development, and the shape to change before real traffic — either paginate at
the web layer or return a listing projection (name, slug, price range) instead of full
variants. The repository method is deliberately unpaged, so the decision lives in the
controller.

Entities are never serialised directly. `ProductResponse` / `ProductVariantResponse`
exist because `ProductVariant` points back at its `Product`, which would recurse forever,
and because lazy associations would fail outside a transaction.

## Fixtures

On startup, if the catalogue is empty, `CatalogFixtures` seeds it. It is a no-op as soon
as one product exists, so restarting never duplicates anything.

```yaml
app:
  fixtures:
    enabled: true          # set false anywhere the data matters
    product-count: 10000
    max-variants-per-product: 3
```

Measured: **10 categories, 10,000 products and 20,074 variants in 965 ms**. That speed
comes from writing in chunks of 500 through `saveAll` with
`hibernate.jdbc.batch_size: 500` and `order_inserts: true`. UUID keys help — they are
generated in the application, so no insert has to round-trip for a key.

The data is deterministic (`Random` with a fixed seed), so the same catalogue appears
every time. One product in ten is `active = false`, which gives the `active` filters
something real to work on. Variant colour/size pairs are walked consecutively from a
random start rather than drawn independently, because `uq_product_variant_name` is
`UNIQUE (product_id, name)` and independent draws collide.

Tests disable fixtures with `@TestPropertySource(properties = "app.fixtures.enabled=false")`
— seeding 10k rows would dominate the run time.

To reseed from scratch:

```bash
docker compose down -v && docker compose up -d --build   # destroys the data
```

## Front end

Angular **21.2** with **PrimeNG 21** (Aura preset via `@primeuix/themes` 2.x). The version
pin is not cosmetic: PrimeNG 21 peer-depends on `@angular/core: ^21.0.0` and does not
support Angular 22, so the workspace was regenerated on the 21 line rather than forcing
the peer range.

Two version traps worth remembering:

- `@primeuix/themes` must be **2.x**, not 3.x. PrimeNG 21 and themes 2.x both resolve
  `@primeuix/styled@^0.7.4`; themes 3.x pulls `^1.0.0`, which gives two copies of the
  styling engine and a theme that silently does nothing. `npm ls @primeuix/styled`
  should show a single deduped entry.
- PrimeNG 21 animates in CSS through `@primeuix/motion` and never imports
  `@angular/animations`, so `provideAnimationsAsync()` is **not** needed. Adding it pulls
  in a package Angular 21 no longer installs by default and breaks the build.

The single route renders `Products`, which calls `GET /api/products` with `httpResource`
and feeds a PrimeNG `p-table`. Each row expands to a nested table of its variants.

### Everything renders at once

No paginator, no virtual scroll — every product is in the DOM. Measured in Chrome against
the 10k fixture set:

| | |
| --- | --- |
| Rows rendered | **10,000** (plus 20,000 `p-tag` elements) |
| DOM nodes | ~150,000 |
| JS heap | ~654 MB |
| API response | 4.56 MB in 191 ms |
| Paginator present | no |

It works and it is responsive once painted, but 654 MB of heap for one page is the price
of the "show everything" requirement. The two ways out, in order of effort: turn on
`[virtualScroll]` on the `p-table` (keeps the single request, renders only visible rows),
or paginate at the API and page the table against it.

## Common commands

```bash
# backend
cd backend
./mvnw test                  # tests; starts its own Postgres via Testcontainers
./mvnw spring-boot:run       # dev server, with devtools restart (needs the DB up)
./mvnw package               # executable jar in target/

# frontend
cd frontend
npm start                    # dev server with the API proxy
npm test                     # Vitest, single run: npx ng test --no-watch
npm run build                # production bundle in dist/frontend
```

Health check: http://localhost:8080/actuator/health — the `db` component reports the
Postgres connection. `application.yml` sets `show-details: when-authorized`, so with no
security configured the response is just `{"status":"UP"}`; to see the per-component
detail locally, start with `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always`.

## Layout

```
docker/postgres/
  Dockerfile                     postgres:18-alpine + init scripts
  init/01-extensions.sql         pgcrypto, pg_trgm
  init/02-search-path.sql        psql search_path convenience

backend/src/main/java/com/funkytest/backend/catalog/
  Category.java                  groups products
  Product.java                   aggregate root, owns its variants
  ProductVariant.java            sku, price, stock
  *Repository.java               Spring Data JPA repositories
  CatalogFixtures.java           seeds 10k products on an empty catalogue
  FixtureProperties.java         binds app.fixtures.*

backend/src/main/resources/db/migration/
  V1__initial_schema.sql         Flyway owns the funkytest schema
  V2__product_catalog.sql        category / product / product_variant

backend/src/main/java/com/funkytest/backend/
  BackendApplication.java        entry point
  config/CorsProperties.java     binds app.cors.*
  config/WebConfig.java          applies the CORS rules to /api/**
  web/GreetingController.java    GET /api/greeting
  web/Greeting.java              response record
  web/ProductController.java     GET /api/products (unpaged)
  web/ProductResponse.java       API shape, keeps entities off the wire
  web/ProductVariantResponse.java
backend/src/test/java/com/funkytest/backend/
  TestcontainersConfiguration.java  throwaway Postgres for @SpringBootTest
  DatabaseConnectionTests.java      asserts the connection and that Flyway migrated
  catalog/ProductCatalogTests.java  cascade, constraints, entity/migration agreement
  web/ProductControllerTests.java   JSON shape of GET /api/products

frontend/src/app/
  app.ts / app.html              shell: header + router outlet
  app.config.ts                  router, HttpClient (fetch), PrimeNG theme
  app.routes.ts                  '' -> lazy-loaded Products
  product.ts                     mirrors ProductResponse
  products/products.ts           httpResource call to /api/products
```

## Next steps

- Auth: add `spring-boot-starter-security`.
- Generate Angular code with `cd frontend && npx ng generate component <name>`.
