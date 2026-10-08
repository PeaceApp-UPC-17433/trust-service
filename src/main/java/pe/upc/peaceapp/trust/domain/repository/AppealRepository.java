package pe.upc.peaceapp.trust.domain.repository;

import pe.upc.peaceapp.trust.domain.model.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppealRepository extends JpaRepository<Appeal, UUID> {
}
