import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthRole, AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);

  if (!auth.requiresAuth() || auth.isAuthenticated()) {
    return true;
  }

  auth.login(state.url);
  return false;
};

export function roleGuard(roles: AuthRole[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.hasAnyRole(roles)) {
      return true;
    }

    return router.createUrlTree(['/toggles']);
  };
}
