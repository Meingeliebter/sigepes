# SIGEPES — Backend (MVP v1.0 · Núcleo de control y legalidad)

API REST del **Sistema de Gestión de Pesca Sostenible**. Cubre el alcance del MVP v1.0:
registro y autenticación (RF-01), embarcaciones y artes (RF-02), bitácora de faenas y capturas
(RF-03), vedas (RF-05), catálogo de especies y tallas mínimas (RF-06), permisos y licencias
(RF-08) y administración con auditoría (RF-20).

El frontend de `../frontend` consume esta API tal cual: los nombres de recurso y de campo se
mantuvieron en español y con la forma que ya esperaba la interfaz.

---

## 1. Requisitos

| Herramienta | Versión probada |
|---|---|
| JDK | 21 |
| Maven | 3.9 |
| Docker / Docker Compose | 28 (solo para PostgreSQL) |
| PostgreSQL | 16 |

---

## 2. Puesta en marcha

### Opción A — Todo con Docker (recomendada)

```bash
cd backend
cp .env.example .env          # ajusta JWT_SECRET antes de nada serio
docker compose up --build     # levanta PostgreSQL + backend en :8080
```

### Opción B — PostgreSQL en Docker, backend en local

```bash
cd backend
docker compose up -d postgres
./mvnw spring-boot:run                       # Linux / macOS
mvnw.cmd spring-boot:run                     # Windows
```

### Opción C — Sin Docker, base H2 en memoria

Útil para una demostración rápida; los datos se pierden al parar el proceso.

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

La API queda en `http://localhost:8080/api/v1` y la documentación interactiva en
`http://localhost:8080/swagger-ui.html` (OpenAPI en `/v3/api-docs`).

### Migraciones

Las gestiona **Flyway** y se aplican solas al arrancar (`src/main/resources/db/migration`). El SQL
es portable a propósito (PostgreSQL y H2), así que las pruebas ejercitan las mismas migraciones que
producción. Hibernate arranca en modo `validate`: si una entidad y el esquema se separan, la
aplicación no levanta.

### Datos de demostración

Con `SEED_DEMO_DATA=true` (por defecto en dev) se cargan datos **solo si la base está vacía**. Todo
lo creado queda marcado con la columna `demo = true` y los nombres empiezan por `DEMO`.

Todas las cuentas usan la contraseña **`Pesca#2026`**:

| Correo | Rol |
|---|---|
| `pescador@sigepes.gov.co` | Pescador artesanal |
| `armador@sigepes.gov.co` | Armador |
| `inspector@sigepes.gov.co` | Inspector AUNAP |
| `analista@sigepes.gov.co` | Analista de sostenibilidad |
| `admin@sigepes.gov.co` | Administrador |

Incluye además: 5 artes (2 marcadas como destructivas para probar RN-04), 3 embarcaciones,
5 especies con talla mínima, 2 vedas (una vigente y una programada) y 5 permisos
(vigente, vencido, pendiente, próximo a vencer y suspendido).

---

## 3. Variables de entorno

Ver `.env.example`. Las relevantes:

| Variable | Por defecto | Para qué sirve |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/sigepes` | Cadena JDBC |
| `DATABASE_USER` / `DATABASE_PASSWORD` | `sigepes` / `sigepes` | Credenciales |
| `JWT_SECRET` | secreto de desarrollo | Firma HS256. **Mínimo 32 caracteres**; el arranque falla si es más corto |
| `JWT_EXPIRATION_SECONDS` | `86400` | Vigencia del access token |
| `API_PORT` | `8080` | Puerto HTTP |
| `FRONTEND_URL` | `http://localhost:5173,...` | Orígenes permitidos por CORS, separados por comas |
| `LOGIN_ATTEMPTS_PER_MINUTE` | `10` | Límite por IP en login y registro |
| `PERMIT_EXPIRY_WARNING_DAYS` | `30` | Antelación del aviso «permiso por vencer» |
| `SEED_DEMO_DATA` | `true` (`false` en prod) | Carga de datos DEMO |

