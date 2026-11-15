import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthApiService } from '../../core/services/auth-api.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-card mx-auto">
      <p class="eyebrow">BIENVENIDO DE VUELTA</p>
      <h1 class="h2 fw-bold">Iniciar sesión</h1>
      <p class="text-secondary mb-4">Ingresa para hacer pedidos y consultar su seguimiento.</p>
      @if (exito) { <div class="alert alert-success" role="status">{{ exito }}</div> }
      @if (error) { <div class="alert alert-danger" role="alert">{{ error }}</div> }
      <form #form="ngForm" (ngSubmit)="entrar()" novalidate>
        <label class="form-label" for="correo">Correo electrónico</label>
        <input id="correo" class="form-control form-control-lg mb-3" type="email" name="correo"
          [(ngModel)]="correo" required email autocomplete="username">
        <label class="form-label" for="password">Contraseña</label>
        <input id="password" class="form-control form-control-lg mb-4" type="password" name="password"
          [(ngModel)]="password" required autocomplete="current-password">
        <button class="btn btn-primary w-100 btn-lg" type="submit" [disabled]="form.invalid || cargando">
          {{ cargando ? 'Ingresando…' : 'Ingresar' }}
        </button>
      </form>
      <p class="mt-4 mb-0 text-center">¿No tienes cuenta? <a routerLink="/registro">Regístrate</a></p>
    </div>
  `,
  styles: [`.auth-card{max-width:30rem;padding:2rem;background:#fff;border:1px solid var(--color-border);border-radius:1.25rem;box-shadow:0 1rem 3rem #512f1710}.eyebrow{color:var(--color-primary);font-size:.75rem;font-weight:800;letter-spacing:.14em}.btn-primary{--bs-btn-bg:#754024;--bs-btn-border-color:#754024;--bs-btn-hover-bg:#60331c;--bs-btn-hover-border-color:#60331c}`],
})
export class LoginPageComponent {
  private readonly auth = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  correo = '';
  password = '';
  error = '';
  exito = '';
  cargando = false;

  constructor() {
    if (this.route.snapshot.queryParamMap.get('registrado') === 'true') {
      this.exito = 'Tu cuenta se creó correctamente. Inicia sesión para continuar.';
    }
  }

  entrar(): void {
    this.error = '';
    this.cargando = true;
    this.auth.iniciarSesion({ correo: this.correo.trim(), password: this.password }).subscribe({
      next: (usuario) => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        const destino = returnUrl?.startsWith('/') ? returnUrl : this.destinoPorRol(usuario.rol);
        void this.router.navigateByUrl(destino);
      },
      error: (error: unknown) => {
        this.error = error instanceof HttpErrorResponse
          ? error.error?.message ?? 'No se pudo iniciar sesión. Revisa tus credenciales y la conexión.'
          : 'No se pudo iniciar sesión.';
        this.cargando = false;
      },
      complete: () => { this.cargando = false; },
    });
  }

  private destinoPorRol(rol: string): string {
    if (rol === 'PERSONAL') return '/personal/pedidos';
    if (rol === 'ADMIN') return '/admin/catalogo';
    return '/menu';
  }
}
