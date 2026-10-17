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
| Frontend | Vercel (`frontend/vercel.json`) o cualquier CDN | Build estático; define `VITE_API_BASE_URL=https://<api>/api/v1` en el panel. |
| Despliegue continuo | `.github/workflows/deploy.yml` | Tras CI verde en `main`, dispara el deploy hook de Render y espera a que la API responda `UP`. |

**Configuración única**

1. En GitHub → *Settings → Branches* protege `main` con *Require status checks to pass* (jobs
   `Backend`, `Frontend`, `Imágenes Docker`): un pipeline en rojo bloquea el merge.
2. En Render crea el Blueprint desde este repositorio y completa `FRONTEND_URL` y `DATABASE_URL`
   (formato `jdbc:postgresql://host:5432/db`). `JWT_SECRET` se genera solo.
3. Copia el *Deploy Hook* del servicio `sigepes-api` como secreto `RENDER_DEPLOY_HOOK_URL` del
   repositorio y la URL pública de la API como variable `API_URL`.
4. En Vercel importa el repositorio con *Root Directory* `frontend` y define `VITE_API_BASE_URL`.

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

**Implementado (MVP v1.0):** RF-01 autenticación · RF-02 embarcaciones y artes · RF-03 bitácora de
faenas y capturas · RF-05 vedas · RF-06 especies y tallas mínimas · RF-08 permisos y licencias ·
RF-20 administración y auditoría.

**Fuera de alcance por ahora:** cuotas (RF-07), geolocalización (RF-04), trazabilidad (RF-09),
denuncias (RF-10), alertas (RF-11), tablero (RF-12), AMP (RF-13), datos ambientales (RF-14),
documentos (RF-15), marketplace (RF-16), RMS (RF-17), reportes (RF-18) y educación (RF-19).
Corresponden a v1.1 y 2.0; el modelo de datos ya está preparado para acogerlos.

## Documentación

- `backend/README.md` — instalación, variables, migraciones, endpoints, roles, reglas de negocio y pruebas.
- `frontend/README.md` — instalación, variables y credenciales de prueba.
