import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthApiService } from '../services/auth-api.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthApiService);
  const router = inject(Router);
  return auth.cargarSesion().pipe(
    map((usuario) => usuario ? true : router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url },
    })),
  );
};

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthApiService);
  const router = inject(Router);
  const roles = route.data['roles'] as string[];
  return auth.cargarSesion().pipe(
    map((usuario) => {
      if (!usuario) {
        return router.createUrlTree(['/login']);
      }
      return roles.includes(usuario.rol) ? true : router.createUrlTree(['/']);
    }),
  );
};
