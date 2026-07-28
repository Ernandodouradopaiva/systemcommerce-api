package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.SessionRevocation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRevocationRepository extends JpaRepository<SessionRevocation, UUID> {}
