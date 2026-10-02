package com.funkytest.backend.web;

import java.util.List;
import java.util.UUID;

import com.funkytest.backend.catalog.Product;

/**
 * API shape of a product. Entities are never serialised directly: {@code
 * ProductVariant} points back at its {@code Product}, which would recurse, and
 * the lazy associations would blow up outside a transaction.
 */
public record ProductResponse(UUID id, String name, String slug, String description,
		boolean active, String categoryName, String categorySlug,
		List<ProductVariantResponse> variants) {

	static ProductResponse from(Product product) {
		return new ProductResponse(product.getId(), product.getName(), product.getSlug(),
				product.getDescription(), product.isActive(), product.getCategory().getName(),
				product.getCategory().getSlug(),
				product.getVariants().stream().map(ProductVariantResponse::from).toList());
	}

	static List<ProductResponse> from(List<Product> products) {
		return products.stream().map(ProductResponse::from).toList();
	}

}
