package Configuracion.example.Configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.DriverManager;
import java.sql.Statement;

@SpringBootApplication
public class ConfiguracionApplication {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionApplication.class);

    public static void main(String[] args) {
        createDatabaseIfNotExists();
        SpringApplication.run(ConfiguracionApplication.class, args);
    }

    /**
     * Conecta a la BD 'postgres' (admin) e intenta crear 'Configuracion'.
     * Si ya existe, PostgreSQL lanza un error que se ignora silenciosamente.
     * Se ejecuta ANTES de que Spring inicialice el DataSource.
     */
    private static void createDatabaseIfNotExists() {
        String url      = System.getenv().getOrDefault("SPRING_DATASOURCE_URL",
                          "jdbc:postgresql://localhost:5432/Configuracion");
        String username = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "postgres");
        String password = System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", "postgres");

        // Extraer nombre de la BD desde la URL y conectarse a 'postgres' (BD de admin)
        String dbName  = url.substring(url.lastIndexOf('/') + 1).split("\\?")[0];
        String adminUrl = url.substring(0, url.lastIndexOf('/')) + "/postgres";

        try (var conn = DriverManager.getConnection(adminUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE \"" + dbName + "\"");
            log.info("Base de datos '{}' creada.", dbName);
        } catch (Exception e) {
            log.debug("Base de datos '{}' ya existe (normal): {}", dbName, e.getMessage());
        }
    }
}
