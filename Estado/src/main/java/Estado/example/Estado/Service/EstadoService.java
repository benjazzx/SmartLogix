package Estado.example.Estado.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Estado.example.Estado.Model.Estado;
import Estado.example.Estado.Repository.EstadoRepository;

@Service
public class EstadoService {

    @Autowired
    private EstadoRepository estadoRepository;

    public List<Estado> getAll() {
        return estadoRepository.findAll();
    }

    public Estado getById(UUID id) {
        return estadoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Estado no encontrado con id: " + id));
    }

    public Estado getByNombre(String nombre) {
        return estadoRepository.findByNombre(nombre).orElse(null);
    }

    public List<Estado> getByTipoNombre(String tipoNombre) {
        return estadoRepository.findByTipoDeEstadoNombre(tipoNombre);
    }

    public Estado create(Estado estado) {
        return estadoRepository.save(estado);
    }

    public Estado update(UUID id, Estado estado) {
        Estado found = estadoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Estado no encontrado con id: " + id));
        found.setNombre(estado.getNombre());
        found.setDescripcion(estado.getDescripcion());
        found.setTipoDeEstado(estado.getTipoDeEstado());
        return estadoRepository.save(found);
    }

    public void delete(UUID id) {
        estadoRepository.deleteById(id);
    }

    public Estado assignEstadoByRol(String roleName) {
        String tipoNombre = determineTipoNombre(roleName);
        String estadoNombre = determineEstadoNombre(roleName);

        Estado estado = getByNombre(estadoNombre);
        if (estado == null) {
            estado = getByNombre("pendiente_verificacion");
        }
        return estado;
    }

    private String determineTipoNombre(String roleName) {
        if (roleName == null) return "cuenta";
        return switch (roleName.toLowerCase()) {
            case "bodeguero", "transportista" -> "laboral";
            default -> "cuenta";
        };
    }

    private String determineEstadoNombre(String roleName) {
        if (roleName == null) return "pendiente_verificacion";
        return switch (roleName.toLowerCase()) {
            case "admin" -> "activo";
            case "bodeguero", "transportista" -> "disponible";
            default -> "pendiente_verificacion";
        };
    }
}
                    