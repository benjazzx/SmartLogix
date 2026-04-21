package Estado.example.Estado.Component;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import Estado.example.Estado.Model.Estado;
import Estado.example.Estado.Model.TipoDeEstadoModel;
import Estado.example.Estado.Repository.EstadoRepository;
import Estado.example.Estado.Repository.TipoDeEstadoRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private TipoDeEstadoRepository tipoDeEstadoRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Override
    public void run(String... args) throws Exception {
        if (tipoDeEstadoRepository.count() == 0) {
            TipoDeEstadoModel cuenta = tipoDeEstadoRepository.save(
                new TipoDeEstadoModel(null, "cuenta", "Tipo de estado para cuentas de usuario")
            );
            TipoDeEstadoModel laboral = tipoDeEstadoRepository.save(
                new TipoDeEstadoModel(null, "laboral", "Tipo de estado para empleados de la empresa")
            );
            TipoDeEstadoModel producto = tipoDeEstadoRepository.save(
                new TipoDeEstadoModel(null, "producto", "Tipo de estado para publicaciones de productos en el marketplace")
            );
            TipoDeEstadoModel envio = tipoDeEstadoRepository.save(
                new TipoDeEstadoModel(null, "envio", "Tipo de estado para el envío de un pedido")
            );
            List<Estado> estados = Arrays.asList(
                // cuenta
                new Estado(null, "activo",                 "Usuario activo en el sistema",                         cuenta),
                new Estado(null, "inactivo",               "Usuario inactivo en el sistema",                       cuenta),
                new Estado(null, "suspendido",             "Usuario suspendido por incumplimiento",                 cuenta),
                new Estado(null, "pendiente_verificacion", "Cuenta pendiente de verificación",                     cuenta),
                // laboral
                new Estado(null, "disponible",             "Empleado disponible para tareas",                      laboral),
                new Estado(null, "ocupado",                "Empleado actualmente ocupado con una tarea",           laboral),
                new Estado(null, "en_descanso",            "Empleado en período de descanso",                     laboral),
                // producto
                new Estado(null, "publicado",              "Producto visible y disponible para compra",            producto),
                new Estado(null, "pausado",                "Publicación pausada temporalmente por el vendedor",    producto),
                new Estado(null, "bajo_stock",             "Producto con pocas unidades disponibles",              producto),
                new Estado(null, "sin_stock",              "Producto agotado, no disponible para compra",          producto),
                new Estado(null, "pendiente_revision",     "Producto en revisión antes de ser publicado",          producto),
                new Estado(null, "descontinuado",          "Producto retirado permanentemente del marketplace",    producto),
                // envio
                new Estado(null, "pendiente_envio",        "Pedido confirmado, aún no despachado",                 envio),
                new Estado(null, "en_preparacion",         "Pedido siendo preparado por el bodeguero",             envio),
                new Estado(null, "despachado",             "Pedido entregado al transportista",                    envio),
                new Estado(null, "en_camino",              "Pedido en tránsito hacia el destinatario",             envio),
                new Estado(null, "entregado",              "Pedido entregado exitosamente al cliente",             envio),
                new Estado(null, "devuelto",               "Pedido devuelto al vendedor",                         envio)
            );
            estadoRepository.saveAll(estados);

            System.out.println("Tipos de estado y estados iniciales insertados en la base de datos.");
        }
    }
}
