import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CarritoService } from './carrito.service';
import { PedidosApiService } from './pedidos-api.service';
import { MenuApiService } from '../../menu/services/menu-api.service';

describe('PedidosApiService', () => {
  let httpTesting: HttpTestingController;
  let carrito: CarritoService;
  let pedidosApi: PedidosApiService;

  beforeEach(() => {
    localStorage.removeItem('cafeteria-las-rosas-carrito');
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    carrito = TestBed.inject(CarritoService);
    pedidosApi = TestBed.inject(PedidosApiService);
  });

  afterEach(() => httpTesting.verify());

  it('sends product ids and quantities only, then empties the cart on success', () => {
    carrito.agregar({
      id: 8,
      categoriaId: 3,
      categoria: 'Postres',
      nombre: 'Torta',
      descripcion: null,
      precio: 6.25,
      imagenUrl: null,
      disponible: true,
      fechaCreacion: '2026-01-01T00:00:00Z',
    });

    pedidosApi.confirmarCarrito(carrito, 'Para llevar').subscribe();
    const request = httpTesting.expectOne('http://localhost:8081/api/pedidos');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      detalles: [{ productoId: 8, cantidad: 1 }],
      observaciones: 'Para llevar',
    });
    expect(request.request.body.total).toBeUndefined();
    expect(request.request.body.usuarioId).toBeUndefined();

    request.flush({
      id: 15,
      usuarioId: 9,
      fechaPedido: '2026-01-01T00:00:00Z',
      estado: 'RECIBIDO',
      subtotal: 6.25,
      total: 6.25,
      observaciones: 'Para llevar',
      detalles: [],
    });
    expect(carrito.estaVacio()).toBe(true);
  });

  it('keeps the cart when the API rejects order creation', () => {
    carrito.agregar({
      id: 8,
      categoriaId: 3,
      categoria: 'Postres',
      nombre: 'Torta',
      descripcion: null,
      precio: 6.25,
      imagenUrl: null,
      disponible: true,
      fechaCreacion: '2026-01-01T00:00:00Z',
    });

    pedidosApi.confirmarCarrito(carrito).subscribe({ error: () => undefined });
    httpTesting.expectOne('http://localhost:8081/api/pedidos').flush(
      { message: 'Producto no disponible.' },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    expect(carrito.estaVacio()).toBe(false);
  });

  it('loads menu products from the backend API', () => {
    const menuApi = TestBed.inject(MenuApiService);
    menuApi.listarDisponibles().subscribe();
    const request = httpTesting.expectOne('http://localhost:8081/api/productos/disponibles');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('uses the operational, status, cancellation, and history endpoints', () => {
    pedidosApi.listarOperativos().subscribe();
    const operational = httpTesting.expectOne('http://localhost:8081/api/pedidos/operativos');
    expect(operational.request.method).toBe('GET');
    operational.flush([]);

    pedidosApi.cambiarEstado(15, { estado: 'EN_PREPARACION' }).subscribe();
    const changeState = httpTesting.expectOne('http://localhost:8081/api/pedidos/15/estado');
    expect(changeState.request.method).toBe('PATCH');
    expect(changeState.request.body).toEqual({ estado: 'EN_PREPARACION' });
    changeState.flush({});

    pedidosApi.cancelar(15, 'Cancelación solicitada').subscribe();
    const cancel = httpTesting.expectOne('http://localhost:8081/api/pedidos/15/cancelar');
    expect(cancel.request.method).toBe('PATCH');
    expect(cancel.request.body).toEqual({ observacion: 'Cancelación solicitada' });
    cancel.flush({});

    pedidosApi.listarHistorial(15).subscribe();
    const history = httpTesting.expectOne('http://localhost:8081/api/pedidos/15/historial');
    expect(history.request.method).toBe('GET');
    history.flush([]);
  });
});
