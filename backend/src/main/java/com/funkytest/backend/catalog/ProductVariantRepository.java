package com.funkytest.backend.catalog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Variants are normally reached through their {@link Product}; this exists for
 * the one case that genuinely stands alone — looking a SKU up directly.
 */
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

	Optional<ProductVariant> findBySku(String sku);

}
