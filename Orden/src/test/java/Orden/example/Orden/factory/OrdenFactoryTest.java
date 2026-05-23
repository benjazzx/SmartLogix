package Orden.example.Orden.factory;

import Orden.example.Orden.model.DetalleOrdenModel;
import Orden.example.Orden.model.HistorialModel;
import Orden.example.Orden.model.OrdenModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrdenFactoryTest {

    @Test
    void crearOrden_debeInicializarCamposCorrectos() {
        UUID userId = UUID.randomUUID();
        UUID direccionId = UUID.randomUUID();

        OrdenModel orden = OrdenFactory.crearOrden(userId, "Juan García", direccionId);

        assertEquals(userId, orden.getUserId());
        assertEquals("Juan García", orden.getUserNombre());
        assertEquals(direccionId, orden.getDireccionId());
        assertEquals("pendiente", orden.getEstadoActual());
        assertNotNull(orden.getFechaOrden());
    }

    @Test
    void crearOrden_conNombreNull_debePermitirlo() {
        UUID userId = UUID.randomUUID();
        UUID direccionId = UUID.randomUUID();

        OrdenModel orden = OrdenFactory.crearOrden(userId, null, direccionId);

        assertNull(orden.getUserNombre());
        assertEquals("pendiente", orden.getEstadoActual());
    }

    @Test
    void crearDetalle_debeInicializarCamposCorrectos() {
        OrdenModel orden = OrdenFactory.crearOrden(UUID.randomUUID(), "Test", UUID.randomUUID());
        UUID productoId = UUID.randomUUID();
        BigDecimal precio = new BigDecimal("499.99");

        DetalleOrdenModel detalle = OrdenFactory.crearDetalle(orden, productoId, "Laptop", precio, 3);

        assertEquals(orden, detalle.getOrden());
        assertEquals(productoId, detalle.getProductoId());
        assertEquals("Laptop", detalle.getProductoNombre());
        assertEquals(0, precio.compareTo(detalle.getPrecioUnitario()));
        assertEquals(3, detalle.getCantidad());
    }

    @Test
    void crearDetalle_conPrecioNull_debePermitirlo() {
        OrdenModel orden = OrdenFactory.crearOrden(UUID.randomUUID(), "Test", UUID.randomUUID());

        DetalleOrdenModel detalle = OrdenFactory.crearDetalle(orden, UUID.randomUUID(), "Producto", null, 1);

        assertNull(detalle.getPrecioUnitario());
        assertEquals(1, detalle.getCantidad());
    }

    @Test
    void crearHistorial_debeInicializarCamposCorrectos() {
        OrdenModel orden = OrdenFactory.crearOrden(UUID.randomUUID(), "Test", UUID.randomUUID());
        UUID estadoId = UUID.randomUUID();

        HistorialModel historial = OrdenFactory.crearHistorial(orden, estadoId, "enviado", "En camino");

        assertEquals(orden, historial.getOrden());
        assertEquals(estadoId, historial.getEstadoId());
        assertEquals("enviado", historial.getEstadoNombre());
        assertEquals("En camino", historial.getComentario());
        assertNotNull(historial.getFecha());
    }

    @Test
    void crearHistorial_sinComentario_debePermitirNull() {
        OrdenModel orden = OrdenFactory.crearOrden(UUID.randomUUID(), "Test", UUID.randomUUID());

        HistorialModel historial = OrdenFactory.crearHistorial(orden, UUID.randomUUID(), "pendiente", null);

        assertNull(historial.getComentario());
        assertEquals("pendiente", historial.getEstadoNombre());
    }
}
