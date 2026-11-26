package pe.upc.peaceapp.trust.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GatewayIdentityFilterTest {
    private final GatewayIdentityFilter filter = new GatewayIdentityFilter();
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void run(String userId, String role) throws Exception {
        var request = new MockHttpServletRequest();
        if (userId != null) request.addHeader("X-User-Id", userId);
        if (role != null) request.addHeader("X-User-Role", role);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
    }

    @Test
    void validHeadersAuthenticateWithRole() throws Exception {
        var id = UUID.randomUUID().toString();
        run(id, "MODERADOR");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo(id);
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_MODERADOR");
    }

    @Test
    void missingOrMalformedHeadersLeaveRequestAnonymous() throws Exception {
        run(null, null);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        run("no-es-uuid", "CIUDADANO");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        run(UUID.randomUUID().toString(), "rol invalido!");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
