package pe.upc.peaceapp.trust.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    private String runAndCapture(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        var seen = new AtomicReference<String>();
        filter.doFilter(request, response, (req, res) -> seen.set(MDC.get("requestId")));
        assertThat(MDC.get("requestId")).isNull();
        return seen.get();
    }

    @Test
    void reusesValidIncomingRequestIdAndEchoesIt() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "abc12345-trace");
        var response = new MockHttpServletResponse();
        assertThat(runAndCapture(request, response)).isEqualTo("abc12345-trace");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("abc12345-trace");
    }

    @Test
    void generatesIdWhenMissingOrInvalid() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "mal formado\n");
        var response = new MockHttpServletResponse();
        var id = runAndCapture(request, response);
        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(id);
    }
}
