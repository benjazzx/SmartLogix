package Orden.example.Orden.exception;

// Señala que una llamada a otro microservicio fallo por una causa transitoria
// (circuit breaker abierto, timeout, conexion rechazada) en vez de un rechazo
// de negocio real (404 producto inexistente, 409 stock insuficiente). Se usa
// para distinguir "el sistema esta ocupado, reintenta" de "esto realmente no
// se puede hacer" -- antes ambos casos devolvian el mismo mensaje generico.
public class ServicioOcupadoException extends RuntimeException {

    public ServicioOcupadoException(String message) {
        super(message);
    }
}
