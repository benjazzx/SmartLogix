package User.example.Users.exception;

// Falla al hablar con la API de administración de RabbitMQ (no disponible, credenciales, timeout).
public class RabbitAdminException extends RuntimeException {

    public RabbitAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
