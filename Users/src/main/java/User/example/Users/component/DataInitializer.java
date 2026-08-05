package User.example.Users.component;

import User.example.Users.dto.UserRequestDto;
import User.example.Users.model.*;
import User.example.Users.repository.*;
import User.example.Users.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Profile("dev")
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private RegionRepository regionRepository;
    @Autowired private ComunaRepository comunaRepository;
    @Autowired private DireccionRepository direccionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;

    // Contraseñas de las cuentas semilla (solo perfil "dev") — deben venir de variables
    // de entorno propias de cada ambiente, nunca quedar hardcodeadas en el código fuente.
    @Value("${SEED_ADMIN_PASSWORD:}")         private String seedAdminPassword;
    @Value("${SEED_BODEGUERO_PASSWORD:}")     private String seedBodegueroPassword;
    @Value("${SEED_TRANSPORTISTA_PASSWORD:}") private String seedTransportistaPassword;
    @Value("${SEED_CLIENTE_PASSWORD:}")       private String seedClientePassword;

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
        log.info("[DataInitializer] Regiones insertadas.");
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
        log.info("[DataInitializer] Comunas insertadas.");
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
        log.info("[DataInitializer] Direcciones insertadas.");
    }

    private void corregirRolExistente(String correo, String rolNombre) {
        userRepository.findByCorreo(correo).ifPresent(user -> {
            if (!rolNombre.equals(user.getRolNombre())) {
                user.setRolNombre(rolNombre);
                user.setActivo(true);
                userRepository.save(user);
                log.info("[DataInitializer] Rol corregido: {} -> {}", correo, rolNombre);
            }
        });
    }

    private void seedUsuarios() {
        corregirRolExistente("admin@smartlogix.cl",         "admin");
        corregirRolExistente("bodeguero@smartlogix.cl",     "bodeguero");
        corregirRolExistente("transportista@smartlogix.cl", "transportista");
        if (userRepository.count() > 0) return;

        if (isBlank(seedAdminPassword) || isBlank(seedBodegueroPassword)
                || isBlank(seedTransportistaPassword) || isBlank(seedClientePassword)) {
            throw new IllegalStateException(
                "Faltan variables de entorno SEED_ADMIN_PASSWORD / SEED_BODEGUERO_PASSWORD / "
                + "SEED_TRANSPORTISTA_PASSWORD / SEED_CLIENTE_PASSWORD. No se hardcodean "
                + "contraseñas de las cuentas semilla en el código — defínelas en tu .env local.");
        }

        List<DireccionModel> dirs = direccionRepository.findAll();

        UserRequestDto admin = new UserRequestDto();
        admin.setNombre("Admin"); admin.setApellido("Sistema");
        admin.setRut("00000000-0"); admin.setCorreo("admin@smartlogix.cl");
        admin.setClave(seedAdminPassword); admin.setCargo("Administrador");
        admin.setRolNombre("admin"); admin.setDireccionId(dirs.get(0).getId());

        UserRequestDto bodeguero = new UserRequestDto();
        bodeguero.setNombre("Juan"); bodeguero.setApellido("Pérez");
        bodeguero.setRut("11111111-1"); bodeguero.setCorreo("bodeguero@smartlogix.cl");
        bodeguero.setClave(seedBodegueroPassword); bodeguero.setCargo("Bodeguero");
        bodeguero.setRolNombre("bodeguero"); bodeguero.setDireccionId(dirs.get(0).getId());

        UserRequestDto transportista = new UserRequestDto();
        transportista.setNombre("Carlos"); transportista.setApellido("González");
        transportista.setRut("22222222-2"); transportista.setCorreo("transportista@smartlogix.cl");
        transportista.setClave(seedTransportistaPassword); transportista.setCargo("Transportista");
        transportista.setRolNombre("transportista"); transportista.setDireccionId(dirs.get(1).getId());

        UserRequestDto cliente = new UserRequestDto();
        cliente.setNombre("María"); cliente.setApellido("López");
        cliente.setRut("33333333-3"); cliente.setCorreo("cliente@smartlogix.cl");
        cliente.setClave(seedClientePassword); cliente.setRolNombre("cliente");
        cliente.setDireccionId(dirs.get(2).getId());

        for (UserRequestDto dto : List.of(admin, bodeguero, transportista, cliente)) {
            userService.createUser(dto);
        }
        log.info("[DataInitializer] Usuarios insertados y eventos user-created-topic publicados.");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
