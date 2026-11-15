import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ProductoMenu } from '../../core/models/producto-menu.model';
import { CategoriasApiService } from '../catalog/services/categorias-api.service';
import { CarritoService } from '../pedidos/services/carrito.service';
import { MenuPageComponent } from './menu-page.component';
import { MenuApiService } from './services/menu-api.service';

describe('MenuPageComponent', () => {
  const producto: ProductoMenu = {
    id: 7,
    categoriaId: 2,
    categoria: 'Cafés',
    nombre: 'Café',
    descripcion: null,
    precio: 3.5,
    imagenUrl: null,
    disponible: true,
    fechaCreacion: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    localStorage.removeItem('cafeteria-las-rosas-carrito');
    await TestBed.configureTestingModule({
      imports: [MenuPageComponent],
      providers: [
        {
          provide: MenuApiService,
          useValue: {
            listarDisponibles: () => of([producto]),
            listarDisponiblesPorCategoria: () => of([producto]),
          },
        },
        { provide: CategoriasApiService, useValue: { listar: () => of([]) } },
      ],
    }).compileComponents();
  });

  it('adds an available menu product to the cart and updates its visible quantity', async () => {
    const fixture = TestBed.createComponent(MenuPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector(
      'button[aria-label="Agregar Café al carrito"]',
    ) as HTMLButtonElement;
    expect(button.textContent).toContain('Agregar al carrito');

    button.click();
    fixture.detectChanges();

    expect(TestBed.inject(CarritoService).items()).toEqual([
      { productoId: 7, nombre: 'Café', precioUnitario: 3.5, cantidad: 1 },
    ]);
    expect(fixture.nativeElement.querySelector('[role="status"]')?.textContent).toContain('1 en el carrito');
  });
});
