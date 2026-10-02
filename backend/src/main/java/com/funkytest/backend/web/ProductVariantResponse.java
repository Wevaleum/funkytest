package com.funkytest.backend.web;

import java.math.BigDecimal;
import java.util.UUID;

import com.funkytest.backend.catalog.ProductVariant;

public record ProductVariantResponse(UUID id, String sku, String name, BigDecimal price,
		int stockQuantity) {

	static ProductVariantResponse from(ProductVariant variant) {
		return new ProductVariantResponse(variant.getId(), variant.getSku(), variant.getName(),
				variant.getPrice(), variant.getStockQuantity());
	}

}
