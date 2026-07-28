package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.PrivilegedAccessRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegedAccessRequestRepository extends JpaRepository<PrivilegedAccessRequest, UUID> {
    List<PrivilegedAccessRequest> findByStatusOrderByCreatedAtDesc(PrivilegedAccessRequest.Status status);
}
