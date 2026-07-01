package User.example.Users.factory;

import User.example.Users.dto.UserRequestDto;
import User.example.Users.model.UserModel;

// Fábrica para usuarios con rol de empleado (admin, bodeguero, transportista):
// - Tienen un cargo laboral asignado
// - La dirección es opcional
public class EmpleadoFactory extends UserFactory {

    @Override
    public UserModel crearUsuario(UserRequestDto dto) {
        UserModel user = new UserModel();
        user.setNombre(dto.getNombre());
        user.setApellido(dto.getApellido());
        user.setRut(dto.getRut());
        user.setCorreo(dto.getCorreo());
        user.setClave(dto.getClave());
        user.setCargo(dto.getCargo());
        user.setActivo(true);
        user.setRolId(dto.getRolId());
        user.setRolNombre(dto.getRolNombre());
        return user;
    }
}
