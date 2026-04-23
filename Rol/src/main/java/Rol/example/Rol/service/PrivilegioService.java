package Rol.example.Rol.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Rol.example.Rol.model.PrivilegioModel;
import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.repository.PrivilegioRepository;
import Rol.example.Rol.repository.TipoRepository;

@Service
public class PrivilegioService {

    @Autowired
    private PrivilegioRepository privilegioRepository;

    @Autowired
    private TipoRepository tipoRepository;

    public List<PrivilegioModel> getAllPrivilegios() {
        return privilegioRepository.findAll();
    }

    public PrivilegioModel getPrivilegioById(UUID id) {
        return privilegioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Privilegio no encontrado con id: " + id));
    }

    public List<PrivilegioModel> getPrivilegiosByTipo(UUID tipoId) {
        tipoRepository.findById(tipoId)
            .orElseThrow(() -> new RuntimeException("Tipo no encontrado con id: " + tipoId));
        return privilegioRepository.findByTipoId(tipoId);
    }

    public PrivilegioModel createPrivilegio(PrivilegioModel privilegio) {
        if (privilegioRepository.findByNombre(privilegio.getNombre()).isPresent()) {
            throw new RuntimeException("Ya existe un privilegio con el nombre: " + privilegio.getNombre());
        }
        TipoModel tipo = tipoRepository.findById(privilegio.getTipo().getId())
            .orElseThrow(() -> new RuntimeException("Tipo no encontrado con id: " + privilegio.getTipo().getId()));
        privilegio.setTipo(tipo);
        return privilegioRepository.save(privilegio);
    }

    public PrivilegioModel updatePrivilegio(UUID id, PrivilegioModel privilegio) {
        PrivilegioModel existing = privilegioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Privilegio no encontrado con id: " + id));
        existing.setNombre(privilegio.getNombre());
        existing.setDescripcion(privilegio.getDescripcion());
        if (privilegio.getTipo() != null) {
            TipoModel tipo = tipoRepository.findById(privilegio.getTipo().getId())
                .orElseThrow(() -> new RuntimeException("Tipo no encontrado con id: " + privilegio.getTipo().getId()));
            existing.setTipo(tipo);
        }
        return privilegioRepository.save(existing);
    }

    public void deletePrivilegio(UUID id) {
        privilegioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Privilegio no encontrado con id: " + id));
        privilegioRepository.deleteById(id);
    }
}
