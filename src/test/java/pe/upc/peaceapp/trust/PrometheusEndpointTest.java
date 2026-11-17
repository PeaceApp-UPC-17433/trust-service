package pe.upc.peaceapp.trust;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 4.3: el endpoint que consume Prometheus responde sin autenticacion y con la etiqueta del servicio. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class PrometheusEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesPrometheusMetricsWithApplicationTag() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(containsString("application=\"trust-service\"")));
    }
}
