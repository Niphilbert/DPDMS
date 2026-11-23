import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from './models';
import { SessionService } from './session.service';

/** Only signed-in people may open the app. Everyone else goes to the sign-in page. */
export const authGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  return session.signedIn() ? true : inject(Router).createUrlTree(['/login']);
};

/** Keeps signed-in people out of the sign-in and register pages. */
export const guestGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  return session.signedIn() ? inject(Router).createUrlTree(['/dashboard']) : true;
};

/** Allows a route only for the given roles, set in the route data: `data: { roles: ['ADMIN'] }`. */
export const roleGuard: CanActivateFn = (route) => {
  const session = inject(SessionService);
  const roles = (route.data['roles'] ?? []) as Role[];
  if (!session.signedIn()) {
    return inject(Router).createUrlTree(['/login']);
  }
  return session.hasRole(...roles) ? true : inject(Router).createUrlTree(['/dashboard']);
};
