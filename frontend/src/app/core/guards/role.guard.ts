import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../../models/auth.models';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const allowed = route.data['roles'] as UserRole[] | undefined;
  if (!allowed?.length) {
    return true;
  }
  const role = auth.getRole();
  if (role && allowed.includes(role)) {
    return true;
  }
  if (auth.isAuthenticated()) {
    return router.createUrlTree([auth.getHomeRoute()]);
  }
  return router.createUrlTree(['/login']);
};