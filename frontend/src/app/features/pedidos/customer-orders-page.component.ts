import { HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MetodoPago, PagoResponse } from '../pagos/models/pago.model';
import { PagosApiService } from '../pagos/services/pagos-api.service';
import { EstadoPedido, HistorialEstadoResponse, PedidoResponse } from './models/pedido.model';
import { PedidosApiService } from './services/pedidos-api.service';

@Component({
  selector: 'app-customer-orders-page',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule],
  template: `
    <header class="mb-4"><p class="eyebrow mb-1">TU ACTIVIDAD</p><h1 class="display-6 fw-bold">Mis pedidos</h1><p class="text-secondary">Consulta el avance y registra tus pagos.</p></header>
    @if (error()) { <div class="alert alert-danger" role="alert">{{ error() }}</div> }
    @if (cargando()) { <div class="text-center py-5"><div class="spinner-border" role="status"></div><p class="mt-2">Cargando pedidos…</p></div> }
    @if (!cargando() && !pedidos().length) { <div class="empty-state text-center"><h2 class="h4">Aún no tienes pedidos</h2><p class="text-secondary">Cuando confirmes un pedido, aparecerá aquí.</p></div> }
    @for (pedido of pedidos(); track pedido.id) {
      <article class="order-card mb-4">
        <div class="d-flex flex-wrap justify-content-between gap-3 align-items-start">
          <div><p class="small text-secondary mb-1">PEDIDO #{{ pedido.id }} · {{ pedido.fechaPedido | date:'medium' }}</p>
            <h2 class="h5 fw-bold mb-2">Total {{ pedido.total | currency:'PEN':'symbol':'1.2-2' }}</h2>
            <span class="status-badge" [class]="claseEstado(pedido.estado)">{{ etiquetaEstado(pedido.estado) }}</span>
          </div>
          <button class="btn btn-sm btn-outline-secondary" type="button" (click)="alternarHistorial(pedido.id)">
            {{ pedidoSeleccionado === pedido.id ? 'Ocultar historial' : 'Ver historial' }}
          </button>
        </div>
        <ul class="items-list mt-3 mb-3">
          @for (detalle of pedido.detalles; track detalle.id) {
            <li><span>{{ detalle.cantidad }} × {{ detalle.producto }}</span><strong>{{ detalle.subtotal | currency:'PEN':'symbol':'1.2-2' }}</strong></li>
          }
        </ul>
        @if (pedidoSeleccionado === pedido.id) {
          <section class="history-panel mb-3"><h3 class="h6 fw-bold">Historial de estados</h3>
            @if (historiales()[pedido.id]?.length) {
              @for (movimiento of historiales()[pedido.id]; track movimiento.id) {
                <div class="history-row"><span class="history-dot"></span><div><strong>{{ etiquetaEstado(movimiento.estado) }}</strong><p class="small text-secondary mb-0">{{ movimiento.fechaCambio | date:'medium' }} · usuario #{{ movimiento.usuarioId }}{{ movimiento.observacion ? ' · ' + movimiento.observacion : '' }}</p></div></div>
              }
            } @else { <p class="small text-secondary mb-0">{{ cargandoHistorial() ? 'Cargando historial…' : 'No se pudo cargar el historial.' }}</p> }
          </section>
        }
        <section class="payment-panel">
          <div class="d-flex flex-wrap justify-content-between gap-2"><h3 class="h6 fw-bold mb-0">Pagos</h3>
            <span class="small">Confirmado: <strong>{{ totalPagado(pedido.id) | currency:'PEN':'symbol':'1.2-2' }}</strong>
              · Por verificar: <strong>{{ totalPorVerificar(pedido.id) | currency:'PEN':'symbol':'1.2-2' }}</strong>
              · Saldo por registrar: <strong>{{ saldoDisponible(pedido) | currency:'PEN':'symbol':'1.2-2' }}</strong></span>
          </div>
          @if (pagos()[pedido.id]?.length) {
            <ul class="payment-list">
              @for (pago of pagos()[pedido.id]; track pago.id) {
                <li><span>{{ etiquetaMetodo(pago.metodoPago) }} · {{ pago.estadoPago }}</span><strong>{{ pago.monto | currency:'PEN':'symbol':'1.2-2' }}</strong></li>
              }
            </ul>
          } @else if (cargandoPagos()[pedido.id]) { <p class="small text-secondary mt-2 mb-0">Cargando pagos…</p> }
          @if (pedido.estado !== 'CANCELADO' && pedido.estado !== 'ENTREGADO' && saldoDisponible(pedido) > 0) {
            <form class="row g-2 align-items-end mt-2" (ngSubmit)="registrarPago(pedido)" #pagoForm="ngForm">
              <div class="col-sm-5"><label class="form-label small" [for]="'metodo-' + pedido.id">Método</label>
                <select class="form-select form-select-sm" [id]="'metodo-' + pedido.id" name="metodo" [(ngModel)]="metodos[pedido.id]">
                  <option value="EFECTIVO">Efectivo</option><option value="YAPE">Yape</option><option value="TRANSFERENCIA">Transferencia bancaria</option>
                </select>
              </div>
              <div class="col-sm-4"><label class="form-label small" [for]="'monto-' + pedido.id">Monto (S/)</label>
                <input class="form-control form-control-sm" type="number" min="0.01" step="0.01" [max]="saldoDisponible(pedido)" [id]="'monto-' + pedido.id" name="monto" [(ngModel)]="montos[pedido.id]" required>
              </div>
              <div class="col-sm-3"><button class="btn btn-sm btn-primary w-100" [disabled]="pagoForm.invalid || guardandoPago()[pedido.id]">{{ guardandoPago()[pedido.id] ? 'Enviando…' : 'Registrar' }}</button></div>
            </form>
            <p class="small text-secondary mt-2 mb-0">Los pagos pendientes reservan saldo; espera la verificación del personal antes de registrar otro.</p>
          }
        </section>
      </article>
    }
  `,
  styles: [`
    .eyebrow{color:var(--color-primary);font-size:.75rem;font-weight:800;letter-spacing:.14em}.order-card,.empty-state{background:#fff;border:1px solid var(--color-border);border-radius:1rem;padding:1.25rem}
    .status-badge{display:inline-flex;border-radius:999px;padding:.35rem .7rem;font-size:.8rem;font-weight:700;background:#f2e6d9;color:#754024}.status-badge.progress{background:#e6effc;color:#244e83}.status-badge.ready{background:#e8f4e8;color:#27633a}.status-badge.final{background:#ece9e6;color:#51453e}.status-badge.cancel{background:#fae5e2;color:#8e3024}
    .items-list,.payment-list{padding:0;list-style:none}.items-list li,.payment-list li{display:flex;justify-content:space-between;gap:1rem;padding:.45rem 0;border-bottom:1px solid #f0ebe6}
    .history-panel,.payment-panel{padding:1rem;background:#faf7f3;border-radius:.75rem}.history-row{display:flex;gap:.7rem;padding:.55rem 0}.history-dot{width:.65rem;height:.65rem;border-radius:50%;background:#9b633e;margin-top:.3rem;flex:none}.payment-list{margin:.5rem 0}
    .btn-primary{--bs-btn-bg:#754024;--bs-btn-border-color:#754024;--bs-btn-hover-bg:#60331c;--bs-btn-hover-border-color:#60331c}
  `],
})
export class CustomerOrdersPageComponent implements OnInit {
  private readonly pedidosApi = inject(PedidosApiService);
  private readonly pagosApi = inject(PagosApiService);
  readonly pedidos = signal<PedidoResponse[]>([]);
  readonly pagos = signal<Record<number, PagoResponse[]>>({});
  readonly historiales = signal<Record<number, HistorialEstadoResponse[]>>({});
  metodos: Record<number, MetodoPago> = {};
  montos: Record<number, number | null> = {};
  readonly cargandoPagos = signal<Record<number, boolean>>({});
  readonly guardandoPago = signal<Record<number, boolean>>({});
  readonly cargandoHistorial = signal(false);
  pedidoSeleccionado: number | null = null;
  readonly cargando = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    this.pedidosApi.listarMisPedidos().subscribe({
      next: (pedidos) => {
        this.pedidos.set(pedidos);
        this.cargando.set(false);
        pedidos.forEach((pedido) => {
          this.metodos[pedido.id] = 'EFECTIVO';
          this.montos[pedido.id] = null;
          this.cargarPagos(pedido.id);
        });
      },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'No se pudieron cargar tus pedidos.'));
        this.cargando.set(false);
      },
    });
  }

  alternarHistorial(pedidoId: number): void {
    if (this.pedidoSeleccionado === pedidoId) {
      this.pedidoSeleccionado = null;
      return;
    }
    this.pedidoSeleccionado = pedidoId;
    this.cargandoHistorial.set(true);
    this.pedidosApi.listarHistorial(pedidoId).subscribe({
      next: (historial) => {
        this.historiales.update((items) => ({ ...items, [pedidoId]: historial }));
        this.cargandoHistorial.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'No se pudo cargar el historial.'));
        this.cargandoHistorial.set(false);
      },
    });
  }

  registrarPago(pedido: PedidoResponse): void {
    const monto = Number(this.montos[pedido.id]);
    if (!Number.isFinite(monto) || monto <= 0) return;
    this.guardandoPago.update((guardando) => ({ ...guardando, [pedido.id]: true }));
    this.error.set('');
    this.pagosApi.registrar(pedido.id, {
      metodoPago: this.metodos[pedido.id] ?? 'EFECTIVO',
      monto,
    }).subscribe({
      next: () => {
        this.cargarPagos(pedido.id);
        this.montos[pedido.id] = null;
        this.guardandoPago.update((guardando) => ({ ...guardando, [pedido.id]: false }));
      },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'No se pudo registrar el pago.'));
        this.guardandoPago.update((guardando) => ({ ...guardando, [pedido.id]: false }));
      },
    });
  }

  totalPagado(pedidoId: number): number {
    return (this.pagos()[pedidoId] ?? [])
      .filter((pago) => pago.estadoPago === 'PAGADO')
      .reduce((total, pago) => total + pago.monto, 0);
  }

  totalPorVerificar(pedidoId: number): number {
    return (this.pagos()[pedidoId] ?? [])
      .filter((pago) => pago.estadoPago === 'PENDIENTE')
      .reduce((total, pago) => total + pago.monto, 0);
  }

  saldoDisponible(pedido: PedidoResponse): number {
    return Math.max(0, pedido.total - this.totalPagado(pedido.id) - this.totalPorVerificar(pedido.id));
  }

  etiquetaEstado(estado: EstadoPedido): string {
    return ({
      RECIBIDO: 'Recibido',
      EN_PREPARACION: 'En preparación',
      LISTO: 'Listo',
      ENTREGADO: 'Entregado',
      CANCELADO: 'Cancelado',
    })[estado];
  }

  claseEstado(estado: EstadoPedido): string {
    if (estado === 'EN_PREPARACION') return 'status-badge progress';
    if (estado === 'LISTO') return 'status-badge ready';
    if (estado === 'CANCELADO') return 'status-badge cancel';
    if (estado === 'ENTREGADO') return 'status-badge final';
    return 'status-badge';
  }

  etiquetaMetodo(metodo: MetodoPago): string {
    return metodo === 'TRANSFERENCIA' ? 'Transferencia bancaria' : metodo === 'YAPE' ? 'Yape' : 'Efectivo';
  }

  private cargarPagos(pedidoId: number): void {
    this.cargandoPagos.update((cargas) => ({ ...cargas, [pedidoId]: true }));
    this.pagosApi.listarPorPedido(pedidoId).subscribe({
      next: (pagos) => {
        this.pagos.update((items) => ({ ...items, [pedidoId]: pagos }));
        this.cargandoPagos.update((cargas) => ({ ...cargas, [pedidoId]: false }));
      },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'No se pudieron consultar los pagos.'));
        this.cargandoPagos.update((cargas) => ({ ...cargas, [pedidoId]: false }));
      },
    });
  }

  private mensajeError(error: unknown, fallback: string): string {
    return error instanceof HttpErrorResponse ? error.error?.message ?? fallback : fallback;
  }
}
