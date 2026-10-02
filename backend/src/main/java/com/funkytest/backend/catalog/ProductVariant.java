package com.funkytest.backend.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * The sellable unit of a {@link Product} — one SKU, one price, one stock level.
 * "T-shirt" is a product; "T-shirt / red / L" is a variant.
 */
@Entity
@Table(name = "product_variant")
public class ProductVariant {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	/** Stock-keeping unit, unique across the whole catalogue. */
	@NotBlank
	@Size(max = 64)
	@Column(name = "sku", nullable = false, length = 64, unique = true)
	private String sku;

	/** What distinguishes it within the product, e.g. "red / L". */
	@NotBlank
	@Size(max = 200)
	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@NotNull
	@PositiveOrZero
	@Column(name = "price", nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@PositiveOrZero
	@Column(name = "stock_quantity", nullable = false)
	private int stockQuantity;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ProductVariant() {
		// for JPA
	}

	public ProductVariant(String sku, String name, BigDecimal price, int stockQuantity) {
		this.sku = sku;
		this.name = name;
		this.price = price;
		this.stockQuantity = stockQuantity;
	}

	public UUID getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	/** Set through {@link Product#addVariant}, which owns the association. */
	void setProduct(Product product) {
		this.product = product;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public int getStockQuantity() {
		return stockQuantity;
	}

	public void setStockQuantity(int stockQuantity) {
		this.stockQuantity = stockQuantity;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof ProductVariant that && this.id != null && this.id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return ProductVariant.class.hashCode();
	}

	@Override
	public String toString() {
		return "ProductVariant[id=%s, sku=%s]".formatted(id, sku);
	}

}
