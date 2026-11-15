import { HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Categoria, CategoriasApiService } from './services/categorias-api.service';
import { ProductoMenu } from '../../core/models/producto-menu.model';
import { ProductoRequest, ProductosApiService } from './services/productos-api.service';
import { UsuariosAdminApiService } from './services/usuarios-admin-api.service';

@Component({
  selector: 'app-admin-catalog-page',
  standalone: true,
  imports: [CurrencyPipe, FormsModule],
  template: `
    <header class="mb-4"><p class="eyebrow mb-1">ADMINISTRACIÓN</p><h1 class="display-6 fw-bold">Catálogo</h1><p class="text-secondary">Gestiona categorías y disponibilidad del menú.</p></header>
    @if (error()) { <div class="alert alert-danger" role="alert">{{ error() }}</div> }
    @if (mensaje()) { <div class="alert alert-success" role="status">{{ mensaje() }}</div> }
    <details class="admin-card mb-4">
      <summary class="h5 fw-bold">Crear cuenta de personal</summary>
      <form class="mt-3" #personalForm="ngForm" (ngSubmit)="crearPersonal()">
        <div class="row g-3">
          <div class="col-md-6"><label class="form-label" for="personalNombres">Nombres</label><input id="personalNombres" class="form-control" name="personalNombres" [(ngModel)]="personal.nombres" required maxlength="100"></div>
          <div class="col-md-6"><label class="form-label" for="personalApellidos">Apellidos</label><input id="personalApellidos" class="form-control" name="personalApellidos" [(ngModel)]="personal.apellidos" required maxlength="100"></div>
          <div class="col-md-6"><label class="form-label" for="personalCorreo">Correo</label><input id="personalCorreo" class="form-control" type="email" name="personalCorreo" [(ngModel)]="personal.correo" required email maxlength="254"></div>
          <div class="col-md-6"><label class="form-label" for="personalTelefono">Teléfono</label><input id="personalTelefono" class="form-control" name="personalTelefono" [(ngModel)]="personal.telefono" maxlength="20"></div>
          <div class="col-md-6"><label class="form-label" for="personalPassword">Contraseña inicial</label><input id="personalPassword" class="form-control" type="password" name="personalPassword" [(ngModel)]="personal.password" required minlength="8" maxlength="72" autocomplete="new-password"></div>
        </div>
        <button class="btn btn-primary mt-3" [disabled]="personalForm.invalid || guardando()">Crear cuenta PERSONAL</button>
      </form>
    </details>
    <div class="row g-4 align-items-start">
      <div class="col-xl-5">
        <section class="admin-card mb-4">
          <div class="d-flex justify-content-between align-items-center gap-2"><h2 class="h5 fw-bold">{{ categoriaIdEditar ? 'Editar categoría' : 'Nueva categoría' }}</h2>
            @if (categoriaIdEditar) { <button class="btn btn-sm btn-link" (click)="limpiarCategoria()">Cancelar edición</button> }
          </div>
          <form #catForm="ngForm" (ngSubmit)="guardarCategoria()">
            <label class="form-label" for="nombreCat">Nombre</label><input id="nombreCat" class="form-control mb-3" name="nombre" [(ngModel)]="categoriaForm.nombre" required maxlength="100">
            <label class="form-label" for="descCat">Descripción</label><textarea id="descCat" class="form-control mb-3" name="descripcion" [(ngModel)]="categoriaForm.descripcion" maxlength="500" rows="2"></textarea>
            <button class="btn btn-primary" [disabled]="catForm.invalid || guardando()">{{ categoriaIdEditar ? 'Guardar cambios' : 'Crear categoría' }}</button>
          </form>
        </section>
        <section class="admin-card">
          <h2 class="h5 fw-bold">Categorías</h2>
          @if (!categorias().length) { <p class="text-secondary mb-0">No hay categorías.</p> }
          @for (categoria of categorias(); track categoria.id) {
            <div class="catalog-row"><div><strong>{{ categoria.nombre }}</strong><div class="small text-secondary">{{ categoria.activo ? 'Activa' : 'Inactiva' }}</div></div>
              <div class="d-flex gap-2"><button class="btn btn-sm btn-outline-secondary" (click)="editarCategoria(categoria)">Editar</button>
                <button class="btn btn-sm" [class.btn-outline-danger]="categoria.activo" [class.btn-outline-success]="!categoria.activo" (click)="alternarCategoria(categoria)">
                  {{ categoria.activo ? 'Desactivar' : 'Activar' }}
                </button></div>
            </div>
          }
        </section>
      </div>
      <div class="col-xl-7">
        <section class="admin-card mb-4">
          <div class="d-flex justify-content-between align-items-center gap-2"><h2 class="h5 fw-bold">{{ productoIdEditar ? 'Editar producto' : 'Nuevo producto' }}</h2>
            @if (productoIdEditar) { <button class="btn btn-sm btn-link" (click)="limpiarProducto()">Cancelar edición</button> }
          </div>
          <form #prodForm="ngForm" (ngSubmit)="guardarProducto()">
            <div class="row g-3">
              <div class="col-md-6"><label class="form-label" for="nombreProd">Nombre</label><input id="nombreProd" class="form-control" name="prodNombre" [(ngModel)]="productoForm.nombre" required maxlength="150"></div>
              <div class="col-md-6"><label class="form-label" for="categoriaProd">Categoría</label>
                <select id="categoriaProd" class="form-select" name="categoriaId" [(ngModel)]="productoForm.categoriaId" required>
                  <option [ngValue]="null" disabled>Selecciona una categoría activa</option>
                  @for (categoria of categorias(); track categoria.id) { @if (categoria.activo) { <option [ngValue]="categoria.id">{{ categoria.nombre }}</option> } }
                </select>
              </div>
              <div class="col-md-4"><label class="form-label" for="precioProd">Precio (S/)</label><input id="precioProd" class="form-control" type="number" name="precio" [(ngModel)]="productoForm.precio" min="0.01" step="0.01" required></div>
              <div class="col-12">
                <label class="form-label" for="imagenProd">Imagen del producto <span class="text-secondary">(opcional)</span></label>
                <input #imagenProdInput id="imagenProd" class="form-control" type="file" accept="image/jpeg,image/png,image/webp" (change)="seleccionarImagen($event)">
                <div class="form-text">JPG, PNG o WebP; máximo 5 MB. Se guardará localmente en el servidor.</div>
                @if (imagenSeleccionada(); as imagen) { <p class="small mt-2 mb-0">Archivo seleccionado: {{ imagen.name }}</p> }
                @else if (productoIdEditar && productoForm.imagenUrl) { <p class="small mt-2 mb-0">La imagen actual se conservará si no eliges otra.</p> }
              </div>
              <div class="col-12"><label class="form-label" for="descProd">Descripción</label><textarea id="descProd" class="form-control" name="prodDescripcion" [(ngModel)]="productoForm.descripcion" rows="2" maxlength="1000"></textarea></div>
            </div>
            <button class="btn btn-primary mt-3" [disabled]="prodForm.invalid || guardando() || !productoForm.categoriaId">{{ productoIdEditar ? 'Guardar cambios' : 'Crear producto' }}</button>
          </form>
        </section>
        <section class="admin-card">
          <h2 class="h5 fw-bold">Productos</h2>
          @if (!productos().length) { <p class="text-secondary mb-0">No hay productos registrados.</p> }
          @for (producto of productos(); track producto.id) {
            <div class="catalog-row product-row">
              <div class="flex-grow-1"><strong>{{ producto.nombre }}</strong><div class="small text-secondary">{{ producto.categoria }} · {{ producto.precio | currency:'PEN':'symbol':'1.2-2' }} · {{ producto.disponible ? 'Disponible' : 'No disponible' }}</div></div>
              <div class="d-flex gap-2"><button class="btn btn-sm btn-outline-secondary" (click)="editarProducto(producto)">Editar</button>
                <button class="btn btn-sm" [class.btn-outline-danger]="producto.disponible" [class.btn-outline-success]="!producto.disponible" (click)="alternarProducto(producto)">
                  {{ producto.disponible ? 'Desactivar' : 'Activar' }}
                </button></div>
            </div>
          }
        </section>
      </div>
    </div>
  `,
  styles: [`
    .eyebrow{color:var(--color-primary);font-size:.75rem;font-weight:800;letter-spacing:.14em}.admin-card{background:#fff;border:1px solid var(--color-border);border-radius:1rem;padding:1.25rem}
    .catalog-row{display:flex;justify-content:space-between;align-items:center;gap:.75rem;padding:.85rem 0;border-bottom:1px solid #f0ebe6}.catalog-row:last-child{border-bottom:0}.product-row{align-items:flex-start}
    .btn-primary{--bs-btn-bg:#754024;--bs-btn-border-color:#754024;--bs-btn-hover-bg:#60331c;--bs-btn-hover-border-color:#60331c}
    @media(max-width:575px){.catalog-row{align-items:flex-start;flex-direction:column}.catalog-row>div:last-child{width:100%}.catalog-row button{flex:1}}
  `],
})
export class AdminCatalogPageComponent implements OnInit {
  @ViewChild('imagenProdInput') private imagenProdInput?: ElementRef<HTMLInputElement>;
  private readonly categoriasApi = inject(CategoriasApiService);
  private readonly productosApi = inject(ProductosApiService);
  private readonly usuariosApi = inject(UsuariosAdminApiService);
  readonly categorias = signal<Categoria[]>([]);
  readonly productos = signal<ProductoMenu[]>([]);
  readonly imagenSeleccionada = signal<File | null>(null);
  categoriaIdEditar: number | null = null;
  productoIdEditar: number | null = null;
  categoriaForm = { nombre: '', descripcion: '' };
  productoForm: ProductoRequest = {
    categoriaId: 0,
    nombre: '',
    descripcion: '',
    precio: 0,
    imagenUrl: '',
    disponible: true,
  };
  personal = { nombres: '', apellidos: '', correo: '', telefono: '', password: '' };
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');

