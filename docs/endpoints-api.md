# API de Cafetería Las Rosas

La API REST usa el prefijo `/api`.

## Autenticación y usuarios

| Método | Ruta | Acceso |
|---|---|---|
| `GET` | `/api/auth/csrf` | Público; emite token CSRF y cookie `XSRF-TOKEN`. |
| `POST` | `/api/auth/register` | Público con token CSRF; crea siempre un CLIENTE. |
| `POST` | `/api/auth/login` | Público con token CSRF; devuelve usuario y establece cookie JWT HttpOnly. |
| `POST` | `/api/auth/logout` | Autenticado y con token CSRF; vence la cookie JWT. |
| `GET` | `/api/auth/me` | Autenticado; devuelve datos públicos y rol. |
| `POST` | `/api/usuarios/personal` | Solo ADMIN con token CSRF; crea una cuenta PERSONAL y cifra la contraseña. |

Las operaciones de escritura requieren el encabezado `X-XSRF-TOKEN` que corresponde a la cookie `XSRF-TOKEN`. La cookie `AUTH_TOKEN` contiene el JWT y no es accesible desde JavaScript.

Al iniciar el backend se crean, si no existen, las cuentas iniciales `admin@cafeteria.com` y `jhordan@cafeteria.com`. Sus contraseñas iniciales son `admin123` y `per123`, respectivamente; se almacenan con BCrypt. Cambia ambas contraseñas después de la primera autenticación.

## Categorías y productos

| Método | Ruta | Acceso | Función |
|---|---|---|---|
| `GET` | `/api/categorias` | CLIENTE, PERSONAL, ADMIN | Lista categorías activas; ADMIN ve todas. |
| `GET` | `/api/categorias/{id}` | CLIENTE, PERSONAL, ADMIN | Consulta una categoría; las inactivas solo son visibles para ADMIN. |
| `POST` | `/api/categorias` | ADMIN | Crea una categoría. |
| `PUT` | `/api/categorias/{id}` | ADMIN | Actualiza nombre y descripción. |
| `PATCH` | `/api/categorias/{id}/estado` | ADMIN | Cambia `activo`, con cuerpo `{"activo": false}`. |
| `GET` | `/api/productos` | CLIENTE, PERSONAL, ADMIN | Lista disponibles; ADMIN ve todos. |
| `GET` | `/api/productos/disponibles` | CLIENTE, PERSONAL, ADMIN | Lista productos disponibles de categorías activas. |
| `GET` | `/api/productos/{id}` | CLIENTE, PERSONAL, ADMIN | Consulta un producto; los no disponibles solo son visibles para ADMIN. |
| `GET` | `/api/productos/categoria/{categoriaId}` | CLIENTE, PERSONAL, ADMIN | Lista disponibles de la categoría; ADMIN ve todos los productos de esa categoría. |
| `POST` | `/api/productos` | ADMIN | Registra un producto asociado a una categoría activa; acepta JSON o `multipart/form-data` con archivo local. |
| `PUT` | `/api/productos/{id}` | ADMIN | Actualiza producto y categoría asociada; acepta JSON o `multipart/form-data` para reemplazar su imagen. |
| `PATCH` | `/api/productos/{id}/disponibilidad` | ADMIN | Cambia `disponible`, con cuerpo `{"disponible": false}`. |

Las operaciones de escritura requieren el encabezado `X-XSRF-TOKEN` que corresponde a la cookie `XSRF-TOKEN`; las operaciones protegidas requieren además la cookie `AUTH_TOKEN`.

Para crear un producto sin imagen se puede enviar JSON:

```json
{
  "categoriaId": 1,
  "nombre": "Nombre del producto",
  "descripcion": "Descripción opcional",
  "precio": 10.50,
  "imagenUrl": null,
  "disponible": true
}
```

Para guardar una imagen local, envía `multipart/form-data` con una parte `producto` de tipo `application/json` (los mismos campos del ejemplo) y una parte `imagen` con el archivo JPG, PNG o WebP (máximo 5 MB). El servidor guarda el archivo en `UPLOAD_DIR`, expone la imagen en `/uploads/product-images/{nombre-generado}` y almacena esa ruta en `imagenUrl`. No se debe establecer manualmente el encabezado `Content-Type` del multipart; el cliente debe generar su límite (`boundary`).

## Administración

La creación de cuentas PERSONAL está implementada. La consulta de usuarios y la gestión restringida de roles siguen reservadas para fases posteriores.

## Pedidos (Fase 5)

| Método | Ruta | Acceso | Función |
|---|---|---|---|
| `POST` | `/api/pedidos` | CLIENTE con JWT y CSRF | Confirma el carrito y crea pedido y detalles. |
| `GET` | `/api/pedidos/mis-pedidos` | CLIENTE autenticado | Lista los pedidos del usuario autenticado. |
| `GET` | `/api/pedidos/{id}` | CLIENTE, PERSONAL, ADMIN | CLIENTE solo ve sus pedidos; PERSONAL y ADMIN pueden consultar pedidos operativos. |

El cuerpo de creación solo admite productos, cantidades y observaciones. El backend obtiene el cliente autenticado y los precios vigentes de PostgreSQL, calcula subtotales y total, fija el estado `RECIBIDO` y registra el historial inicial en una transacción. Los totales y precios enviados por el cliente no forman parte del contrato de solicitud.

