package com.funkytest.backend.catalog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * A catalogue entry. Holds what every variant shares; price and stock live on
 * {@link ProductVariant}.
 *
 * <p>The product is the aggregate root for its variants: they cascade and are
 * orphan-removed, so {@code productRepository.save(product)} persists the whole
 * thing and {@link #removeVariant} deletes the row.
 */
@Entity
@Table(name = "product")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@NotBlank
	@Size(max = 200)
	@Column(name = "name", nullable = false, length = 200)
	private String name;

	/** URL-safe identifier, unique across products. */
	@NotBlank
	@Size(max = 220)
	@Column(name = "slug", nullable = false, length = 220, unique = true)
	private String slug;

	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	@Column(name = "description")
	private String description;

	/** Whether the product is offered for sale; kept out of deletion. */
	@Column(name = "active", nullable = false)
	private boolean active = true;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductVariant> variants = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Product() {
		// for JPA
	}

	public Product(Category category, String name, String slug) {
		this.category = category;
		this.name = name;
		this.slug = slug;
	}

	/** Adds a variant and keeps both sides of the association in step. */
	public void addVariant(ProductVariant variant) {
		this.variants.add(variant);
		variant.setProduct(this);
	}

	/** Detaches a variant, which deletes its row on flush. */
	public void removeVariant(ProductVariant variant) {
		this.variants.remove(variant);
		variant.setProduct(null);
	}

	public UUID getId() {
		return id;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	/** Unmodifiable: go through {@link #addVariant} / {@link #removeVariant}. */
	public List<ProductVariant> getVariants() {
		return Collections.unmodifiableList(variants);
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
		return other instanceof Product that && this.id != null && this.id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Product.class.hashCode();
	}

	@Override
	public String toString() {
		return "Product[id=%s, slug=%s]".formatted(id, slug);
	}

}
