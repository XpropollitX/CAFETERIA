import { HttpBackend, HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';

const safeMethods = new Set(['GET', 'HEAD', 'OPTIONS']);

export const authCookieInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(environment.apiBaseUrl)) {
    return next(request);
  }

  const credentialedRequest = request.clone({ withCredentials: true });
  if (safeMethods.has(request.method)) {
    return next(credentialedRequest);
  }

  const csrfClient = new HttpClient(inject(HttpBackend));
  return csrfClient.get<{ token: string; headerName: string }>(
    `${environment.apiBaseUrl}/auth/csrf`,
    { withCredentials: true },
  ).pipe(
    switchMap(({ token, headerName }) => next(
      credentialedRequest.clone({ setHeaders: { [headerName]: token } }),
    )),
  );
};
