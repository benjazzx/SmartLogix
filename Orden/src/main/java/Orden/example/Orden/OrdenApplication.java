package Orden.example.Orden;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.DriverManager;
import java.sql.Statement;

@SpringBootApplication
public class OrdenApplication {

	private static final Logger log = LoggerFactory.getLogger(OrdenApplication.class);

	public static void main(String[] args) {
		createDatabaseIfNotExists();
		SpringApplication.run(OrdenApplication.class, args);
	}

	private static void createDatabaseIfNotExists() {
		String url      = System.getenv().getOrDefault("SPRING_DATASOURCE_URL",
		                  "jdbc:postgresql://localhost:5432/Orden");
		String username = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "postgres");
		String password = System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", "postgres");

		String dbName   = url.substring(url.lastIndexOf('/') + 1).split("\\?")[0];
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
