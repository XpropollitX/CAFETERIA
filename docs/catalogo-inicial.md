# Catálogo adicional

El archivo `catalogo-inicial.sql` agrega cinco categorías y dos productos por categoría. Es idempotente: se puede ejecutar otra vez sin duplicar las categorías ni productos con el mismo nombre dentro de la categoría.

Los diez archivos JPG están preparados en `backend/uploads/product-images/` a 1200 x 900 px y las rutas locales están incluidas en el SQL. El backend debe ejecutarse con `backend` como directorio de trabajo (o con `UPLOAD_DIR` apuntando a esa carpeta) para servirlos.

## Carga en PostgreSQL

Desde la raíz del proyecto, ejecuta en PowerShell. Esto pide la contraseña local de PostgreSQL de forma interactiva y conserva los acentos:

```powershell
$env:PGCLIENTENCODING = "UTF8"
try {
    psql -h localhost -p 5432 -U postgres -d cafeteria_las_rosas -W -f .\docs\catalogo-inicial.sql
    if ($LASTEXITCODE -ne 0) { throw "No se pudo cargar el catalogo." }
} finally {
    Remove-Item Env:PGCLIENTENCODING -ErrorAction SilentlyContinue
}
```

No escribas la contraseña en el archivo ni la compartas por chat. El SQL no elimina ni modifica productos existentes.

## Imágenes

Las fotografías se descargaron de Unsplash y se almacenan localmente para que el menú no dependa de la URL externa. Fuentes por producto:

| Producto | Archivo local | Fuente |
|---|---|---|
| Cold brew de la casa | `dc5dde58-4928-4279-96c2-8cb48529c6d1.jpg` | [Unsplash](https://images.unsplash.com/photo-1461023058943-07fcbe16d735) |
| Refresco cítrico de la casa | `5545b8e1-c260-4cbb-a962-b8f46044976c.jpg` | [Unsplash](https://images.unsplash.com/photo-1544145945-f90425340c7e) |
| Infusión de hierbas | `a1633500-32da-4393-9795-96dac7190d76.jpg` | [Unsplash](https://images.unsplash.com/photo-1576092768241-dec231879fc3) |
| Chai latte artesanal | `4df45958-ca96-403c-b903-60fad6786a3b.jpg` | [Unsplash](https://images.unsplash.com/photo-1544787219-7f47ccb76574) |
| Panini de queso y tomate | `a5ee0a7d-c5f3-4790-824c-eaa0db63bf71.jpg` | [Unsplash](https://images.unsplash.com/photo-1528735602780-2552fd46c7af) |
| Sándwich de pollo crocante | `25e938bc-1484-4296-9496-95b31a33de4d.jpg` | [Unsplash](https://images.unsplash.com/photo-1606755962773-d324e0a13086) |
| Bowl verde con palta y huevo | `4a190f2b-19d0-4cc8-a38a-c29ccc71c1a5.jpg` | [Unsplash](https://images.unsplash.com/photo-1490645935967-10de6ba17061) |
| Tostadas francesas con frutos rojos | `7f50d6b0-ce61-4ce0-97d7-9d65317e89f3.jpg` | [Unsplash](https://images.unsplash.com/photo-1484723091739-30a097e8f929) |
| Galleta de chocolate | `e12beda9-0f38-48fe-8542-41db015bd631.jpg` | [Unsplash](https://images.unsplash.com/photo-1499636136210-6f4ee915583e) |
| Brownie de cacao y nuez | `4cc23ea9-855f-46c1-9634-77e4178f369d.jpg` | [Unsplash](https://images.unsplash.com/photo-1606313564200-e75d5e30476c) |
