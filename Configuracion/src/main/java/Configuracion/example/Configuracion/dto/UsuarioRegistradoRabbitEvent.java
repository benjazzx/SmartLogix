package Configuracion.example.Configuracion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

// Evento que publica Users (routing key usuario.registrado) cuando se crea una cuenta.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRegistradoRabbitEvent implements Serializable {
    private UUID userId;
    private String rolNombre;
}
