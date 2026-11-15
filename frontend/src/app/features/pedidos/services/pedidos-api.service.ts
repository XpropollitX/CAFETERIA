import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  CambiarEstadoPedidoRequest,
  CrearPedidoRequest,
  HistorialEstadoResponse,
  PedidoResponse,
} from '../models/pedido.model';
import { CarritoService } from './carrito.service';

@Injectable({ providedIn: 'root' })
export class PedidosApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/pedidos`;

  crear(request: CrearPedidoRequest): Observable<PedidoResponse> {
    return this.http.post<PedidoResponse>(this.baseUrl, request);
  }

  confirmarCarrito(
    carrito: CarritoService,
    observaciones?: string | null,
  ): Observable<PedidoResponse> {
    const items = carrito.items();
    if (items.length === 0) {
      return throwError(() => new Error('No se puede confirmar un carrito vacío.'));
    }

    return this.crear({
      detalles: items.map(({ productoId, cantidad }) => ({ productoId, cantidad })),
      observaciones,
    }).pipe(tap(() => carrito.vaciar()));
  }

  listarMisPedidos(): Observable<PedidoResponse[]> {
    return this.http.get<PedidoResponse[]>(`${this.baseUrl}/mis-pedidos`);
  }

  listarOperativos(): Observable<PedidoResponse[]> {
    return this.http.get<PedidoResponse[]>(`${this.baseUrl}/operativos`);
  }

  obtener(pedidoId: number): Observable<PedidoResponse> {
    return this.http.get<PedidoResponse>(`${this.baseUrl}/${pedidoId}`);
  }

  listarHistorial(pedidoId: number): Observable<HistorialEstadoResponse[]> {
    return this.http.get<HistorialEstadoResponse[]>(`${this.baseUrl}/${pedidoId}/historial`);
  }

  cambiarEstado(
    pedidoId: number,
    request: CambiarEstadoPedidoRequest,
  ): Observable<PedidoResponse> {
    return this.http.patch<PedidoResponse>(`${this.baseUrl}/${pedidoId}/estado`, request);
  }

  cancelar(pedidoId: number, observacion: string): Observable<PedidoResponse> {
    return this.http.patch<PedidoResponse>(`${this.baseUrl}/${pedidoId}/cancelar`, {
      observacion,
    });
  }
}
