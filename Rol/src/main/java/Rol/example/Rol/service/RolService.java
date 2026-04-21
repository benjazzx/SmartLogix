package Rol.example.Rol.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Rol.example.Rol.messaging.RoleEventProcessor;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.repository.RolRepository;

@Service
public class RolService {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private RoleEventProcessor roleEventProcessor;

    public List<RolModel> getAllRoles() {
        return rolRepository.findAll();
    }

    public RolModel getRolById(UUID id) {
        return rolRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + id));
    }

    public RolModel getRolByNombre(String nombre) {
        return rolRepository.findByNombre(nombre).orElse(null);
    }

    public RolModel createRol(RolModel rol) {
        return rolRepository.save(rol);
    }

    public RolModel updateRol(UUID id, RolModel rol) {
        RolModel rolFound = rolRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + id));
        rolFound.setNombre(rol.getNombre());
        rolFound.setDescripcion(rol.getDescripcion());
        return rolRepository.save(rolFound);
    }

    public void deleteRol(UUID id) {
        rolRepository.deleteById(id);
    }
    
    public RolModel assignRoleByEmail(String email) {
        String roleName = determineRoleName(email);
        RolModel rol = getRolByNombre(roleName);

        if (rol == null) {
            rol = getRolByNombre("cliente");
        }

        if (rol != null) {
            roleEventProcessor.publishRoleAssigned(email, rol.getId(), rol.getNombre());
        }

        return rol;
    }

    private String determineRoleName(String email) {
        if (email == null || !email.contains("@")) {
            return "cliente";
        }
        
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        
        if (domain.equals("smartb.cl")) {
            return "bodeguero";
        } else if (domain.equals("smartt.cl")) {
            return "transportista";
        } else if (domain.equals("smartadmin.cl") || domain.equals("admin.smart.cl")) {
            return "admin";
        } else {
            return "cliente"; 
        }
    }
}
