# Guía de Estudio del Código y Mapa de Vulnerabilidades (TPI - SDS 4K2A)

**Materia:** Seguridad en el Desarrollo de Software – UTN FRC  
**Comisión:** 4K2A  
**Integrantes:** Cardoso, Candela Florencia (93092) · Paez, Areli (400802) · Quiroga, Lucía (402597)

> Este documento es nuestra **bitácora viva del proyecto**. Explica qué hace cada archivo, por qué se tomó cada decisión técnica y en qué archivo y línea exacta está cada vulnerabilidad para poder estudiar el código y defenderlo en vivo frente a la cátedra.

---

## Estado de Avance del Proyecto

- [x] **Paso 1:** Configuración base (`application.properties`, `.gitignore`, conexión a `cuidadores_db`), entidades JPA (`User`, `Profile`), repositorios y carga automática de datos de prueba (`DataInitializer`).
- [x] **Paso 2:** Seguridad con Spring Security + JWT + BCrypt y **Vulnerabilidad 5.3 (Broken Authentication)**.
- [x] **Paso 3:** API REST de Perfiles con **Vulnerabilidad 5.1 (SQL Injection)** y **Vulnerabilidad 5.2 (IDOR)** + test automático en JUnit.
- [x] **Paso 4:** Frontend completo en React + Vite (puerto `5173`), liviano y sin librerías pesadas para cuidar la memoria RAM.
- [x] **Paso 5:** Documentación técnica completa (`README.md`, matriz de auditoría CWE/OWASP y guía conceptual para la defensa oral).
- [ ] **Paso 6:** Creación de la rama `remediado` con 3 commits separados (uno por cada CWE corregido).

---

## 1. Cómo viaja una petición en nuestro Backend (Flujo completo)

Cuando el navegador o Postman hace un pedido HTTP a `http://localhost:8080`, el camino dentro de `backend/src/main/java/com/utn/cuidadores_api/` es siempre este:

```text
Pedido HTTP (Navegador / Postman)
   │
   ▼
1. JwtAuthenticationFilter (security/)
   ├── ¿Trae cabecera "Authorization: Bearer <token>"?
   │     ├── Sí -> Verifica firma con JwtService y marca al usuario como autenticado.
   │     └── No -> Sigue adelante como usuario anónimo.
   ▼
2. SecurityConfig (security/)
   ├── ¿Es /api/auth/** o GET /api/profiles/**? -> Pasa libre (Público).
   └── ¿Es POST o PUT /api/profiles/**? -> Exige estar autenticado (si no, devuelve 401 JSON).
   ▼
3. Controller (controller/AuthController o ProfileController)
   ├── Recibe el JSON en un DTO de entrada (dto/*Request) y valida campos obligatorios (@Valid).
   ▼
4. Service (service/AuthService o ProfileService)
   ├── Ejecuta la lógica de negocio (acá están las 3 vulnerabilidades en la rama 'main').
   ▼
5. Repository / EntityManager (repository/*Repository + model/*)
   ├── Se comunica con PostgreSQL ('cuidadores_db') en las tablas 'users' y 'profiles'.
   ▼
6. DTO de salida (dto/*Response)
   └── Devuelve JSON limpio al cliente sin exponer hashes de contraseñas.
```

---

## 2. Mapa Archivo por Archivo del Backend (Qué hace cada uno y por qué)

### Configuración general
| Archivo | Qué hace y por qué está así |
| :--- | :--- |
| `backend/pom.xml` | Declara las dependencias Maven: `webmvc` (API REST), `data-jpa` (Hibernate/BD), `security` (BCrypt y filtros), `validation` (`@NotBlank`, `@Email`), `postgresql` (driver JDBC) y `jjwt` (firma y lectura de tokens JWT). |
| `backend/src/main/resources/application.properties` | Configura puerto `8080`, conexión a `cuidadores_db` usando `${DB_PASSWORD:1607}` (usa `1607` por defecto en tu PC sin Docker, pero permite que tus compañeras usen otra clave con variable de entorno), `ddl-auto=update` (crea tablas solas) y el secreto JWT. |
| `.gitignore` | Evita subir carpetas pesadas (`node_modules/`, `target/`), pero **sí permite subir** `mvnw` y `.mvn/` para que cualquiera corra el proyecto sin instalar Maven manualmente. |
| `database/schema_referencia.sql` | Documenta en SQL puro las tablas `users` y `profiles` como apoyo para el informe. |
| `docker-compose.yml` | Archivo **opcional** para quien prefiera Docker; en tu PC con 8 GB de RAM trabajamos directo contra PostgreSQL nativo de Windows (~40 MB de RAM). |

