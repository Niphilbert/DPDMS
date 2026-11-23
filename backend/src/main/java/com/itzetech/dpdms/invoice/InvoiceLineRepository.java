package com.itzetech.dpdms.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {

    List<InvoiceLine> findByInvoiceIdInOrderBySortOrderAsc(Collection<UUID> invoiceIds);

    void deleteByInvoiceId(UUID invoiceId);
}
