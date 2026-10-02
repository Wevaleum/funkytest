package com.funkytest.backend.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the startup catalogue seeding. Bound from {@code app.fixtures.*}.
 */
@ConfigurationProperties(prefix = "app.fixtures")
public record FixtureProperties(boolean enabled, int productCount, int maxVariantsPerProduct) {

	public FixtureProperties {
		if (productCount < 0) {
			throw new IllegalArgumentException("app.fixtures.product-count must not be negative");
		}
		if (maxVariantsPerProduct < 1) {
			maxVariantsPerProduct = 1;
		}
	}

}