Nunca se versiona un `.env` real: `.gitignore` lo excluye.

---

## 4. Pruebas

```bash
./mvnw test
```

58 pruebas de integración sobre el contexto completo y H2 con las migraciones reales:

| Clase | Qué cubre |
|---|---|
| `AuthApiTest` (9) | Registro, RN-01 (documento único), correo duplicado, validación de formato, login correcto e incorrecto, hash BCrypt, 401 sin token, `/auth/me` |
| `TripRulesTest` (19) | RN-16 permiso vigente/ausente/vencido/rechazado/suspendido y excepción de subsistencia; embarcación inactiva o ajena; RN-12 talla mínima; RN-10 veda por zona y veda «TODAS»; RN-06 cierre con capturas, bloqueo de faena vacía, declaración nula; sincronización idempotente |
| `PermitLifecycleTest` (10) | Solicitud en PENDING, imposibilidad de auto-aprobarse, aprobación, motivo obligatorio en rechazo y suspensión, permiso vencido, aviso de vencimiento, verificación por número, aislamiento por titular |
| `VesselGearRulesTest` (6) | Alta con arte permitida, resolución de arte por nombre, RN-04 arte destructiva, matrícula duplicada, visibilidad por rol |
| `AuditAndAuthorizationTest` (11) | Auditoría de login, login fallido, apertura y cierre de faena, aprobación con estado anterior/posterior, corrección de faena cerrada; bitácora append-only; permisos por rol; suspensión de usuario con efecto inmediato |
| `TripWithoutAmbientTransactionTest` (3) | Ciclo completo **sin** transacción de test, para detectar accesos perezosos fuera de transacción (`open-in-view=false`) |

---

## 5. Arquitectura

Monolito modular por dominio. Cada paquete contiene su entidad, repositorio, servicio,
controlador y DTOs:

```
com.sigepes.backend
├── common/      ErrorCode, ApiException, ApiErrorResponse, GlobalExceptionHandler, Ids
├── config/      SecurityConfig (CORS, JWT, RBAC), OpenApiConfig
├── security/    JwtTokenProvider, JwtAuthenticationFilter, RateLimitFilter, CurrentUser
├── users/       RF-01 · usuarios, roles y estado
├── auth/        RF-01 · registro, login, sesión, logout
├── gears/       RF-02 · catálogo de artes de pesca
├── vessels/     RF-02 · embarcaciones
├── species/     RF-06 · especies y tallas mínimas
├── closures/    RF-05 · vedas y temporadas
├── permits/     RF-08 · permisos y licencias
├── trips/       RF-03 · faenas y capturas
├── rules/       ComplianceService — todas las reglas de negocio, centralizadas
├── audit/       RF-20 · bitácora append-only
├── dashboard/   resumen de inicio y notificaciones derivadas
└── seed/        datos DEMO
```

Dos decisiones que conviene conocer antes de tocar el código:

- **Las reglas de negocio viven solo en `rules/ComplianceService`.** Los servicios de dominio la
  invocan; ningún controlador valida por su cuenta.
- **Los DTO se construyen dentro de la transacción del servicio.** `spring.jpa.open-in-view` está
  desactivado, así que mapear una entidad en el controlador rompería al tocar una asociación
  perezosa. `TripWithoutAmbientTransactionTest` existe justamente para que eso no vuelva a colarse.

---

## 6. Modelo de datos

10 tablas. Identificadores `VARCHAR` legibles generados por la aplicación (`usr-…`, `emb-…`),
enumerados como `VARCHAR` + `CHECK`, `created_at` / `updated_at` en todas las tablas de negocio.

