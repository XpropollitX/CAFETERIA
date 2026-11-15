import { CarritoService } from './carrito.service';
import { ProductoMenu } from '../../../core/models/producto-menu.model';

describe('CarritoService', () => {
  const producto: ProductoMenu = {
    id: 7,
    categoriaId: 2,
    categoria: 'Bebidas',
    nombre: 'Café',
    descripcion: null,
    precio: 3.5,
    imagenUrl: null,
    disponible: true,
    fechaCreacion: '2026-01-01T00:00:00Z',
  };

  beforeEach(() => {
    localStorage.removeItem('cafeteria-las-rosas-carrito');
  });

  it('combines duplicate products and calculates money in cents', () => {
    const carrito = new CarritoService();
    carrito.agregar(producto);
    carrito.agregar(producto);

    expect(carrito.items()).toEqual([
      { productoId: 7, nombre: 'Café', precioUnitario: 3.5, cantidad: 2 },
    ]);
    expect(carrito.resumen()[0].subtotal).toBe(7);
    expect(carrito.total()).toBe(7);
  });

  it('persists cart data and rejects non-positive quantities and unavailable products', () => {
    const carrito = new CarritoService();
    carrito.agregar(producto);

    expect(() => carrito.establecerCantidad(producto.id, 0)).toThrow(RangeError);
    expect(() => carrito.agregar({ ...producto, disponible: false })).toThrow(
      'No se puede agregar un producto no disponible.',
    );
    expect(JSON.parse(localStorage.getItem('cafeteria-las-rosas-carrito') ?? '[]')).toHaveLength(1);

    carrito.disminuir(producto.id);
    expect(carrito.estaVacio()).toBe(true);
  });
});