### Paquete `model/` (Tablas en PostgreSQL)
* **`model/User.java`**: Representa la tabla `users` *(se usa `@Table(name = "users")` en plural porque `user` es palabra reservada de PostgreSQL)*. Guarda `id`, `email` (único) y `password` (cifrada con BCrypt).
* **`model/Profile.java`**: Representa la tabla `profiles`. Tiene relación `@OneToOne` con `User` mediante `user_id` único (1 cuenta = 1 perfil propio) y los campos de la propuesta: `nombre`, `apellido`, `zona`, `descripcion`, `intereses`, `estudios`.

### Paquete `repository/` (Acceso a datos con Spring Data JPA)
* **`repository/UserRepository.java`**: Permite buscar usuarios por email (`findByEmail`) y saber si un correo ya está registrado (`existsByEmail`).
* **`repository/ProfileRepository.java`**: Permite consultar si un usuario ya creó su perfil (`existsByUserId`, `findByUserEmail`). *(En la rama `remediado` agregaremos acá `findByZonaContainingIgnoreCase` para mitigar SQL Injection).*

### Paquete `dto/` (Objetos de transferencia de datos)
> **¿Por qué usamos DTOs?** Buena práctica de seguridad: nunca devolvemos la entidad `Profile` directo al exterior porque adentro tiene el objeto `User` con su campo `password` (el hash BCrypt). `ProfileResponse` filtra y expone únicamente los datos públicos necesarios.

* **`dto/RegisterRequest.java`**: Datos que entran en `POST /api/auth/register` (`email`, `password`).
* **`dto/LoginRequest.java`**: Datos que entran en `POST /api/auth/login` (`email`, `password`).
* **`dto/AuthResponse.java`**: Respuesta de login/registro exitoso (`token`, `userId`, `email`).
* **`dto/ProfileRequest.java`**: Datos para crear o editar un perfil, con límites de longitud (`@Size`) y campos obligatorios (`@NotBlank`).
* **`dto/ProfileResponse.java`**: Datos públicos que devuelve la API para cada tarjeta de cuidador (`id`, `userId`, `userEmail`, `nombre`, `apellido`, `zona`, `descripcion`, `intereses`, `estudios`).

### Paquete `security/` y `config/`
* **`security/JwtService.java`**: Crea tokens JWT firmados con `HS256` poniendo el `email` del usuario dentro del campo estándar `sub` (*subject*), y verifica la firma cuando vuelven en futuros pedidos.
* **`security/JwtAuthenticationFilter.java`**: Filtro que se ejecuta una vez por pedido (`OncePerRequestFilter`). Lee `Authorization: Bearer ...` y autentica al usuario en el contexto de Spring Security.
* **`security/SecurityConfig.java`**: Configura la seguridad del servidor:
  1. `SessionCreationPolicy.STATELESS`: el servidor no guarda sesiones en memoria.
  2. `BCryptPasswordEncoder`: algoritmo seguro con *salt* aleatorio para contraseñas.
  3. `CORS`: solo acepta pedidos del navegador desde `http://localhost:5173`.
  4. Rutas públicas vs. rutas que exigen JWT.
* **`config/GlobalExceptionHandler.java`**: Captura errores de negocio y validaciones para responder siempre JSON prolijo `{"error": "mensaje claro"}`.
* **`config/DataInitializer.java`**: Al arrancar Spring Boot, si `cuidadores_db` está vacía, inserta automáticamente **4 usuarios y perfiles de Córdoba** para tener datos listos para mostrar.

---

## 3. Mapa Exacto de las 3 Vulnerabilidades Intencionales (Para la Defensa)

En la rama `main`, todas las vulnerabilidades tienen un bloque de comentario grande que empieza con `// VULNERABILIDAD INTENCIONAL ...` para encontrarlas rápido con `Ctrl + Shift + F` en VS Code.

---

### Vulnerabilidad 5.1: Inyección SQL (`CWE-89` / OWASP A03:2021 Injection)
* **Dónde está exactamente:**
  * Archivo: `backend/src/main/java/com/utn/cuidadores_api/service/ProfileService.java`
  * Método: `buscarPerfiles(String zona)` (**líneas 44 a 58**)
  * Endpoint HTTP: `GET /api/profiles?zona=...`
* **Código vulnerable actual (`main`):**
  ```java
  String sql = "SELECT * FROM profiles WHERE zona ILIKE '%" + zona + "%' OR zona = '" + zona + "'";
  List<Profile> resultados = entityManager.createNativeQuery(sql, Profile.class).getResultList();
  ```
