# Cafetería Las Rosas

Sistema web para pedidos de la cafetería. El repositorio separa la API Spring Boot, la aplicación Angular y la documentación del modelo de datos.

## Requisitos

- JDK 22 (el backend configura Java 22; se verificó con JDK 22.0.2).
- Node.js compatible con Angular CLI 22 (el entorno de desarrollo comprobado usa Node.js 24).
- PostgreSQL 17 o compatible.
- Maven Wrapper incluido en `backend`; no es necesario instalar Maven globalmente.

En el entorno actual están disponibles Java/JDK 22.0.2, Node.js 24.17.0, Angular CLI global 22.0.3 y PostgreSQL 17.6. El proyecto Angular resuelve Angular CLI 22.2.x mediante `package-lock.json`. No se encontró Maven global; el wrapper del proyecto descarga Maven cuando se ejecute por primera vez.

## Preparar PostgreSQL

1. Crea una base de datos vacía llamada `cafeteria_las_rosas` desde pgAdmin o con una cuenta que tenga permiso para crear bases de datos:

   ```sql
   CREATE DATABASE cafeteria_las_rosas;
   ```

2. Configura las variables para la terminal de PowerShell que utilizarás para iniciar el backend. Sustituye el valor de contraseña local por el tuyo; no lo guardes en el repositorio:

   ```powershell
   $env:DB_URL = "jdbc:postgresql://localhost:5432/cafeteria_las_rosas"
   $env:DB_USERNAME = "postgres"
   $env:DB_PASSWORD = "<tu contraseña local de PostgreSQL>"
   $env:SERVER_PORT = "8081"
   $env:FRONTEND_URL = "http://localhost:4200"
   $env:AUTH_COOKIE_SECURE = "false"
   $env:AUTH_COOKIE_SAME_SITE = "Lax"
   $env:ADMIN_INITIAL_PASSWORD = "<contraseña inicial para el administrador>"
   ```

Genera y guarda una clave JWT aleatoria en las variables persistentes de tu usuario de Windows (ejecútalo una sola vez; no la guardes en el repositorio):

```powershell
$jwtBytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$jwtSecret = "base64:" + [Convert]::ToBase64String($jwtBytes)
[Environment]::SetEnvironmentVariable("JWT_SECRET", $jwtSecret, "User")
```

La clave persistida permanece disponible aunque cierres la terminal o reinicies Windows. Después de crearla, reinicia VS Code una vez para que los terminales nuevos hereden la variable; luego ejecuta el backend normalmente. No vuelvas a generar la clave en cada inicio, porque cambiarla invalida los JWT existentes. La aplicación toma las variables del entorno; `.env.example` es solo una referencia y Spring Boot no carga automáticamente archivos `.env`. La migración Flyway `V1__initial_schema.sql` crea las ocho tablas y los roles iniciales al iniciar el backend. Hibernate está configurado para validar el esquema, no modificarlo.

Al iniciar, si falta `admin@cafeteria.com`, la aplicación crea la cuenta ADMIN usando BCrypt y `ADMIN_INITIAL_PASSWORD`. Si la cuenta ya existe, no modifica sus datos ni su contraseña. La variable es obligatoria solo cuando la cuenta todavía no existe; no se registra su valor en logs ni se almacena en archivos versionados.

## Ejecutar backend

Desde la raíz del proyecto:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

El backend escucha en `http://localhost:8081` por defecto. Cambia `SERVER_PORT` para usar otro puerto. Debes configurar una `JWT_SECRET` aleatoria de al menos 32 bytes; sin ella la aplicación no inicia. También se requieren credenciales válidas y la base de datos creada.

## Ejecutar frontend

En otra terminal:

```powershell
cd frontend
npm start -- --port 4200
```

El puerto de Angular se puede cambiar por separado, por ejemplo `npm start -- --port 4300`. Para compilar:

```powershell
npm run build
```

