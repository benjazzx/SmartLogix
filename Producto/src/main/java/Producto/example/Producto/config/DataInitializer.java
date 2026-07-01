package Producto.example.Producto.config;

import Producto.example.Producto.model.CategoriaModel;
import Producto.example.Producto.model.ProductoModel;
import Producto.example.Producto.repository.CategoriaRepository;
import Producto.example.Producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoriaRepository.count() > 0) {
            log.info("Producto ya inicializado — omitiendo seed");
            return;
        }
        log.info("Inicializando datos de Producto...");
        seedCategorias();
        log.info("Producto inicializado correctamente");
    }

    private void seedCategorias() {
        CategoriaModel electronica = categoriaRepository.save(CategoriaModel.builder()
                .nombre("Electrónica")
                .descripcion("Dispositivos electrónicos y tecnología")
                .build());

        CategoriaModel alimentos = categoriaRepository.save(CategoriaModel.builder()
                .nombre("Alimentos")
                .descripcion("Productos alimenticios y bebidas")
                .build());

        CategoriaModel limpieza = categoriaRepository.save(CategoriaModel.builder()
                .nombre("Limpieza")
                .descripcion("Artículos de limpieza e higiene")
                .build());

        CategoriaModel herramientas = categoriaRepository.save(CategoriaModel.builder()
                .nombre("Herramientas")
                .descripcion("Herramientas y equipos de trabajo")
                .build());

        CategoriaModel oficina = categoriaRepository.save(CategoriaModel.builder()
                .nombre("Oficina")
                .descripcion("Insumos y equipos de oficina")
                .build());

        log.info("5 categorías creadas");

        productoRepository.save(ProductoModel.builder()
                .nombre("Laptop Dell Inspiron 15")
                .descripcion("Laptop 15.6\" Intel Core i5, 8GB RAM, 512GB SSD")
                .precio(new BigDecimal("699990"))
                .stock(15)
                .categoria(electronica)
                .estadoNombre("publicado")
                .activo(true)
                .build());

        productoRepository.save(ProductoModel.builder()
                .nombre("Monitor LG 24\" Full HD")
                .descripcion("Monitor IPS 1920x1080, 75Hz, HDMI")
                .precio(new BigDecimal("189990"))
                .stock(30)
                .categoria(electronica)
                .estadoNombre("publicado")
                .activo(true)
                .build());

        productoRepository.save(ProductoModel.builder()
                .nombre("Aceite Vegetal 1L")
                .descripcion("Aceite vegetal refinado para cocina, botella 1 litro")
                .precio(new BigDecimal("2490"))
                .stock(200)
                .categoria(alimentos)
                .estadoNombre("publicado")
                .activo(true)
                .build());

        productoRepository.save(ProductoModel.builder()
                .nombre("Jabón Líquido 500ml")
                .descripcion("Jabón líquido antibacterial para manos, fragancia lavanda")
                .precio(new BigDecimal("3990"))
                .stock(150)
                .categoria(limpieza)
                .estadoNombre("publicado")
                .activo(true)
                .build());

        productoRepository.save(ProductoModel.builder()
                .nombre("Taladro Bosch 600W")
                .descripcion("Taladro percutor 600W con maletín y accesorios incluidos")
                .precio(new BigDecimal("79990"))
                .stock(25)
                .categoria(herramientas)
                .estadoNombre("publicado")
                .activo(true)
                .build());

        productoRepository.save(ProductoModel.builder()
                .nombre("Resma Papel A4 500 hojas")
                .descripcion("Papel bond 75g/m² apto para impresoras láser e inkjet")
                .precio(new BigDecimal("5990"))
                .stock(8)
                .categoria(oficina)
                .estadoNombre("bajo_stock")
                .activo(true)
                .build());

        productoRepository.save(ProductoModel.builder()
                .nombre("Detergente Concentrado 2L")
                .descripcion("Detergente líquido concentrado para ropa, 40 lavados")
                .precio(new BigDecimal("6990"))
                .stock(120)
                .categoria(limpieza)
                .estadoNombre("publicado")
                .activo(true)
                .build());

        productoRepository.save(ProductoModel.builder()
                .nombre("Teclado Mecánico USB")
                .descripcion("Teclado mecánico español, switches azules, retroiluminado RGB")
                .precio(new BigDecimal("49990"))
                .stock(0)
                .categoria(electronica)
                .estadoNombre("sin_stock")
                .activo(true)
                .build());

        log.info("8 productos de ejemplo creados");
    }
}
