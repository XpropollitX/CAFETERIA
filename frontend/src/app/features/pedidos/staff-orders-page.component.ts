import { HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EstadoPago, PagoResponse } from '../pagos/models/pago.model';
import { PagosApiService } from '../pagos/services/pagos-api.service';
import { EstadoPedido, HistorialEstadoResponse, PedidoResponse } from './models/pedido.model';
import { PedidosApiService } from './services/pedidos-api.service';

@Component({
  selector: 'app-staff-orders-page',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule],
  template: `
    <header class="mb-4"><p class="eyebrow mb-1">OPERACIONES</p><h1 class="display-6 fw-bold">Pedidos por gestionar</h1><p class="text-secondary">Los pagos pendientes no habilitan la preparación.</p></header>
    @if (error()) { <div class="alert alert-danger" role="alert">{{ error() }}</div> }
    @if (mensaje()) { <div class="alert alert-success" role="status">{{ mensaje() }}</div> }
    @if (cargando()) { <div class="text-center py-5"><div class="spinner-border" role="status"></div><p class="mt-2">Cargando pedidos operativos…</p></div> }
    @if (!cargando() && !pedidos().length) { <div class="empty-state text-center"><h2 class="h4">No hay pedidos pendientes de gestión</h2><p class="text-secondary mb-0">Los pedidos nuevos aparecerán aquí.</p></div> }
    <div class="row g-3">
      @for (pedido of pedidos(); track pedido.id) {
        <div class="col-xl-6">
          <article class="order-card h-100">
            <header class="d-flex justify-content-between gap-3">
              <div><p class="small text-secondary mb-1">PEDIDO #{{ pedido.id }} · {{ pedido.fechaPedido | date:'short' }}</p>
                <h2 class="h5 fw-bold">Total {{ pedido.total | currency:'PEN':'symbol':'1.2-2' }}</h2></div>
              <span class="badge rounded-pill text-bg-light border h-100">{{ etiquetaEstado(pedido.estado) }}</span>
            </header>
            <ul class="items-list">
              @for (detalle of pedido.detalles; track detalle.id) {
                <li><span>{{ detalle.cantidad }} × {{ detalle.producto }}</span><strong>{{ detalle.subtotal | currency:'PEN':'symbol':'1.2-2' }}</strong></li>
              }
            </ul>
            <button class="btn btn-sm btn-link px-0 mb-2" type="button" (click)="alternarHistorial(pedido.id)">
              {{ historialAbierto === pedido.id ? 'Ocultar historial' : 'Consultar historial' }}
            </button>
            @if (historialAbierto === pedido.id) {
              <section class="history-panel mb-3" aria-label="Historial del pedido">
                @for (movimiento of historiales()[pedido.id] ?? []; track movimiento.id) {
                  <p class="small mb-1"><strong>{{ etiquetaEstado(movimiento.estado) }}</strong> · {{ movimiento.fechaCambio | date:'short' }} · usuario #{{ movimiento.usuarioId }}{{ movimiento.observacion ? ' · ' + movimiento.observacion : '' }}</p>
                }
                @if (cargandoHistorial()[pedido.id]) { <span class="small text-secondary">Cargando historial…</span> }
              </section>
            }
            <section class="payment-summary">
              <div class="d-flex flex-wrap justify-content-between gap-2"><strong>Pagado: {{ totalPagado(pedido.id) | currency:'PEN':'symbol':'1.2-2' }}</strong>
                <span [class.text-success]="completamentePagado(pedido)" [class.text-danger]="!completamentePagado(pedido)">
                  {{ completamentePagado(pedido) ? 'Pago completo' : 'Pendiente de pago' }}
                </span>
              </div>
              @if (pagos()[pedido.id]?.length) {
                <ul class="payment-list mb-0 mt-2">
                  @for (pago of pagos()[pedido.id]; track pago.id) {
                    <li><span>{{ etiquetaMetodo(pago) }} · {{ pago.estadoPago }}</span>
                      <strong>{{ pago.monto | currency:'PEN':'symbol':'1.2-2' }}</strong>
                      @if (pago.estadoPago === 'PENDIENTE') {
                        <span class="actions">
                          <button class="btn btn-sm btn-success" [disabled]="accionando()[pedido.id]" (click)="resolverPago(pedido, pago, true)">Verificar</button>
                          <button class="btn btn-sm btn-outline-danger" [disabled]="accionando()[pedido.id]" (click)="resolverPago(pedido, pago, false)">Anular</button>
                        </span>
                      }
                    </li>
                  }
                </ul>
              } @else if (cargandoPagos()[pedido.id]) { <p class="small text-secondary mt-2 mb-0">Consultando pagos…</p> }
              @else { <p class="small text-secondary mt-2 mb-0">Aún no hay pagos registrados.</p> }
            </section>
            <footer class="mt-3">
              <div class="d-flex flex-wrap gap-2">
                @if (siguienteEstado(pedido.estado); as siguiente) {
                  <button class="btn btn-primary" [disabled]="accionando()[pedido.id] || (siguiente === 'EN_PREPARACION' && !completamentePagado(pedido))" (click)="cambiarEstado(pedido, siguiente)">
                    {{ accionando()[pedido.id] ? 'Guardando…' : 'Marcar ' + etiquetaEstado(siguiente) }}
                  </button>
                }
                <button class="btn btn-outline-danger" type="button" (click)="alternarCancelacion(pedido.id)">Cancelar pedido</button>
              </div>
              @if (pedidoCancelacion === pedido.id) {
                <form class="cancel-form mt-3" (ngSubmit)="cancelar(pedido)" #cancelForm="ngForm">
                  <label class="form-label" [for]="'motivo-' + pedido.id">Motivo de cancelación</label>
                  <div class="d-flex gap-2"><input class="form-control" [id]="'motivo-' + pedido.id" [name]="'motivo-' + pedido.id" [(ngModel)]="motivos[pedido.id]" required maxlength="1000">
                    <button class="btn btn-danger text-nowrap" [disabled]="cancelForm.invalid || accionando()[pedido.id]">Confirmar cancelación</button></div>
                </form>
              }
            </footer>
          </article>
        </div>
      }
    </div>
  `,
  styles: [`
    .eyebrow{color:var(--color-primary);font-size:.75rem;font-weight:800;letter-spacing:.14em}.order-card,.empty-state{background:#fff;border:1px solid var(--color-border);border-radius:1rem;padding:1.25rem}
    .items-list,.payment-list{padding:0;list-style:none}.items-list{margin:1rem 0}.items-list li,.payment-list li{display:flex;align-items:center;justify-content:space-between;gap:.7rem;padding:.45rem 0;border-bottom:1px solid #f0ebe6}
    .payment-summary,.cancel-form,.history-panel{background:#faf7f3;border-radius:.75rem;padding:1rem}.actions{display:flex;gap:.4rem}.btn-primary{--bs-btn-bg:#754024;--bs-btn-border-color:#754024;--bs-btn-hover-bg:#60331c;--bs-btn-hover-border-color:#60331c}
    @media(max-width:575px){.payment-list li{flex-wrap:wrap}.actions{width:100%}.actions button{flex:1}}
  `],
})
export class StaffOrdersPageComponent implements OnInit {
  private readonly pedidosApi = inject(PedidosApiService);
  private readonly pagosApi = inject(PagosApiService);
  readonly pedidos = signal<PedidoResponse[]>([]);
  readonly pagos = signal<Record<number, PagoResponse[]>>({});
  readonly historiales = signal<Record<number, HistorialEstadoResponse[]>>({});
  readonly cargandoPagos = signal<Record<number, boolean>>({});
  readonly cargandoHistorial = signal<Record<number, boolean>>({});
  readonly accionando = signal<Record<number, boolean>>({});
  motivos: Record<number, string> = {};
  pedidoCancelacion: number | null = null;
  historialAbierto: number | null = null;
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly mensaje = signal('');

