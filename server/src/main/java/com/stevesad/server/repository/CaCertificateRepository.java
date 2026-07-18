package com.stevesad.server.repository;

import com.stevesad.server.domain.CaCertificate;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaCertificateRepository extends CrudRepository<CaCertificate, UUID> {

    @Query("select * from ca_certificate")
    List<CaCertificate> getAllCaCertificates();
}
