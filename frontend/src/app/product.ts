/** Mirrors `ProductVariantResponse` on the backend. */
export interface ProductVariant {
  id: string;
  sku: string;
  name: string;
  price: number;
  stockQuantity: number;
}

/** Mirrors `ProductResponse`, the payload of `GET /api/products`. */
export interface Product {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  active: boolean;
  categoryName: string;
  categorySlug: string;
  variants: ProductVariant[];
}