* **Por qué falla:** Concatena texto no confiable del usuario directamente dentro del comando SQL en vez de enviarlo como parámetro separado (*Prepared Statement*). El motor PostgreSQL no puede distinguir dónde termina el dato y dónde empieza código SQL inyectado.
* **Qué pasa al atacar con `zzz' OR '1'='1`:**
  La consulta resultante en PostgreSQL se transforma en:
  ```sql
  SELECT * FROM profiles WHERE zona ILIKE '%zzz' OR '1'='1%' OR zona = 'zzz' OR '1'='1'
  ```
  Como la última condición `'1'='1'` es siempre verdadera para todas las filas, ignora el filtro de zona y devuelve **toda la tabla `profiles`**.
* **Cómo la vamos a corregir en la rama `remediado`:**
  Reemplazando la consulta nativa concatenada por el método derivado de Spring Data JPA `profileRepository.findByZonaContainingIgnoreCase(zona)` (que usa `PreparedStatement` con parámetros `?` internamente) y validando la longitud máxima del parámetro `zona`.

---

### Vulnerabilidad 5.2: IDOR – Referencia Directa Insegura a Objetos (`CWE-639` / OWASP A01:2021 Broken Access Control)
* **Dónde está exactamente:**
  * Archivo: `backend/src/main/java/com/utn/cuidadores_api/service/ProfileService.java`
  * Método: `actualizarPerfil(Long id, ProfileRequest request, String authenticatedEmail)` (**líneas 95 a 123**)
  * Endpoint HTTP: `PUT /api/profiles/{id}`
