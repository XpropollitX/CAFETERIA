export type MetodoPago = 'EFECTIVO' | 'YAPE' | 'TRANSFERENCIA';

export type EstadoPago = 'PENDIENTE' | 'PAGADO' | 'ANULADO';

export interface RegistrarPagoRequest {
  metodoPago: MetodoPago;
  monto: number;
  referencia?: string | null;
}

export interface PagoResponse {
  id: number;
  pedidoId: number;
  metodoPago: MetodoPago;
  monto: number;
  estadoPago: EstadoPago;
  registradoPorId: number | null;
  fechaPago: string | null;
  referencia: string | null;
}
