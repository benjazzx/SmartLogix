package User.example.Users.component;

import User.example.Users.model.*;
import User.example.Users.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private RegionRepository regionRepository;
    @Autowired private ComunaRepository comunaRepository;
    @Autowired private DireccionRepository direccionRepository;
    @Autowired private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        seedRegiones();
        seedComunas();
        seedDirecciones();
        seedUsuarios();
    }

    private void seedRegiones() {
        if (regionRepository.count() > 0) return;
        regionRepository.saveAll(List.of(
            new RegionModel(null, "Región Metropolitana"),
            new RegionModel(null, "Región de Valparaíso"),
            new RegionModel(null, "Región del Biobío")
        ));
        System.out.println("[DataInitializer] Regiones insertadas.");
    }

    private void seedComunas() {
        if (comunaRepository.count() > 0) return;
        RegionModel rm = regionRepository.findByNombre("Región Metropolitana").orElseThrow();
        RegionModel valpo = regionRepository.findByNombre("Región de Valparaíso").orElseThrow();
        comunaRepository.saveAll(List.of(
            new ComunaModel(null, "Santiago",    rm),
            new ComunaModel(null, "Providencia", rm),
            new ComunaModel(null, "Las Condes",  rm),
            new ComunaModel(null, "Viña del Mar", valpo)
        ));
        System.out.println("[DataInitializer] Comunas insertadas.");
    }

    private void seedDirecciones() {
        if (direccionRepository.count() > 0) return;
        ComunaModel santiago    = comunaRepository.findByNombre("Santiago").orElseThrow();
        ComunaModel providencia = comunaRepository.findByNombre("Providencia").orElseThrow();
        ComunaModel lasCondes   = comunaRepository.findByNombre("Las Condes").orElseThrow();
        direccionRepository.saveAll(List.of(
            new DireccionModel(null, "Av. Libertador Bernardo O'Higgins", "1234", "8320000", santiago),
            new DireccionModel(null, "Av. Providencia",                  "2345", "7500000", providencia),
            new DireccionModel(null, "Av. Apoquindo",                    "6500", "7550000", lasCondes)
        ));
        System.out.println("[DataInitializer] Direcciones insertadas.");
    }

    private void seedUsuarios() {
        if (userRepository.count() > 0) return;

        DireccionModel dir1 = direccionRepository.findAll().get(0);
        DireccionModel dir2 = direccionRepository.findAll().get(1);
        DireccionModel dir3 = direccionRepository.findAll().get(2);

        userRepository.saveAll(List.of(
            // Admin: acceso total al sistema
            new UserModel(null, "Admin", "Sistema", "00000000-0",
                "admin@smartlogix.cl",    "admin123",    "Administrador", true,
                null, "admin", null, null, dir1),

            // Bodeguero: gestiona inventario y productos
            new UserModel(null, "Juan", "Pérez", "11111111-1",
                "bodeguero@smartlogix.cl", "bodega123",  "Bodeguero", true,
                null, "bodeguero", null, null, dir1),

            // Transportista: coordina envíos
            new UserModel(null, "Carlos", "González", "22222222-2",
                "transportista@smartlogix.cl", "trans123", "Transportista", true,
                null, "transportista", null, null, dir2),

            // Cliente: realiza pedidos
            new UserModel(null, "María", "López", "33333333-3",
                "cliente@smartlogix.cl",  "cliente123",  null, true,
                null, "cliente", null, null, dir3)
        ));
        System.out.println("[DataInitializer] Usuarios insertados.");
    }
}