* **Por qué falla (Diferencia clave entre Autenticación y Autorización):**
  * **Autenticación** (*¿quién sos?*): Sí se cumple, porque `SecurityConfig` obliga a enviar un JWT válido.
  * **Autorización** (*¿tenés permiso sobre este objeto #id?*): **Falla**. El método recibe quién está logueado (`authenticatedEmail`), busca el perfil por `id` en la base, pero **nunca compara** si `profile.getUser().getEmail()` es igual a `authenticatedEmail`.
  * En el Frontend ocultamos el botón *Editar* en tarjetas ajenas, pero un atacante saltea la interfaz enviando el `PUT` directo a la API con su propio token hacia el `id` de otra persona (por ejemplo `PUT /api/profiles/1`).
* **Cómo la vamos a corregir en la rama `remediado`:**
  Verificando en el servidor antes de modificar:
  ```java
  if (!profile.getUser().getEmail().equalsIgnoreCase(authenticatedEmail)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tenés permiso para editar un perfil ajeno");
  }
  ```

---

### Vulnerabilidad 5.3: Autenticación Defectuosa – Broken Authentication (`OWASP A07:2021` / `CWE-287`)
Esta vulnerabilidad combina **tres debilidades reales** repartidas en el flujo de autenticación:

#### 5.3 (a) Secreto JWT débil y hardcodeado en el código + Expiración excesiva (`CWE-798`, `CWE-321`)
* **Dónde está:**
  * `backend/src/main/resources/application.properties` (**líneas 16 a 22**):
    `jwt.secret=clave_secreta_super_debil_1234567890` y `jwt.expiration=86400000` (24 horas).
  * `backend/src/main/java/com/utn/cuidadores_api/security/JwtService.java` (**líneas 20 a 32**).
* **Por qué falla:** Cualquiera con acceso de lectura al repositorio conoce la clave de firma `HS256`. Con ella puede fabricar un token falso poniendo `"sub": "ana.garcia@cuidadores.com"`, firmarlo y tomar control total de la cuenta de Ana sin saber su contraseña.
* **Corrección en `remediado`:** Leer el secreto obligatoriamente desde una variable de entorno externa (`${JWT_SECRET}`), exigir entropía/longitud fuerte y bajar la expiración a 30 minutos (`1800000` ms).

#### 5.3 (b) Sin política de contraseñas en registro (`CWE-521`) ni límite de intentos en login (`CWE-307`)
* **Dónde está:**
  * Política débil: `backend/src/main/java/com/utn/cuidadores_api/dto/RegisterRequest.java` (**líneas 14 a 21**) — solo tiene `@NotBlank`, dejando crear cuentas con contraseña `"1234"`.
  * Sin límite de intentos: `backend/src/main/java/com/utn/cuidadores_api/service/AuthService.java` (**líneas 52 a 57**) — permite probar cientos de claves por diccionario sin bloquear.
* **Corrección en `remediado`:** Exigir mínimo 8 caracteres con mayúscula, minúscula y número en `RegisterRequest`, e incorporar un `LoginAttemptService` que bloquee temporalmente la cuenta (`429 Too Many Requests`) tras 5 intentos fallidos consecutivos.

#### 5.3 (c) Enumeración de cuentas de usuario (`CWE-204`)
* **Dónde está:**
  * `backend/src/main/java/com/utn/cuidadores_api/service/AuthService.java` (**líneas 59 a 77**).
* **Por qué falla:** Si el correo no existe devuelve `"No existe ningún usuario registrado con el email: ..."`, y si el correo existe pero la clave está mal devuelve `"Contraseña incorrecta para el usuario ingresado"`. Un atacante usa esa diferencia para armar una lista de correos reales antes de lanzar fuerza bruta.
* **Corrección en `remediado`:** Devolver exactamente el mismo estado `401` y el mismo mensaje genérico `"Credenciales inválidas"` en ambos casos.

---

## 4. Cuentas Precargadas en `cuidadores_db` (Para probar y demostrar)

Al levantar el Backend, `DataInitializer.java` dejó creadas estas 4 cuentas (todas las claves están guardadas con hash **BCrypt** en la tabla `users`):

| ID Perfil | Nombre | Zona | Email | Contraseña real | Para qué nos sirve en la defensa |
| :---: | :--- | :--- | :--- | :--- | :--- |
| `1` | Ana García | Nueva Córdoba | `ana.garcia@cuidadores.com` | `cuidadores123` | **Víctima ideal** para: (1) ataque de diccionario/fuerza bruta, (2) falsificación de su JWT y (3) víctima de edición ajena por IDOR (`PUT /api/profiles/1`). |
| `2` | Carlos López | Alberdi | `carlos.lopez@cuidadores.com` | `Carlos2026!` | **Atacante autenticado** para demostrar IDOR (iniciamos sesión como Carlos y modificamos el perfil `#1` de Ana). |
| `3` | Marta Rodríguez | Cerro de las Rosas | `marta.rodriguez@cuidadores.com` | `MartaSegura99!` | Datos variados para mostrar el buscador por zona y el volcado completo con SQL Injection. |
| `4` | Lucas Fernández | Centro | `lucas.fernandez@cuidadores.com` | `1234` | Ejemplo vivo de cuenta con contraseña débil permitida por `CWE-521`. |

---

## 5. Mapa Archivo por Archivo del Frontend (`frontend/`)

El Frontend está construido con **React 18 + Vite** en el puerto fijo `5173` (el único origen habilitado por CORS en `SecurityConfig.java`). Se diseñó con **CSS3 puro** (sin librerías pesadas) para consumir el mínimo posible de RAM (~60 MB).

| Archivo | Qué hace y por qué está así |
| :--- | :--- |
| `frontend/vite.config.js` | Fija el servidor de desarrollo en el puerto `5173` (`strictPort: true`) para que coincida siempre con la lista blanca de CORS del Backend. |
| `frontend/src/services/api.js` | Centraliza todas las llamadas `fetch` a `http://localhost:8080/api`. Agrega automáticamente la cabecera `Authorization: Bearer <token>` en `POST` y `PUT`, y extrae los mensajes JSON `{"error": "..."}` del servidor. |
| `frontend/src/App.jsx` | Componente raíz. Maneja el estado con hooks (`useState`, `useEffect`, `useCallback`): sesión guardada en `localStorage`, lista de perfiles filtrados, sincronización del perfil propio (`ownProfile`) y apertura de ventanas modales. |
| `frontend/src/components/Navbar.jsx` | Barra superior: muestra si hay sesión activa (`email`) y adapta el botón principal entre *"Crear mi perfil"* (si la cuenta es nueva) y *"Editar mi perfil"* (si ya lo publicó). |
| `frontend/src/components/SearchBar.jsx` | Buscador público por zona. Usa `<datalist>` con sugerencias de barrios de Córdoba + botones rápidos, pero deja **texto libre** para poder escribir el ataque `zzz' OR '1'='1` en vivo desde el navegador. |
| `frontend/src/components/ProfileCard.jsx` | Tarjeta de cada cuidador/a. Muestra `ID #n` (útil para explicar qué ID atacamos en IDOR) y **solo dibuja el botón *"Editar perfil"* si `isOwner` es verdadero** (**líneas 54 a 67**), demostrando que ocultar un botón en el cliente no protege la API si el servidor no valida permisos. |
| `frontend/src/components/AuthModal.jsx` | Ventana modal para Registro e Inicio de Sesión. Muestra textualmente el error devuelto por el Backend (ideal para ver en pantalla la enumeración de cuentas en `main` vs. el mensaje unificado y el bloqueo `429` en `remediado`). |
| `frontend/src/components/ProfileFormModal.jsx` | Formulario único reutilizable tanto para dar de alta el perfil propio (`POST /api/profiles`) como para actualizarlo (`PUT /api/profiles/{id}`). |
| `frontend/src/styles.css` | Estilos modernos, responsivos y accesibles con variables CSS. |

---

## 6. Cómo levantar la Aplicación Completa (Backend + Frontend)

Necesitás dos terminales abiertas (por ejemplo, dividiendo la terminal en VS Code):

1. **Terminal 1 – Backend (Puerto 8080):**
   ```powershell
   cd D:\Facultad\2026\SDS\tpi\TP_SDS_Cuidadores\backend
   .\mvnw.cmd spring-boot:run
   ```

2. **Terminal 2 – Frontend (Puerto 5173):**
   ```powershell
   cd D:\Facultad\2026\SDS\tpi\TP_SDS_Cuidadores\frontend
   npm run dev
   ```
   Luego abrí en el navegador: **http://localhost:5173**

*(Para apagar ambos y liberar tu RAM al terminar: `Ctrl + C` en cada terminal).*

---

## 7. Guía Conceptual para la Defensa Oral (Preguntas Frecuentes de Cátedra)

Esta sección resume los conceptos teóricos que suelen preguntar los docentes al evaluar cada vulnerabilidad y su correspondiente parche en `remediado`:

### 1. Sobre Inyección SQL (`CWE-89`)
* **Principio de seguridad violado en `main`:** *Separación estricta entre el plano de instrucciones (código SQL) y el plano de datos (entrada del usuario)*. Al concatenar cadenas, la entrada del usuario se interpreta como parte de la gramática SQL.
* **¿Por qué `findByZonaContainingIgnoreCase` de Spring Data JPA lo soluciona de raíz?** Porque Hibernate traduce internamente el método derivado a un `PreparedStatement` de JDBC (`WHERE UPPER(zona) LIKE UPPER(?)`). El motor PostgreSQL compila primero el árbol sintáctico de la consulta y recién después inyecta el valor del parámetro como literal de texto puro: cualquier comilla o palabra reservada se trata únicamente como texto a buscar y jamás puede cambiar la estructura del `WHERE`.
* **¿Por qué además validamos longitud en la entrada?** Por *Defensa en Profundidad*: limitar el tamaño del parámetro evita consultas innecesariamente largas y reduce superficie de abuso.

### 2. Sobre IDOR (`CWE-639` — Broken Object Level Authorization)
* **¿Cuál es la diferencia entre Autenticación y Autorización en nuestro código?**
  * **Autenticación (`401 Unauthorized`):** Verifica la identidad (*"este pedido viene de Carlos López porque su JWT tiene firma válida"*). Esto lo hace `JwtAuthenticationFilter` + `SecurityConfig`.
  * **Autorización a nivel de objeto (`403 Forbidden`):** Verifica permisos sobre un recurso puntual (*"¿tiene permiso Carlos López para modificar específicamente el Perfil #1 que pertenece a Ana García?"*). En `main` falta este segundo chequeo dentro de `ProfileService.actualizarPerfil`.
* **¿Por qué no alcanza con ocultar el botón "Editar" en React (`ProfileCard.jsx`)?** Porque el cliente (navegador) está bajo control total del usuario: cualquier cliente HTTP puede invocar directamente el endpoint `PUT /api/profiles/{id}` sin pasar por la interfaz gráfica. Todo control de acceso debe imponerse obligatoriamente en el servidor.

### 3. Sobre Autenticación Defectuosa (`OWASP A07` / `CWE-287`)
* **¿Si usamos `BCrypt` para las contraseñas, por qué igual es vulnerable la autenticación en `main`?**
  Porque la seguridad de un sistema equivale a la de su eslabón más débil:
  1. **Secreto criptográfico expuesto (`CWE-798`):** Aunque la contraseña en PostgreSQL tenga hash BCrypt irreversible, si la clave `HS256` que firma los JWT está publicada en el repositorio, no hace falta romper BCrypt para emitir tokens válidos a nombre de otro usuario.
  2. **Contraseñas débiles (`CWE-521`) + ausencia de límite de intentos (`CWE-307`):** BCrypt ralentiza el cálculo por intento, pero si se permite elegir claves triviales y probar intentos ilimitados contra `/api/auth/login` sin bloqueo temporal (`429 Too Many Requests`), las cuentas siguen expuestas a diccionarios comunes.
  3. **Enumeración de usuarios (`CWE-204`):** Dar mensajes distintos ante usuario inexistente vs. clave errónea filtra información confidencial sobre qué correos están registrados en la plataforma.


