package com.itzetech.dpdms.request;

import com.itzetech.dpdms.request.RequestService.QuoteLine;
import com.itzetech.dpdms.request.RequestService.RequestDto;
import com.itzetech.dpdms.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
class RequestController {

    record CreateRequest(
            @NotBlank(message = "Enter a short title.") @Size(max = 200, message = "Keep the title under 200 characters.") String title,
            @NotBlank(message = "Describe what you need.") @Size(max = 4000, message = "Keep the description under 4000 characters.") String description,
            @Size(max = 100, message = "Keep the budget under 100 characters.") String budget,
            @NotNull(message = "Choose the date you need it by.") LocalDate deadline) {
    }

    record QuoteItemRequest(
            @NotBlank(message = "Describe this line.") @Size(max = 200, message = "Keep the line under 200 characters.") String description,
            @NotNull(message = "Enter the amount.") @Min(value = 1, message = "Each amount must be more than zero.") @Max(value = 1_000_000_000_000L, message = "That amount is too large.") Long amount) {
    }

    record QuoteRequest(
            @NotEmpty(message = "Add at least one line to the quote.") @Size(max = 30, message = "A quote can have at most 30 lines.") List<@Valid QuoteItemRequest> items,
            @NotNull(message = "Enter the number of weeks.") @Min(value = 1, message = "The work takes at least 1 week.") @Max(value = 104, message = "The work cannot take more than 104 weeks.") Integer weeks,
            @NotNull(message = "Choose how long the quote is valid.") LocalDate validUntil) {
    }

    record ChangesRequest(
            @NotBlank(message = "Tell the team what you would like changed.") @Size(max = 1000, message = "Keep the note under 1000 characters.") String note) {
    }

    record DeclineRequest(@Size(max = 1000, message = "Keep the reason under 1000 characters.") String reason) {
    }

    private final RequestService service;

    RequestController(RequestService service) {
        this.service = service;
    }

    @GetMapping
    List<RequestDto> list(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) ClientRequest.Status status) {
        return service.list(me, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RequestDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateRequest r) {
        return service.create(me, r.title(), r.description(), r.budget(), r.deadline());
    }

    @GetMapping("/{id}")
    RequestDto get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.get(me, id);
    }

    @PostMapping("/{id}/quote")
    RequestDto quote(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody QuoteRequest r) {
        return service.quote(me, id, r.items().stream().map(i -> new QuoteLine(i.description(), i.amount())).toList(),
                r.weeks(), r.validUntil());
    }

    @PostMapping("/{id}/accept")
    RequestDto accept(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.accept(me, id);
    }

    @PostMapping("/{id}/request-changes")
    RequestDto requestChanges(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody ChangesRequest r) {
        return service.requestChanges(me, id, r.note());
    }

    @PostMapping("/{id}/decline")
    RequestDto decline(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id, @Valid @RequestBody(required = false) DeclineRequest r) {
        return service.decline(me, id, r == null ? null : r.reason());
    }
}
