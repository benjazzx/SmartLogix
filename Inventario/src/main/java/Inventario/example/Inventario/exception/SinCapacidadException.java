package Inventario.example.Inventario.exception;

// No hay ubicaciones habilitadas ni bodegas activas para asignar: es un fallo recuperable
// (el administrador puede habilitar capacidad), por eso el consumidor lo reintenta una vez.
public class SinCapacidadException extends RuntimeException {

    public SinCapacidadException(String message) {
        super(message);
    }
}
