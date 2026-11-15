import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { authCookieInterceptor } from '../interceptors/auth-cookie.interceptor';
import { AuthApiService } from './auth-api.service';

describe('AuthApiService', () => {
  let service: AuthApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authCookieInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores the authenticated user after login without reading a JWT', () => {
    let receivedRole: string | undefined;
    service.iniciarSesion({ correo: 'ana@example.test', password: 'valid-password' })
      .subscribe((user) => { receivedRole = user.rol; });

    http.expectOne(`${environment.apiBaseUrl}/auth/csrf`).flush({
      token: 'csrf-token',
      headerName: 'X-XSRF-TOKEN',
    });
    const login = http.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toEqual({ correo: 'ana@example.test', password: 'valid-password' });
    expect(login.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    login.flush({
      usuario: {
        id: 8,
        nombres: 'Ana',
        apellidos: 'Prueba',
        correo: 'ana@example.test',
        telefono: null,
        rol: 'CLIENTE',
        activo: true,
        fechaCreacion: '2026-01-01T00:00:00Z',
      },
    });

    expect(receivedRole).toBe('CLIENTE');
    expect(service.usuario()?.id).toBe(8);
    expect(service.autenticado()).toBe(true);
  });

  it('treats an expired or absent session as signed out', () => {
    let user: unknown = 'not-set';
    service.cargarSesion().subscribe((session) => { user = session; });
    http.expectOne(`${environment.apiBaseUrl}/auth/me`).flush(
      { message: 'No autenticado.' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(user).toBeNull();
    expect(service.cargado()).toBe(true);
    expect(service.autenticado()).toBe(false);
  });

});
