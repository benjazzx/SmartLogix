package Producto.example.Producto.service;

import Producto.example.Producto.dto.CategoriaRequestDTO;
import Producto.example.Producto.dto.CategoriaResponseDTO;
import Producto.example.Producto.model.CategoriaModel;
import Producto.example.Producto.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<CategoriaResponseDTO> getAll() {
        return categoriaRepository.findAll().stream()
                .map(CategoriaResponseDTO::from)
                .toList();
    }

    public CategoriaResponseDTO getById(UUID id) {
        CategoriaModel cat = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + id));
        return CategoriaResponseDTO.from(cat);
    }

    @Transactional
    public CategoriaResponseDTO crear(CategoriaRequestDTO dto) {
        if (categoriaRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + dto.getNombre());
        }
        CategoriaModel cat = CategoriaModel.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .build();
        return CategoriaResponseDTO.from(categoriaRepository.save(cat));
    }

    @Transactional
    public CategoriaResponseDTO actualizar(UUID id, CategoriaRequestDTO dto) {
        CategoriaModel cat = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + id));
        cat.setNombre(dto.getNombre());
        cat.setDescripcion(dto.getDescripcion());
        return CategoriaResponseDTO.from(categoriaRepository.save(cat));
    }

    @Transactional
    public void eliminar(UUID id) {
        if (!categoriaRepository.existsById(id)) {
            throw new RuntimeException("Categoría no encontrada: " + id);
        }
        categoriaRepository.deleteById(id);
    }
}
