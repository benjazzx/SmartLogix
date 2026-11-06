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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rabbitmq")
@Tag(name = "RabbitMQ Admin", description = "Administración de colas, exchanges y bindings de RabbitMQ")
@RequiredArgsConstructor
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    // ── Colas ────────────────────────────────────────────────────────────────

    @Operation(summary = "Listar las colas")
    @GetMapping("/queues")
    public ResponseEntity<List<Map<String, Object>>> listarQueues() {
        return ResponseEntity.ok(rabbitAdminService.listarQueues());
    }

    @Operation(summary = "Crear una cola (opcionalmente con su dead-letter exchange y TTL)")
    @PostMapping("/queues")
    public ResponseEntity<Void> crearQueue(@Valid @RequestBody QueueRequestDto dto) {
        rabbitAdminService.crearQueue(dto);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Eliminar una cola (las colas de la aplicación están protegidas)")
    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<Void> eliminarQueue(@PathVariable String nombre) {
        rabbitAdminService.eliminarQueue(nombre);
        return ResponseEntity.noContent().build();
    }

    // ── Exchanges ────────────────────────────────────────────────────────────

    @Operation(summary = "Listar los exchanges")
    @GetMapping("/exchanges")
    public ResponseEntity<List<Map<String, Object>>> listarExchanges() {
        return ResponseEntity.ok(rabbitAdminService.listarExchanges());
    }

    @Operation(summary = "Crear un exchange")
    @PostMapping("/exchanges")
    public ResponseEntity<Void> crearExchange(@Valid @RequestBody ExchangeRequestDto dto) {
        rabbitAdminService.crearExchange(dto);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Eliminar un exchange (los exchanges de la aplicación están protegidos)")
    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String nombre) {
        rabbitAdminService.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    @Operation(summary = "Listar los bindings")
    @GetMapping("/bindings")
    public ResponseEntity<List<Map<String, Object>>> listarBindings() {
        return ResponseEntity.ok(rabbitAdminService.listarBindings());
    }

    @Operation(summary = "Crear un binding entre una cola y un exchange")
    @PostMapping("/bindings")
    public ResponseEntity<Void> crearBinding(@Valid @RequestBody BindingRequestDto dto) {
        rabbitAdminService.crearBinding(dto);
        return ResponseEntity.status(201).build();
    }

    // DELETE sin cuerpo: cola, exchange y routingKey viajan como query params
    // (DELETE /api/rabbitmq/bindings?cola=...&exchange=...&routingKey=...).
    @Operation(summary = "Eliminar un binding entre una cola y un exchange")
    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @ModelAttribute BindingRequestDto dto) {
        rabbitAdminService.eliminarBinding(dto);
        return ResponseEntity.noContent().build();
    }
}
