import { Injectable, computed, signal } from '@angular/core';
import { ProductoMenu } from '../../../core/models/producto-menu.model';
import { ItemCarrito, ItemCarritoResumen } from '../models/carrito.model';

const STORAGE_KEY = 'cafeteria-las-rosas-carrito';

@Injectable({ providedIn: 'root' })
export class CarritoService {
  private readonly itemsState = signal<ItemCarrito[]>(this.cargar());
  readonly items = this.itemsState.asReadonly();
  readonly estaVacio = computed(() => this.itemsState().length === 0);
  readonly total = computed(() => this.itemsState().reduce(
    (totalCents, item) => totalCents + this.enCentimos(item.precioUnitario) * item.cantidad,
    0,
  ) / 100);

  resumen(): ItemCarritoResumen[] {
    return this.itemsState().map((item) => ({
      ...item,
      subtotal: this.enCentimos(item.precioUnitario) * item.cantidad / 100,
    }));
  }

  agregar(producto: ProductoMenu): void {
    if (!producto.disponible) {
      throw new Error('No se puede agregar un producto no disponible.');
    }
    this.enCentimos(producto.precio);

    const items = this.itemsState();
    const existente = items.find((item) => item.productoId === producto.id);
    const actualizados = existente
      ? items.map((item) => item.productoId === producto.id
        ? {
            ...item,
            nombre: producto.nombre,
            precioUnitario: producto.precio,
            cantidad: item.cantidad + 1,
          }
        : item)
      : [...items, {
          productoId: producto.id,
          nombre: producto.nombre,
          precioUnitario: producto.precio,
          cantidad: 1,
        }];
    this.guardar(actualizados);
  }

  incrementar(productoId: number): void {
    this.actualizarCantidad(productoId, (cantidad) => cantidad + 1);
  }

  disminuir(productoId: number): void {
    const item = this.buscar(productoId);
    if (item.cantidad === 1) {
      this.eliminar(productoId);
      return;
    }
    this.actualizarCantidad(productoId, (cantidad) => cantidad - 1);
  }

  establecerCantidad(productoId: number, cantidad: number): void {
    if (!Number.isSafeInteger(cantidad) || cantidad <= 0) {
      throw new RangeError('La cantidad debe ser un entero mayor que cero.');
    }
    this.buscar(productoId);
    this.guardar(this.itemsState().map((item) =>
      item.productoId === productoId ? { ...item, cantidad } : item,
    ));
  }

  eliminar(productoId: number): void {
    this.guardar(this.itemsState().filter((item) => item.productoId !== productoId));
  }

  vaciar(): void {
    this.guardar([]);
  }

  private actualizarCantidad(productoId: number, actualizar: (cantidad: number) => number): void {
    const item = this.buscar(productoId);
    this.establecerCantidad(productoId, actualizar(item.cantidad));
  }

  private buscar(productoId: number): ItemCarrito {
    const item = this.itemsState().find((actual) => actual.productoId === productoId);
    if (!item) {
      throw new Error('El producto no está en el carrito.');
    }
    return item;
  }

  private guardar(items: ItemCarrito[]): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    }
    this.itemsState.set(items);
  }

  private cargar(): ItemCarrito[] {
    if (typeof localStorage === 'undefined') {
      return [];
    }
    const contenido = localStorage.getItem(STORAGE_KEY);
    if (contenido === null) {
      return [];
    }

    const datos: unknown = JSON.parse(contenido);
    if (!Array.isArray(datos) || !datos.every(esItemCarrito)) {
      throw new Error('El carrito guardado tiene un formato inválido; elimínalo y vuelve a cargar el menú.');
    }
    return datos;
  }

  private enCentimos(precio: number): number {
    if (!Number.isFinite(precio) || precio <= 0) {
      throw new RangeError('El precio del producto debe ser mayor que cero.');
    }
    const centimos = Math.round(precio * 100);
    if (!Number.isSafeInteger(centimos)) {
      throw new RangeError('El precio del producto está fuera del rango permitido.');
    }
    return centimos;
  }
}

function esItemCarrito(value: unknown): value is ItemCarrito {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const item = value as Record<string, unknown>;
  return Number.isSafeInteger(item['productoId'])
    && typeof item['nombre'] === 'string'
    && typeof item['precioUnitario'] === 'number'
    && item['precioUnitario'] > 0
    && Number.isSafeInteger(item['cantidad'])
    && (item['cantidad'] as number) > 0;
}
