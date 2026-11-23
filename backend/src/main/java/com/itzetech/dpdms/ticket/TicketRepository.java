package com.itzetech.dpdms.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByCompanyId(UUID companyId);

    List<Ticket> findByAssigneeId(UUID assigneeId);

    List<Ticket> findByStatusIn(Collection<Ticket.Status> statuses);

    /** Marks the "close to the deadline" alert as sent. Returns 1 only for the caller that gets to send it. */
    @Modifying
    @Query("update Ticket t set t.slaWarnedAt = :at where t.id = :id and t.slaWarnedAt is null")
    int claimWarning(@Param("id") UUID id, @Param("at") Instant at);

    /** Marks the "deadline missed" alert as sent (and the earlier warning as done). Returns 1 only for the caller that sends it. */
    @Modifying
    @Query("update Ticket t set t.slaBreachNotifiedAt = :at, t.slaWarnedAt = coalesce(t.slaWarnedAt, :at) where t.id = :id and t.slaBreachNotifiedAt is null")
    int claimBreach(@Param("id") UUID id, @Param("at") Instant at);
}
