package com.stevesad.server.repository;

import com.stevesad.server.domain.ServerCertificate;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerCertificateRepository extends CrudRepository<ServerCertificate, UUID> {

    @Query("select * from server_certificate where private_key_pem is not null order by preferred desc limit 1")
    Optional<ServerCertificate> getAny();
}
