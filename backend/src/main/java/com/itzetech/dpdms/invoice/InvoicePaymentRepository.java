package com.itzetech.dpdms.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, UUID> {

    List<InvoicePayment> findByInvoiceIdInOrderByRecordedAtAsc(Collection<UUID> invoiceIds);

    List<InvoicePayment> findByInvoiceIdAndReversedFalse(UUID invoiceId);

    Optional<InvoicePayment> findByIdAndInvoiceId(UUID id, UUID invoiceId);

    List<InvoicePayment> findByReversedFalseAndPaidOnBetween(LocalDate from, LocalDate to);
}
