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

```
Pull request ──► CI (backend, frontend, Docker) ──► Vista previa en Vercel (comentada en el PR)
Merge a main ──► CI ──► Imágenes en GHCR ──► API en Render (health UP) ──► Frontend en Vercel ──► Prueba de humo
```

| Pieza | Dónde | Qué hace |
|---|---|---|
| Integración continua | `.github/workflows/ci.yml` | En cada push/PR: `mvnw verify`, lint y build del frontend, build de las imágenes. En `main` publica las imágenes en GitHub Container Registry con la etiqueta del commit. |
| Vista previa | `.github/workflows/preview.yml` | Con CI verde en un PR, publica el frontend en una URL de Vercel y la comenta en el PR. |
| Despliegue continuo | `.github/workflows/deploy.yml` | Con CI verde en `main`: despliega en Render el commit exacto, espera `/actuator/health` = `UP`, publica el frontend en Vercel y comprueba que la web responde y que CORS acepta el dominio. También se puede lanzar a mano (*Actions → Deploy → Run workflow*). |
| API + PostgreSQL | Render (`render.yaml`) | Blueprint en plan gratuito: base de datos gestionada conectada sola, `JWT_SECRET` generado por Render y health check con reinicio automático. |
| Frontend | Vercel (`frontend/vercel.json`) | Estáticos comprimidos en CDN. El despliegue automático propio de Vercel está desactivado: solo publica el pipeline con el CI en verde. |

### Configuración única (≈15 minutos)

Las cuentas y los tokens los crea la persona responsable del despliegue; nunca se escriben en el
repositorio (AC-21.4). Se necesitan cuentas de GitHub, [Render](https://render.com) y
[Vercel](https://vercel.com); las dos últimas se pueden abrir con "Continuar con GitHub".

1. **API en Render.** *New → Blueprint →* elige este repositorio *→ Apply*. Render crea
   `sigepes-db` y `sigepes-api`. Cuando termine:
   - Copia la URL pública del servicio (p. ej. `https://sigepes-api.onrender.com`).
   - En *sigepes-api → Settings → Deploy Hook* copia la URL del hook.
2. **Proyecto en Vercel.** *Add New → Project →* importa este repositorio con
   **Root Directory = `frontend`** y *Deploy*. Anota el dominio (p. ej. `https://sigepes.vercel.app`).
   Luego:
   - *Account Settings → Tokens*: crea un token para GitHub Actions.
   - *Project → Settings → General*: copia el **Project ID**. El **Team/Org ID** está en
     *Team Settings → General* (en cuentas personales, *Account Settings → General → Your ID*).
3. **CORS en Render.** En *sigepes-api → Environment* define
   `FRONTEND_URL = https://sigepes.vercel.app,https://sigepes-*.vercel.app` (dominio de producción
   y vistas previas) y guarda.
4. **Secretos y variables en GitHub.** En una terminal con `gh` autenticado, desde la raíz del
   repositorio (cada `gh secret set` pide el valor sin mostrarlo):

   ```bash
   gh secret set RENDER_DEPLOY_HOOK_URL
   gh secret set VERCEL_TOKEN
   gh secret set VERCEL_ORG_ID
   gh secret set VERCEL_PROJECT_ID
   gh variable set API_URL --body "https://sigepes-api.onrender.com"
   gh variable set FRONTEND_URL --body "https://sigepes.vercel.app"
   ```

   Opcional, mapa de Google: crea en Google Cloud una clave de navegador de *Maps JavaScript API*
   restringida a `*.vercel.app` y guárdala con `gh secret set GOOGLE_MAPS_API_KEY`. Sin ella la app
   usa el mapa esquemático.
5. **Primer despliegue.** *Actions → Deploy → Run workflow*, o simplemente fusiona a `main`.
6. **Protección de `main`.** En *Settings → Branches* exige los checks `Backend (Spring Boot)`,
   `Frontend (React + Vite)` e `Imágenes Docker`: un pipeline en rojo bloquea el merge (AC-21.1).

Credenciales de demostración: las del `frontend/README.md` (los datos DEMO se cargan si
`SEED_DEMO_DATA=true`, como define el blueprint). Desactívalo para una instalación real.

**Plan gratuito.** El servicio de Render se duerme tras 15 minutos sin tráfico; la primera
petición tarda alrededor de un minuto. No tiene disco persistente, así que los archivos subidos se
pierden al redesplegar: para cumplir AC-15.6 cambia a `plan: starter` y activa el bloque `disk` de
`render.yaml`. La base de datos gratuita de Render caduca a los 30 días.

Ningún secreto vive en el repositorio. HTTPS lo terminan Render y Vercel; la API respeta
`X-Forwarded-*` y CORS solo admite los orígenes de `FRONTEND_URL`.

### Rollback (AC-21.6)

- *Frontend:* en Vercel → *Deployments*, elige el despliegue anterior → **Promote to Production**
  (o `npx vercel@62.2.0 rollback`).
- *API:* en Render → `sigepes-api` → *Events*, elige el despliegue anterior → **Rollback**. Las
  migraciones Flyway son solo aditivas, así que la versión anterior arranca sobre el esquema nuevo.
- *Imagen exacta:* cada commit de `main` está en `ghcr.io/<owner>/sigepes-backend:<sha>` y
  `ghcr.io/<owner>/sigepes-frontend:<sha>`.
- *Código:* `git revert <commit>` en `main`; CI y Deploy se ejecutan solos.

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
