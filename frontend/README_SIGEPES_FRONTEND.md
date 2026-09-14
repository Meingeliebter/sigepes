# SIGEPES — Frontend (Base arquitectónica)

React 19 + TypeScript + Vite + Material UI + React Query + Zustand + React Router + React Hook Form + Zod + PWA.

## 1. Cómo instalar estos archivos

Copia el contenido de este paquete **dentro de tu carpeta `sigepes`**, respetando las rutas.
Sobrescribe `src/App.tsx`, `src/main.tsx` y `vite.config.ts` cuando lo pida.

Archivos incluidos:

```
vite.config.ts                      -> alias @/ + PWA
tsconfig.paths.json                 -> alias @/ para TypeScript
.env.example                        -> variables de entorno
src/App.tsx
src/main.tsx
src/app/providers/AppProviders.tsx  -> Theme + React Query + Router
src/app/router/AppRouter.tsx        -> rutas con lazy loading + RBAC
src/app/layouts/AppLayout.tsx       -> AppBar + BottomNav (móvil)
src/app/guards/RequireAuth.tsx      -> protección por auth + permiso
src/shared/theme/theme.ts           -> paleta SIGEPES
src/shared/api/httpClient.ts        -> Axios + interceptor JWT (refresh)
src/shared/storage/sessionStore.ts  -> Zustand (sesión)
src/shared/permissions/permissions.ts -> RBAC (roles/capacidades)
src/shared/errors/AppError.ts       -> manejo de errores normalizado
src/shared/constants/app.ts
src/shared/components/PageContainer.tsx
src/modules/**                      -> páginas (login, home, trips, ...)
```

## 2. Configurar el alias @/ en tsconfig

Abre `tsconfig.app.json` y agrega dentro de `compilerOptions`:

```json
"baseUrl": ".",
"paths": { "@/*": ["src/*"] }
```

## 3. Variables de entorno

Copia `.env.example` a `.env`:

```
VITE_API_BASE_URL=/api/v1
```

## 4. Ejecutar

```powershell
npm run dev        # desarrollo   -> http://localhost:5173
npm run build      # producción
npm run preview    # probar build (aquí funciona la PWA)
```

## 5. Login de desarrollo

El login está **simulado (mock)**. Usa cualquier correo válido:

- `pescador@sigepes.co`  -> rol FISHER
- `inspector@sigepes.co` -> rol INSPECTOR
- `admin@sigepes.co`     -> rol ADMIN

Los permisos se filtran automáticamente (verás distintas tarjetas en Home
y acceso o bloqueo en /app/dashboard y /app/administration/audit-logs).

## 6. Decisiones de arquitectura fijadas

- Estado remoto -> React Query. Estado de sesión/UI -> Zustand. Nunca al revés.
- Access token en memoria; refresh token en cookie HttpOnly (backend).
- RBAC en frontend solo oculta/bloquea UI; el backend valida cada operación.
- Estados de sincronización offline: LOCAL_DRAFT, PENDING_SYNC, SYNCING,
  SYNCHRONIZED, SYNC_ERROR, CONFLICT.
- Code splitting por ruta con React.lazy.

## 7. Próximos pasos sugeridos

1. Reemplazar el login mock por `POST /auth/login` real.
2. Implementar el módulo Trips (bitácora offline-first con IndexedDB).
3. Añadir MSW para simular `/api/v1` mientras el backend está en desarrollo.
4. Layout de autoridad/admin con menú lateral (Drawer permanente en desktop).
