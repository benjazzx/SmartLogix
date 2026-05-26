# SmartLogix — Frontend Angular

Aplicación web de gestión logística. SPA construida con Angular 21 y Tailwind CSS, conectada al backend a través del API Gateway (BFF).

**Repositorio backend:** https://github.com/benjazzx/SmartLogix

## Stack técnico

| Componente | Versión | Propósito |
|---|---|---|
| Angular | 21 | Framework principal SPA (Standalone Components) |
| TypeScript | 5.x | Tipado estático |
| Tailwind CSS | 3.x | Estilos utilitarios |
| RxJS | 7.x | Programación reactiva (BehaviorSubject) |
| Angular HTTP Client | — | Comunicación REST con el Gateway/BFF |

## Prerrequisitos

- Node.js 18+
- npm 9+
- Backend SmartLogix corriendo (Docker Compose en el repo backend)

## Instalación y ejecución

```bash
# Clonar repositorio
git clone https://github.com/vvillalobos7/SmartLogix.git
cd SmartLogix

# Instalar dependencias
npm install

# Servidor de desarrollo
npm start
# Disponible en http://localhost:4200
```

## Variables de entorno

Configuradas en `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  services: {
    gateway:    'http://localhost:8080/api',
    usuarios:   'http://localhost:8080/api/usuarios',
    ordenes:    'http://localhost:8080/api/ordenes',
    inventario: 'http://localhost:8080/api/inventario',
    productos:  'http://localhost:8080/api/productos',
  }
};
```

## Estructura de módulos

```
src/app/
├── core/
│   ├── guards/          → authGuard, roleGuard
│   ├── interceptors/    → authInterceptor (JWT), errorInterceptor
│   └── services/        → AuthService, ToastService
├── features/
│   ├── login/           → Inicio de sesión
│   ├── registro/        → Registro de clientes
│   ├── recuperar/       → Recuperación de contraseña
│   ├── dashboard/       → KPIs diferenciados por rol
│   ├── productos/       → Catálogo, CRUD, filtros país/bodega, imágenes
│   ├── inventario/      → Bodegas, pasillos, estantes
│   ├── ordenes/         → Órdenes, historial de estados, nuevo pedido
│   ├── envios/          → Seguimiento de envíos (transportista)
│   ├── usuarios/        → Gestión de usuarios y asignación de roles
│   └── perfil/          → Mi Perfil: dirección de entrega (solo cliente)
└── shared/
    ├── models/          → models.ts — interfaces TypeScript del dominio
    └── components/      → ToastComponent
```

## Rutas y roles

| Ruta | Roles permitidos |
|---|---|
| `/dashboard` | admin, bodeguero, transportista, cliente |
| `/productos` | admin, bodeguero |
| `/inventario` | admin, bodeguero |
| `/ordenes` | admin, bodeguero, cliente |
| `/envios` | admin, transportista |
| `/usuarios` | admin |
| `/perfil` | cliente |

## Credenciales de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Admin | admin@smartlogix.cl | admin123 |
| Bodeguero | bodeguero@smartlogix.cl | bodega123 |
| Transportista | transportista@smartlogix.cl | trans123 |
| Cliente | cliente@smartlogix.cl | cliente123 |

## Flujo de trabajo Git (Git Flow)

```
main      ──●──────────────────────────────────●── (producción)
             \                                /
develop        ●──●──●──●──●──●──●──●──●──●    (integración)
                \__/    \__/    \__/
              feature  feature  feature         (desarrollo)
```

| Rama | Propósito |
|---|---|
| `main` | Código estable listo para producción. |
| `develop` | Integración de features. Base de trabajo del equipo. |
| `feature/*` | Una rama por funcionalidad (ej: `feature/dashboard-transportista`). |

**Convención de commits:**
```
feat(modulo):  nueva funcionalidad
fix(modulo):   corrección de bug
docs:          documentación / README
chore:         tareas de mantenimiento
```

## Patrones aplicados

- **BFF** — toda comunicación pasa por el Gateway; el frontend nunca llama microservicios directamente.
- **Observer (RxJS BehaviorSubject)** — estado reactivo en cada service.
- **Interceptor** — JWT automático y manejo global de errores 401/403.
- **Guard** — `authGuard` para rutas autenticadas; `roleGuard` para restricción por rol.
- **Lazy Loading** — cada feature se carga bajo demanda con `loadComponent`.
