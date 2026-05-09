package Orden.example.Orden.service;

import Orden.example.Orden.client.EstadoClient;
import Orden.example.Orden.client.ProductoClient;
import Orden.example.Orden.client.UsersClient;
import Orden.example.Orden.dto.*;
import Orden.example.Orden.messaging.OrdenEventProducer;
import Orden.example.Orden.model.DetalleOrdenModel;
import Orden.example.Orden.model.HistorialModel;
import Orden.example.Orden.model.OrdenModel;
import Orden.example.Orden.repository.HistorialRepository;
import Orden.example.Orden.repository.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrdenServiceTest {

    @InjectMocks
    private OrdenService ordenService;

    @Mock private OrdenRepository ordenRepository;
    @Mock private HistorialRepository historialRepository;
    @Mock private UsersClient usersClient;
    @Mock private EstadoClient estadoClient;
    @Mock private ProductoClient productoClient;
    @Mock private OrdenEventProducer eventProducer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private OrdenModel ordenSample(UUID userId) {
        OrdenModel orden = new OrdenModel();
        orden.setId(1L);
        orden.setUserId(userId);
        orden.setUserNombre("Juan García");
        orden.setDireccionId(UUID.randomUUID());
        orden.setEstadoActual("pendiente");
        orden.setFechaOrden(LocalDateTime.now());
        return orden;
    }

    private Map<String, Object> productoDataSample() {
        Map<String, Object> data = new HashMap<>();
        data.put("nombre", "Laptop Pro");
        data.put("precio", 999.99);
        return data;
    }

    @Test
    void createOrden_conUsuarioEncontrado_debeCrearOrdenConNombreDelServicio() {
        UUID userId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        OrdenRequestDto.DetalleDto detalleRequest = new OrdenRequestDto.DetalleDto();
        detalleRequest.setProductoId(productoId);
        detalleRequest.setCantidad(2);

        OrdenRequestDto dto = new OrdenRequestDto();
        dto.setUserNombre("Juan Request");
        dto.setDireccionId(UUID.randomUUID());
        dto.setDetalles(List.of(detalleRequest));

        when(usersClient.getNombreUsuario(userId)).thenReturn("Juan García");
        when(productoClient.getProducto(productoId)).thenReturn(productoDataSample());

        OrdenModel saved = ordenSample(userId);
        DetalleOrdenModel detalle = new DetalleOrdenModel();
        detalle.setProductoId(productoId);
        detalle.setProductoNombre("Laptop Pro");
        detalle.setPrecioUnitario(new BigDecimal("999.99"));
        detalle.setCantidad(2);
        saved.getDetalles().add(detalle);

        when(ordenRepository.save(any(OrdenModel.class))).thenReturn(saved);
        doNothing().when(eventProducer).publishOrdenCreada(any());

        OrdenResponseDto result = ordenService.createOrden(dto, userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("Juan García", result.getUserNombre());
        verify(ordenRepository, times(1)).save(any());
        verify(eventProducer, times(1)).publishOrdenCreada(any());
    }

    @Test
    void createOrden_usersClientRetornaNull_usaNombreDelRequest() {
        UUID userId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        OrdenRequestDto.DetalleDto detalleRequest = new OrdenRequestDto.DetalleDto();
        detalleRequest.setProductoId(productoId);
        detalleRequest.setCantidad(1);

        OrdenRequestDto dto = new OrdenRequestDto();
        dto.setUserNombre("Nombre Fallback");
        dto.setDireccionId(UUID.randomUUID());
        dto.setDetalles(List.of(detalleRequest));

        when(usersClient.getNombreUsuario(userId)).thenReturn(null);
        when(productoClient.getProducto(productoId)).thenReturn(productoDataSample());

        OrdenModel saved = ordenSample(userId);
        saved.setUserNombre("Nombre Fallback");
        when(ordenRepository.save(any())).thenReturn(saved);
        doNothing().when(eventProducer).publishOrdenCreada(any());

        OrdenResponseDto result = ordenService.createOrden(dto, userId);

        assertNotNull(result);
        assertEquals("Nombre Fallback", result.getUserNombre());
    }

    @Test
    void createOrden_productoClientRetornaNull_lanzaExcepcion() {
        UUID userId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        OrdenRequestDto.DetalleDto detalleRequest = new OrdenRequestDto.DetalleDto();
        detalleRequest.setProductoId(productoId);
        detalleRequest.setCantidad(1);

        OrdenRequestDto dto = new OrdenRequestDto();
        dto.setUserNombre("Usuario");
        dto.setDireccionId(UUID.randomUUID());
        dto.setDetalles(List.of(detalleRequest));

        when(usersClient.getNombreUsuario(userId)).thenReturn("Usuario");
        when(productoClient.getProducto(productoId)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> ordenService.createOrden(dto, userId));
    }

    @Test
    void getById_rolAdmin_puedeVerCualquierOrden() {
        UUID adminId = UUID.randomUUID();
        UUID otroUsuarioId = UUID.randomUUID();
        OrdenModel orden = ordenSample(otroUsuarioId);
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        OrdenResponseDto result = ordenService.getById(1L, adminId, "admin");

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_rolBodeguero_puedeVerCualquierOrden() {
        UUID bodegueroId = UUID.randomUUID();
        OrdenModel orden = ordenSample(UUID.randomUUID());
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        OrdenResponseDto result = ordenService.getById(1L, bodegueroId, "bodeguero");

        assertNotNull(result);
    }

    @Test
    void getById_rolCliente_puedeVerSuPropia() {
        UUID userId = UUID.randomUUID();
        OrdenModel orden = ordenSample(userId);
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        OrdenResponseDto result = ordenService.getById(1L, userId, "cliente");

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    void getById_rolCliente_noAccedeAOrdenAjena_debeLanzarExcepcion() {
        UUID clienteId = UUID.randomUUID();
        UUID otroId = UUID.randomUUID();
        OrdenModel orden = ordenSample(otroId);
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        assertThrows(RuntimeException.class, () -> ordenService.getById(1L, clienteId, "cliente"));
    }

    @Test
    void getById_ordenNoExistente_debeLanzarRuntimeException() {
        when(ordenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> ordenService.getById(99L, UUID.randomUUID(), "admin"));
    }

    @Test
    void getMisOrdenes_debeRetornarOrdenesDelUsuario() {
        UUID userId = UUID.randomUUID();
        when(ordenRepository.findByUserId(userId)).thenReturn(List.of(ordenSample(userId)));

        List<OrdenResponseDto> result = ordenService.getMisOrdenes(userId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
    }

    @Test
    void getMisOrdenes_sinOrdenes_debeRetornarListaVacia() {
        UUID userId = UUID.randomUUID();
        when(ordenRepository.findByUserId(userId)).thenReturn(List.of());

        List<OrdenResponseDto> result = ordenService.getMisOrdenes(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAll_debeRetornarTodasLasOrdenes() {
        when(ordenRepository.findAll()).thenReturn(List.of(
                ordenSample(UUID.randomUUID()),
                ordenSample(UUID.randomUUID())
        ));

        List<OrdenResponseDto> result = ordenService.getAll();

        assertEquals(2, result.size());
    }

    @Test
    void addHistorial_admin_debeAgregarHistorialYPublicarEvento() {
        UUID userId = UUID.randomUUID();
        UUID estadoId = UUID.randomUUID();
        OrdenModel orden = ordenSample(userId);

        HistorialRequestDto dto = new HistorialRequestDto();
        dto.setEstadoId(estadoId);
        dto.setEstadoNombre("enviado");
        dto.setComentario("Paquete en camino");

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(estadoClient.existeEstado(estadoId)).thenReturn(true);

        HistorialModel historial = new HistorialModel();
        historial.setId(1L);
        historial.setEstadoId(estadoId);
        historial.setEstadoNombre("enviado");
        historial.setFecha(LocalDateTime.now());
        when(historialRepository.save(any())).thenReturn(historial);
        when(ordenRepository.save(any())).thenReturn(orden);
        doNothing().when(eventProducer).publishEstadoOrden(any());

        OrdenResponseDto result = ordenService.addHistorial(1L, dto, userId, "admin");

        assertNotNull(result);
        assertEquals("enviado", orden.getEstadoActual());
        verify(historialRepository, times(1)).save(any());
        verify(eventProducer, times(1)).publishEstadoOrden(any());
    }

    @Test
    void addHistorial_clienteOrdenPropia_debePermitir() {
        UUID userId = UUID.randomUUID();
        UUID estadoId = UUID.randomUUID();
        OrdenModel orden = ordenSample(userId);

        HistorialRequestDto dto = new HistorialRequestDto();
        dto.setEstadoId(estadoId);
        dto.setEstadoNombre("recibido");
        dto.setComentario("Orden recibida");

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(estadoClient.existeEstado(estadoId)).thenReturn(true);

        HistorialModel historial = new HistorialModel();
        historial.setFecha(LocalDateTime.now());
        when(historialRepository.save(any())).thenReturn(historial);
        when(ordenRepository.save(any())).thenReturn(orden);
        doNothing().when(eventProducer).publishEstadoOrden(any());

        OrdenResponseDto result = ordenService.addHistorial(1L, dto, userId, "cliente");

        assertNotNull(result);
    }

    @Test
    void addHistorial_clienteOrdenAjena_debeLanzarExcepcion() {
        UUID clienteId = UUID.randomUUID();
        UUID otroId = UUID.randomUUID();
        OrdenModel orden = ordenSample(otroId);

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        HistorialRequestDto dto = new HistorialRequestDto();
        dto.setEstadoId(UUID.randomUUID());
        dto.setEstadoNombre("test");

        assertThrows(RuntimeException.class,
                () -> ordenService.addHistorial(1L, dto, clienteId, "cliente"));
        verify(historialRepository, never()).save(any());
    }

    @Test
    void getHistorial_admin_debeRetornarHistorialCompleto() {
        UUID userId = UUID.randomUUID();
        OrdenModel orden = ordenSample(userId);

        HistorialModel h1 = new HistorialModel();
        h1.setId(1L);
        h1.setEstadoNombre("pendiente");
        h1.setFecha(LocalDateTime.now().minusDays(1));

        HistorialModel h2 = new HistorialModel();
        h2.setId(2L);
        h2.setEstadoNombre("enviado");
        h2.setFecha(LocalDateTime.now());

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(historialRepository.findByOrden(orden)).thenReturn(List.of(h1, h2));

        List<OrdenResponseDto.HistorialDto> result = ordenService.getHistorial(1L, userId, "admin");

        assertEquals(2, result.size());
    }

    @Test
    void getHistorial_clienteOrdenAjena_debeLanzarExcepcion() {
        UUID clienteId = UUID.randomUUID();
        OrdenModel orden = ordenSample(UUID.randomUUID());

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        assertThrows(RuntimeException.class,
                () -> ordenService.getHistorial(1L, clienteId, "cliente"));
    }
}
