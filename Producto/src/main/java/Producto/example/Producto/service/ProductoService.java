package Producto.example.Producto.service;

import Producto.example.Producto.client.InventarioClient;
import Producto.example.Producto.dto.ProductoActualizadoEvent;
import Producto.example.Producto.dto.ProductoRequestDTO;
import Producto.example.Producto.dto.ProductoResponseDTO;
import Producto.example.Producto.messaging.ProductoEventProducer;
import Producto.example.Producto.model.CategoriaModel;
import Producto.example.Producto.model.HistorialStockModel;
import Producto.example.Producto.model.ProductoModel;
import Producto.example.Producto.model.TipoAccionHistorial;
import Producto.example.Producto.repository.CategoriaRepository;
import Producto.example.Producto.repository.HistorialStockRepository;
import Producto.example.Producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

        registrarHistorialStock(saved, 0, saved.getStock(), userId, userName,
                TipoAccionHistorial.CREADO,
                "Producto creado con stock inicial " + saved.getStock() + " por " + (userName != null ? userName : "sistema"),
                null, null);

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
            TipoAccionHistorial tipo = dto.getStock() > stockAnterior
                    ? TipoAccionHistorial.STOCK_AUMENTADO
                    : TipoAccionHistorial.STOCK_DECREMENTADO;
            registrarHistorialStock(saved, stockAnterior, dto.getStock(), userId, userName, tipo,
                    "Stock " + (tipo == TipoAccionHistorial.STOCK_AUMENTADO ? "aumentado" : "disminuido")
                            + " de " + stockAnterior + " a " + dto.getStock()
                            + " por " + (userName != null ? userName : "sistema"),
                    null, null);
        }

        if (estanteCambio) {
            if (estanteAnterior != null) eventProducer.publishUbicacionChanged(estanteAnterior, -1);
            if (estanteNuevo != null) eventProducer.publishUbicacionChanged(estanteNuevo, 1);
            registrarHistorialStock(saved, saved.getStock(), saved.getStock(), userId, userName,
                    TipoAccionHistorial.UBICACION_CAMBIADA,
                    "Ubicación cambiada de estante " + (estanteAnterior != null ? estanteAnterior : "—")
                            + " a estante " + (estanteNuevo != null ? estanteNuevo : "—")
                            + " por " + (userName != null ? userName : "sistema"),
                    estanteAnterior, estanteNuevo);
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
        TipoAccionHistorial tipo = nuevoStock > anterior
                ? TipoAccionHistorial.STOCK_AUMENTADO
                : TipoAccionHistorial.STOCK_DECREMENTADO;
        registrarHistorialStock(saved, anterior, nuevoStock, null, null, tipo,
                descripcionCambioStock(tipo, anterior, nuevoStock, null), null, null);
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO);
        return ProductoResponseDTO.from(saved);
    }

    // Reserva atómica de stock al crear una orden — a diferencia de decrementarStock (que asume
    // que ya se validó disponibilidad antes), este método hace el chequeo y el descuento en el
    // mismo UPDATE de base de datos, para que dos pedidos concurrentes no puedan pasar ambos
    // contra el mismo stock leído.
    @Transactional
    public boolean reservarStock(UUID id, int cantidad, Long ordenId) {
        int filasActualizadas = productoRepository.reservarStockAtomico(id, cantidad);
        if (filasActualizadas == 0) {
            return false;
        }
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        int nuevoStock = p.getStock();
        if (nuevoStock == 0) p.setEstadoNombre("sin_stock");
        else if (nuevoStock <= 10) p.setEstadoNombre("bajo_stock");
        ProductoModel saved = productoRepository.save(p);
        registrarHistorialStock(saved, nuevoStock + cantidad, nuevoStock, null, null,
                TipoAccionHistorial.STOCK_DECREMENTADO,
                "Stock reservado (" + cantidad + " unidad(es)) por la orden #" + ordenId,
                null, null, ordenId);
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO);
        return true;
    }

    @Transactional
    public ProductoResponseDTO decrementarStock(UUID id, int cantidad, Long ordenId,
                                                 UUID compradorId, String compradorNombre) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        int anterior = p.getStock();
        int nuevoStock = Math.max(0, anterior - cantidad);
        p.setStock(nuevoStock);
        if (nuevoStock == 0) p.setEstadoNombre("sin_stock");
        else if (nuevoStock <= 10) p.setEstadoNombre("bajo_stock");
        else p.setEstadoNombre("publicado");
        ProductoModel saved = productoRepository.save(p);
        String descripcion = ordenId != null
                ? "Stock disminuido de " + anterior + " a " + nuevoStock + " por la orden #" + ordenId
                : descripcionCambioStock(TipoAccionHistorial.STOCK_DECREMENTADO, anterior, nuevoStock, null);
        registrarHistorialStock(saved, anterior, nuevoStock, compradorId, compradorNombre,
                TipoAccionHistorial.STOCK_DECREMENTADO, descripcion, null, null, ordenId);
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO);
        return ProductoResponseDTO.from(saved);
    }

    @Transactional
    public ProductoResponseDTO registrarDevolucion(UUID id, int cantidad, boolean danado, Long ordenId) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        String descripcion;
        if (danado) {
            int anteriorMerma = p.getStockMerma();
            p.setStockMerma(anteriorMerma + cantidad);
            descripcion = "Devolución de la orden #" + ordenId + ": " + cantidad
                    + " unidad(es) dañada(s), registradas como merma";
            ProductoModel saved = productoRepository.save(p);
            registrarHistorialStock(saved, anteriorMerma, saved.getStockMerma(), null, null,
                    TipoAccionHistorial.MERMA_REGISTRADA, descripcion, null, null, ordenId);
            return ProductoResponseDTO.from(saved);
        }
        int anterior = p.getStock();
        int nuevoStock = anterior + cantidad;
        p.setStock(nuevoStock);
        if (nuevoStock > 10 && "sin_stock".equals(p.getEstadoNombre())) p.setEstadoNombre("publicado");
        else if (nuevoStock > 0 && "sin_stock".equals(p.getEstadoNombre())) p.setEstadoNombre("bajo_stock");
        descripcion = "Devolución de la orden #" + ordenId + ": " + cantidad + " unidad(es) reintegradas al stock";
        ProductoModel saved = productoRepository.save(p);
        registrarHistorialStock(saved, anterior, nuevoStock, null, null,
                TipoAccionHistorial.STOCK_AUMENTADO, descripcion, null, null, ordenId);
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO);
        return ProductoResponseDTO.from(saved);
    }

    @Transactional
    public void liberarUbicacionPorBodega(Long idBodega) {
        List<ProductoModel> productos = productoRepository.findByIdBodega(idBodega);
        if (productos.isEmpty()) {
            log.info("[Producto] Bodega {} desactivada — no tenía productos asignados", idBodega);
            return;
        }
        for (ProductoModel p : productos) {
            p.setIdBodega(null);
            p.setIdPasillo(null);
            p.setIdEstante(null);
            ProductoModel saved = productoRepository.save(p);
            registrarHistorialStock(saved, saved.getStock(), saved.getStock(), null, null,
                    TipoAccionHistorial.UBICACION_CAMBIADA,
                    "Bodega " + idBodega + " desactivada — producto quedó sin ubicación, requiere reasignación",
                    null, null);
        }
        log.info("[Producto] Bodega {} desactivada — {} producto(s) quedaron sin ubicación", idBodega, productos.size());
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
        registrarHistorialStock(saved, saved.getStock(), saved.getStock(), userId, userName,
                TipoAccionHistorial.DESACTIVADO,
                "Producto desactivado " + quien(userName), null, null);
        eventProducer.publishProductoActualizado(saved, ProductoActualizadoEvent.TipoEvento.DESACTIVADO);
    }

    @Transactional
    public ProductoResponseDTO toggleActivo(UUID id) {
        ProductoModel p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        p.setActivo(!p.getActivo());
        p.setEstadoNombre(p.getActivo() ? "publicado" : "descontinuado");
        ProductoModel saved = productoRepository.save(p);
        ProductoActualizadoEvent.TipoEvento tipoEvento = p.getActivo()
                ? ProductoActualizadoEvent.TipoEvento.ACTUALIZADO
                : ProductoActualizadoEvent.TipoEvento.DESACTIVADO;
        TipoAccionHistorial tipoHistorial = p.getActivo()
                ? TipoAccionHistorial.REACTIVADO
                : TipoAccionHistorial.DESACTIVADO;
        registrarHistorialStock(saved, saved.getStock(), saved.getStock(), null, null, tipoHistorial,
                "Producto " + (p.getActivo() ? "reactivado" : "desactivado") + " " + quien(null), null, null);
        eventProducer.publishProductoActualizado(saved, tipoEvento);
        return ProductoResponseDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public List<HistorialStockModel> getHistorialStock(UUID productoId) {
        productoRepository.findById(productoId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Producto no encontrado con id: " + productoId));
        return historialStockRepository.findByProductoIdOrderByFechaDesc(productoId);
    }

    @Transactional(readOnly = true)
    public Page<HistorialStockModel> getHistorialGlobal(UUID productoId, UUID bodegueroId,
                                                          LocalDateTime desde, LocalDateTime hasta,
                                                          Pageable pageable) {
        Specification<HistorialStockModel> spec = (root, query, cb) -> cb.conjunction();
        if (productoId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("productoId"), productoId));
        }
        if (bodegueroId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("modificadoPorId"), bodegueroId));
        }
        if (desde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), desde));
        }
        if (hasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), hasta));
        }
        return historialStockRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<HistorialStockModel> getHistorialReciente(Integer limite, UUID modificadoPorId,
                                                            LocalDateTime desde, LocalDateTime hasta) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limite,
                org.springframework.data.domain.Sort.by("fecha").descending());
        return getHistorialGlobal(null, modificadoPorId, desde, hasta, pageable).getContent();
    }

    private static final String SISTEMA = "sistema";

    private void registrarHistorialStock(ProductoModel p, int anterior, int nuevo, UUID userId, String userName,
                                          TipoAccionHistorial tipoAccion, String descripcion,
                                          Long idEstanteAnterior, Long idEstanteNuevo) {
        registrarHistorialStock(p, anterior, nuevo, userId, userName, tipoAccion, descripcion,
                idEstanteAnterior, idEstanteNuevo, null);
    }

    // ordenId != null indica que la baja de stock la causó una orden de un cliente
    // (userId/userName aquí representan al comprador, no a quien la registró manualmente).
    private void registrarHistorialStock(ProductoModel p, int anterior, int nuevo, UUID userId, String userName,
                                          TipoAccionHistorial tipoAccion, String descripcion,
                                          Long idEstanteAnterior, Long idEstanteNuevo, Long ordenId) {
        HistorialStockModel historial = HistorialStockModel.builder()
                .productoId(p.getId())
                .productoNombre(p.getNombre())
                .cantidadAnterior(anterior)
                .cantidadNueva(nuevo)
                .modificadoPorId(userId)
                .modificadoPorNombre(userName)
                .tipoAccion(tipoAccion)
                .descripcion(descripcion)
                .idEstanteAnterior(idEstanteAnterior)
                .idEstanteNuevo(idEstanteNuevo)
                .ordenId(ordenId)
                .fecha(LocalDateTime.now())
                .build();
        historialStockRepository.save(historial);
        log.info("[HistorialStock] productoId={} {} {} → {}", p.getId(), tipoAccion, anterior, nuevo);
    }

    private String quien(String userName) {
        return "por " + (userName != null ? userName : SISTEMA);
    }

    private String descripcionCambioStock(TipoAccionHistorial tipo, int anterior, int nuevo, String userName) {
        String accion = tipo == TipoAccionHistorial.STOCK_AUMENTADO ? "aumentado" : "disminuido";
        return "Stock " + accion + " de " + anterior + " a " + nuevo + " " + quien(userName);
    }
}
