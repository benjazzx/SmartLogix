package Producto.example.Producto.config;

import Producto.example.Producto.model.CategoriaModel;
import Producto.example.Producto.model.ProductoModel;
import Producto.example.Producto.repository.CategoriaRepository;
import Producto.example.Producto.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_productosYaExisten_noInsertaNada() {
        when(categoriaRepository.count()).thenReturn(5L);

        dataInitializer.run();

        verify(categoriaRepository, never()).save(any());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void run_categoriasVacias_insertaCategoriasYProductos() {
        when(categoriaRepository.count()).thenReturn(0L);
        when(categoriaRepository.save(any(CategoriaModel.class))).thenReturn(mock(CategoriaModel.class));
        when(productoRepository.save(any(ProductoModel.class))).thenReturn(mock(ProductoModel.class));

        dataInitializer.run();

        verify(categoriaRepository, times(5)).save(any(CategoriaModel.class));
        verify(productoRepository, times(8)).save(any(ProductoModel.class));
    }
}
