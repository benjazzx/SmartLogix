package Rol.example.Rol.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Rol.example.Rol.model.PermisoModel;
import Rol.example.Rol.model.PrivilegioModel;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.repository.PermisoRepository;
import Rol.example.Rol.repository.PrivilegioRepository;
import Rol.example.Rol.repository.RolRepository;

@Service
public class PermisoService {

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PrivilegioRepository privilegioRepository;

    // Retorna todos los permisos del sistema
    public List<PermisoModel> getAllPermisos() {
        return permisoRepository.findAll();
    }

    // Retorna todos los privilegios que tiene un rol específico
    public List<PermisoModel> getPermisosByRol(UUID rolId) {
        rolRepository.findById(rolId)
            .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + rolId));
        return permisoRepository.findByRolId(rolId);
    }

    // Lógica principal: verifica si un rol tiene un privilegio determinado.
    // Usado por otros microservicios para controlar el acceso a recursos.
    public boolean tienePermiso(UUID rolId, UUID privilegioId) {
        return permisoRepository.existsByRolIdAndPrivilegioId(rolId, privilegioId);
    }

    // Asigna un privilegio a un rol. Valida que ambos existen y que no estén ya asignados.
    public PermisoModel asignarPermiso(UUID rolId, UUID privilegioId) {
        RolModel rol = rolRepository.findById(rolId)
            .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + rolId));
        PrivilegioModel privilegio = privilegioRepository.findById(privilegioId)
            .orElseThrow(() -> new RuntimeException("Privilegio no encontrado con id: " + privilegioId));

        if (permisoRepository.existsByRolIdAndPrivilegioId(rolId, privilegioId)) {
            throw new RuntimeException("El rol ya tiene asignado ese privilegio");
        }

        PermisoModel permiso = new PermisoModel(null, rol, privilegio);
        return permisoRepository.save(permiso);
    }

    // Revoca un privilegio de un rol
    @Transactional
    public void revocarPermiso(UUID rolId, UUID privilegioId) {
        if (!permisoRepository.existsByRolIdAndPrivilegioId(rolId, privilegioId)) {
            throw new RuntimeException("El rol no tiene asignado ese privilegio");
        }
        permisoRepository.deleteByRolIdAndPrivilegioId(rolId, privilegioId);
    }

    public void deletePermiso(UUID id) {
        permisoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Permiso no encontrado con id: " + id));
        permisoRepository.deleteById(id);
    }
}
