package Configuracion.example.Configuracion.service;

import Configuracion.example.Configuracion.model.PreferenciaModel;
import Configuracion.example.Configuracion.repository.PreferenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConfiguracionService {

    private final PreferenciaRepository repository;

    public ConfiguracionService(PreferenciaRepository repository) {
        this.repository = repository;
    }

    public Map<String, String> getPreferencias(UUID userId) {
        return repository.findAllByUserId(userId)
                .stream()
                .collect(Collectors.toMap(PreferenciaModel::getClave, PreferenciaModel::getValor));
    }

    @Transactional
    public void setPreferencia(UUID userId, String clave, String valor) {
        PreferenciaModel pref = repository
                .findByUserIdAndClave(userId, clave)
                .orElseGet(() -> new PreferenciaModel(userId, clave, valor));
        pref.setValor(valor);
        repository.save(pref);
    }

    public String getPreferencia(UUID userId, String clave) {
        return repository.findByUserIdAndClave(userId, clave)
                .map(PreferenciaModel::getValor)
                .orElse(null);
    }
}
