# SIGEPES — Sistema de Gestión de Pesca Sostenible

Aplicación del **MVP v1.0 — Núcleo de control y legalidad**: registro digital de faenas, control de
permisos, catálogo de especies con tallas mínimas, vedas y auditoría, alineado con la Ley 13 de 1990,
la Ley 1851 de 2017 y la meta 14.4 del ODS 14.

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
