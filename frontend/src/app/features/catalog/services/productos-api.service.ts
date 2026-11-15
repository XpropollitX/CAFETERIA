import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ProductoMenu } from '../../../core/models/producto-menu.model';

export interface ProductoRequest {
  categoriaId: number;
  nombre: string;
  descripcion?: string | null;
  precio: number;
  imagenUrl?: string | null;
  disponible: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProductosApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/productos`;

  listarTodos(): Observable<ProductoMenu[]> {
    return this.http.get<ProductoMenu[]>(this.baseUrl);
  }

  crear(request: ProductoRequest, imagen?: File | null): Observable<ProductoMenu> {
    if (imagen) {
      return this.http.post<ProductoMenu>(this.baseUrl, this.crearFormulario(request, imagen));
    }
    return this.http.post<ProductoMenu>(this.baseUrl, request);
  }

  actualizar(id: number, request: ProductoRequest, imagen?: File | null): Observable<ProductoMenu> {
    if (imagen) {
      return this.http.put<ProductoMenu>(`${this.baseUrl}/${id}`, this.crearFormulario(request, imagen));
    }
    return this.http.put<ProductoMenu>(`${this.baseUrl}/${id}`, request);
  }

  cambiarDisponibilidad(id: number, disponible: boolean): Observable<ProductoMenu> {
    return this.http.patch<ProductoMenu>(`${this.baseUrl}/${id}/disponibilidad`, { disponible });
  }

  private crearFormulario(request: ProductoRequest, imagen: File): FormData {
    const formData = new FormData();
    formData.append('producto', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    formData.append('imagen', imagen, imagen.name);
    return formData;
  }
}
