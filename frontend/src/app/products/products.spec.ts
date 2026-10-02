import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { Products } from './products';
import { Product } from '../product';

function product(id: string, name: string, variants: number): Product {
  return {
    id,
    name,
    slug: name.toLowerCase().replace(/ /g, '-'),
    description: null,
    active: true,
    categoryName: 'Clothing',
    categorySlug: 'clothing',
    variants: Array.from({ length: variants }, (_, i) => ({
      id: `${id}-${i}`,
      sku: `SKU-${id}-${i}`,
      name: `variant ${i}`,
      price: 10 + i,
      stockQuantity: i,
    })),
  };
}

describe('Products', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Products],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        providePrimeNG({ theme: { preset: Aura } }),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('renders one row per product, with no paginator', async () => {
    const fixture = TestBed.createComponent(Products);
    // Kicks off the resource's effect. Do not await whenStable() first: the
    // pending request keeps the app unstable.
    fixture.detectChanges();

    httpMock.expectOne('/api/products').flush([
      product('1', 'Cotton T-shirt', 2),
      product('2', 'Rain jacket', 1),
      product('3', 'Wool socks', 3),
    ]);
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelectorAll('tbody tr').length).toBe(3);
    expect(host.textContent).toContain('Cotton T-shirt');
    expect(host.textContent).toContain('3 products');
    expect(host.textContent).toContain('6 variants');
    expect(host.querySelector('p-paginator')).toBeNull();
  });

  it('shows an error with a retry when the API is unreachable', async () => {
    const fixture = TestBed.createComponent(Products);
    fixture.detectChanges();

    httpMock
      .expectOne('/api/products')
      .error(new ProgressEvent('failed'), { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('Could not reach /api/products');
    expect(host.querySelector('p-button')).not.toBeNull();
  });
});
