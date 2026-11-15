import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PagosApiService } from './pagos-api.service';

describe('PagosApiService', () => {
  let service: PagosApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PagosApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('registers a transfer payment using the allowed payment methods', () => {
    service.registrar(24, { metodoPago: 'TRANSFERENCIA', monto: 12.5 }).subscribe();
    const request = httpTesting.expectOne('http://localhost:8081/api/pedidos/24/pagos');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ metodoPago: 'TRANSFERENCIA', monto: 12.5 });
    request.flush({
      id: 8,
      pedidoId: 24,
      metodoPago: 'TRANSFERENCIA',
      monto: 12.5,
      estadoPago: 'PENDIENTE',
      registradoPorId: null,
      fechaPago: null,
      referencia: null,
    });
  });

  it('queries and verifies payments through the API', () => {
    service.listarPorPedido(24).subscribe();
    httpTesting.expectOne('http://localhost:8081/api/pedidos/24/pagos').flush([]);

    service.verificar(8).subscribe();
    const request = httpTesting.expectOne('http://localhost:8081/api/pagos/8/verificar');
    expect(request.request.method).toBe('PATCH');
    request.flush({
      id: 8,
      pedidoId: 24,
      metodoPago: 'EFECTIVO',
      monto: 12.5,
      estadoPago: 'PAGADO',
      registradoPorId: 3,
      fechaPago: '2026-01-01T00:00:00Z',
      referencia: null,
    });
  });
});
