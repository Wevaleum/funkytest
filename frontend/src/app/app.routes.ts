import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./products/products').then((m) => m.Products),
    title: 'Products',
  },
];
