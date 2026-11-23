package com.itzetech.dpdms.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {

    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    /** Rows of {ticketId, count}. Internal notes are counted only when {@code withInternal} is true. */
    @Query("""
            select c.ticketId, count(c) from TicketComment c
            where c.ticketId in :ids and (:withInternal = true or c.internal = false)
            group by c.ticketId
            """)
    List<Object[]> countByTicket(@Param("ids") Collection<UUID> ids, @Param("withInternal") boolean withInternal);
}
