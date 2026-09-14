# SIGEPES Frontend

Sistema de Gestión de Pesca Sostenible — interfaz del MVP v1.0.

## Stack
React 19, TypeScript, Vite, Material UI, React Router, React Query, Zustand, React Hook Form, Zod, PWA.

## Requisitos
- Node.js 22 LTS o superior
- npm 10+
- El backend en marcha (ver `../backend/README.md`)

## Instalación

```bash
npm install
npm run dev      # http://localhost:5173
```

`vite.config.ts` redirige `/api` a `http://localhost:8080`, así que **el backend debe estar
levantado** para que la aplicación funcione: ya no hay datos simulados.

## Variables de entorno

Copia `.env.example` a `.env` si necesitas cambiar algo:

| Variable | Por defecto | Para qué sirve |
|---|---|---|
| `VITE_APP_NAME` | `SIGEPES` | Título de la barra superior |
| `VITE_API_BASE_URL` | `/api/v1` | Base de la API (pasa por el proxy de Vite) |
| `VITE_REQUEST_TIMEOUT_MS` | `15000` | Tiempo máximo de espera de Axios |

## Credenciales de prueba

Las cargan los datos DEMO del backend. Contraseña común: **`Pesca#2026`**.

| Correo | Rol | Qué se ve |
|---|---|---|
| `pescador@sigepes.gov.co` | Pescador | Sus embarcaciones, faenas y permisos |
| `inspector@sigepes.gov.co` | Inspector AUNAP | Todo; aprueba y suspende permisos; auditoría |
| `admin@sigepes.gov.co` | Administrador | Todo lo anterior más la gestión de usuarios |
| `armador@sigepes.gov.co` | Armador | Como el pescador |
| `analista@sigepes.gov.co` | Analista | Catálogos y vedas |

## Comandos

```bash
npm run dev       # desarrollo
npm run build     # tsc -b + build de producción
npm run preview   # sirve el build (aquí sí funciona la PWA)
npm run lint      # oxlint
```

## Notas

- La sesión (token y usuario) se guarda con Zustand en `localStorage`; un `401` la limpia sola.
- Las faenas registradas **sin conexión** quedan en `localStorage` y se envían con el botón
  «Sincronizar» de la bitácora. Un rechazo del servidor (veda, talla, permiso) **no** se guarda como
  pendiente: se muestra el error, porque volver a enviarlo daría el mismo resultado.
- Los mocks de MSW se retiraron al conectar la API real.
