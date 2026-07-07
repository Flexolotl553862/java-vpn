package com.stevesad.server.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name = "server_certificate")
public class ServerCertificate extends AbstractCertificate {

    @Column("preferred")
    private boolean preferred;
}
