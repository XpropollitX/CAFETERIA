import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ProductoMenu } from '../../../core/models/producto-menu.model';

@Injectable({ providedIn: 'root' })
export class MenuApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/productos`;

  listarDisponibles(): Observable<ProductoMenu[]> {
    return this.http.get<ProductoMenu[]>(`${this.baseUrl}/disponibles`);
  }

  listarDisponiblesPorCategoria(categoriaId: number): Observable<ProductoMenu[]> {
    return this.http.get<ProductoMenu[]>(`${this.baseUrl}/categoria/${categoriaId}`);
  }
}
