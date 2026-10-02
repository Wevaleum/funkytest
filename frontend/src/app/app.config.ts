import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideRouter } from '@angular/router';
// PrimeNG 21 animates in CSS through @primeuix/motion and never imports
// @angular/animations, so provideAnimationsAsync() is not needed.
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withFetch()),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          // Follow the OS setting instead of a toggle we do not have yet.
          darkModeSelector: '@media (prefers-color-scheme: dark)',
        },
      },
    }),
    provideRouter(routes),
  ],
};
