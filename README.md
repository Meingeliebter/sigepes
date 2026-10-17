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
Merge a main ──► CI ──► Imágenes en GHCR ──► API en Railway (health UP) ──► Frontend en Vercel ──► Prueba de humo
```

| Pieza | Dónde | Qué hace |
|---|---|---|
| Integración continua | `.github/workflows/ci.yml` | En cada push/PR: `mvnw verify`, lint y build del frontend, build de las imágenes. En `main` publica las imágenes en GitHub Container Registry con la etiqueta del commit. |
| Vista previa | `.github/workflows/preview.yml` | Con CI verde en un PR, publica el frontend en una URL de Vercel y la comenta en el PR. |
| Despliegue continuo | `.github/workflows/deploy.yml` | Con CI verde en `main`: sube a Railway el commit exacto, espera `/actuator/health` = `UP`, publica el frontend en Vercel y comprueba que la web responde y que CORS acepta el dominio. También se puede lanzar a mano (*Actions → Deploy → Run workflow*). |
| API (contenedor) + PostgreSQL | Railway (`backend/railway.json`) | Railway construye `backend/Dockerfile` y solo activa la versión nueva si el health check responde; reinicia el contenedor si falla. PostgreSQL gestionado en el mismo proyecto y volumen persistente en `/var/data` para los archivos subidos (AC-15.6). |
| Frontend | Vercel (`frontend/vercel.json`) | Estáticos comprimidos en CDN. El despliegue automático propio de Vercel está desactivado: solo publica el pipeline con el CI en verde. |

### Configuración única (≈15 minutos)

Las cuentas y los tokens los crea la persona responsable del despliegue; nunca se escriben en el
repositorio (AC-21.4). Se necesitan cuentas de GitHub, [Railway](https://railway.com) y
[Vercel](https://vercel.com); las dos últimas se pueden abrir con "Continuar con GitHub".

1. **Proyecto en Railway.** *New Project → Empty Project*. Dentro del proyecto:
   - *+ Create → Database → PostgreSQL* (queda como servicio `Postgres`).
   - *+ Create → Empty Service*, renómbralo a **`sigepes-api`**. No lo conectes al repositorio:
     lo despliega GitHub Actions cuando el CI pasa.
   - En `sigepes-api` → *Variables → Raw Editor*, pega:

     ```
     SPRING_PROFILES_ACTIVE=prod
     JWT_SECRET=${{secret(64)}}
     DATABASE_HOST=${{Postgres.PGHOST}}
     DATABASE_PORT=${{Postgres.PGPORT}}
     DATABASE_NAME=${{Postgres.PGDATABASE}}
     DATABASE_USER=${{Postgres.PGUSER}}
     DATABASE_PASSWORD=${{Postgres.PGPASSWORD}}
     UPLOADS_DIR=/var/data/uploads
     SEED_DEMO_DATA=true
     JAVA_OPTS=-XX:MaxRAMPercentage=75
     FRONTEND_URL=https://sigepes.vercel.app,https://sigepes-*.vercel.app
     ```

     `JWT_SECRET` lo genera Railway y las credenciales de la base se enlazan solas. Ajusta
     `FRONTEND_URL` cuando conozcas tu dominio de Vercel (paso 2).
   - En `sigepes-api` → *Settings*: en **Volumes** añade un volumen montado en **`/var/data`**, y en
     **Networking → Generate Domain** crea la URL pública (p. ej.
     `https://sigepes-api-production.up.railway.app`).
   - *Project Settings → Tokens*: crea un **token de proyecto** para el entorno `production`.
2. **Proyecto en Vercel.** *Add New → Project →* importa este repositorio con
   **Root Directory = `frontend`** y *Deploy*. Anota el dominio (p. ej. `https://sigepes.vercel.app`).
   Luego:
   - *Account Settings → Tokens*: crea un token para GitHub Actions.
   - *Project → Settings → General*: copia el **Project ID**. El **Team/Org ID** está en
     *Team Settings → General* (en cuentas personales, *Account Settings → General → Your ID*).
3. **Secretos y variables en GitHub.** En una terminal con `gh` autenticado, desde la raíz del
   repositorio (cada `gh secret set` pide el valor sin mostrarlo):

   ```bash
   gh secret set RAILWAY_TOKEN
   gh secret set VERCEL_TOKEN
   gh secret set VERCEL_ORG_ID
   gh secret set VERCEL_PROJECT_ID
   gh variable set API_URL --body "https://sigepes-api-production.up.railway.app"
   gh variable set FRONTEND_URL --body "https://sigepes.vercel.app"
   ```

   Si el servicio de la API no se llama `sigepes-api`, define también
   `gh variable set RAILWAY_SERVICE --body "<nombre>"`.

   Opcional, mapa de Google: crea en Google Cloud una clave de navegador de *Maps JavaScript API*
   restringida a `*.vercel.app` y guárdala con `gh secret set GOOGLE_MAPS_API_KEY`. Sin ella la app
   usa el mapa esquemático.
4. **Primer despliegue.** *Actions → Deploy → Run workflow*, o simplemente fusiona a `main`.
5. **Protección de `main`.** En *Settings → Branches* exige los checks `Backend (Spring Boot)`,
   `Frontend (React + Vite)` e `Imágenes Docker`: un pipeline en rojo bloquea el merge (AC-21.1).

Credenciales de demostración: las del `frontend/README.md` (los datos DEMO se cargan con
`SEED_DEMO_DATA=true`). Desactívalo para una instalación real.

**Costo.** Railway no tiene plan gratuito permanente: ofrece un periodo de prueba con crédito y
luego el plan *Hobby* (alrededor de 5 USD al mes con ese consumo incluido), suficiente para la API
y la base de datos de este proyecto. Vercel (plan Hobby) y GitHub Actions en repositorios públicos
no tienen costo. Revisa los precios vigentes en railway.com/pricing.

Ningún secreto vive en el repositorio. HTTPS lo terminan Railway y Vercel; la API respeta
`X-Forwarded-*` y CORS solo admite los orígenes de `FRONTEND_URL`. Dentro del contenedor, la JVM
corre con un usuario sin privilegios: `docker-entrypoint.sh` solo usa root para dejar escribible el
volumen y en seguida cambia de usuario.

### Rollback (AC-21.6)

- *Frontend:* en Vercel → *Deployments*, elige el despliegue anterior → **Promote to Production**
  (o `npx vercel@62.2.0 rollback`).
- *API:* en Railway → `sigepes-api` → *Deployments*, abre el despliegue anterior → **Redeploy**.
  Las migraciones Flyway son solo aditivas, así que la versión anterior arranca sobre el esquema
  nuevo.
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
| HU-21 | Despliegue | CI/CD en GitHub Actions, contenedor de la API en Railway, frontend en Vercel, health checks y rollback |
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
