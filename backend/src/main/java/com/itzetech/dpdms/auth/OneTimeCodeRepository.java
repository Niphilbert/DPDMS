package com.itzetech.dpdms.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface OneTimeCodeRepository extends JpaRepository<OneTimeCode, UUID> {

    Optional<OneTimeCode> findFirstByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(UUID userId, OneTimeCode.Purpose purpose);

    @Modifying
    @Query("delete from OneTimeCode c where c.userId = :userId and c.purpose = :purpose")
    void deleteFor(@Param("userId") UUID userId, @Param("purpose") OneTimeCode.Purpose purpose);
}
