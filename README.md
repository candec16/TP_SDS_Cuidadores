# RedCuidar Córdoba — Plataforma Web de Perfiles de Cuidadores

Trabajo Práctico Integrador de **Seguridad en el Desarrollo de Software**  
**Universidad Tecnológica Nacional – Facultad Regional Córdoba (UTN FRC)** · Comisión **4K2A**

### Integrantes
* **Cardoso, Candela Florencia** – Legajo `93092`
* **Paez, Areli** – Legajo `400802`
* **Quiroga, Lucía** – Legajo `402597`

---

## 1. Propósito del Proyecto

Aplicación web de tres capas donde profesionales del cuidado de personas mayores pueden registrarse y publicar su perfil profesional (nombre, apellido, zona de residencia, descripción, estudios e intereses), permitiendo la consulta pública filtrada por zona sin necesidad de iniciar sesión.

El repositorio funciona como entorno académico local para analizar el ciclo completo de seguridad en el desarrollo:
* **Rama `main` ( Estado auditable / vulnerable ):** Aplicación funcional con tres debilidades de seguridad documentadas en el código fuente para su análisis técnico.
* **Rama `remediado` ( Estado mitigado / seguro ):** Parte de `main` e incorpora los controles defensivos en commits atómicos separados por cada debilidad corregida, facilitando la comparación directa mediante `git diff main..remediado`.

> **Aviso de uso académico:** Este proyecto está diseñado exclusivamente para ejecutarse en entornos locales (`localhost`) con datos ficticios precargados.

---

## 2. Arquitectura y Tecnologías

```text
[ Navegador: React + Vite (:5173) ]  ── HTTP / JSON + JWT ──►  [ API REST: Spring Boot + Security (:8080) ]  ── JPA / JDBC ──►  [ PostgreSQL (:5432) ]
```

* **Frontend (`frontend/`):** React 18, Vite, JavaScript (ES6+) y CSS3 puro (liviano en consumo de memoria RAM).
* **Backend (`backend/`):** Java 17/21, Maven Wrapper, Spring Boot, Spring Web, Spring Data JPA (Hibernate), Spring Security (filtro JWT `STATELESS` y hash `BCrypt`), Jakarta Validation y `jjwt`.
* **Base de datos (`database/`):** PostgreSQL (`cuidadores_db`) con las tablas `users` y `profiles`.

---

## 3. Ejecución Local (Sin Docker — Recomendado para equipos con 8 GB de RAM)

### Requisitos previos
* **Java JDK 17 o 21**
* **Node.js 18+** y `npm`
* **PostgreSQL** ejecutándose localmente en el puerto `5432` con una base de datos vacía llamada **`cuidadores_db`**.

*(Opcional: si algún integrante prefiere usar contenedores en lugar de PostgreSQL nativo, se incluye `docker-compose.yml` con `docker compose up -d`).*

### Paso 1: Configurar credenciales de BD (solo si difieren del valor por defecto)
Por defecto, `backend/src/main/resources/application.properties` se conecta a `jdbc:postgresql://localhost:5432/cuidadores_db` con usuario `postgres`. Podés definir variables de entorno si tu clave local es distinta:
```powershell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="tu_password_local"
```

### Paso 2: Iniciar el Backend (Puerto `8080`)
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
Al iniciar por primera vez, Hibernate crea las tablas `users` y `profiles` automáticamente y `DataInitializer` carga **4 perfiles ficticios** de distintas zonas de Córdoba.

### Paso 3: Iniciar el Frontend (Puerto `5173`)
En otra terminal:
```powershell
cd frontend
npm install
npm run dev
```
Abrir en el navegador: **http://localhost:5173**

---

## 4. Endpoints de la API REST

| Método | Ruta | Acceso | Descripción |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/auth/register` | Público | Registra un usuario (hash BCrypt) y emite un token JWT. |
| `POST` | `/api/auth/login` | Público | Autentica credenciales y devuelve un token JWT. |
| `GET` | `/api/profiles?zona=` | Público | Lista todos los perfiles o filtra por zona de residencia. |
| `GET` | `/api/profiles/{id}` | Público | Obtiene el detalle público de un perfil por su identificador. |
| `POST` | `/api/profiles` | Protegido (JWT) | Crea el perfil propio asociado al usuario del token. |
| `PUT` | `/api/profiles/{id}` | Protegido (JWT) | Edita los datos de un perfil existente. |

---

## 5. Matriz de Auditoría de Seguridad (`main` vs. `remediado`)

El detalle línea por línea del código fuente se encuentra documentado en **[GUIA_CODIGO_Y_VULNERABILIDADES.md](./GUIA_CODIGO_Y_VULNERABILIDADES.md)**.

| Categoría / CWE | Ubicación en el código | Causa raíz en `main` | Mitigación aplicada en `remediado` |
| :--- | :--- | :--- | :--- |
| **5.1 Inyección SQL**<br>`CWE-89` · OWASP A03 | `ProfileService.buscarPerfiles()` | Concatenación directa del parámetro `zona` en una sentencia SQL nativa (`createNativeQuery`). | Consulta parametrizada mediante método derivado de Spring Data JPA (`findByZonaContainingIgnoreCase`) y validación de entrada. |
| **5.2 IDOR (Control de acceso roto)**<br>`CWE-639` · OWASP A01 | `ProfileService.actualizarPerfil()` | Se exige un JWT válido (autenticación), pero el servidor no comprueba que el perfil pertenezca al usuario del token (autorización). | Verificación estricta de propiedad en el servidor comparando el dueño del perfil con el usuario autenticado (`403 Forbidden` si no coinciden). |
| **5.3 (a) Secreto JWT expuesto y expiración extensa**<br>`CWE-798` / `CWE-321` · OWASP A07 | `application.properties` y `JwtService` | Clave de firma `HS256` predecible escrita en el repositorio y validez de 24 horas. | Externalización obligatoria del secreto en variable de entorno (`JWT_SECRET`), validación de longitud mínima y reducción del tiempo de vida del token (30 min). |
| **5.3 (b) Ausencia de límite de intentos y clave débil**<br>`CWE-307` / `CWE-521` · OWASP A07 | `AuthService.login()` y `RegisterRequest` | Login sin protección ante intentos fallidos repetidos y registro sin requisitos de complejidad de contraseña. | Bloqueo temporal tras 5 intentos fallidos (`LoginAttemptService` devolviendo `429 Too Many Requests`) y política mínima de contraseñas con `@Pattern` y `@Size(min=8)`. |
| **5.3 (c) Enumeración de usuarios**<br>`CWE-204` · OWASP A07 | `AuthService.login()` | Mensajes de error diferenciados para correo inexistente vs. contraseña inválida. | Respuesta uniforme `401 Unauthorized` con mensaje genérico `"Credenciales inválidas"` para cualquier fallo de autenticación. |

---

## 6. Comparación de Parches para la Defensa

Para inspeccionar exactamente qué líneas mitigan cada vulnerabilidad entre ambas ramas:
```powershell
git diff main..remediado
```
O bien revisando los commits individuales dentro de la rama `remediado`:
```powershell
git log main..remediado --oneline
```
