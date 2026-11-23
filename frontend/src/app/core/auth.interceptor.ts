import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { SessionService } from './session.service';

/**
 * Adds the Bearer token to calls to our own API. If a signed-in person gets a 401 (token expired, or the account
 * was deactivated), the session is cleared and they go back to the sign-in page.
 * 401 answers from the sign-in endpoints themselves ("wrong password") are left alone.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionService);
  const router = inject(Router);

  const isApi = req.url.startsWith(environment.apiUrl);
  const isPublicAuthCall = req.url.startsWith(`${environment.apiUrl}/auth/`) && !req.url.endsWith('/auth/me');
  const token = session.token();

  const outgoing = isApi && token && !isPublicAuthCall ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(outgoing).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401 && isApi && !isPublicAuthCall && session.signedIn()) {
        session.clear();
        void router.navigateByUrl('/login');
      }
      return throwError(() => err);
    }),
  );
};
