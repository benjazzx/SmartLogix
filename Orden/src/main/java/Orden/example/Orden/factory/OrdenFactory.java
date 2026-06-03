package Orden.example.Orden.factory;

import Orden.example.Orden.model.DetalleOrdenModel;
import Orden.example.Orden.model.HistorialModel;
import Orden.example.Orden.model.OrdenModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class OrdenFactory {

    private OrdenFactory() {}

    public static OrdenModel crearOrden(UUID userId, String userNombre, UUID direccionId) {
        OrdenModel orden = new OrdenModel();
        orden.setUserId(userId);
        orden.setUserNombre(userNombre);
        orden.setDireccionId(direccionId);
        orden.setFechaOrden(LocalDateTime.now());
        orden.setEstadoActual("pendiente");
        return orden;
    }

    public static DetalleOrdenModel crearDetalle(OrdenModel orden, UUID productoId,
                                                  String productoNombre, BigDecimal precioUnitario,
                                                  int cantidad) {
        DetalleOrdenModel det = new DetalleOrdenModel();
        det.setOrden(orden);
        det.setProductoId(productoId);
        det.setProductoNombre(productoNombre);
        det.setPrecioUnitario(precioUnitario);
        det.setCantidad(cantidad);
        return det;
    }

    public static HistorialModel crearHistorial(OrdenModel orden, UUID estadoId,
                                                 String estadoNombre, String comentario) {
        return crearHistorial(orden, estadoId, estadoNombre, comentario, null, null);
    }

    public static HistorialModel crearHistorial(OrdenModel orden, UUID estadoId,
                                                 String estadoNombre, String comentario,
                                                 UUID realizadoPorId, String realizadoPorNombre) {
        HistorialModel historial = new HistorialModel();
        historial.setOrden(orden);
        historial.setEstadoId(estadoId);
        historial.setEstadoNombre(estadoNombre);
        historial.setComentario(comentario);
        historial.setFecha(LocalDateTime.now());
        historial.setRealizadoPorId(realizadoPorId);
        historial.setRealizadoPorNombre(realizadoPorNombre);
        return historial;
    }
}
