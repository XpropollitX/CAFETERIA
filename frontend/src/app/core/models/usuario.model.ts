export type RolUsuario = 'CLIENTE' | 'PERSONAL' | 'ADMIN';

export interface UsuarioSesion {
  id: number;
  nombres: string;
  apellidos: string;
  correo: string;
  telefono: string | null;
  rol: RolUsuario;
  activo: boolean;
  fechaCreacion: string;
}

export interface RegistroClienteRequest {
  nombres: string;
  apellidos: string;
  correo: string;
  telefono?: string | null;
  password: string;
}
