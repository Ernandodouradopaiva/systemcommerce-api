package br.com.systemcommerce.bi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bi_refresh_log")
public class BiRefreshLog {

    @Id
    private UUID id;

    @Column(name = "object_name", nullable = false, length = 120)
    private String objectName;

    @Column(name = "refresh_type", nullable = false, length = 30)
    private String refreshType = "MATERIALIZED_VIEW";

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "RUNNING";

    @Column(name = "rows_affected")
    private Long rowsAffected;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