  ngOnInit(): void {
    this.recargar();
  }

  totalPagado(pedidoId: number): number {
    return (this.pagos()[pedidoId] ?? [])
      .filter((pago) => pago.estadoPago === 'PAGADO')
      .reduce((total, pago) => total + pago.monto, 0);
  }

  completamentePagado(pedido: PedidoResponse): boolean {
    return this.totalPagado(pedido.id) >= pedido.total;
  }

  siguienteEstado(actual: EstadoPedido): EstadoPedido | null {
    if (actual === 'RECIBIDO') return 'EN_PREPARACION';
    if (actual === 'EN_PREPARACION') return 'LISTO';
    if (actual === 'LISTO') return 'ENTREGADO';
    return null;
  }

  etiquetaEstado(estado: EstadoPedido): string {
    return ({
      RECIBIDO: 'recibido',
      EN_PREPARACION: 'en preparación',
      LISTO: 'listo',
      ENTREGADO: 'entregado',
      CANCELADO: 'cancelado',
    })[estado];
  }

  etiquetaMetodo(pago: PagoResponse): string {
    return pago.metodoPago === 'TRANSFERENCIA'
      ? 'Transferencia bancaria'
      : pago.metodoPago === 'YAPE' ? 'Yape' : 'Efectivo';
  }

