package Producto.example.Producto.service;

import Producto.example.Producto.client.InventarioClient;
import Producto.example.Producto.dto.ProductoActualizadoEvent;
import Producto.example.Producto.dto.ProductoRequestDTO;
import Producto.example.Producto.dto.ProductoResponseDTO;
import Producto.example.Producto.messaging.ProductoEventProducer;
import Producto.example.Producto.model.CategoriaModel;
import Producto.example.Producto.model.HistorialStockModel;
import Producto.example.Producto.model.ProductoModel;
import Producto.example.Producto.repository.CategoriaRepository;
import Producto.example.Producto.repository.HistorialStockRepository;
import Producto.example.Producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoEventProducer eventProducer;
    private final HistorialStockRepository historialStockRepository;
    private final InventarioClient inventarioClient;

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

    public List<ProductoResponseDTO> getByPais(String pais) {
        return productoRepository.findByActivoTrueAndPais(pais).stream()
                .map(ProductoResponseDTO::from)
                .toList();
    }

    public List<ProductoResponseDTO> getPorBodeguero(UUID userId) {
        List<ProductoModel> creados = productoRepository.findByCreadoPorId(userId);
        List<UUID> creadosIds = creados.stream().map(ProductoModel::getId).toList();
        List<ProductoModel> modificados = productoRepository.findByModificadoPorId(userId).stream()
                .filter(p -> !creadosIds.contains(p.getId()))
                .toList();
        return java.util.stream.Stream.concat(creados.stream(), modificados.stream())
                .map(ProductoResponseDTO::from)
                .toList();
    }

    public List<ProductoResponseDTO> getPorUbicacion(Long bodegaId, Long pasilloId, Long estanteId) {
        List<ProductoModel> productos;
        if (estanteId != null && pasilloId != null) {
            productos = productoRepository.findByIdBodegaAndIdPasilloAndIdEstante(bodegaId, pasilloId, estanteId);
        } else if (pasilloId != null) {
            productos = productoRepository.findByIdBodegaAndIdPasillo(bodegaId, pasilloId);
        } else {
            productos = productoRepository.findByIdBodega(bodegaId);
        }
        return productos.stream().map(ProductoResponseDTO::from).toList();
    }

    @Transactional
    public ProductoResponseDTO crear(ProductoRequestDTO dto, UUID userId, String userName) {
        CategoriaModel cat = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + dto.getCategoriaId()));

        if (dto.getIdEstante() != null && !inventarioClient.isEstanteActivo(dto.getIdEstante())) {
            throw new IllegalArgumentException("El estante indicado no está activo: " + dto.getIdEstante());
        }

        ProductoModel p = ProductoModel.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precio(dto.getPrecio())
                .stock(dto.getStock())
                .categoria(cat)
                .estadoNombre(dto.getEstadoNombre() != null ? dto.getEstadoNombre() : "publicado")
                .idBodega(dto.getIdBodega())
                .idPasillo(dto.getIdPasillo())
                .idEstante(dto.getIdEstante())
                .pais(dto.getPais() != null ? dto.getPais() : "Chile")
                .creadoPorId(userId)
                .creadoPorNombre(userName)
                .modificadoPorId(userId)
                .modificadoPorNombre(userName)
                .activo(true)
                .build();

        ProductoModel saved = productoRepository.save(p);

        if (dto.getIdEstante() != null) {
            eventProducer.publishUbicacionChanged(dto.getIdEstante(), 1);
        }

        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.CREADO);
        return ProductoResponseDTO.from(saved);
    }

    @Transactional
    public ProductoResponseDTO actualizar(UUID id, ProductoRequestDTO dto, UUID userId, String userName) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));

        CategoriaModel cat = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + dto.getCategoriaId()));

        Long estanteAnterior = p.getIdEstante();
        Long estanteNuevo = dto.getIdEstante();
        boolean estanteCambio = !java.util.Objects.equals(estanteAnterior, estanteNuevo);

        if (estanteCambio && estanteNuevo != null && !inventarioClient.isEstanteActivo(estanteNuevo)) {
            throw new IllegalArgumentException("El estante indicado no está activo: " + estanteNuevo);
        }

        boolean stockCambio = !p.getStock().equals(dto.getStock());
        int stockAnterior = p.getStock();

        p.setNombre(dto.getNombre());
        p.setDescripcion(dto.getDescripcion());
        p.setPrecio(dto.getPrecio());
        p.setCategoria(cat);
        if (dto.getEstadoNombre() != null) p.setEstadoNombre(dto.getEstadoNombre());
        p.setIdBodega(dto.getIdBodega());
        p.setIdPasillo(dto.getIdPasillo());
        p.setIdEstante(estanteNuevo);
        if (dto.getPais() != null) p.setPais(dto.getPais());
        p.setStock(dto.getStock());
        p.setModificadoPorId(userId);
        p.setModificadoPorNombre(userName);

        ProductoModel saved = productoRepository.save(p);

        if (stockCambio) {
            registrarHistorialStock(saved, stockAnterior, dto.getStock(), userId, userName);
        }

        if (estanteCambio) {
            if (estanteAnterior != null) eventProducer.publishUbicacionChanged(estanteAnterior, -1);
            if (estanteNuevo != null) eventProducer.publishUbicacionChanged(estanteNuevo, 1);
        }

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
        int anterior = p.getStock();
        p.setStock(nuevoStock);
        if (nuevoStock == 0) p.setEstadoNombre("sin_stock");
        else if (nuevoStock <= 10) p.setEstadoNombre("bajo_stock");
        else p.setEstadoNombre("publicado");
        ProductoModel saved = productoRepository.save(p);
        registrarHistorialStock(saved, anterior, nuevoStock, null, null);
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO);
        return ProductoResponseDTO.from(saved);
    }

    @Transactional
    public ProductoResponseDTO decrementarStock(UUID id, int cantidad) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        int anterior = p.getStock();
        int nuevoStock = Math.max(0, anterior - cantidad);
        p.setStock(nuevoStock);
        if (nuevoStock == 0) p.setEstadoNombre("sin_stock");
        else if (nuevoStock <= 10) p.setEstadoNombre("bajo_stock");
        else p.setEstadoNombre("publicado");
        ProductoModel saved = productoRepository.save(p);
        registrarHistorialStock(saved, anterior, nuevoStock, null, null);
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO);
        return ProductoResponseDTO.from(saved);
    }

    @Transactional
    public void desactivar(UUID id, UUID userId, String userName) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        Long estante = p.getIdEstante();
        p.setActivo(false);
        p.setEstadoNombre("descontinuado");
        p.setModificadoPorId(userId);
        p.setModificadoPorNombre(userName);
        ProductoModel saved = productoRepository.save(p);
        if (estante != null) {
            eventProducer.publishUbicacionChanged(estante, -1);
        }
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.DESACTIVADO);
    }

    @Transactional
    public ProductoResponseDTO toggleActivo(UUID id) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        p.setActivo(!p.getActivo());
        p.setEstadoNombre(p.getActivo() ? "publicado" : "descontinuado");
        ProductoModel saved = productoRepository.save(p);
        ProductoActualizadoEvent.TipoEvento tipo = p.getActivo()
                ? ProductoActualizadoEvent.TipoEvento.ACTUALIZADO
                : ProductoActualizadoEvent.TipoEvento.DESACTIVADO;
        eventProducer.publishProductoActualizado(saved, tipo);
        return ProductoResponseDTO.from(saved);
    }

    private void registrarHistorialStock(ProductoModel p, int anterior, int nuevo, UUID userId, String userName) {
        HistorialStockModel historial = HistorialStockModel.builder()
                .productoId(p.getId())
                .productoNombre(p.getNombre())
                .cantidadAnterior(anterior)
                .cantidadNueva(nuevo)
                .modificadoPorId(userId)
                .modificadoPorNombre(userName)
                .fecha(LocalDateTime.now())
                .build();
        historialStockRepository.save(historial);
        log.info("[HistorialStock] productoId={} {} → {}", p.getId(), anterior, nuevo);
    }
}