| Tabla | Contenido | Claves y restricciones |
|---|---|---|
| `users` | Cuentas y roles | PK `id`; **UNIQUE `national_id`** (RN-01), UNIQUE `email`; CHECK de rol y estado |
| `fishing_gears` | Catálogo de artes | PK `id`; UNIQUE `name`; `destructive`, CHECK de estado |
| `vessels` | Embarcaciones | PK `id`; UNIQUE `registration`; FK `owner_id → users`; CHECK de estado y capacidad |
| `vessel_gears` | Artes por embarcación (N:M) | PK compuesta; FK a `vessels` (ON DELETE CASCADE) y a `fishing_gears` |
| `species` | Especies y talla mínima | PK `id`; UNIQUE `common_name`; CHECK `min_size_cm > 0` |
| `permits` | Permisos y licencias | PK `id`; UNIQUE `number` y `verification_code`; FK `owner_id` y `reviewed_by → users`; CHECK de estado y `expires_at >= issued_at` |
| `closures` | Vedas | PK `id`; FK `species_id → species`, `created_by → users`; CHECK de estado y `end_date >= start_date` |
| `fishing_trips` | Faenas | PK `id`; FK a `users`, `vessels`, `fishing_gears`, `permits`; CHECK de estado |
| `catches` | Capturas | PK `id`; FK `trip_id → fishing_trips` (CASCADE), `species_id → species`; CHECK de peso, talla y cantidad positivos |
| `audit_logs` | Bitácora (append-only) | PK `id`; índices por fecha, actor y módulo |

Índices en las claves foráneas y en los campos por los que se filtra
(`permits.expires_at`, `closures(start_date, end_date)`, `fishing_trips.trip_date`, …).

**Preparado para v1.1 / 2.0 sin rehacer nada:** la faena guarda ya `gear_id`, `permit_id`, horas de
salida y llegada y observaciones; las capturas guardan `quantity`; las vedas tienen zona y motivo.
Sobre esa base, cuotas, geolocalización, trazabilidad, alertas, AMP, RMS y marketplace se añaden
como tablas y módulos nuevos. **No están implementados: son v1.1 y 2.0.**

---

## 7. API

Base: `/api/v1`. Todo exige `Authorization: Bearer <token>` salvo login y registro.

### Autenticación (RF-01)

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/auth/register` | público | Alta de usuario. `201` con `{accessToken, user}` |
| POST | `/auth/login` | público | Inicio de sesión |
| GET | `/auth/me` | autenticado | Sesión actual |
| POST | `/auth/logout` | autenticado | `204`; deja constancia en auditoría |

### Embarcaciones y artes (RF-02)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/embarcaciones` | autenticado (operador ve las suyas; autoridad, todas) |
| GET | `/embarcaciones/{id}` | autenticado |
| POST | `/embarcaciones` | FISHER, SHIPOWNER, ADMIN |
| PATCH | `/embarcaciones/{id}` | FISHER, SHIPOWNER, ADMIN |
| GET | `/artes-pesca` | autenticado |
| POST | `/artes-pesca` | ADMIN, ANALYST |

### Especies y vedas (RF-06 / RF-05)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/especies` · `/especies/{id}` | autenticado |
| POST | `/especies` · PATCH `/especies/{id}` | ADMIN, ANALYST |
| GET | `/vedas` | autenticado (todas) |
| GET | `/vedas/activas` | autenticado (solo vigentes hoy) |
| POST | `/vedas` · PATCH `/vedas/{id}` | ADMIN, ANALYST, INSPECTOR |

### Permisos (RF-08)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/permisos` | autenticado (operador ve los suyos) |
| GET | `/permisos/validar/{number}` | autenticado |
| POST | `/permisos` | autenticado (nace `PENDING`) |
| PATCH | `/permisos/{id}/evaluar` | INSPECTOR, ADMIN |

### Faenas (RF-03)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/faenas` · `/faenas/{id}` | autenticado |
| POST | `/faenas` | FISHER, SHIPOWNER, ADMIN |
| POST | `/faenas/{id}/capturas` | FISHER, SHIPOWNER, ADMIN |
| POST | `/faenas/{id}/cerrar` | FISHER, SHIPOWNER, ADMIN |
| PATCH | `/faenas/{id}` | dueño si está abierta; solo autoridad si está cerrada |
| POST | `/faenas/sincronizar` | FISHER, SHIPOWNER, ADMIN |

