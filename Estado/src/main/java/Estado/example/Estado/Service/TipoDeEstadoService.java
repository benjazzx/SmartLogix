package Estado.example.Estado.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Estado.example.Estado.Model.TipoDeEstadoModel;
import Estado.example.Estado.Repository.TipoDeEstadoRepository;

@Service
public class TipoDeEstadoService {

    @Autowired
    private TipoDeEstadoRepository tipoDeEstadoRepository;

    public List<TipoDeEstadoModel> getAll() {
        return tipoDeEstadoRepository.findAll();
    }

    public TipoDeEstadoModel getById(UUID id) {
        return tipoDeEstadoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("TipoDeEstado no encontrado con id: " + id));
    }

    public TipoDeEstadoModel getByNombre(String nombre) {
        return tipoDeEstadoRepository.findByNombre(nombre).orElse(null);
    }

    public TipoDeEstadoModel create(TipoDeEstadoModel tipo) {
        return tipoDeEstadoRepository.save(tipo);
    }

    public TipoDeEstadoModel update(UUID id, TipoDeEstadoModel tipo) {
        TipoDeEstadoModel found = tipoDeEstadoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("TipoDeEstado no encontrado con id: " + id));
        found.setNombre(tipo.getNombre());
        found.setDescripcion(tipo.getDescripcion());
        return tipoDeEstadoRepository.save(found);
    }

    public void delete(UUID id) {
        tipoDeEstadoRepository.deleteById(id);
    }
}
