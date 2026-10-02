package com.funkytest.backend.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	Optional<Product> findBySlug(String slug);

	/** Loads the variants in the same query, so rendering a product page is one round trip. */
	@EntityGraph(attributePaths = "variants")
	Optional<Product> findWithVariantsBySlug(String slug);

	/** Loads the category eagerly, for callers that read it outside a transaction. */
	@EntityGraph(attributePaths = "category")
	Optional<Product> findWithCategoryBySlug(String slug);

	List<Product> findByCategorySlugAndActiveTrue(String categorySlug);

	/**
	 * Every product with its category and variants in one query. Deliberately
	 * unpaged, so the whole catalogue is loaded and held in memory — see the
	 * warning on GET /api/products.
	 */
	@EntityGraph(attributePaths = { "category", "variants" })
	List<Product> findAllWithCategoryAndVariantsBy();

}
