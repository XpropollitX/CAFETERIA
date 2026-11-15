export interface ProductoMenu {
  id: number;
  categoriaId: number;
  categoria: string;
  nombre: string;
  descripcion: string | null;
  precio: number;
  imagenUrl: string | null;
  disponible: boolean;
  fechaCreacion: string;
}
