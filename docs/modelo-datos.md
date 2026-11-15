# Modelo de datos inicial

La migración `backend/src/main/resources/db/migration/V1__initial_schema.sql` crea el esquema PostgreSQL de Cafetería Las Rosas. La base de datos debe llamarse `cafeteria_las_rosas` por defecto; se puede cambiar mediante `DB_URL`.

| Tabla | Propósito y relaciones |
|---|---|
| `roles` | Catálogo de roles; inicia con `CLIENTE`, `PERSONAL` y `ADMIN`. |
| `usuarios` | Cuentas asociadas a un rol; el hash BCrypt se almacena en `password_hash`. El correo es único. |
| `categorias` | Categorías administrables; nombre único y estado activo. |
| `productos` | Productos asociados a categorías, con precio `NUMERIC(10,2)` no negativo y disponibilidad. |
| `pedidos` | Pedidos de un usuario, con estado, subtotal, total y fecha. |
| `detalle_pedido` | Productos y cantidades del pedido; conserva el precio unitario histórico. |
| `pagos` | Uno o más pagos por pedido; valida los métodos y estados definidos. `registrado_por` y `fecha_pago` pueden quedar nulos mientras el pago está pendiente, y se completarán al verificarlo. |
| `historial_estado_pedido` | Cambios de estado vinculados al pedido y al usuario que los registra. |

Las claves foráneas usan `ON DELETE RESTRICT` para proteger el historial. Se incluyen índices sobre relaciones y consultas operativas; las cantidades, importes, métodos de pago y estados tienen restricciones en la base de datos. Los roles iniciales se insertan en la migración.

Los requisitos de al menos un detalle por pedido, coherencia entre totales y detalles, pagos confirmados que no superen el total y transiciones válidas son reglas de negocio que se implementarán y probarán en las fases del backend; no pueden imponerse con restricciones de fila simples en esta migración.

## Mapeo y lógica de backend

Las entidades JPA de `backend/src/main/java/pe/edu/cafeterialasrosas/entity` mantienen las claves foráneas mediante relaciones diferidas y no aplican cascadas de borrado. Los importes se representan con `BigDecimal`; los estados y métodos de pago se guardan como texto para coincidir con las restricciones de la migración.

Los repositorios cubren búsquedas por correo, categorías y productos activos, pedidos por usuario/estado, detalles, pagos e historial. Los servicios trabajan con DTO y aplican cálculos del pedido en servidor, comprobación de disponibilidad, transiciones de estado, registro del historial y verificación de pagos con bloqueo pesimista del pedido. Las mutaciones de categorías y productos requieren un actor ADMIN; la preparación, verificación y anulación de pagos requieren PERSONAL o ADMIN. Los usuarios de contexto se vuelven a cargar desde la base de datos antes de aplicar estas reglas.

El registro público, la comprobación de correo único, BCrypt y la autenticación JWT en cookie HttpOnly se implementaron en la Fase 3. Los controladores REST de autenticación son la única superficie HTTP disponible hasta completar las fases siguientes.
