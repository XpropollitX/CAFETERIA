import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PagoResponse, RegistrarPagoRequest } from '../models/pago.model';

@Injectable({ providedIn: 'root' })
export class PagosApiService {
  private readonly http = inject(HttpClient);
  private readonly pedidosUrl = `${environment.apiBaseUrl}/pedidos`;
  private readonly pagosUrl = `${environment.apiBaseUrl}/pagos`;

  registrar(pedidoId: number, request: RegistrarPagoRequest): Observable<PagoResponse> {
    return this.http.post<PagoResponse>(`${this.pedidosUrl}/${pedidoId}/pagos`, request);
  }

  listarPorPedido(pedidoId: number): Observable<PagoResponse[]> {
    return this.http.get<PagoResponse[]>(`${this.pedidosUrl}/${pedidoId}/pagos`);
  }

  verificar(pagoId: number): Observable<PagoResponse> {
    return this.http.patch<PagoResponse>(`${this.pagosUrl}/${pagoId}/verificar`, null);
  }

  anular(pagoId: number): Observable<PagoResponse> {
    return this.http.patch<PagoResponse>(`${this.pagosUrl}/${pagoId}/anular`, null);
  }
}
