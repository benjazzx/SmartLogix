package Orden.example.Orden.dto;

import Orden.example.Orden.model.DetalleOrdenModel;
import Orden.example.Orden.model.HistorialModel;
import Orden.example.Orden.model.OrdenModel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrdenResponseDto {

    private Long id;
    private LocalDateTime fechaOrden;
    private UUID direccionId;
    private String direccionTexto;
    private UUID userId;
    private String userNombre;
    private String estadoActual;
    private boolean tomada;
    private UUID transportistaId;
    private String transportistaNombre;
    private List<DetalleDto> detalles;
    private List<HistorialDto> historial;

    public static OrdenResponseDto from(OrdenModel orden) {
        OrdenResponseDto dto = new OrdenResponseDto();
        dto.setId(orden.getId());
        dto.setFechaOrden(orden.getFechaOrden());
        dto.setDireccionId(orden.getDireccionId());
        dto.setDireccionTexto(orden.getDireccionTexto());
        dto.setUserId(orden.getUserId());
        dto.setUserNombre(orden.getUserNombre());
        dto.setEstadoActual(orden.getEstadoActual());
        dto.setTomada(orden.isTomada());
        dto.setTransportistaId(orden.getTransportistaId());
        dto.setTransportistaNombre(orden.getTransportistaNombre());
        dto.setDetalles(orden.getDetalles().stream().map(DetalleDto::from).toList());
        dto.setHistorial(orden.getHistorial().stream().map(HistorialDto::from).toList());
        return dto;
    }

    public static OrdenResponseDto from(OrdenModel orden, String rolNombre, UUID requestingUserId) {
        OrdenResponseDto dto = from(orden);
        if ("bodeguero".equals(rolNombre)) {
            dto.setUserId(null);
            dto.setUserNombre(null);
        }
        if (!"admin".equals(rolNombre)) {
            boolean esPropiaDelTransportista = "transportista".equals(rolNombre)
                    && requestingUserId != null
                    && requestingUserId.equals(orden.getTransportistaId());
            if (!esPropiaDelTransportista) {
                dto.setTransportistaId(null);
            }
            // Bodeguero no ve el nombre del transportista
            if ("bodeguero".equals(rolNombre)) {
                dto.setTransportistaNombre(null);
            }
        }
        return dto;
    }

    @Data
    public static class DetalleDto {
        private Long id;
        private UUID productoId;
        private String productoNombre;
        private BigDecimal precioUnitario;
        private Integer cantidad;

        public static DetalleDto from(DetalleOrdenModel d) {
            DetalleDto dto = new DetalleDto();
            dto.setId(d.getId());
            dto.setProductoId(d.getProductoId());
            dto.setProductoNombre(d.getProductoNombre());
            dto.setPrecioUnitario(d.getPrecioUnitario());
            dto.setCantidad(d.getCantidad());
            return dto;
        }
    }

    @Data
    public static class HistorialDto {
        private Long id;
        private LocalDateTime fecha;
        private UUID estadoId;
        private String estadoNombre;
        private String comentario;

        public static HistorialDto from(HistorialModel h) {
            HistorialDto dto = new HistorialDto();
            dto.setId(h.getId());
            dto.setFecha(h.getFecha());
            dto.setEstadoId(h.getEstadoId());
            dto.setEstadoNombre(h.getEstadoNombre());
            dto.setComentario(h.getComentario());
            return dto;
        }
    }
}
