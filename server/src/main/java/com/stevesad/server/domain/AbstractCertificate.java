package com.stevesad.server.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class AbstractCertificate {

    @Id
    private UUID id;

    @Column("cert_pem")
    private String certPem;

    @Column("private_key_pem")
    private String privateKeyPem;

    @Column("added_at")
    private Instant addedAt;

    @Column("description")
    private String description;
}