### Administración y resumen (RF-20)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/usuarios` · POST `/usuarios` · PATCH `/usuarios/{id}` | ADMIN |
| GET | `/auditoria?module=&limit=` | ADMIN, INSPECTOR |
| GET | `/resumen` | autenticado |
| GET | `/notificaciones` | autenticado |

### Contrato por pantalla

| Pantalla | Endpoint | Campos que consume |
|---|---|---|
| Login / Registro | `POST /auth/login`, `POST /auth/register` | `accessToken`, `user{id,nationalId,fullName,email,role}` |
| Dashboard | `GET /resumen` | `fullName`, `alerts[]`, `vessels/permits/trips/warnings{value,helper}`, `recent[]`, `compliance[]` |
| Embarcaciones | `GET/POST /embarcaciones`, `GET /artes-pesca` | `id,name,registration,port,gears[],status` |
| Bitácora | `GET /faenas` | `id,vessel,zone,date,status,captures[],nullDeclaration` |
| Nueva faena | `POST /faenas` → `/capturas` → `/cerrar`, `GET /vedas/activas` | ver más abajo |
| Especies | `GET /especies` | `commonName,scientificName,minSizeCm,status` |
| Vedas | `GET /vedas` | `species,zone,from,to,status` |
| Permisos | `GET/POST /permisos`, `PATCH /permisos/{id}/evaluar` | `number,type,holder,issued,expires,status,expiringSoon,verificationCode` |
| Notificaciones | `GET /notificaciones` | `type,title,body,date` |
| Usuarios | `GET/POST /usuarios` | `fullName,email,nationalId,role,status` |
| Auditoría | `GET /auditoria` | `timestamp,actor,action,entity,detail` |

Ejemplo del flujo central:

```http
POST /api/v1/faenas
{ "vesselId": "emb-…", "zone": "Pacífico Norte" }
→ 201 { "id":"fae-…", "status":"IN_PROGRESS", "permitNumber":"PES-0231", … }

POST /api/v1/faenas/fae-…/capturas
{ "speciesId": "esp-…", "quantity": 40, "weightKg": 120.5, "averageSizeCm": 41 }
→ 200 { …, "captures":[{"species":"Pargo rojo", …}], "totalWeightKg":120.5 }

POST /api/v1/faenas/fae-…/cerrar
{ "nullDeclaration": false }
→ 200 { …, "status":"CLOSED" }
```

---

## 8. Errores

Toda respuesta de error tiene la misma forma. Los campos planos son los que lee el cliente
(`shared/errors/AppError.ts`); `error` anidado se mantiene para consumo externo.

```json
{
  "success": false,
  "timestamp": "2026-09-14T13:02:11Z",
  "status": 422,
  "path": "/api/v1/faenas/fae-1/capturas",
  "code": "MINIMUM_SIZE_VIOLATION",
  "message": "La talla registrada (20 cm) está por debajo del mínimo legal de «Pargo rojo» (35 cm).",
  "fieldErrors": [{ "field": "nationalId", "message": "…" }],
  "error": { "code": "MINIMUM_SIZE_VIOLATION", "message": "…" }
}
```

