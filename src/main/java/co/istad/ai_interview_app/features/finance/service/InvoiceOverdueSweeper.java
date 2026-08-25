package co.istad.ai_interview_app.features.finance.service;

import co.istad.ai_interview_app.features.finance.entity.Invoice;
import co.istad.ai_interview_app.features.finance.repository.InvoiceRepository;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Moves issued invoices to OVERDUE once their due date passes.
 *
 * <p>Without this the status is unreachable: nothing else in the lifecycle sets
 * it, so an unpaid invoice sat as ISSUED forever and the finance list could not
 * distinguish "sent last week" from "sent last quarter and ignored".
 *
 * <p>Recording it rather than deriving it at read time is deliberate. The date
 * a bill went overdue is a fact about the account, and computing it on the fly
 * would mean every list, filter, and report had to reimplement the same
 * comparison and agree about it.
 *
 * <p>Single-instance assumption: two application instances would both run this.
 * The write is idempotent, so the cost is a duplicated update rather than a
 * wrong result — but a shared scheduler lock is the fix if it ever scales out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceOverdueSweeper {

    private final InvoiceRepository invoiceRepository;

    /**
     * Hourly rather than at a fixed hour, so an invoice does not stay current
     * for most of a day after falling due, and so a restart cannot skip the run.
     */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    @Transactional
    public void markOverdueInvoices() {
        List<Invoice> overdue = invoiceRepository.findOverdue(Instant.now());

        if (overdue.isEmpty()) return;

        for (Invoice invoice : overdue) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
        }

        log.info("Marked {} invoice(s) overdue", overdue.size());
    }
}
