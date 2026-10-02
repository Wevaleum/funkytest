package com.funkytest.backend.catalog;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Groups products. Deliberately has no {@code List<Product>}: loading a whole
 * category's products is a query on {@link ProductRepository}, not a field.
 */
@Entity
@Table(name = "category")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotBlank
	@Size(max = 120)
	@Column(name = "name", nullable = false, length = 120, unique = true)
	private String name;

	/** URL-safe identifier, unique across categories. */
	@NotBlank
	@Size(max = 140)
	@Column(name = "slug", nullable = false, length = 140, unique = true)
	private String slug;

	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	@Column(name = "description")
	private String description;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Category() {
		// for JPA
	}

	public Category(String name, String slug) {
		this.name = name;
		this.slug = slug;
	}

	public UUID getId() {
		return id;
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

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	// Identity is the generated id. Two unsaved instances are never equal, and
	// hashCode stays constant so an entity can sit in a HashSet before and
	// after it is persisted.
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof Category that && this.id != null && this.id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Category.class.hashCode();
	}

	@Override
	public String toString() {
		return "Category[id=%s, slug=%s]".formatted(id, slug);
	}

}
