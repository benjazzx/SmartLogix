package Rol.example.Rol.component;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.repository.RolRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RolRepository rolRepository;

    @Override
    public void run(String... args) throws Exception {
        if (rolRepository.count() == 0) {
            List<RolModel> roles = Arrays.asList(
                new RolModel(null, "cliente", "Rol asignado a los clientes de la plataforma"),
                new RolModel(null, "admin", "Administrador con control total del sistema"),
                new RolModel(null, "bodeguero", "Se encarga de buscar y gestionar productos en la bodega"),
                new RolModel(null, "transportista", "Encargado de transportar y entregar los productos")
            );
            rolRepository.saveAll(roles);
            System.out.println("Roles iniciales (cliente, admin, bodeguero, transportista) han sido insertados en la base de datos.");
        }
    }
}
