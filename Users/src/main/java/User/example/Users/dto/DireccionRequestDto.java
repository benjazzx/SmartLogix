package User.example.Users.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class DireccionRequestDto {
    private String calle;
    private String numero;
    private String codigoPostal;
    private UUID comunaId;
}
