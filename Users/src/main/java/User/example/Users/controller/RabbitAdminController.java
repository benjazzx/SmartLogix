package User.example.Users.controller;

import User.example.Users.dto.BindingRequestDto;
import User.example.Users.dto.ExchangeRequestDto;
import User.example.Users.dto.QueueRequestDto;
import User.example.Users.service.RabbitAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Microservicio administrador de RabbitMQ (EP3 DSY1107): endpoints REST para crear y
// eliminar colas, exchanges y bindings. Solo admin — es infraestructura de mensajería,
// no una operación de negocio.
@RestController
@RequestMapping("/api/rabbitmq")
@Tag(name = "RabbitMQ Admin", description = "Administración de colas, exchanges y bindings de RabbitMQ")
@RequiredArgsConstructor
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    @Operation(summary = "Crear una cola")
    @PostMapping("/queues")
    public ResponseEntity<Void> crearQueue(@Valid @RequestBody QueueRequestDto dto) {
        rabbitAdminService.crearQueue(dto);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Eliminar una cola")
    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<Void> eliminarQueue(@PathVariable String nombre) {
        rabbitAdminService.eliminarQueue(nombre);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear un exchange")
    @PostMapping("/exchanges")
    public ResponseEntity<Void> crearExchange(@Valid @RequestBody ExchangeRequestDto dto) {
        rabbitAdminService.crearExchange(dto);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Eliminar un exchange")
    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String nombre) {
        rabbitAdminService.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear un binding entre una cola y un exchange")
    @PostMapping("/bindings")
    public ResponseEntity<Void> crearBinding(@Valid @RequestBody BindingRequestDto dto) {
        rabbitAdminService.crearBinding(dto);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Eliminar un binding entre una cola y un exchange")
    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequestDto dto) {
        rabbitAdminService.eliminarBinding(dto);
        return ResponseEntity.noContent().build();
    }
}
