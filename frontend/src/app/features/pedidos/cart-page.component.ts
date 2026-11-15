import { HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MenuApiService } from '../menu/services/menu-api.service';
import { CarritoService } from './services/carrito.service';
import { PedidosApiService } from './services/pedidos-api.service';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, RouterLink],
  template: `
    <header class="mb-4"><p class="eyebrow mb-1">TU SELECCIÓN</p><h1 class="display-6 fw-bold">Carrito</h1></header>
    @if (error) { <div class="alert alert-danger" role="alert">{{ error }}</div> }
    @if (confirmacion) {
      <div class="confirmation-card">
        <span class="success-icon">✓</span><p class="eyebrow mb-1">PEDIDO RECIBIDO</p>
        <h2 class="h3">¡Gracias por tu pedido!</h2>
        <p>Tu número de pedido es <strong>#{{ confirmacion.id }}</strong>. Total confirmado por el sistema:
          <strong>{{ confirmacion.total | currency:'PEN':'symbol':'1.2-2' }}</strong>.</p>
        <a class="btn btn-primary" routerLink="/cliente/pedidos">Ver mis pedidos</a>
      </div>
    } @else if (carrito.estaVacio()) {
      <div class="empty-state text-center"><h2 class="h4">Tu carrito está vacío</h2><p class="text-secondary">Explora el menú y agrega algo rico.</p><a class="btn btn-primary" routerLink="/menu">Ir al menú</a></div>
    } @else {
      <div class="row g-4 align-items-start">
        <div class="col-lg-8">
          @for (item of carrito.resumen(); track item.productoId) {
            <article class="cart-row d-flex flex-wrap align-items-center gap-3 mb-3">
              <div class="cart-mark" aria-hidden="true">LR</div>
              <div class="flex-grow-1"><h2 class="h6 fw-bold mb-1">{{ item.nombre }}</h2><span class="small text-secondary">{{ item.precioUnitario | currency:'PEN':'symbol':'1.2-2' }} c/u</span></div>
              <div class="quantity-control">
                <button class="btn btn-sm btn-outline-secondary" type="button" [attr.aria-label]="'Disminuir ' + item.nombre" (click)="carrito.disminuir(item.productoId)">−</button>
                <span>{{ item.cantidad }}</span>
                <button class="btn btn-sm btn-outline-secondary" type="button" [attr.aria-label]="'Aumentar ' + item.nombre" (click)="carrito.incrementar(item.productoId)">+</button>
              </div>
              <strong class="item-subtotal">{{ item.subtotal | currency:'PEN':'symbol':'1.2-2' }}</strong>
              <button class="btn btn-sm btn-link text-danger" type="button" (click)="carrito.eliminar(item.productoId)">Quitar</button>
            </article>
          }
        </div>
        <div class="col-lg-4">
          <section class="summary-card">
            <h2 class="h5 fw-bold">Resumen</h2>
            <div class="d-flex justify-content-between my-3"><span>Subtotal estimado</span><strong>{{ carrito.total() | currency:'PEN':'symbol':'1.2-2' }}</strong></div>
            <p class="small text-secondary">El importe final se calcula con los precios vigentes en el servidor.</p>
            <label for="observaciones" class="form-label">Observaciones <span class="text-secondary">(opcional)</span></label>
            <textarea id="observaciones" class="form-control mb-3" rows="3" maxlength="1000" [(ngModel)]="observaciones"></textarea>
            <button class="btn btn-primary w-100" type="button" [disabled]="cargando" (click)="confirmar()">
              {{ cargando ? 'Verificando productos…' : 'Confirmar pedido' }}
            </button>
            <a class="btn btn-link w-100 mt-2" routerLink="/menu">Seguir comprando</a>
          </section>
        </div>
      </div>
    }
  `,
  styles: [`
    .eyebrow{color:var(--color-primary);font-size:.75rem;font-weight:800;letter-spacing:.14em}.btn-primary{--bs-btn-bg:#754024;--bs-btn-border-color:#754024;--bs-btn-hover-bg:#60331c;--bs-btn-hover-border-color:#60331c}
    .cart-row,.summary-card,.empty-state,.confirmation-card{padding:1.25rem;background:#fff;border:1px solid var(--color-border);border-radius:1rem}
    .cart-mark{width:3.3rem;height:3.3rem;display:grid;place-items:center;border-radius:50%;background:#f4dfca;color:#754024;font-family:Georgia,serif}
    .quantity-control{display:flex;align-items:center;gap:.8rem}.quantity-control span{min-width:1.3rem;text-align:center}.item-subtotal{min-width:5rem;text-align:right}.summary-card{position:sticky;top:1rem}
    .empty-state,.confirmation-card{text-align:center;padding:3rem 1.5rem}.confirmation-card{max-width:42rem;margin:auto}.success-icon{display:inline-grid;place-items:center;width:3.5rem;height:3.5rem;border-radius:50%;background:#e5f4e8;color:#236738;font-size:1.7rem;margin-bottom:1rem}
    @media(max-width:575px){.cart-row{gap:.65rem}.item-subtotal{margin-left:auto}.summary-card{position:static}}
  `],
})
export class CartPageComponent {
  private readonly menuApi = inject(MenuApiService);
  private readonly pedidosApi = inject(PedidosApiService);
  readonly carrito = inject(CarritoService);
  observaciones = '';
  error = '';
  cargando = false;
  confirmacion: { id: number; total: number } | null = null;

  confirmar(): void {
    if (this.carrito.estaVacio() || this.cargando) return;
    this.error = '';
    this.cargando = true;
    this.menuApi.listarDisponibles().subscribe({
      next: (productos) => {
        const disponibles = new Set(productos.filter((producto) => producto.disponible).map((producto) => producto.id));
        const noDisponibles = this.carrito.items().filter((item) => !disponibles.has(item.productoId));
        if (noDisponibles.length) {
          this.error = `Ya no están disponibles: ${noDisponibles.map((item) => item.nombre).join(', ')}. Actualiza tu carrito.`;
          this.cargando = false;
          return;
        }
        this.pedidosApi.confirmarCarrito(this.carrito, this.observaciones.trim() || null).subscribe({
          next: (pedido) => {
            this.confirmacion = { id: pedido.id, total: pedido.total };
            this.cargando = false;
          },
          error: (error: unknown) => {
            this.error = this.mensajeError(error, 'No se pudo registrar el pedido.');
            this.cargando = false;
          },
        });
      },
      error: (error: unknown) => {
        this.error = this.mensajeError(error, 'No se pudo comprobar la disponibilidad de los productos.');
        this.cargando = false;
      },
    });
  }

  private mensajeError(error: unknown, fallback: string): string {
    return error instanceof HttpErrorResponse ? error.error?.message ?? fallback : fallback;
  }
}
