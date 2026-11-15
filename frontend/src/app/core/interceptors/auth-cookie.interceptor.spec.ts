import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { authCookieInterceptor } from './auth-cookie.interceptor';

describe('authCookieInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authCookieInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('fetches a CSRF token before sending API writes with credentials', () => {
    http.post(`${environment.apiBaseUrl}/auth/login`, {}).subscribe();

    const csrf = httpTesting.expectOne(`${environment.apiBaseUrl}/auth/csrf`);
    expect(csrf.request.method).toBe('GET');
    expect(csrf.request.withCredentials).toBe(true);
    csrf.flush({ token: 'csrf-test-token', headerName: 'X-XSRF-TOKEN' });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-test-token');
    request.flush({});
  });

  it('sends API reads with credentials without requesting a CSRF token', () => {
    http.get(`${environment.apiBaseUrl}/categorias`).subscribe();

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/categorias`);
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    request.flush([]);
  });

  it('does not alter requests outside the configured API', () => {
    http.get('https://example.test/health').subscribe();

    const request = httpTesting.expectOne('https://example.test/health');
    expect(request.request.withCredentials).toBe(false);
    expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    request.flush({});
  });
});
