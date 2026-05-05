package Producto.example.Producto.service;

import Producto.example.Producto.dto.CategoriaRequestDTO;
import Producto.example.Producto.dto.CategoriaResponseDTO;
import Producto.example.Producto.model.CategoriaModel;
import Producto.example.Producto.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoriaServiceTest {

    @InjectMocks
    private CategoriaService categoriaService;

    @Mock
    private CategoriaRepository categoriaRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private CategoriaModel categoriaSample() {
        return CategoriaModel.builder()
                .id(UUID.randomUUID())
                .nombre("Electrónica")
                .descripcion("Dispositivos electrónicos")
                .build();
    }

    @Test
    void getAll_debeRetornarTodasLasCategorias() {
        when(categoriaRepository.findAll()).thenReturn(List.of(categoriaSample()));

        List<CategoriaResponseDTO> result = categoriaService.getAll();

        assertEquals(1, result.size());
        assertEquals("Electrónica", result.get(0).getNombre());
    }

    @Test
    void getAll_sinCategorias_debeRetornarListaVacia() {
        when(categoriaRepository.findAll()).thenReturn(List.of());

        List<CategoriaResponseDTO> result = categoriaService.getAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void getById_existente_debeRetornarCategoria() {
        CategoriaModel cat = categoriaSample();
        UUID id = cat.getId();
        when(categoriaRepository.findById(id)).thenReturn(Optional.of(cat));

        CategoriaResponseDTO result = categoriaService.getById(id);

        assertEquals(id, result.getId());
        assertEquals("Electrónica", result.getNombre());
        assertEquals("Dispositivos electrónicos", result.getDescripcion());
    }

    @Test
    void getById_noExistente_debeLanzarRuntimeException() {
        UUID id = UUID.randomUUID();
        when(categoriaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoriaService.getById(id));
    }

    @Test
    void crear_conNombreNuevo_debeCrearCategoria() {
        CategoriaRequestDTO dto = new CategoriaRequestDTO();
        dto.setNombre("Ropa");
        dto.setDescripcion("Prendas de vestir");

        CategoriaModel saved = CategoriaModel.builder()
                .id(UUID.randomUUID())
                .nombre("Ropa")
                .descripcion("Prendas de vestir")
                .build();

        when(categoriaRepository.existsByNombreIgnoreCase("Ropa")).thenReturn(false);
        when(categoriaRepository.save(any())).thenReturn(saved);

        CategoriaResponseDTO result = categoriaService.crear(dto);

        assertEquals("Ropa", result.getNombre());
        verify(categoriaRepository, times(1)).save(any());
    }

    @Test
    void crear_conNombreDuplicado_debeLanzarRuntimeException() {
        CategoriaRequestDTO dto = new CategoriaRequestDTO();
        dto.setNombre("Electrónica");

        when(categoriaRepository.existsByNombreIgnoreCase("Electrónica")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> categoriaService.crear(dto));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void actualizar_existente_debeActualizarCategoria() {
        CategoriaModel cat = categoriaSample();
        UUID id = cat.getId();

        CategoriaRequestDTO dto = new CategoriaRequestDTO();
        dto.setNombre("Electrónica Premium");
        dto.setDescripcion("Dispositivos de alta gama");

        when(categoriaRepository.findById(id)).thenReturn(Optional.of(cat));
        when(categoriaRepository.save(any())).thenReturn(cat);

        CategoriaResponseDTO result = categoriaService.actualizar(id, dto);

        assertNotNull(result);
        verify(categoriaRepository, times(1)).save(cat);
    }

    @Test
    void actualizar_noExistente_debeLanzarRuntimeException() {
        UUID id = UUID.randomUUID();
        CategoriaRequestDTO dto = new CategoriaRequestDTO();
        dto.setNombre("Test");

        when(categoriaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> categoriaService.actualizar(id, dto));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void eliminar_existente_debeEliminarCategoria() {
        UUID id = UUID.randomUUID();
        when(categoriaRepository.existsById(id)).thenReturn(true);
        doNothing().when(categoriaRepository).deleteById(id);

        categoriaService.eliminar(id);

        verify(categoriaRepository, times(1)).deleteById(id);
    }

    @Test
    void eliminar_noExistente_debeLanzarRuntimeException() {
        UUID id = UUID.randomUUID();
        when(categoriaRepository.existsById(id)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> categoriaService.eliminar(id));
        verify(categoriaRepository, never()).deleteById(any());
    }

    @Test
    void actualizar_debeModificarNombreYDescripcion() {
        CategoriaModel cat = categoriaSample();
        UUID id = cat.getId();
        String nuevoNombre = "Hogar";
        String nuevaDesc = "Artículos para el hogar";

        CategoriaRequestDTO dto = new CategoriaRequestDTO();
        dto.setNombre(nuevoNombre);
        dto.setDescripcion(nuevaDesc);

        when(categoriaRepository.findById(id)).thenReturn(Optional.of(cat));
        when(categoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoriaResponseDTO result = categoriaService.actualizar(id, dto);

        assertEquals(nuevoNombre, result.getNombre());
        assertEquals(nuevaDesc, result.getDescripcion());
    }
}