Ejemplo de creación:

```json
{
  "detalles": [
    { "productoId": 1, "cantidad": 2 },
    { "productoId": 3, "cantidad": 1 }
  ],
  "observaciones": "Sin azúcar"
}
```

El carrito de Angular es temporal y se conserva en `localStorage` del navegador. Se envían al backend únicamente el identificador de cada producto y la cantidad; el carrito se vacía solo cuando la creación del pedido responde correctamente. Los importes mostrados en el carrito son informativos, no valores confiables para persistir una compra.

## Pagos (Fase 6)

| Método | Ruta | Acceso | Función |
|---|---|---|---|
| `POST` | `/api/pedidos/{pedidoId}/pagos` | CLIENTE con JWT y CSRF | Registra un pago `PENDIENTE` en un pedido propio. |
| `GET` | `/api/pedidos/{pedidoId}/pagos` | CLIENTE propietario, PERSONAL, ADMIN | Lista los pagos del pedido según autorización. |
| `PATCH` | `/api/pagos/{pagoId}/verificar` | PERSONAL, ADMIN con JWT y CSRF | Confirma el pago pendiente y registra verificador y fecha. |
| `PATCH` | `/api/pagos/{pagoId}/anular` | PERSONAL, ADMIN con JWT y CSRF | Marca un pago pendiente como anulado sin eliminarlo. |

Los únicos métodos permitidos son `EFECTIVO`, `YAPE` y `TRANSFERENCIA` (transferencia bancaria). Los pagos pendientes reservan saldo para evitar que varios registros excedan el total; solo `PAGADO` se considera confirmado. Verificar o anular un pago no cambia automáticamente el estado del pedido. El bloqueo pesimista del pedido serializa los registros y verificaciones concurrentes.

Ejemplo de registro de pago:

```json
{
  "metodoPago": "YAPE",
  "monto": 20.00,
  "referencia": "referencia-opcional"
}
```

La migración Flyway V2 reemplaza cualquier valor histórico `PLIN` por `TRANSFERENCIA` y cambia la restricción de la tabla para permitir solo los tres métodos vigentes. Conserva los demás campos e identificadores del registro.

## Seguimiento de pedidos (Fase 7)

| Método | Ruta | Acceso | Función |
|---|---|---|---|
| `GET` | `/api/pedidos/operativos` | PERSONAL, ADMIN | Lista pedidos en `RECIBIDO`, `EN_PREPARACION` o `LISTO`, con sus detalles, ordenados por fecha. |
| `PATCH` | `/api/pedidos/{id}/estado` | PERSONAL, ADMIN con JWT y CSRF | Actualiza al siguiente estado permitido y registra el movimiento y responsable. |
| `PATCH` | `/api/pedidos/{id}/cancelar` | PERSONAL, ADMIN con JWT y CSRF | Cancela un pedido abierto; requiere motivo en `observacion`. |
| `GET` | `/api/pedidos/{id}/historial` | CLIENTE propietario, PERSONAL, ADMIN | Consulta los cambios históricos en orden cronológico. |

Transiciones permitidas: `RECIBIDO` → `EN_PREPARACION` o `CANCELADO`; `EN_PREPARACION` → `LISTO` o `CANCELADO`; `LISTO` → `ENTREGADO` o `CANCELADO`. `ENTREGADO` y `CANCELADO` son estados finales. El backend rechaza otras transiciones. El paso a `EN_PREPARACION` requiere que los pagos con estado `PAGADO` cubran el total del pedido; pagos pendientes o anulados no cuentan.

Ejemplo para cambiar el estado:

```json
{
  "estado": "EN_PREPARACION",
  "observacion": "Preparación iniciada"
}
```

Ejemplo para cancelar:

```json
{
  "observacion": "Cancelación solicitada por el cliente"
}
```

El cambio de estado y su registro histórico se guardan en la misma transacción. La interfaz Angular permite al cliente seguir sus pedidos y consultar el historial; PERSONAL/ADMIN disponen de la consola operativa para pedidos, pagos y estados. La edición de categorías y productos está restringida al panel ADMIN.

## Interfaz web (Fase 8)

Angular consume los endpoints REST documentados en las secciones anteriores. Rutas principales:

| Ruta | Acceso | Función |
|---|---|---|
| `/` | Público | Presentación y acceso al menú/autenticación. |
| `/login`, `/registro` | Público | Inicio de sesión y registro de CLIENTE. |
| `/menu` | CLIENTE, PERSONAL, ADMIN autenticados | Menú real y selección de productos. |
| `/carrito` | CLIENTE | Revisión de cantidades y confirmación de pedido. |
| `/cliente/pedidos` | CLIENTE | Pedidos propios, historial y registro/consulta de pagos. |
| `/personal/pedidos` | PERSONAL, ADMIN | Pedidos operativos, verificación de pagos y cambios de estado. |
| `/admin/catalogo` | ADMIN | Gestión de categorías/productos y creación de cuentas PERSONAL. |

El inicio de sesión conserva la estrategia existente de cookie JWT HttpOnly y CSRF. TRANSFERENCIA se muestra como transferencia bancaria manual; no existe pasarela ni cobro automático con tarjeta.
