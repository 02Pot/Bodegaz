package com.warehouse.system.Model;


import com.warehouse.system.DTO.Lease.LeaseClause;
import com.warehouse.system.DTO.Lease.LeaseTemplateConfig;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lease_templates", indexes = {
        @Index(name = "idx_lease_templates_owner", columnList = "owner_id"),
        @Index(name = "idx_lease_templates_default", columnList = "platform_default")
})
@Getter
@Setter
@NoArgsConstructor
public class LeaseTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "template_version", nullable = false)
    private int version = 1;

    @Column(name = "platform_default", nullable = false)
    private boolean platformDefault = false;

    @Column(nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private LeaseTemplateConfig config;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<LeaseClause> clauses;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // On your existing Warehouse entity add:
    //   @Column(name = "default_lease_template_id")
    //   private UUID defaultLeaseTemplateId;
}