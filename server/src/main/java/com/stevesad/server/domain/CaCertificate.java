package com.stevesad.server.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name = "ca_certificate")
public class CaCertificate extends AbstractCertificate {}
