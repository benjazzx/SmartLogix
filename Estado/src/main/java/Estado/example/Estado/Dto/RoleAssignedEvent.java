package Estado.example.Estado.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignedEvent {
    private String email;
    private UUID roleId;
    private String roleName;
}
