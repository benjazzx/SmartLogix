import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';
import { OrdenService } from './features/ordenes/orden.service';
import { ProductoService } from './features/productos/producto.service';
import { UsuarioSesion } from './shared/models/models';
import { combineLatest, Subscription } from 'rxjs';
import { ToastComponent } from './shared/components/toast/toast.component';

interface NavItem {
  label: string;
  route: string;
  iconPath: string;
  roles: string[];
}

interface Notificacion {
  tipo: 'danger' | 'warning' | 'info';
  icono: string;
  texto: string;
  ruta: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'SmartLogix';
  showNotifPanel = false;
  notificacionesList: Notificacion[] = [];
  private notifSub!: Subscription;

  private readonly allNavItems: NavItem[] = [
    {
      label: 'Dashboard', route: '/dashboard',
      roles: ['admin', 'bodeguero', 'transportista', 'cliente'],
      iconPath: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
    },
    {
      label: 'Productos', route: '/productos',
      roles: ['admin', 'bodeguero'],
      iconPath: 'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4',
    },
    {
      label: 'Inventario', route: '/inventario',
      roles: ['admin', 'bodeguero'],
      iconPath: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01',
    },
    {
      label: 'Órdenes', route: '/ordenes',
      roles: ['admin', 'bodeguero', 'transportista', 'cliente'],
      iconPath: 'M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z',
    },
    {
      label: 'Envíos', route: '/envios',
      roles: ['admin', 'bodeguero', 'transportista'],
      iconPath: 'M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4',
    },
    {
      label: 'Usuarios', route: '/usuarios',
      roles: ['admin'],
      iconPath: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z',
    },
    {
      label: 'Roles', route: '/roles',
      roles: ['admin'],
      iconPath: 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z',
    },
  ];

  constructor(
    public authService: AuthService,
    private ordenService: OrdenService,
    private productoService: ProductoService,
  ) {}

  ngOnInit(): void {
    this.notifSub = combineLatest([
      this.ordenService.ordenes$,
      this.productoService.productos$,
    ]).subscribe(([ordenes, productos]) => {
      const lista: Notificacion[] = [];

      const pendientes = ordenes.filter(o => o.estadoActual === 'Pendiente').length;
      if (pendientes > 0) lista.push({
        tipo: 'warning',
        icono: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2',
        texto: `${pendientes} orden${pendientes !== 1 ? 'es' : ''} pendiente${pendientes !== 1 ? 's' : ''} de procesar`,
        ruta: '/ordenes',
      });

      const enTransito = ordenes.filter(o => o.estadoActual === 'En tránsito').length;
      if (enTransito > 0) lista.push({
        tipo: 'info',
        icono: 'M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4',
        texto: `${enTransito} envío${enTransito !== 1 ? 's' : ''} activo${enTransito !== 1 ? 's' : ''} en ruta`,
        ruta: '/envios',
      });

      const agotados = productos.filter(p => p.stock === 0).length;
      if (agotados > 0) lista.push({
        tipo: 'danger',
        icono: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z',
        texto: `${agotados} producto${agotados !== 1 ? 's' : ''} sin stock`,
        ruta: '/productos',
      });

      const stockBajo = productos.filter(p => (p.stock ?? 0) > 0 && (p.stock ?? 0) <= 10).length;
      if (stockBajo > 0) lista.push({
        tipo: 'warning',
        icono: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z',
        texto: `${stockBajo} producto${stockBajo !== 1 ? 's' : ''} con stock bajo`,
        ruta: '/productos',
      });

      this.notificacionesList = lista;
    });
  }

  ngOnDestroy(): void { this.notifSub?.unsubscribe(); }

  get notificaciones(): number { return this.notificacionesList.length; }

  toggleNotifPanel(): void { this.showNotifPanel = !this.showNotifPanel; }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const t = event.target as HTMLElement;
    if (!t.closest('#notif-btn') && !t.closest('#notif-panel')) {
      this.showNotifPanel = false;
    }
  }

  get currentUser(): UsuarioSesion | null {
    return this.authService.getCurrentUser();
  }

  get navItems(): NavItem[] {
    const rol = this.currentUser?.rolNombre;
    if (!rol) return [];
    return this.allNavItems.filter(item => item.roles.includes(rol));
  }

  get userInitials(): string {
    return this.authService.getInitials(this.currentUser?.correo ?? 'U');
  }

  getRouteTitle(): string {
    if (typeof window === 'undefined') return 'SmartLogix';
    const map: Record<string, string> = {
      dashboard: 'Dashboard', productos: 'Productos', inventario: 'Inventario',
      ordenes: 'Órdenes', envios: 'Envíos', estados: 'Estados', usuarios: 'Usuarios', roles: 'Roles',
    };
    return map[window.location.pathname.split('/')[1]] ?? 'SmartLogix';
  }

  logout(): void { this.authService.logout(); }
}
