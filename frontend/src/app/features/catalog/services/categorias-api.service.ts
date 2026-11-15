import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface Categoria {
  id: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
}

export interface CategoriaRequest {
  nombre: string;
  descripcion?: string | null;
}

@Injectable({ providedIn: 'root' })
export class CategoriasApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/categorias`;

  listar(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(this.baseUrl);
  }

  crear(request: CategoriaRequest): Observable<Categoria> {
    return this.http.post<Categoria>(this.baseUrl, request);
  }

  actualizar(id: number, request: CategoriaRequest): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.baseUrl}/${id}`, request);
  }

  cambiarEstado(id: number, activo: boolean): Observable<Categoria> {
    return this.http.patch<Categoria>(`${this.baseUrl}/${id}/estado`, { activo });
  }
}
