package Producto.example.Producto.messaging;

import Producto.example.Producto.dto.ProductoActualizadoEvent;
import Producto.example.Producto.model.CategoriaModel;
import Producto.example.Producto.model.ProductoModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoEventProducerTest {

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private ProductoEventProducer productoEventProducer;

    private ProductoModel buildProducto() {
        CategoriaModel cat = mock(CategoriaModel.class);
        when(cat.getNombre()).thenReturn("Electrónica");

        ProductoModel p = mock(ProductoModel.class);
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getNombre()).thenReturn("Laptop Test");
        when(p.getPrecio()).thenReturn(new BigDecimal("699990"));
        when(p.getStock()).thenReturn(10);
        when(p.getCategoria()).thenReturn(cat);
        when(p.getEstadoNombre()).thenReturn("publicado");
        when(p.getActivo()).thenReturn(true);
        return p;
    }

    @Test
    void publishProductoActualizado_enviaoAlTopicoCorrecto() {
        ProductoModel p = buildProducto();

        productoEventProducer.publishProductoActualizado(p, ProductoActualizadoEvent.TipoEvento.CREADO);

        verify(streamBridge).send(eq("producto-actualizado-topic"), any(ProductoActualizadoEvent.class));
    }

    @Test
    void publishProductoActualizado_eventoConDatosCorrecto() {
        ProductoModel p = buildProducto();

        productoEventProducer.publishProductoActualizado(p, ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO);

        ArgumentCaptor<ProductoActualizadoEvent> captor = ArgumentCaptor.forClass(ProductoActualizadoEvent.class);
        verify(streamBridge).send(eq("producto-actualizado-topic"), captor.capture());

        ProductoActualizadoEvent event = captor.getValue();
        assertEquals("Laptop Test", event.getNombre());
        assertEquals(ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO, event.getTipoEvento());
        assertEquals("Electrónica", event.getCategoriaNombre());
    }

    @Test
    void publishProductoActualizado_distintoTipoEvento_enviaCorrectamente() {
        ProductoModel p = buildProducto();

        productoEventProducer.publishProductoActualizado(p, ProductoActualizadoEvent.TipoEvento.DESACTIVADO);

        ArgumentCaptor<ProductoActualizadoEvent> captor = ArgumentCaptor.forClass(ProductoActualizadoEvent.class);
        verify(streamBridge).send(anyString(), captor.capture());
        assertEquals(ProductoActualizadoEvent.TipoEvento.DESACTIVADO, captor.getValue().getTipoEvento());
    }
}
