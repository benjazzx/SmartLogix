package Rol.example.Rol.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import Rol.example.Rol.messaging.RoleEventProcessor;
import lombok.RequiredArgsConstructor;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.repository.RolRepository;

@Service
@RequiredArgsConstructor
public class RolService {

    private static final String ROL_ADMIN         = "admin";
    private static final String ROL_BODEGUERO     = "bodeguero";
    private static final String ROL_TRANSPORTISTA = "transportista";
    private static final String ROL_CLIENTE       = "cliente";

    private final RolRepository rolRepository;
    private final RoleEventProcessor roleEventProcessor;

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
            rol = getRolByNombre(ROL_CLIENTE);
        }
        if (rol != null) {
            roleEventProcessor.publishRoleAssigned(email, rol.getId(), rol.getNombre());
        }
        return rol;
    }

    /**
     * Asigna directamente el rol indicado en el evento (respeta lo que el admin especificó).
     * Solo cae a lógica por dominio de email si rolNombre es nulo o no existe en BD.
     */
    public RolModel assignRoleFromEvent(String email, String rolNombre) {
        RolModel rol = (rolNombre != null && !rolNombre.isBlank()) ? getRolByNombre(rolNombre) : null;
        if (rol == null) {
            return assignRoleByEmail(email);
        }
        roleEventProcessor.publishRoleAssigned(email, rol.getId(), rol.getNombre());
        return rol;
    }

    private String determineRoleName(String email) {
        if (email == null || !email.contains("@")) return ROL_CLIENTE;
        String localPart = email.substring(0, email.indexOf("@")).toLowerCase();
        String domain    = email.substring(email.indexOf("@") + 1).toLowerCase();
        if (domain.equals("smartb.cl")) return ROL_BODEGUERO;
        if (domain.equals("smartt.cl")) return ROL_TRANSPORTISTA;
        if (domain.equals("smartadmin.cl") || domain.equals("admin.smart.cl")) return ROL_ADMIN;
        if (domain.equals("smartlogix.cl")) {
            return switch (localPart) {
                case ROL_ADMIN         -> ROL_ADMIN;
                case ROL_BODEGUERO     -> ROL_BODEGUERO;
                case ROL_TRANSPORTISTA -> ROL_TRANSPORTISTA;
                default                -> ROL_CLIENTE;
            };
        }
        return ROL_CLIENTE;
    }
}
