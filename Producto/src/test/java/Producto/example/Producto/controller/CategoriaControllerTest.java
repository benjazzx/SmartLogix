package Producto.example.Producto.controller;

import Producto.example.Producto.dto.CategoriaRequestDTO;
import Producto.example.Producto.dto.CategoriaResponseDTO;
import Producto.example.Producto.service.CategoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CategoriaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private CategoriaController categoriaController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(categoriaController).build();
    }

    private CategoriaResponseDTO buildResponse(UUID id, String nombre) {
        CategoriaResponseDTO dto = new CategoriaResponseDTO();
        dto.setId(id);
        dto.setNombre(nombre);
        dto.setDescripcion("Descripción de " + nombre);
        return dto;
    }

    @Test
    void getAll_retornaLista() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoriaService.getAll()).thenReturn(List.of(buildResponse(id, "Electrónica")));

        mockMvc.perform(get("/api/categorias"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].nombre").value("Electrónica"));
    }

    @Test
    void getById_existente_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoriaService.getById(id)).thenReturn(buildResponse(id, "Ropa"));

        mockMvc.perform(get("/api/categorias/" + id))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.nombre").value("Ropa"));
    }

    @Test
    void crear_valido_retorna201() throws Exception {
        UUID id = UUID.randomUUID();
        CategoriaRequestDTO req = new CategoriaRequestDTO();
        req.setNombre("Herramientas");
        req.setDescripcion("Categoría de herramientas");

        when(categoriaService.crear(any())).thenReturn(buildResponse(id, "Herramientas"));

        mockMvc.perform(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.nombre").value("Herramientas"));
    }

    @Test
    void actualizar_existente_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        CategoriaRequestDTO req = new CategoriaRequestDTO();
        req.setNombre("Electrónica Actualizada");

        when(categoriaService.actualizar(eq(id), any())).thenReturn(buildResponse(id, "Electrónica Actualizada"));

        mockMvc.perform(put("/api/categorias/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.nombre").value("Electrónica Actualizada"));
    }

    @Test
    void eliminar_existente_retorna204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(categoriaService).eliminar(id);

        mockMvc.perform(delete("/api/categorias/" + id))
               .andExpect(status().isNoContent());
    }
}
