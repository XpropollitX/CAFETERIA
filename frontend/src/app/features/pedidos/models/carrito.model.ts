export interface ItemCarrito {
  productoId: number;
  nombre: string;
  precioUnitario: number;
  cantidad: number;
}

export interface ItemCarritoResumen extends ItemCarrito {
  subtotal: number;
}
