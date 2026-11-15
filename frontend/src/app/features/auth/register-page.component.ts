import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from '../../core/services/auth-api.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="auth-card mx-auto">
      <p class="eyebrow">ÚNETE A LAS ROSAS</p>
      <h1 class="h2 fw-bold">Crear cuenta</h1>
      <p class="text-secondary mb-4">El registro crea una cuenta de cliente.</p>
      @if (error) { <div class="alert alert-danger" role="alert">{{ error }}</div> }
      @if (exito) { <div class="alert alert-success" role="status">{{ exito }}</div> }
      <form #form="ngForm" (ngSubmit)="registrar()" novalidate>
        <div class="row">
          <div class="col-sm-6"><label class="form-label" for="nombres">Nombres</label>
            <input id="nombres" class="form-control mb-3" name="nombres" [(ngModel)]="nombres" required maxlength="100"></div>
          <div class="col-sm-6"><label class="form-label" for="apellidos">Apellidos</label>
            <input id="apellidos" class="form-control mb-3" name="apellidos" [(ngModel)]="apellidos" required maxlength="100"></div>
        </div>
        <label class="form-label" for="correo">Correo electrónico</label>
        <input id="correo" class="form-control mb-3" type="email" name="correo" [(ngModel)]="correo" required email maxlength="254">
        <label class="form-label" for="telefono">Teléfono <span class="text-secondary">(opcional)</span></label>
        <input id="telefono" class="form-control mb-3" name="telefono" [(ngModel)]="telefono" maxlength="20">
        <label class="form-label" for="password">Contraseña</label>
        <input id="password" class="form-control mb-4" type="password" name="password" [(ngModel)]="password" required minlength="8" maxlength="72" autocomplete="new-password">
        <button class="btn btn-primary w-100 btn-lg" type="submit" [disabled]="form.invalid || cargando">
          {{ cargando ? 'Creando cuenta…' : 'Crear cuenta' }}
        </button>
      </form>
      <p class="mt-4 mb-0 text-center">¿Ya tienes cuenta? <a routerLink="/login">Inicia sesión</a></p>
    </div>
  `,
  styles: [`.auth-card{max-width:34rem;padding:2rem;background:#fff;border:1px solid var(--color-border);border-radius:1.25rem;box-shadow:0 1rem 3rem #512f1710}.eyebrow{color:var(--color-primary);font-size:.75rem;font-weight:800;letter-spacing:.14em}.btn-primary{--bs-btn-bg:#754024;--bs-btn-border-color:#754024;--bs-btn-hover-bg:#60331c;--bs-btn-hover-border-color:#60331c}`],
})
export class RegisterPageComponent {
  private readonly auth = inject(AuthApiService);
  private readonly router = inject(Router);
  nombres = '';
  apellidos = '';
  correo = '';
  telefono = '';
  password = '';
  error = '';
  exito = '';
  cargando = false;

  registrar(): void {
    this.error = '';
    this.exito = '';
    this.cargando = true;
    this.auth.registrar({
      nombres: this.nombres.trim(),
      apellidos: this.apellidos.trim(),
      correo: this.correo.trim(),
      telefono: this.telefono.trim() || null,
      password: this.password,
    }).subscribe({
      next: () => {
        this.exito = 'Cuenta creada. Ya puedes iniciar sesión.';
        this.cargando = false;
        void this.router.navigate(['/login'], { queryParams: { registrado: 'true' } });
      },
      error: (error: unknown) => {
        this.error = error instanceof HttpErrorResponse
          ? error.error?.message ?? 'No se pudo completar el registro. Revisa los datos e inténtalo otra vez.'
          : 'No se pudo completar el registro.';
        this.cargando = false;
      },
    });
  }
}
