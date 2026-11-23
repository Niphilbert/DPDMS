package com.itzetech.dpdms.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    List<UserAccount> findByRoleAndStatus(Role role, UserStatus status);

    long countByRoleAndStatus(Role role, UserStatus status);

    List<UserAccount> findByCompanyIdAndStatus(UUID companyId, UserStatus status);

    @Query("""
            select u from UserAccount u left join fetch u.company c
            where (:role is null or u.role = :role)
              and (:q is null or lower(u.fullName) like :q or lower(u.email) like :q
                   or lower(coalesce(c.name, '')) like :q)
            order by u.createdAt desc
            """)
    List<UserAccount> search(@Param("role") Role role, @Param("q") String q);
}
