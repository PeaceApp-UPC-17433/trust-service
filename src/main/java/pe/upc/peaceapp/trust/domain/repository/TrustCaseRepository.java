package pe.upc.peaceapp.trust.domain.repository;

import pe.upc.peaceapp.trust.domain.model.TrustCase;
import pe.upc.peaceapp.trust.domain.model.TrustStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TrustCaseRepository extends JpaRepository<TrustCase, UUID> {
    List<TrustCase> findByTypeAndOccurredAtAfter(String type, Instant after);
    List<TrustCase> findByStatus(TrustStatus status);
}
