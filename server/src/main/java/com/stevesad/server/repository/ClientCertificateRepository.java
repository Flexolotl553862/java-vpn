package com.stevesad.server.repository;

import com.stevesad.server.domain.ClientCertificate;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientCertificateRepository extends CrudRepository<ClientCertificate, UUID> {

    @Query("select * from client_certificate where trusted = true")
    List<ClientCertificate> getTrusted();
}
