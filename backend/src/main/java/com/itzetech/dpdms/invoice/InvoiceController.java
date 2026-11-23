package com.itzetech.dpdms.invoice;

import com.itzetech.dpdms.invoice.InvoiceDtos.InvoiceDto;
import com.itzetech.dpdms.invoice.InvoiceDtos.InvoiceInput;
import com.itzetech.dpdms.invoice.InvoiceDtos.InvoiceSuggestion;
import com.itzetech.dpdms.invoice.InvoiceDtos.InvoiceTotals;
import com.itzetech.dpdms.invoice.InvoiceDtos.PaymentInput;
import com.itzetech.dpdms.invoice.InvoiceDtos.ProjectMoney;
import com.itzetech.dpdms.invoice.InvoiceDtos.ReverseInput;
import com.itzetech.dpdms.invoice.InvoiceDtos.VoidInput;
import com.itzetech.dpdms.invoice.InvoiceService.Filter;
import com.itzetech.dpdms.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Invoices and payments. Who may do what is decided in {@link InvoiceService}. */
@RestController
class InvoiceController {

    private final InvoiceService service;

    InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @GetMapping("/api/invoices")
    List<InvoiceDto> list(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) String status,
                          @RequestParam(required = false) UUID projectId, @RequestParam(required = false) UUID companyId,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(me, new Filter(status, projectId, companyId, from, to));
    }

    @GetMapping("/api/invoices/export.csv")
    ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) String status,
                                  @RequestParam(required = false) UUID projectId, @RequestParam(required = false) UUID companyId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.exportCsv(me, new Filter(status, projectId, companyId, from, to));
    }

    @GetMapping("/api/invoices/summary")
    InvoiceTotals summary(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) UUID projectId,
                          @RequestParam(required = false) UUID companyId) {
        return service.summary(me, new Filter(null, projectId, companyId, null, null));
    }

    @GetMapping("/api/invoices/{id}")
    InvoiceDto get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.detail(me, id);
    }

    @PostMapping("/api/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    InvoiceDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody InvoiceInput in) {
        return service.create(me, in);
    }

    @PutMapping("/api/invoices/{id}")
    InvoiceDto update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody InvoiceInput in) {
        return service.update(me, id, in);
    }

    @PostMapping("/api/invoices/{id}/send")
    InvoiceDto send(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.send(me, id);
    }

    @PostMapping("/api/invoices/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    InvoiceDto pay(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody PaymentInput in) {
        return service.recordPayment(me, id, in);
    }

    @PostMapping("/api/invoices/{id}/payments/{paymentId}/reverse")
    InvoiceDto reverse(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @PathVariable UUID paymentId,
                       @Valid @RequestBody ReverseInput in) {
        return service.reversePayment(me, id, paymentId, in.reason());
    }

    @PostMapping("/api/invoices/{id}/void")
    InvoiceDto voidInvoice(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                           @Valid @RequestBody(required = false) VoidInput in) {
        return service.voidInvoice(me, id, in == null ? null : in.reason());
    }

    @PostMapping("/api/invoices/{id}/remind")
    InvoiceDto remind(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.remind(me, id);
    }

    @GetMapping("/api/projects/{projectId}/invoices")
    List<InvoiceDto> forProject(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId) {
        return service.forProject(me, projectId);
    }

    @GetMapping("/api/projects/{projectId}/money")
    ProjectMoney money(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId) {
        return service.projectMoney(me, projectId);
    }

    @GetMapping("/api/projects/{projectId}/invoice-suggestion")
    InvoiceSuggestion suggestion(@AuthenticationPrincipal AuthUser me, @PathVariable UUID projectId) {
        return service.suggestion(me, projectId);
    }
}
