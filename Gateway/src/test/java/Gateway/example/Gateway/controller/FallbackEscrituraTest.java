package Gateway.example.Gateway.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** D-07: cada método HTTP del fallback está declarado explícitamente y responde 503. */
class FallbackEscrituraTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FallbackController()).build();

    @ParameterizedTest
    @CsvSource({
            "POST, users", "PUT, rol", "PATCH, estado", "DELETE, inventario", "POST, orden", "PUT, producto",
            "GET, users", "GET, producto", "GET, configuracion", "PUT, configuracion"
    })
    void fallback_responde503ParaLecturaYEscritura(String metodo, String servicio) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(metodo), "/fallback/" + servicio))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.service").value(servicio + "-service"));
    }
}
