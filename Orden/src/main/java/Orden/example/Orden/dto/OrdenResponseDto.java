package Orden.example.Orden.dto;

import Orden.example.Orden.model.DetalleOrdenModel;
import Orden.example.Orden.model.HistorialModel;
import Orden.example.Orden.model.OrdenModel;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class OrdenResponseDto {

    private Long id;
    private LocalDateTime fechaOrden;
    private Long direccionId;
    private UUID userId;
    private String userNombre;
    private String estadoActual;
    private List<DetalleDto> detalles;
    private List<HistorialDto> historial;

    public static OrdenResponseDto from(OrdenModel orden) {
        OrdenResponseDto dto = new OrdenResponseDto();
        dto.setId(orden.getId());
        dto.setFechaOrden(orden.getFechaOrden());
        dto.setDireccionId(orden.getDireccionId());
        dto.setUserId(orden.getUserId());
        dto.setUserNombre(orden.getUserNombre());
        dto.setEstadoActual(orden.getEstadoActual());
        dto.setDetalles(orden.getDetalles().stream()
                .map(DetalleDto::from)
                .collect(Collectors.toList()));
        dto.setHistorial(orden.getHistorial().stream()
                .map(HistorialDto::from)
                .collect(Collectors.toList()));
        return dto;
    }

    @Data
    public static class DetalleDto {
        private Long id;
        private UUID productoId;
        private Integer cantidad;

        public static DetalleDto from(DetalleOrdenModel d) {
            DetalleDto dto = new DetalleDto();
            dto.setId(d.getId());
            dto.setProductoId(d.getProductoId());
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
