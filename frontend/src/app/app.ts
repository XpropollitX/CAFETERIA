import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthApiService } from './core/services/auth-api.service';
import { RolUsuario } from './core/models/usuario.model';
import { CarritoService } from './features/pedidos/services/carrito.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  readonly auth = inject(AuthApiService);
  private readonly carrito = inject(CarritoService);
  private readonly router = inject(Router);
  readonly errorSesion = signal('');

  cartCount(): number {
    return this.carrito.items().reduce((count, item) => count + item.cantidad, 0);
  }

  ngOnInit(): void {
    this.auth.cargarSesion().subscribe({
      error: () => this.errorSesion.set('No se pudo validar la sesión. Revisa la conexión con el servidor y recarga.'),
    });
  }

  cerrarSesion(): void {
    this.errorSesion.set('');
    this.auth.cerrarSesion().subscribe({
      next: () => void this.router.navigate(['/']),
      error: () => this.errorSesion.set('No se pudo cerrar la sesión. Inténtalo nuevamente.'),
    });
  }

  esCliente(rol: RolUsuario | null): boolean {
    return rol === 'CLIENTE';
  }

  esPersonal(rol: RolUsuario | null): boolean {
    return rol === 'PERSONAL' || rol === 'ADMIN';
  }
}