Códigos principales: `INVALID_CREDENTIALS`, `ACCOUNT_SUSPENDED`, `DOCUMENT_ALREADY_REGISTERED`,
`EMAIL_ALREADY_REGISTERED`, `PERMIT_REQUIRED`, `PERMIT_EXPIRED`, `PERMIT_NOT_APPROVED`,
`PERMIT_SUSPENDED`, `PERMIT_REASON_REQUIRED`, `SPECIES_UNDER_CLOSURE`, `MINIMUM_SIZE_VIOLATION`,
`PROHIBITED_GEAR`, `VESSEL_NOT_OWNED`, `VESSEL_NOT_AVAILABLE`, `TRIP_CANNOT_CLOSE`,
`TRIP_ALREADY_CLOSED`, `NULL_DECLARATION_WITH_CATCHES`, `REGISTRATION_ALREADY_REGISTERED`,
`VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `TOO_MANY_REQUESTS`.

---

## 9. Autenticación, roles y permisos

El login devuelve un JWT HS256 con `sub`, `email`, `name` y `role`. `JwtAuthenticationFilter`
**relee el rol y el estado desde la base en cada petición**: si el administrador suspende una cuenta
o cambia un rol, surte efecto de inmediato sin esperar a que caduque el token.

| Rol | Puede |
|---|---|
| `FISHER` (pescador artesanal) | Sus embarcaciones y faenas; solicitar permisos; consultar especies, vedas y notificaciones |
| `SHIPOWNER` (armador) | Lo mismo que el pescador |
| `INSPECTOR` (AUNAP) | Ver **todas** las embarcaciones, faenas y permisos; aprobar, rechazar y suspender permisos; gestionar vedas; corregir faenas cerradas; leer auditoría |
| `ANALYST` (sostenibilidad) | Ver todo; gestionar especies, tallas mínimas, vedas y artes |
| `ADMIN` | Todo lo anterior más la gestión de usuarios y roles |

Un operador solo ve sus propios datos; un pescador que conozca la URL de un endpoint administrativo
recibe `403 FORBIDDEN` (probado en `AuditAndAuthorizationTest`).

---

## 10. Reglas de negocio implementadas

Todas viven en `rules/ComplianceService` y se validan en el servidor aunque la interfaz también
avise:

| Regla | Enunciado | Error |
|---|---|---|
| **RN-01** | Un documento de identidad no puede registrarse más de una vez | `DOCUMENT_ALREADY_REGISTERED` |
| **RN-04** | No se registran artes destructivas o prohibidas en una embarcación | `PROHIBITED_GEAR` |
| **RN-06** | Una faena no se cierra sin capturas ni declaración de captura nula, y la declaración nula no convive con capturas | `TRIP_CANNOT_CLOSE`, `NULL_DECLARATION_WITH_CATCHES` |
| **RN-10** | Una especie en veda no admite captura en esa zona y periodo (`TODAS` cubre cualquier zona) | `SPECIES_UNDER_CLOSURE` |
| **RN-12** | No se registra captura por debajo de la talla mínima legal | `MINIMUM_SIZE_VIOLATION` |
| **RN-16** | Ninguna faena se abre sin permiso aprobado y vigente, **salvo subsistencia** | `PERMIT_REQUIRED`, `PERMIT_EXPIRED`, `PERMIT_NOT_APPROVED`, `PERMIT_SUSPENDED` |
| **RN-40** | Toda operación sensible queda en la bitácora de auditoría | — |

Reglas complementarias: la embarcación debe estar activa y a nombre de quien registra; el rechazo y
la suspensión de un permiso exigen motivo; el estado de un permiso solo lo fija la autoridad (un
operador no puede auto-aprobarse); una faena cerrada solo la corrige la autoridad y la corrección
queda auditada.

**Excepción de subsistencia (RN-16).** La pesca de subsistencia no está sujeta a la caducidad del
permiso: un permiso *aprobado* cuyo tipo figure en `sigepes.rules.subsistence-permit-types`
(`SUBSISTENCIA`, `PESCA DE SUBSISTENCIA`) habilita la operación aunque su vigencia haya terminado.
Es una decisión de negocio configurable, no una constante del código.

---

## 11. Seguridad

- Contraseñas con **BCrypt**; nunca se almacenan ni se devuelven en claro.
- JWT firmado HS256; el secreto llega por variable de entorno y el arranque **falla** si tiene menos
  de 32 caracteres.
- Autorización por rol con `@PreAuthorize` y filtrado por propietario en los servicios.
- Validación de entrada con Bean Validation en todos los DTO, y de nuevo en las reglas de negocio.
- JPA con consultas parametrizadas: sin concatenación de SQL.
- CORS restringido a los orígenes de `FRONTEND_URL`.
- Límite de intentos por IP en `/auth/login` y `/auth/register` (`429 TOO_MANY_REQUESTS`).
- La bitácora de auditoría no expone escritura ni borrado por API: `POST`/`DELETE` responden `405`.
