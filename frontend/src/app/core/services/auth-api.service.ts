import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest } from '../../features/auth/models/login.model';
import { RegistroClienteRequest, RolUsuario, UsuarioSesion } from '../models/usuario.model';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;
  private readonly usuarioState = signal<UsuarioSesion | null>(null);
  private readonly cargadoState = signal(false);
  private cargaSesion: Observable<UsuarioSesion | null> | null = null;
  readonly usuario = this.usuarioState.asReadonly();
  readonly cargado = this.cargadoState.asReadonly();
  readonly autenticado = computed(() => this.usuarioState() !== null);
  readonly rol = computed(() => this.usuarioState()?.rol ?? null);

  cargarSesion(): Observable<UsuarioSesion | null> {
    if (this.cargadoState()) {
      return of(this.usuarioState());
    }
    if (this.cargaSesion) {
      return this.cargaSesion;
    }
    this.cargaSesion = this.http.get<UsuarioSesion>(`${this.baseUrl}/me`).pipe(
      tap((usuario) => this.establecerUsuario(usuario)),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.usuarioState.set(null);
          this.cargadoState.set(true);
          return of(null);
        }
        this.cargaSesion = null;
        return throwError(() => error);
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.cargaSesion;
  }

  iniciarSesion(request: LoginRequest): Observable<UsuarioSesion> {
    return this.http.post<{ usuario: UsuarioSesion }>(`${this.baseUrl}/login`, request).pipe(
      map((response) => response.usuario),
      tap((usuario) => this.establecerUsuario(usuario)),
    );
  }

  registrar(request: RegistroClienteRequest): Observable<UsuarioSesion> {
    return this.http.post<UsuarioSesion>(`${this.baseUrl}/register`, request);
  }

  cerrarSesion(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, null).pipe(
      tap(() => {
        this.usuarioState.set(null);
        this.cargadoState.set(true);
      }),
    );
  }

  tieneRol(roles: RolUsuario[]): boolean {
    const rol = this.usuarioState()?.rol;
    return rol !== undefined && roles.includes(rol);
  }

  private establecerUsuario(usuario: UsuarioSesion): void {
    this.usuarioState.set(usuario);
    this.cargadoState.set(true);
  }
}
