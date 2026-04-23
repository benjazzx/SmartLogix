package Rol.example.Rol.component;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import Rol.example.Rol.model.PermisoModel;
import Rol.example.Rol.model.PrivilegioModel;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.repository.PermisoRepository;
import Rol.example.Rol.repository.PrivilegioRepository;
import Rol.example.Rol.repository.RolRepository;
import Rol.example.Rol.repository.TipoRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private RolRepository rolRepository;
    @Autowired private TipoRepository tipoRepository;
    @Autowired private PrivilegioRepository privilegioRepository;
    @Autowired private PermisoRepository permisoRepository;

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
        seedTipos();
        seedPrivilegios();
        seedPermisos();
    }

    // ─── ROLES ───────────────────────────────────────────────────────────────────
    private void seedRoles() {
        if (rolRepository.count() > 0) return;
        rolRepository.saveAll(List.of(
            new RolModel(null, "admin",         "Administrador con control total del sistema SmartLogix"),
            new RolModel(null, "bodeguero",     "Gestiona inventario y productos en bodega"),
            new RolModel(null, "transportista", "Coordina y ejecuta los envíos de pedidos"),
            new RolModel(null, "cliente",       "Usuario final que realiza pedidos en la plataforma")
        ));
        System.out.println("[DataInitializer] Roles insertados.");
    }

    // ─── TIPOS ───────────────────────────────────────────────────────────────────
    // Los tipos categorizan la naturaleza de cada privilegio
    private void seedTipos() {
        if (tipoRepository.count() > 0) return;
        tipoRepository.saveAll(List.of(
            new TipoModel(null, "LECTURA"),       // Solo ver información
            new TipoModel(null, "ESCRITURA"),     // Crear nuevos registros
            new TipoModel(null, "MODIFICACION"),  // Actualizar registros existentes
            new TipoModel(null, "GESTION"),       // CRUD completo sobre un módulo
            new TipoModel(null, "ADMINISTRACION") // Control total del sistema
        ));
        System.out.println("[DataInitializer] Tipos insertados.");
    }

    // ─── PRIVILEGIOS ─────────────────────────────────────────────────────────────
    // Alineados con los 3 módulos del caso semestral: Inventario, Pedidos, Envíos
    private void seedPrivilegios() {
        if (privilegioRepository.count() > 0) return;

        TipoModel lectura       = tipoRepository.findByNombre("LECTURA").orElseThrow();
        TipoModel escritura     = tipoRepository.findByNombre("ESCRITURA").orElseThrow();
        TipoModel modificacion  = tipoRepository.findByNombre("MODIFICACION").orElseThrow();
        TipoModel gestion       = tipoRepository.findByNombre("GESTION").orElseThrow();
        TipoModel administracion = tipoRepository.findByNombre("ADMINISTRACION").orElseThrow();

        privilegioRepository.saveAll(List.of(

            // --- Módulo: Gestión de Inventario ---
            new PrivilegioModel(null, "VER_INVENTARIO",       "Ver niveles de stock en bodega",                lectura),
            new PrivilegioModel(null, "ACTUALIZAR_STOCK",     "Modificar cantidad de stock de un producto",    modificacion),
            new PrivilegioModel(null, "GESTIONAR_INVENTARIO", "CRUD completo sobre el inventario",             gestion),

            // --- Módulo: Productos ---
            new PrivilegioModel(null, "VER_PRODUCTOS",        "Ver catálogo de productos disponibles",         lectura),
            new PrivilegioModel(null, "CREAR_PRODUCTO",       "Agregar nuevos productos al sistema",           escritura),
            new PrivilegioModel(null, "EDITAR_PRODUCTO",      "Modificar datos de un producto existente",      modificacion),
            new PrivilegioModel(null, "ELIMINAR_PRODUCTO",    "Eliminar productos del catálogo",               gestion),

            // --- Módulo: Procesamiento de Pedidos ---
            new PrivilegioModel(null, "VER_ORDENES",          "Ver pedidos del sistema",                       lectura),
            new PrivilegioModel(null, "CREAR_ORDEN",          "Crear nuevas órdenes de compra",                escritura),
            new PrivilegioModel(null, "ACTUALIZAR_ORDEN",     "Actualizar estado o datos de una orden",        modificacion),
            new PrivilegioModel(null, "GESTIONAR_ORDENES",    "CRUD completo sobre pedidos",                   gestion),

            // --- Módulo: Coordinación de Envíos ---
            new PrivilegioModel(null, "VER_ENVIOS",           "Ver estado de envíos en curso",                 lectura),
            new PrivilegioModel(null, "ACTUALIZAR_ENVIO",     "Actualizar estado o ruta de un envío",          modificacion),
            new PrivilegioModel(null, "GESTIONAR_ENVIOS",     "CRUD completo sobre envíos y rutas",            gestion),

            // --- Administración del sistema ---
            new PrivilegioModel(null, "VER_USUARIOS",         "Ver usuarios registrados en el sistema",        lectura),
            new PrivilegioModel(null, "GESTIONAR_USUARIOS",   "Administrar cuentas de usuario",                administracion),
            new PrivilegioModel(null, "GESTIONAR_ROLES",      "Administrar roles del sistema",                 administracion),
            new PrivilegioModel(null, "GESTIONAR_PERMISOS",   "Administrar permisos por rol",                  administracion)
        ));
        System.out.println("[DataInitializer] Privilegios insertados.");
    }

    // ─── PERMISOS ─────────────────────────────────────────────────────────────────
    // Define qué puede hacer cada rol según su responsabilidad en SmartLogix
    private void seedPermisos() {
        if (permisoRepository.count() > 0) return;

        RolModel admin        = rolRepository.findByNombre("admin").orElseThrow();
        RolModel bodeguero    = rolRepository.findByNombre("bodeguero").orElseThrow();
        RolModel transportista = rolRepository.findByNombre("transportista").orElseThrow();
        RolModel cliente      = rolRepository.findByNombre("cliente").orElseThrow();

        // Admin: acceso total a todo el sistema
        privilegioRepository.findAll().forEach(p ->
            permisoRepository.save(new PermisoModel(null, admin, p))
        );

        // Bodeguero: gestión de inventario y productos, puede ver órdenes para preparar despacho
        asignar(bodeguero,
            "VER_INVENTARIO", "ACTUALIZAR_STOCK", "GESTIONAR_INVENTARIO",
            "VER_PRODUCTOS", "CREAR_PRODUCTO", "EDITAR_PRODUCTO",
            "VER_ORDENES", "ACTUALIZAR_ORDEN"
        );

        // Transportista: gestiona envíos y puede ver órdenes para coordinar entregas
        asignar(transportista,
            "VER_ORDENES",
            "VER_ENVIOS", "ACTUALIZAR_ENVIO", "GESTIONAR_ENVIOS",
            "VER_INVENTARIO"
        );

        // Cliente: puede ver productos y hacer/ver sus pedidos
        asignar(cliente,
            "VER_PRODUCTOS",
            "VER_ORDENES", "CREAR_ORDEN"
        );

        System.out.println("[DataInitializer] Permisos insertados.");
    }

    // Método auxiliar para asignar múltiples privilegios a un rol
    private void asignar(RolModel rol, String... privilegios) {
        for (String nombre : privilegios) {
            PrivilegioModel p = privilegioRepository.findByNombre(nombre).orElseThrow();
            permisoRepository.save(new PermisoModel(null, rol, p));
        }
    }
}
