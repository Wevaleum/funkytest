package com.funkytest.backend.catalog;

import java.math.BigDecimal;

import com.funkytest.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the Testcontainers Postgres with the Flyway schema, so these
 * tests also prove the entities and V2__product_catalog.sql agree.
 *
 * <p>{@code NOT_SUPPORTED} turns off the transaction {@code @DataJpaTest} would
 * wrap each test in. Every repository call then gets its own transaction and
 * its own persistence context, so a read really goes back to the database
 * instead of returning the instance a previous write left in the first-level
 * cache. The trade-off is that nothing rolls back, hence {@link #cleanUp()}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProductCatalogTests {

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private ProductRepository products;

	@Autowired
	private ProductVariantRepository variants;

	/** Only for the one check that deliberately bypasses the entities. */
	@Autowired
	private JdbcClient jdbcClient;

	@AfterEach
	void cleanUp() {
		products.deleteAll();
		categories.deleteAll();
	}

	private Product persistedTshirt() {
		Category clothing = categories.save(new Category("Clothing", "clothing"));
		Product tshirt = new Product(clothing, "Cotton T-shirt", "cotton-tshirt");
		tshirt.addVariant(new ProductVariant("TS-RED-L", "red / L", new BigDecimal("19.90"), 12));
		tshirt.addVariant(new ProductVariant("TS-BLU-M", "blue / M", new BigDecimal("19.90"), 4));
		return products.save(tshirt);
	}

	@Test
	void savingAProductCascadesToItsVariants() {
		Product saved = persistedTshirt();

		Product reloaded = products.findWithVariantsBySlug("cotton-tshirt").orElseThrow();
		assertThat(reloaded.getId()).isEqualTo(saved.getId());
		assertThat(reloaded.getVariants())
			.extracting(ProductVariant::getSku)
			.containsExactlyInAnyOrder("TS-RED-L", "TS-BLU-M");
	}

	@Test
	void generatesIdsAndTimestamps() {
		Product saved = persistedTshirt();

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(saved.getVariants()).allSatisfy(v -> assertThat(v.getId()).isNotNull());
	}

	@Test
	void keepsTheCategoryAssociation() {
		persistedTshirt();

		Product reloaded = products.findWithCategoryBySlug("cotton-tshirt").orElseThrow();
		assertThat(reloaded.getCategory().getSlug()).isEqualTo("clothing");
		assertThat(products.findByCategorySlugAndActiveTrue("clothing")).hasSize(1);
	}

	@Test
	void storesPriceWithTwoDecimals() {
		persistedTshirt();

		ProductVariant variant = variants.findBySku("TS-RED-L").orElseThrow();
		assertThat(variant.getPrice()).isEqualByComparingTo("19.90");
		assertThat(variant.getPrice().scale()).isEqualTo(2);
		assertThat(variant.getStockQuantity()).isEqualTo(12);
	}

	@Test
	void removingAVariantDeletesTheRow() {
		persistedTshirt();

		Product loaded = products.findWithVariantsBySlug("cotton-tshirt").orElseThrow();
		loaded.removeVariant(loaded.getVariants().get(0));
		products.save(loaded);

		Product reloaded = products.findWithVariantsBySlug("cotton-tshirt").orElseThrow();
		assertThat(reloaded.getVariants()).hasSize(1);
		assertThat(variants.count()).isEqualTo(1);
	}

	@Test
	void rejectsADuplicateSku() {
		persistedTshirt();

		Category other = categories.save(new Category("Outerwear", "outerwear"));
		Product jacket = new Product(other, "Rain jacket", "rain-jacket");
		jacket.addVariant(new ProductVariant("TS-RED-L", "yellow / S", new BigDecimal("89.00"), 2));

		assertThatThrownBy(() -> products.saveAndFlush(jacket))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uq_product_variant_sku");
	}

	@Test
	void beanValidationRejectsANegativePrice() {
		Category clothing = categories.save(new Category("Clothing", "clothing"));
		Product socks = new Product(clothing, "Wool socks", "wool-socks");
		socks.addVariant(new ProductVariant("SK-GRY-42", "grey / 42", new BigDecimal("-1.00"), 1));

		assertThatThrownBy(() -> products.saveAndFlush(socks))
			.isInstanceOf(ConstraintViolationException.class)
			.hasMessageContaining("price");
	}

	/**
	 * Belt and braces: the CHECK holds even for writes that bypass the entities.
	 * The table is schema-qualified because a plain JDBC connection has no
	 * search_path set — only Hibernate knows about {@code default_schema}.
	 */
	@Test
	void theDatabaseAlsoRejectsANegativePrice() {
		Product saved = persistedTshirt();

		assertThatThrownBy(() -> jdbcClient.sql("""
				INSERT INTO funkytest.product_variant (product_id, sku, name, price, stock_quantity)
				VALUES (:productId, 'RAW-NEG-1', 'raw', -5.00, 1)
				""")
			.param("productId", saved.getId())
			.update())
			.hasMessageContaining("ck_product_variant_price");
	}

}
