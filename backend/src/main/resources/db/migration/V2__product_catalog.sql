-- Product catalogue: a category holds products, a product holds its variants.
-- Variants are the sellable units (a SKU, a price, a stock level); the product
-- carries what is common to all of them.

CREATE TABLE category (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(120) NOT NULL,
    slug        varchar(140) NOT NULL,
    description text,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_category_name UNIQUE (name),
    CONSTRAINT uq_category_slug UNIQUE (slug)
);

CREATE TABLE product (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid         NOT NULL REFERENCES category (id),
    name        varchar(200) NOT NULL,
    slug        varchar(220) NOT NULL,
    description text,
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_slug UNIQUE (slug)
);

-- Listing a category's products is the most common read.
CREATE INDEX idx_product_category ON product (category_id);

CREATE TABLE product_variant (
    id             uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Variants have no life of their own: deleting the product deletes them.
    product_id     uuid          NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    sku            varchar(64)   NOT NULL,
    name           varchar(200)  NOT NULL,
    price          numeric(12,2) NOT NULL,
    stock_quantity integer       NOT NULL DEFAULT 0,
    created_at     timestamptz   NOT NULL DEFAULT now(),
    updated_at     timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_variant_sku UNIQUE (sku),
    CONSTRAINT uq_product_variant_name UNIQUE (product_id, name),
    CONSTRAINT ck_product_variant_price CHECK (price >= 0),
    CONSTRAINT ck_product_variant_stock CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_product_variant_product ON product_variant (product_id);
