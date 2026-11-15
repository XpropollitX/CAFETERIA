import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { UsuarioSesion } from '../../../core/models/usuario.model';

export interface CrearPersonalRequest {
  nombres: string;
  apellidos: string;
  correo: string;
  telefono?: string | null;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class UsuariosAdminApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/usuarios`;

  crearPersonal(request: CrearPersonalRequest): Observable<UsuarioSesion> {
    return this.http.post<UsuarioSesion>(`${this.baseUrl}/personal`, request);
  }
}