  cambiarEstado(pedido: PedidoResponse, estado: EstadoPedido): void {
    this.accionando.update((acciones) => ({ ...acciones, [pedido.id]: true }));
    this.error.set('');
    this.pedidosApi.cambiarEstado(pedido.id, { estado }).subscribe({
      next: () => {
        this.mensaje.set(`Pedido #${pedido.id} actualizado.`);
        this.accionando.update((acciones) => ({ ...acciones, [pedido.id]: false }));
        this.recargar();
      },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'No se pudo actualizar el estado.'));
        this.accionando.update((acciones) => ({ ...acciones, [pedido.id]: false }));
      },
    });
  }

  alternarHistorial(pedidoId: number): void {
    if (this.historialAbierto === pedidoId) {
      this.historialAbierto = null;
      return;
    }
    this.historialAbierto = pedidoId;
    this.cargandoHistorial.update((cargas) => ({ ...cargas, [pedidoId]: true }));
    this.pedidosApi.listarHistorial(pedidoId).subscribe({
      next: (historial) => {
        this.historiales.update((items) => ({ ...items, [pedidoId]: historial }));
        this.cargandoHistorial.update((cargas) => ({ ...cargas, [pedidoId]: false }));
      },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'No se pudo consultar el historial.'));
        this.cargandoHistorial.update((cargas) => ({ ...cargas, [pedidoId]: false }));
      },
    });
  }

  alternarCancelacion(pedidoId: number): void {
    this.pedidoCancelacion = this.pedidoCancelacion === pedidoId ? null : pedidoId;
  }

  cancelar(pedido: PedidoResponse): void {
    const observacion = this.motivos[pedido.id]?.trim();
    if (!observacion) return;
    this.accionando.update((acciones) => ({ ...acciones, [pedido.id]: true }));
    this.error.set('');
    this.pedidosApi.cancelar(pedido.id, observacion).subscribe({
      next: () => {
        this.mensaje.set(`Pedido #${pedido.id} cancelado.`);
        this.pedidoCancelacion = null;
        this.accionando.update((acciones) => ({ ...acciones, [pedido.id]: false }));
        this.recargar();
      },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'No se pudo cancelar el pedido.'));
        this.accionando.update((acciones) => ({ ...acciones, [pedido.id]: false }));
      },
    });
  }

  resolverPago(pedido: PedidoResponse, pago: PagoResponse, verificar: boolean): void {
    this.accionando.update((acciones) => ({ ...acciones, [pedido.id]: true }));
    this.error.set('');
    const request = verificar ? this.pagosApi.verificar(pago.id) : this.pagosApi.anular(pago.id);
    request.subscribe({
      next: () => {
        this.mensaje.set(verificar ? 'Pago verificado.' : 'Pago anulado.');
        this.cargarPagos(pedido.id);
        this.accionando.update((acciones) => ({ ...acciones, [pedido.id]: false }));
      },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'No se pudo actualizar el pago.'));
        this.accionando.update((acciones) => ({ ...acciones, [pedido.id]: false }));
      },
    });
  }

  private recargar(): void {
    this.pedidosApi.listarOperativos().subscribe({
      next: (pedidos) => {
        this.pedidos.set(pedidos);
        this.cargando.set(false);
        pedidos.forEach((pedido) => this.cargarPagos(pedido.id));
      },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'No se pudieron cargar los pedidos.'));
        this.cargando.set(false);
      },
    });
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
