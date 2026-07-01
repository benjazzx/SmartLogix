package Rol.example.Rol.messaging;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import Rol.example.Rol.dto.RoleAssignedEvent;

// Productor de eventos Kafka.
// En la arquitectura orientada a eventos, cuando algo importante ocurre (ej: se asigna un rol),
// este componente publica un mensaje en un tópico de Kafka.
// Un tópico es como un canal de mensajes — cualquier microservicio puede suscribirse y escuchar.
@Component
public class RoleEventProcessor {

    // StreamBridge es la forma de Spring Cloud Stream para publicar mensajes a Kafka
    // sin necesidad de declarar un bean Function o Consumer — simplemente enviamos cuando queremos.
    @Autowired
    private StreamBridge streamBridge;

    // Publica un evento al tópico "role-assigned-topic" en Kafka.
    // El mensaje contiene: email del usuario, UUID del rol asignado y nombre del rol.
    // El microservicio Users tiene un Consumer escuchando ese tópico y actualiza su base de datos.
    // Este flujo es ASÍNCRONO: Rol publica y sigue adelante, no espera respuesta de Users.
    public void publishRoleAssigned(String email, UUID roleId, String roleName) {
        RoleAssignedEvent event = new RoleAssignedEvent(email, roleId, roleName);
        streamBridge.send("role-assigned-topic", event);
    }
}
