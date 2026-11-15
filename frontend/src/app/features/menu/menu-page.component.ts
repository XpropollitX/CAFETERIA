import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { CarritoService } from '../pedidos/services/carrito.service';
import { MenuApiService } from './services/menu-api.service';
import { CategoriasApiService, Categoria } from '../catalog/services/categorias-api.service';
import { ProductoMenu } from '../../core/models/producto-menu.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-menu-page',
  standalone: true,
  imports: [CurrencyPipe],
  template: `
    <header class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
      <div><p class="eyebrow mb-1">HECHO CON CARIÑO</p><h1 class="display-6 fw-bold mb-1">Nuestro menú</h1>
        <p class="text-secondary mb-0">Productos y disponibilidad cargados desde el sistema.</p></div>
      <span class="badge rounded-pill text-bg-light border px-3 py-2" role="status" aria-live="polite">{{ cantidadEnCarrito() }} en el carrito</span>
    </header>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
    @if (cargando()) { <div class="text-center py-5"><div class="spinner-border text-secondary" role="status"></div><p class="mt-2">Cargando menú…</p></div> }
    @if (!cargando()) {
      <nav class="category-scroll d-flex gap-2 mb-4" aria-label="Categorías">
        <button class="btn" [class.btn-primary]="categoriaActiva() === null" [class.btn-outline-secondary]="categoriaActiva() !== null" (click)="seleccionar(null)">Todo</button>
        @for (categoria of categorias(); track categoria.id) {
          <button class="btn text-nowrap" [class.btn-primary]="categoriaActiva() === categoria.id" [class.btn-outline-secondary]="categoriaActiva() !== categoria.id" (click)="seleccionar(categoria.id)">{{ categoria.nombre }}</button>
        }
      </nav>
      @if (!productos().length) { <div class="empty-state text-center py-5"><h2 class="h4">No hay productos disponibles</h2><p class="text-secondary mb-0">Vuelve a consultar más tarde.</p></div> }
      <div class="row g-3">
        @for (producto of productos(); track producto.id) {
          <div class="col-sm-6 col-lg-4 col-xl-3">
            <article class="product-card h-100 d-flex flex-column">
              @if (producto.imagenUrl) { <img class="product-image" [src]="imagenSrc(producto.imagenUrl)" [alt]="producto.nombre" loading="lazy"> }
              @else { <div class="product-image product-placeholder" aria-hidden="true"><span>Las Rosas</span></div> }
              <div class="p-3 d-flex flex-column flex-grow-1">
                <p class="small text-uppercase text-secondary mb-1">{{ producto.categoria }}</p>
                <h2 class="h5 fw-bold">{{ producto.nombre }}</h2>
                <p class="text-secondary small flex-grow-1">{{ producto.descripcion || 'Preparado para disfrutar.' }}</p>
                <div class="product-actions mt-auto pt-2">
                  <strong class="price d-block mb-2">{{ producto.precio | currency:'PEN':'symbol':'1.2-2' }}</strong>
                  <button class="btn btn-primary w-100" type="button" [attr.aria-label]="'Agregar ' + producto.nombre + ' al carrito'" [disabled]="!producto.disponible" (click)="agregar(producto)">
                    {{ producto.disponible ? 'Agregar al carrito' : 'No disponible' }}
                  </button>
                </div>
              </div>
            </article>
          </div>
        }
      </div>
      @if (mensaje()) { <div class="toast-message" role="status">{{ mensaje() }}</div> }
    }
  `,
  styles: [`
    .eyebrow{color:var(--color-primary);font-size:.75rem;font-weight:800;letter-spacing:.14em}.btn-primary{--bs-btn-bg:#754024;--bs-btn-border-color:#754024;--bs-btn-hover-bg:#60331c;--bs-btn-hover-border-color:#60331c}
    .category-scroll{overflow-x:auto;padding-bottom:.5rem}.product-card{background:#fff;border:1px solid var(--color-border);border-radius:1rem;overflow:hidden;box-shadow:0 .5rem 1.5rem #4a2c1510}
    .product-image{width:100%;height:12rem;flex:0 0 12rem;object-fit:cover}.product-placeholder{display:grid;place-items:center;background:radial-gradient(circle,#f0d7b6,#b87549);color:#542d1a;font-family:Georgia,serif;font-size:1.4rem}
    .price{color:var(--color-primary)}.product-actions{margin-top:auto}.empty-state{background:#fff;border:1px dashed var(--color-border);border-radius:1rem}.toast-message{position:fixed;right:1rem;bottom:1rem;background:#382116;color:#fff;padding:.8rem 1.2rem;border-radius:.75rem;z-index:5}
  `],
})
export class MenuPageComponent implements OnInit {
  private readonly menuApi = inject(MenuApiService);
  private readonly categoriasApi = inject(CategoriasApiService);
  readonly carrito = inject(CarritoService);
  readonly cantidadEnCarrito = computed(() =>
    this.carrito.items().reduce((total, item) => total + item.cantidad, 0),
  );
  readonly categorias = signal<Categoria[]>([]);
  readonly productos = signal<ProductoMenu[]>([]);
  readonly categoriaActiva = signal<number | null>(null);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly mensaje = signal('');
  private readonly imageBaseUrl = environment.apiBaseUrl.replace(/\/api\/?$/, '');

  ngOnInit(): void {
    this.categoriasApi.listar().subscribe({
      next: (items) => { this.categorias.set(items.filter((categoria) => categoria.activo)); },
      error: (error: unknown) => { this.error.set(this.mensajeError(error, 'No se pudieron cargar las categorías.')); },
    });
    this.cargarProductos();
  }

  seleccionar(categoriaId: number | null): void {
    this.categoriaActiva.set(categoriaId);
    this.cargarProductos();
  }

  imagenSrc(imagenUrl: string | null): string | null {
    return imagenUrl?.startsWith('/') ? `${this.imageBaseUrl}${imagenUrl}` : imagenUrl;
  }

  agregar(producto: ProductoMenu): void {
    try {
      this.carrito.agregar(producto);
      this.mensaje.set(`${producto.nombre} se agregó al carrito.`);
      window.setTimeout(() => { this.mensaje.set(''); }, 2500);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo agregar el producto.');
    }
  }

  private cargarProductos(): void {
    this.cargando.set(true);
    this.error.set('');
    const categoriaId = this.categoriaActiva();
    const request = categoriaId === null
      ? this.menuApi.listarDisponibles()
      : this.menuApi.listarDisponiblesPorCategoria(categoriaId);
    request.subscribe({
      next: (productos) => { this.productos.set(productos); this.cargando.set(false); },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'No se pudo cargar el menú.'));
        this.cargando.set(false);
      },
    });
  }

  private mensajeError(error: unknown, fallback: string): string {
    return error instanceof HttpErrorResponse ? error.error?.message ?? fallback : fallback;
  }
}