La URL base de la API está centralizada en `frontend/src/environments/environment.ts`. El interceptor Angular envía cookies con credenciales y adjunta el token CSRF no HttpOnly a las solicitudes de escritura. Bootstrap 5 se incluye como dependencia npm y su hoja de estilos se carga desde Angular.

## Variables de entorno

Consulta `.env.example` para ver los nombres utilizados. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `SERVER_PORT` configuran la base de datos y el backend; `UPLOAD_DIR` es el directorio local donde se guardan las imágenes (por defecto, `uploads/product-images` relativo a la carpeta desde la que se inicia Spring Boot); `FRONTEND_URL` debe coincidir exactamente con el origen Angular permitido por CORS; `JWT_SECRET` debe ser una clave aleatoria de al menos 32 bytes; `JWT_EXPIRATION` está en milisegundos. En producción usa HTTPS y `AUTH_COOKIE_SECURE=true`; si frontend y API están en sitios distintos, configura también `AUTH_COOKIE_SAME_SITE=None` (requiere Secure). No hay claves JWT reales en archivos versionados.

Las imágenes de productos aceptan JPG, PNG y WebP de hasta 5 MB. Se guardan en `UPLOAD_DIR`; PostgreSQL conserva la ruta de cada archivo en la columna existente `productos.imagen_url`. Si cambias `UPLOAD_DIR` entre ejecuciones, conserva también los archivos para que las imágenes anteriores sigan disponibles.

## Estado de implementación

Las Fases 1 a 7 incluyen estructura, backend REST, persistencia PostgreSQL, autenticación con JWT en cookie HttpOnly, BCrypt, CSRF, permisos por rol, catálogo, pedidos, pagos y seguimiento de estados.

La Fase 8 agrega la interfaz Angular responsive con páginas de inicio, registro e inicio de sesión, menú, carrito, pedidos y pagos del cliente, consola operativa para PERSONAL/ADMIN y gestión administrativa del catálogo y cuentas PERSONAL. El frontend consume el backend real; los guards de Angular complementan, pero no reemplazan, la autorización Spring Security.

Para probar el frontend completo, inicia PostgreSQL, configura las variables requeridas e inicia primero el backend. Después, en otra terminal, inicia Angular:

```powershell
cd frontend
npm start
```

La aplicación se abre en `http://localhost:4200`. Accede con una cuenta CLIENTE para el menú, carrito y seguimiento; PERSONAL para operaciones y verificación de pagos; o ADMIN para operaciones, catálogo y creación de cuentas PERSONAL.

## Probar autenticación en Postman

1. Envía `GET http://localhost:8081/api/auth/csrf`. Conserva la cookie `XSRF-TOKEN` y copia `token` y `headerName` de la respuesta.
2. Para `POST /api/auth/register` y `POST /api/auth/login`, envía `Content-Type: application/json`, el encabezado `X-XSRF-TOKEN` con el token recibido y conserva las cookies del mismo host.
3. Registro: `{"nombres":"Ana","apellidos":"Prueba","correo":"ana@example.com","telefono":"999111222","password":"CafeSeguro2026!"}`. El rol siempre se asigna en el servidor como `CLIENTE`.
4. Login: `{"correo":"ana@example.com","password":"CafeSeguro2026!"}`. Postman recibe la cookie HttpOnly `AUTH_TOKEN`; el JWT no aparece en el JSON.
5. Envía `GET /api/auth/me` con la cookie conservada. Para cerrar sesión, envía `POST /api/auth/logout` con las cookies y el encabezado CSRF; la respuesta vence la cookie de autenticación.
6. El registro/login/logout necesitan CSRF. Las credenciales incorrectas responden 401; los usuarios inactivos también responden 401; las rutas administrativas requieren rol ADMIN y las operativas PERSONAL o ADMIN.

El logout elimina la cookie del cliente; como el backend no guarda sesiones ni listas de revocación, una copia robada del JWT seguiría siendo válida hasta su expiración (15 minutos por defecto).

## Documentación

- [Modelo de datos](docs/modelo-datos.md)
- [API planificada](docs/endpoints-api.md)
