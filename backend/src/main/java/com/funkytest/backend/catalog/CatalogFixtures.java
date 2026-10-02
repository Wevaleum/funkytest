package com.funkytest.backend.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Fills an empty catalogue on startup so there is something to look at.
 *
 * <p>Writes in chunks through {@link ProductRepository#saveAll}, which with
 * {@code hibernate.jdbc.batch_size} turns the load into a handful of batched
 * inserts rather than one statement per row. It is a no-op as soon as a single
 * product exists, so restarting does not duplicate anything.
 */
@Component
@ConditionalOnProperty(name = "app.fixtures.enabled", havingValue = "true")
@EnableConfigurationProperties(FixtureProperties.class)
class CatalogFixtures implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CatalogFixtures.class);

	/** Rows per flush. Matches hibernate.jdbc.batch_size. */
	private static final int CHUNK_SIZE = 500;

	private static final List<String> CATEGORY_NAMES = List.of("Clothing", "Outerwear", "Footwear",
			"Bags", "Accessories", "Home", "Kitchen", "Outdoor", "Sport", "Stationery");

	private static final List<String> COLOURS = List.of("black", "white", "red", "blue", "green");

	private static final List<String> SIZES = List.of("S", "M", "L", "XL");

	private final CategoryRepository categories;

	private final ProductRepository products;

	private final FixtureProperties properties;

	CatalogFixtures(CategoryRepository categories, ProductRepository products,
			FixtureProperties properties) {
		this.categories = categories;
		this.products = products;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		long existing = products.count();
		if (existing > 0) {
			log.info("Catalogue already holds {} products, skipping fixtures", existing);
			return;
		}

		long startedAt = System.currentTimeMillis();
		List<Category> saved = categories.saveAll(buildCategories());

		// Fixed seed: the same catalogue every time, which keeps manual testing
		// and screenshots reproducible.
		Random random = new Random(42L);
		List<Product> chunk = new ArrayList<>(CHUNK_SIZE);
		int variantCount = 0;

		for (int i = 1; i <= properties.productCount(); i++) {
			Product product = buildProduct(i, saved.get(i % saved.size()), random);
			variantCount += product.getVariants().size();
			chunk.add(product);
			if (chunk.size() == CHUNK_SIZE) {
				products.saveAll(chunk);
				chunk.clear();
			}
		}
		if (!chunk.isEmpty()) {
			products.saveAll(chunk);
		}

		log.info("Seeded {} categories, {} products and {} variants in {} ms", saved.size(),
				properties.productCount(), variantCount, System.currentTimeMillis() - startedAt);
	}

	private List<Category> buildCategories() {
		List<Category> result = new ArrayList<>(CATEGORY_NAMES.size());
		for (String name : CATEGORY_NAMES) {
			Category category = new Category(name, slugify(name));
			category.setDescription("Sample category: " + name);
			result.add(category);
		}
		return result;
	}

	private Product buildProduct(int index, Category category, Random random) {
		String reference = "%05d".formatted(index);
		Product product = new Product(category, "Product " + reference, "product-" + reference);
		product.setDescription("Generated fixture product " + reference);
		// A tenth of the catalogue is inactive, so filtering on `active` is
		// exercised by real data.
		product.setActive(index % 10 != 0);

		int variants = 1 + random.nextInt(properties.maxVariantsPerProduct());
		// uq_product_variant_name is UNIQUE (product_id, name), so the colour/size
		// pairs have to differ. Walking consecutive combinations from a random
		// start guarantees that without a retry loop.
		int combinations = COLOURS.size() * SIZES.size();
		int firstCombination = random.nextInt(combinations);

		for (int v = 0; v < variants; v++) {
			int combination = (firstCombination + v) % combinations;
			String colour = COLOURS.get(combination / SIZES.size());
			String size = SIZES.get(combination % SIZES.size());
			BigDecimal price = BigDecimal.valueOf(5 + random.nextInt(29_500), 2)
				.setScale(2, RoundingMode.HALF_UP);
			product.addVariant(new ProductVariant("SKU-%s-%d".formatted(reference, v),
					"%s / %s".formatted(colour, size), price, random.nextInt(250)));
		}
		return product;
	}

	private static String slugify(String value) {
		return value.toLowerCase().replace(' ', '-');
	}

}
