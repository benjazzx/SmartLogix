package Producto.example.Producto.dto;

import Producto.example.Producto.model.HistorialStockModel;
import Producto.example.Producto.model.TipoAccionHistorial;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class HistorialStockResponseDTO {

    private Long id;
    private UUID productoId;
    private String productoNombre;
    private Integer cantidadAnterior;
    private Integer cantidadNueva;
    private UUID modificadoPorId;
    private String modificadoPorNombre;
    private TipoAccionHistorial tipoAccion;
    private String descripcion;
    private Long idEstanteAnterior;
    private Long idEstanteNuevo;
    private Long ordenId;
    private LocalDateTime fecha;

    // El comprador (modificadoPorId/Nombre en filas generadas por una orden) solo se
    // expone a admin — bodeguero ve que el stock bajó y por qué orden, no quién compró.
    public static HistorialStockResponseDTO from(HistorialStockModel h, boolean isAdmin) {
        boolean ocultarComprador = h.getOrdenId() != null && !isAdmin;

        HistorialStockResponseDTO dto = new HistorialStockResponseDTO();
        dto.setId(h.getId());
        dto.setProductoId(h.getProductoId());
        dto.setProductoNombre(h.getProductoNombre());
        dto.setCantidadAnterior(h.getCantidadAnterior());
        dto.setCantidadNueva(h.getCantidadNueva());
        dto.setModificadoPorId(ocultarComprador ? null : h.getModificadoPorId());
        dto.setModificadoPorNombre(ocultarComprador ? null : h.getModificadoPorNombre());
        dto.setTipoAccion(h.getTipoAccion());
        dto.setDescripcion(h.getDescripcion());
        dto.setIdEstanteAnterior(h.getIdEstanteAnterior());
        dto.setIdEstanteNuevo(h.getIdEstanteNuevo());
        dto.setOrdenId(h.getOrdenId());
        dto.setFecha(h.getFecha());
        return dto;
    }
}
