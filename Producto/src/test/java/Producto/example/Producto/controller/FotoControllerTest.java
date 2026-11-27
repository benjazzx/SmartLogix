package Producto.example.Producto.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import Producto.example.Producto.model.CategoriaModel;
import Producto.example.Producto.model.ProductoModel;
import Producto.example.Producto.repository.ProductoRepository;

class FotoControllerTest {

    private final ProductoRepository repositorio = mock(ProductoRepository.class);
    private final FotoController controlador = new FotoController(repositorio);
    private final UUID id = UUID.randomUUID();

    private ProductoModel producto() {
        ProductoModel p = ProductoModel.builder().nombre("Caja").categoria(new CategoriaModel()).build();
        when(repositorio.findById(id)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void subirServirYEliminarFoto() throws Exception {
        ProductoModel p = producto();

        var subida = controlador.subirFoto(id, new MockMultipartFile("file", "a.png", "image/png", new byte[] {7}));
        assertEquals(200, subida.getStatusCode().value());
        assertEquals("image/png", p.getImagenTipo());
        assertEquals(1, controlador.servirFoto(id).getBody().length);

        controlador.eliminarFoto(id);
        assertNull(p.getImagenData());
        assertEquals(404, controlador.servirFoto(id).getStatusCode().value());
    }

    @Test
    void archivoVacio_responde400() throws Exception {
        var r = controlador.subirFoto(id, new MockMultipartFile("file", new byte[0]));
        assertEquals(400, r.getStatusCode().value());
    }

    @Test
    void productoInexistente_lanzaError() {
        when(repositorio.findById(id)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> controlador.servirFoto(id));
    }
}
