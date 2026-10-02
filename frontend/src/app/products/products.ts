import { Component, computed } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ButtonModule } from 'primeng/button';
import { Product, ProductVariant } from '../product';

@Component({
  selector: 'app-products',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    TableModule,
    TagModule,
    MessageModule,
    ProgressSpinnerModule,
    ButtonModule,
  ],
  styleUrl: './products.scss',
  templateUrl: './products.html',
})
export class Products {
  /**
   * The whole catalogue in one request. `GET /api/products` is unpaged, so this
   * is a single multi-megabyte response that the table then renders in full.
   */
  protected readonly products = httpResource<Product[]>(() => '/api/products', {
    defaultValue: [],
  });

  /** Rows the table holds. No paginator: every product is in the DOM at once. */
  protected readonly total = computed(() => this.products.value().length);

  protected readonly variantTotal = computed(() =>
    this.products.value().reduce((sum, product) => sum + product.variants.length, 0),
  );

  protected lowestPrice(product: Product): number | null {
    if (product.variants.length === 0) {
      return null;
    }
    return product.variants.reduce(
      (lowest, variant) => Math.min(lowest, variant.price),
      Number.POSITIVE_INFINITY,
    );
  }

  protected stockOf(variant: ProductVariant): 'success' | 'warn' | 'danger' {
    if (variant.stockQuantity === 0) {
      return 'danger';
    }
    return variant.stockQuantity < 20 ? 'warn' : 'success';
  }
}
