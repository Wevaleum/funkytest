package com.funkytest.backend.web;

import java.util.List;

import com.funkytest.backend.catalog.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductRepository products;

	public ProductController(ProductRepository products) {
		this.products = products;
	}

	/**
	 * The whole catalogue, with each product's category and variants.
	 *
	 * <p>The repository query uses an entity graph, so this is
	 * a single SQL statement rather than N+1 — but it still loads every row into
	 * memory and serialises it into one response. At fixture size (10k products)
	 * that is a multi-megabyte payload; it is fine for development and not
	 * something to expose to real traffic unchanged.
	 *
	 * <p>{@code readOnly} is not decoration: it keeps a session open while the
	 * DTOs are built and lets Hibernate skip dirty checking on every one of
	 * those entities.
	 */
	@GetMapping
	@Transactional(readOnly = true)
	public List<ProductResponse> list() {
		return ProductResponse.from(products.findAllWithCategoryAndVariantsBy());
	}

}
