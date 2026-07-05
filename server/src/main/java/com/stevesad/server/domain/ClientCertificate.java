package com.stevesad.server.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name = "client_certificate")
public class ClientCertificate extends AbstractCertificate {

    @Column("client_address")
    private String clientAddress;
}
