package Rol.example.Rol.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Cuerpo de la asignación de rol: el correo no viaja en la URL (D-08). */
@Data
public class AssignRoleRequestDto {

    @NotBlank
    @Email
    private String email;
}
