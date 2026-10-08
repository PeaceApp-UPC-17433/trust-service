package pe.upc.peaceapp.trust.domain.repository;

import pe.upc.peaceapp.trust.domain.model.ModerationDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ModerationDecisionRepository extends JpaRepository<ModerationDecision, UUID> {
}
