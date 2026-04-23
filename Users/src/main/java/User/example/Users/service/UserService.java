package User.example.Users.service;

import User.example.Users.dto.UserCreatedEvent;
import User.example.Users.dto.UserRequestDto;
import User.example.Users.factory.UserFactory;
import User.example.Users.messaging.UserEventProducer;
import User.example.Users.model.DireccionModel;
import User.example.Users.model.UserModel;
import User.example.Users.repository.DireccionRepository;
import User.example.Users.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private DireccionRepository direccionRepository;
    @Autowired private UserEventProducer eventProducer;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    // Circuit Breaker aplicado: si la BD falla repetidamente, devuelve lista vacía (fallback)
    // Patrón: protege la disponibilidad del servicio ante fallos de infraestructura
    public List<UserModel> getAllUsers() {
        return circuitBreakerFactory.create("getAllUsers").run(
            () -> userRepository.findAll(),
            throwable -> {
                System.err.println("[CircuitBreaker] getAllUsers abierto: " + throwable.getMessage());
                return Collections.emptyList();
            }
        );
    }

    public UserModel getUserById(UUID id) {
        return circuitBreakerFactory.create("getUserById").run(
            () -> userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id)),
            throwable -> { throw new RuntimeException("Servicio no disponible"); }
        );
    }

    public UserModel getUserByCorreo(String correo) {
        return userRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + correo));
    }

    public List<UserModel> getUsersByRol(UUID rolId) {
        return userRepository.findByRolId(rolId);
    }

    @Transactional
    public UserModel createUser(UserRequestDto dto) {
        if (userRepository.existsByCorreo(dto.getCorreo())) {
            throw new RuntimeException("Ya existe un usuario con ese correo: " + dto.getCorreo());
        }
        if (userRepository.existsByRut(dto.getRut())) {
            throw new RuntimeException("Ya existe un usuario con ese RUT: " + dto.getRut());
        }

        // Factory Method: selecciona la fábrica correcta según el rol
        UserFactory factory = UserFactory.obtenerFactory(dto.getRolNombre());
        UserModel user = factory.crearUsuario(dto);

        // Asociar dirección si se provee
        if (dto.getDireccionId() != null) {
            DireccionModel dir = direccionRepository.findById(dto.getDireccionId())
                    .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
            user.setDireccion(dir);
        }

        UserModel saved = userRepository.save(user);

        // Publicar evento al tópico "user-created-topic" — arquitectura orientada a eventos:
        // Estado puede consumir este evento para asignar el estado inicial al usuario
        eventProducer.publishUserCreated(new UserCreatedEvent(
            saved.getId(), saved.getCorreo(), saved.getRolNombre()
        ));

        return saved;
    }

    @Transactional
    public UserModel updateUser(UUID id, UserRequestDto dto) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));

        if (dto.getNombre() != null)    user.setNombre(dto.getNombre());
        if (dto.getApellido() != null)  user.setApellido(dto.getApellido());
        if (dto.getCargo() != null)     user.setCargo(dto.getCargo());
        if (dto.getClave() != null)     user.setClave(dto.getClave());
        if (dto.getRolId() != null)     user.setRolId(dto.getRolId());
        if (dto.getRolNombre() != null) user.setRolNombre(dto.getRolNombre());

        if (dto.getDireccionId() != null) {
            DireccionModel dir = direccionRepository.findById(dto.getDireccionId())
                    .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
            user.setDireccion(dir);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserModel asignarRol(UUID userId, UUID rolId, String rolNombre) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));
        user.setRolId(rolId);
        user.setRolNombre(rolNombre);
        return userRepository.save(user);
    }

    // Llamado desde el consumer de Kafka cuando Rol publica un evento de asignación
    @Transactional
    public void actualizarRolPorCorreo(String correo, UUID rolId, String rolNombre) {
        userRepository.findByCorreo(correo).ifPresent(user -> {
            user.setRolId(rolId);
            user.setRolNombre(rolNombre);
            userRepository.save(user);
            System.out.println("[Users] Rol actualizado para " + correo + " → " + rolNombre);
        });
    }

    // Llamado desde el consumer de Kafka cuando Estado publica un evento de asignación
    @Transactional
    public void actualizarEstadoPorCorreo(String correo, UUID estadoId, String estadoNombre) {
        userRepository.findByCorreo(correo).ifPresent(user -> {
            user.setEstadoId(estadoId);
            user.setEstadoNombre(estadoNombre);
            // Sincronizar campo activo: true si el estado es "activo", false en otro caso
            user.setActivo("activo".equalsIgnoreCase(estadoNombre));
            userRepository.save(user);
            System.out.println("[Users] Estado actualizado para " + correo + " → " + estadoNombre);
        });
    }
}
