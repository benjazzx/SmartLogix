package Producto.example.Producto.service;

import Producto.example.Producto.dto.ProductoActualizadoEvent;
import Producto.example.Producto.dto.ProductoRequestDTO;
import Producto.example.Producto.dto.ProductoResponseDTO;
import Producto.example.Producto.messaging.ProductoEventProducer;
import Producto.example.Producto.model.CategoriaModel;
import Producto.example.Producto.model.ProductoModel;
import Producto.example.Producto.repository.CategoriaRepository;
import Producto.example.Producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoEventProducer eventProducer;

    public List<ProductoResponseDTO> getAll() {
        return productoRepository.findByActivoTrue().stream()
                .map(ProductoResponseDTO::from)
                .toList();
    }

    public List<ProductoResponseDTO> getAllIncluirInactivos() {
        return productoRepository.findAll().stream()
                .map(ProductoResponseDTO::from)
                .toList();
    }

    public ProductoResponseDTO getById(UUID id) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        return ProductoResponseDTO.from(p);
    }

    public List<ProductoResponseDTO> getByCategoria(UUID categoriaId) {
        return productoRepository.findByCategoria_Id(categoriaId).stream()
                .map(ProductoResponseDTO::from)
                .toList();
    }

    public List<ProductoResponseDTO> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre).stream()
                .map(ProductoResponseDTO::from)
                .toList();
    }

    public List<ProductoResponseDTO> getBajoStock(Integer umbral) {
        return productoRepository.findByStockLessThanEqual(umbral).stream()
                .map(ProductoResponseDTO::from)
                .toList();
    }

    @Transactional
    public ProductoResponseDTO crear(ProductoRequestDTO dto) {
        CategoriaModel cat = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + dto.getCategoriaId()));

        ProductoModel p = ProductoModel.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precio(dto.getPrecio())
                .stock(dto.getStock())
                .categoria(cat)
                .estadoNombre(dto.getEstadoNombre() != null ? dto.getEstadoNombre() : "publicado")
                .activo(true)
                .build();

        ProductoModel saved = productoRepository.save(p);
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.CREADO);
        return ProductoResponseDTO.from(saved);
    }

    @Transactional
    public ProductoResponseDTO actualizar(UUID id, ProductoRequestDTO dto) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));

        CategoriaModel cat = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + dto.getCategoriaId()));

        p.setNombre(dto.getNombre());
        p.setDescripcion(dto.getDescripcion());
        p.setPrecio(dto.getPrecio());
        p.setCategoria(cat);
        if (dto.getEstadoNombre() != null) p.setEstadoNombre(dto.getEstadoNombre());

        boolean stockCambio = !p.getStock().equals(dto.getStock());
        p.setStock(dto.getStock());

        ProductoModel saved = productoRepository.save(p);
        ProductoActualizadoEvent.TipoEvento tipo = stockCambio
                ? ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO
                : ProductoActualizadoEvent.TipoEvento.ACTUALIZADO;
        eventProducer.publishProductoActualizado(saved, tipo);
        return ProductoResponseDTO.from(saved);
    }

    @Transactional
    public ProductoResponseDTO actualizarStock(UUID id, Integer nuevoStock) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        p.setStock(nuevoStock);
        if (nuevoStock == 0) p.setEstadoNombre("sin_stock");
        else if (nuevoStock <= 10) p.setEstadoNombre("bajo_stock");
        else p.setEstadoNombre("publicado");
        ProductoModel saved = productoRepository.save(p);
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO);
        return ProductoResponseDTO.from(saved);
    }

    @Transactional
    public void desactivar(UUID id) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        p.setActivo(false);
        p.setEstadoNombre("descontinuado");
        ProductoModel saved = productoRepository.save(p);
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.DESACTIVADO);
    }
}
