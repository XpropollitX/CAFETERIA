export type EstadoPedido =
  | 'RECIBIDO'
  | 'EN_PREPARACION'
  | 'LISTO'
  | 'ENTREGADO'
  | 'CANCELADO';

export interface CrearPedidoDetalleRequest {
  productoId: number;
  cantidad: number;
}

export interface CrearPedidoRequest {
  detalles: CrearPedidoDetalleRequest[];
  observaciones?: string | null;
}

export interface DetallePedidoResponse {
  id: number;
  productoId: number;
  producto: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface PedidoResponse {
  id: number;
  usuarioId: number;
  fechaPedido: string;
  estado: EstadoPedido;
  subtotal: number;
  total: number;
  observaciones: string | null;
  detalles: DetallePedidoResponse[];
}

export interface CambiarEstadoPedidoRequest {
  estado: EstadoPedido;
  observacion?: string | null;
}

export interface HistorialEstadoResponse {
  id: number;
  pedidoId: number;
  estado: EstadoPedido;
  usuarioId: number;
  fechaCambio: string;
  observacion: string | null;
}
