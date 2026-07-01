package User.example.Users.factory;

import User.example.Users.dto.UserRequestDto;
import User.example.Users.model.UserModel;

// Patrón Factory Method: cada subclase decide cómo construir un UserModel
// según el tipo de usuario (cliente vs empleado) sin exponer la lógica de construcción
public abstract class UserFactory {

    public abstract UserModel crearUsuario(UserRequestDto dto);

    // Método estático que devuelve la fábrica correcta según el rolNombre
    public static UserFactory obtenerFactory(String rolNombre) {
        if (rolNombre != null && rolNombre.equalsIgnoreCase("cliente")) {
            return new ClienteFactory();
        }
        return new EmpleadoFactory();
    }
}
