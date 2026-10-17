# SIGEPES — Sistema de Gestión de Pesca Sostenible

Aplicación del **MVP v1.0 — Núcleo de control y legalidad** y del **MVP v1.1 — Control avanzado,
geo-control y trazabilidad**: registro digital de faenas, permisos, especies y tallas mínimas, vedas,
cuotas, geolocalización, trazabilidad, denuncias, alertas, indicadores, documentos, reportes y
auditoría, alineado con la Ley 13 de 1990, la Ley 1851 de 2017 y la meta 14.4 del ODS 14.

```
Sigepes/
├── frontend/   React 19 + TypeScript + Vite + MUI   → :5173
└── backend/    Spring Boot 4 + Java 21 + PostgreSQL → :8080
```

## Arrancar todo

```bash
# 1) Base de datos y API
cd backend
cp .env.example .env
docker compose up -d postgres
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# 2) Interfaz (en otra terminal)
cd frontend
npm install
npm run dev
```

Abre `http://localhost:5173` y entra con `pescador@sigepes.gov.co` / `Pesca#2026`.
La documentación de la API queda en `http://localhost:8080/swagger-ui.html`.

Para levantar API y base con un solo comando: `cd backend && docker compose up --build`.

### Stack completo en contenedores

```bash
cp .env.example .env            # define POSTGRES_PASSWORD y JWT_SECRET
docker compose up --build       # PostgreSQL + API + frontend (nginx) → http://localhost:8081
```

## Despliegue (HU-21)

| Pieza | Dónde | Cómo |
|---|---|---|
| Integración continua | GitHub Actions (`.github/workflows/ci.yml`) | En cada push/PR: `mvnw verify`, lint + build del frontend y build de ambas imágenes Docker. |
| API + PostgreSQL | Render (`render.yaml`) | Blueprint con health check en `/actuator/health`, reinicio automático y disco persistente para archivos. |
| Frontend | Vercel (`frontend/vercel.json`) o cualquier CDN | Build estático; define `VITE_API_BASE_URL=https://<api>/api/v1` en el panel y, opcionalmente, `VITE_GOOGLE_MAPS_API_KEY` y `VITE_GOOGLE_MAPS_MAP_ID`. |
| Despliegue continuo | `.github/workflows/deploy.yml` | Tras CI verde en `main`, dispara el deploy hook de Render y espera a que la API responda `UP`. |

**Configuración única**

1. En GitHub → *Settings → Branches* protege `main` con *Require status checks to pass* (jobs
   `Backend`, `Frontend`, `Imágenes Docker`): un pipeline en rojo bloquea el merge.
2. En Render crea el Blueprint desde este repositorio y completa `FRONTEND_URL` y `DATABASE_URL`
   (formato `jdbc:postgresql://host:5432/db`). `JWT_SECRET` se genera solo.
3. Copia el *Deploy Hook* del servicio `sigepes-api` como secreto `RENDER_DEPLOY_HOOK_URL` del
   repositorio y la URL pública de la API como variable `API_URL`.
4. En Vercel importa el repositorio con *Root Directory* `frontend` y define `VITE_API_BASE_URL`. Para el mapa de Google, crea en Google Cloud una clave de navegador de *Maps JavaScript API* restringida al dominio de Vercel y defínela en `VITE_GOOGLE_MAPS_API_KEY`; sin ella se usa el mapa esquemático.

Ningún secreto vive en el repositorio: todo se lee de variables de entorno del almacén de la
plataforma. HTTPS lo termina la plataforma (Render y Vercel redirigen HTTP→HTTPS); la API respeta
`X-Forwarded-*` y CORS solo admite el dominio de `FRONTEND_URL`.

**Rollback (AC-21.6)**

- *API:* en Render → servicio `sigepes-api` → *Events*, elige el despliegue anterior y pulsa
  **Rollback**. Las migraciones Flyway son solo aditivas, así que la versión anterior arranca sobre
  el esquema nuevo.
- *Frontend:* en Vercel → *Deployments*, abre el despliegue anterior y pulsa **Promote to Production**.
- *Código:* `git revert <commit>` en `main`; CI y el deploy se ejecutan solos.

## Alcance

**MVP v1.0 (Sprint 1):** RF-01 autenticación · RF-02 embarcaciones y artes · RF-03 bitácora de
faenas y capturas · RF-05 vedas · RF-06 especies y tallas mínimas · RF-08 permisos y licencias ·
RF-20 administración y auditoría.

**MVP v1.1 (Sprint 2):**

| HU | Requisito | Qué hace |
| --- | --- | --- |
| HU-21 | Despliegue | CI en GitHub Actions, contenedores Docker, Render + Vercel con health checks y rollback |
| HU-07 | RF-07 Cuotas | Cuotas por especie, zona y periodo con descuento al cierre y umbrales 80/95/100 % |
| HU-04 | RF-04 Geolocalización | GPS de la faena, zonas restringidas (vedas y AMP) y evento de control al ingresar; mapa de Google Maps con respaldo sin conexión |
| HU-10 | RF-10 Denuncias | Denuncia ciudadana anónima sin cuenta, fotos sin EXIF, código de seguimiento y bandeja de inspección |
| HU-11 | RF-11 Alertas | Buzón persistente, alertas por evento y por calendario, entrega en vivo (SSE) y críticas no archivables |
| HU-09 | RF-09 Trazabilidad | Lotes con QR desde captura legal y cadena de custodia encadenada por hash, verificable en `/verificar` |
| HU-12 | RF-12 Indicadores | Tablero de sostenibilidad con filtros, solo datos validados y vista de tabla accesible |
| HU-15 | RF-15 Documentos | Documentos versionados, validación por contenido, enlaces firmados y avisos de vencimiento |
| HU-18 | RF-18 Reportes | Reportes oficiales en CSV y PDF, en segundo plano si superan un año |

Atributos de calidad del sprint: **disponibilidad** (operación sin señal y entrega diferida),
**seguridad** (anonimato, integridad por hash, autorización y auditoría) y **rendimiento**
(prefiltros espaciales, agregados en base de datos y cachés de lectura).

**Fuera de alcance por ahora (versión 2.0):** AMP como módulo propio (RF-13), datos ambientales
(RF-14), marketplace (RF-16), rendimiento máximo sostenible (RF-17) y educación (RF-19).

### Variables nuevas del MVP v1.1

| Variable | Dónde | Para qué |
| --- | --- | --- |
| `VITE_GOOGLE_MAPS_API_KEY` | Frontend (build) | Mapa de Google; sin ella se usa el esquema sin conexión |
| `VITE_GOOGLE_MAPS_MAP_ID` | Frontend (build) | Map ID propio; por defecto `DEMO_MAP_ID` |
| `PUBLIC_REQUESTS_PER_MINUTE` | Backend | Límite por IP de los endpoints públicos (denuncias, verificación de lotes); 20 por defecto |
| `UPLOADS_DIR` | Backend | Disco persistente para fotos de denuncias, documentos y exportaciones |

## Documentación

- `backend/README.md` — instalación, variables, migraciones, endpoints, roles, reglas de negocio y pruebas.
- `frontend/README.md` — instalación, variables y credenciales de prueba.
