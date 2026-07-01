package Inventario.example.Inventario.service;

import Inventario.example.Inventario.model.AlertaBodegaModel;
import Inventario.example.Inventario.repository.AlertaBodegaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaBodegaService {

    private final AlertaBodegaRepository alertaBodegaRepository;

    @Transactional(readOnly = true)
    public List<AlertaBodegaModel> listarNoLeidas() {
        return alertaBodegaRepository.findByLeidaFalseOrderByFechaDesc();
    }

    @Transactional
    public void marcarLeida(Long id) {
        AlertaBodegaModel alerta = alertaBodegaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada: " + id));
        alerta.setLeida(true);
        alertaBodegaRepository.save(alerta);
        log.info("[Alertas] Alerta {} marcada como leída", id);
    }

    @Transactional
    public void crearAlertaSiNoDuplicada(Long idEstante, String codigoEstante, Long idBodega, Double ocupacionPct) {
        if (alertaBodegaRepository.existsByIdEstanteAndLeidaFalse(idEstante)) {
            log.debug("[Alertas] Alerta activa ya existe para estante={} — no se duplica", idEstante);
            return;
        }
        AlertaBodegaModel alerta = AlertaBodegaModel.builder()
                .idEstante(idEstante)
                .codigoEstante(codigoEstante)
                .idBodega(idBodega)
                .ocupacionPct(ocupacionPct)
                .fecha(LocalDateTime.now())
                .build();
        alertaBodegaRepository.save(alerta);
        log.warn("[Alertas] ALERTA creada — estante={} ocupacion={}%", codigoEstante, ocupacionPct);
    }
}
