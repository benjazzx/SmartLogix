package User.example.Users.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QueueRequestDto {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Size(max = 255, message = "El nombre de la cola no puede superar 255 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre solo puede tener letras, números, puntos, guiones y guiones bajos")
    private String nombre;

    private boolean durable = true;

    // Opcionales: permiten crear una cola con su DLQ ya enlazada (x-dead-letter-exchange).
    @Size(max = 255, message = "El exchange de dead-letter no puede superar 255 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]*$", message = "El exchange de dead-letter solo puede tener letras, números, puntos, guiones y guiones bajos")
    private String deadLetterExchange;

    @Size(max = 255, message = "La routing key de dead-letter no puede superar 255 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]*$", message = "La routing key de dead-letter solo puede tener letras, números, puntos, guiones y guiones bajos")
    private String deadLetterRoutingKey;

    @Positive(message = "El TTL debe ser mayor que 0")
    @Max(value = 86_400_000, message = "El TTL no puede superar 24 horas (86400000 ms)")
    private Integer ttlMs;

    // Una routing key de dead-letter sin exchange no tiene efecto en RabbitMQ: se rechaza explícitamente.
    @JsonIgnore
    @AssertTrue(message = "deadLetterRoutingKey requiere deadLetterExchange")
    public boolean isDeadLetterRoutingKeyConsistente() {
        boolean hayRoutingKey = deadLetterRoutingKey != null && !deadLetterRoutingKey.isBlank();
        boolean hayExchange = deadLetterExchange != null && !deadLetterExchange.isBlank();
        return !hayRoutingKey || hayExchange;
    }
}
