package Rol.example.Rol.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.repository.TipoRepository;

@Service
public class TipoService {

    @Autowired
    private TipoRepository tipoRepository;

    public List<TipoModel> getAllTipos() {
        return tipoRepository.findAll();
    }

    public TipoModel getTipoById(UUID id) {
        return tipoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tipo no encontrado con id: " + id));
    }

    public TipoModel getTipoByNombre(String nombre) {
        return tipoRepository.findByNombre(nombre)
            .orElseThrow(() -> new RuntimeException("Tipo no encontrado con nombre: " + nombre));
    }

    public TipoModel createTipo(TipoModel tipo) {
        if (tipoRepository.findByNombre(tipo.getNombre()).isPresent()) {
            throw new RuntimeException("Ya existe un tipo con el nombre: " + tipo.getNombre());
        }
        return tipoRepository.save(tipo);
    }

    public TipoModel updateTipo(UUID id, TipoModel tipo) {
        TipoModel existing = tipoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tipo no encontrado con id: " + id));
        existing.setNombre(tipo.getNombre());
        return tipoRepository.save(existing);
    }

    public void deleteTipo(UUID id) {
        tipoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tipo no encontrado con id: " + id));
        tipoRepository.deleteById(id);
    }
}