  ngOnInit(): void {
    this.recargar();
  }

  guardarCategoria(): void {
    const request = { nombre: this.categoriaForm.nombre.trim(), descripcion: this.categoriaForm.descripcion.trim() || null };
    const save = this.categoriaIdEditar === null
      ? this.categoriasApi.crear(request)
      : this.categoriasApi.actualizar(this.categoriaIdEditar, request);
    this.ejecutar(save, this.categoriaIdEditar === null ? 'Categoría creada.' : 'Categoría actualizada.', () => {
      this.limpiarCategoria();
    });
  }

  editarCategoria(categoria: Categoria): void {
    this.categoriaIdEditar = categoria.id;
    this.categoriaForm = { nombre: categoria.nombre, descripcion: categoria.descripcion ?? '' };
  }

  alternarCategoria(categoria: Categoria): void {
    if (!window.confirm(`${categoria.activo ? '¿Desactivar' : '¿Activar'} la categoría ${categoria.nombre}?`)) return;
    this.ejecutar(this.categoriasApi.cambiarEstado(categoria.id, !categoria.activo), 'Estado de categoría actualizado.');
  }

  limpiarCategoria(): void {
    this.categoriaIdEditar = null;
    this.categoriaForm = { nombre: '', descripcion: '' };
  }

  guardarProducto(): void {
    if (!this.productoForm.categoriaId) return;
    const request = {
      ...this.productoForm,
      nombre: this.productoForm.nombre.trim(),
      descripcion: this.productoForm.descripcion?.trim() || null,
      imagenUrl: this.productoForm.imagenUrl?.trim() || null,
    };
    const save = this.productoIdEditar === null
      ? this.productosApi.crear(request, this.imagenSeleccionada())
      : this.productosApi.actualizar(this.productoIdEditar, request, this.imagenSeleccionada());
    this.ejecutar(save, this.productoIdEditar === null ? 'Producto creado.' : 'Producto actualizado.', () => {
      this.limpiarProducto();
    });
  }

  editarProducto(producto: ProductoMenu): void {
    this.limpiarImagenSeleccionada();
    this.productoIdEditar = producto.id;
    this.productoForm = {
      categoriaId: producto.categoriaId,
      nombre: producto.nombre,
      descripcion: producto.descripcion ?? '',
      precio: producto.precio,
      imagenUrl: producto.imagenUrl ?? '',
      disponible: producto.disponible,
    };
  }

  alternarProducto(producto: ProductoMenu): void {
    this.ejecutar(this.productosApi.cambiarDisponibilidad(producto.id, !producto.disponible), 'Disponibilidad de producto actualizada.');
  }

  limpiarProducto(): void {
    this.productoIdEditar = null;
    this.limpiarImagenSeleccionada();
    this.productoForm = { categoriaId: 0, nombre: '', descripcion: '', precio: 0, imagenUrl: '', disponible: true };
  }

  seleccionarImagen(event: Event): void {
    const input = event.target;
    if (input instanceof HTMLInputElement) {
      this.imagenSeleccionada.set(input.files?.[0] ?? null);
    }
  }

  crearPersonal(): void {
    this.ejecutar(this.usuariosApi.crearPersonal({
      nombres: this.personal.nombres.trim(),
      apellidos: this.personal.apellidos.trim(),
      correo: this.personal.correo.trim(),
      telefono: this.personal.telefono.trim() || null,
      password: this.personal.password,
    }), 'Cuenta PERSONAL creada.', () => {
      this.personal = { nombres: '', apellidos: '', correo: '', telefono: '', password: '' };
    });
  }

  private recargar(): void {
    this.categoriasApi.listar().subscribe({
      next: (categorias) => { this.categorias.set(categorias); },
      error: (error: unknown) => { this.error.set(this.mensajeError(error, 'No se pudieron cargar las categorías.')); },
    });
    this.productosApi.listarTodos().subscribe({
      next: (productos) => { this.productos.set(productos); },
      error: (error: unknown) => { this.error.set(this.mensajeError(error, 'No se pudieron cargar los productos.')); },
    });
  }

  private limpiarImagenSeleccionada(): void {
    this.imagenSeleccionada.set(null);
    if (this.imagenProdInput) {
      this.imagenProdInput.nativeElement.value = '';
    }
  }

  private ejecutar<T>(request: import('rxjs').Observable<T>, mensaje: string, despues?: () => void): void {
    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');
    request.subscribe({
      next: () => {
        this.mensaje.set(mensaje);
        this.guardando.set(false);
        despues?.();
        this.recargar();
      },
      error: (error: unknown) => {
        this.error.set(this.mensajeError(error, 'La operación no se pudo completar.'));
        this.guardando.set(false);
      },
    });
  }

  private mensajeError(error: unknown, fallback: string): string {
    return error instanceof HttpErrorResponse ? error.error?.message ?? fallback : fallback;
  }
}
