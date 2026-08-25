package co.istad.ai_interview_app.features.finance.service;

import co.istad.ai_interview_app.features.finance.dto.CommissionRecordResponse;
import co.istad.ai_interview_app.features.finance.dto.HiringRecordResponse;
import co.istad.ai_interview_app.features.finance.dto.InvoiceItemResponse;
import co.istad.ai_interview_app.features.finance.dto.InvoicePaymentResponse;
import co.istad.ai_interview_app.features.finance.dto.InvoiceResponse;
import co.istad.ai_interview_app.features.finance.entity.CommissionRecord;
import co.istad.ai_interview_app.features.finance.entity.HiringRecord;
import co.istad.ai_interview_app.features.finance.entity.Invoice;
import co.istad.ai_interview_app.features.finance.entity.InvoiceItem;
import co.istad.ai_interview_app.features.finance.entity.InvoicePayment;
import co.istad.ai_interview_app.features.finance.repository.InvoiceItemRepository;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FinanceResponseMapper {

    private final InvoiceItemRepository invoiceItemRepository;

    public HiringRecordResponse toResponse(HiringRecord record, CommissionRecord commission) {
        return new HiringRecordResponse(
                record.getId(),
                record.getApplication().getId(),
                record.getJobPost().getId(),
                record.getJobPost().getTitle(),
                record.getCompany().getId(),
                record.getCompany().getName(),
                record.getJobSeekerProfile().getId(),
                record.getJobSeekerProfile().getHeadline(),
                record.getHiredAt(),
                record.getOfferedSalary(),
                record.getSalaryCurrency(),
                record.getNote(),
                record.getStatus(),
                record.getReviewedAt(),
                record.getReviewNote(),
                commission == null ? null : toResponse(commission)
        );
    }

    public CommissionRecordResponse toResponse(CommissionRecord commission) {
        /*
         * Which invoice picked this up, if any. Read from the item rather than
         * stored on the commission so the link cannot go stale when an invoice
         * is cancelled and the commission returns to the unbilled pool.
         */
        InvoiceItem item = invoiceItemRepository
                .findFirstByCommissionRecord_IdAndInvoice_StatusNot(
                        commission.getId(),
                        InvoiceStatus.CANCELLED
                )
                .orElse(null);

        return new CommissionRecordResponse(
                commission.getId(),
                commission.getHiringRecord().getId(),
                commission.getCompany().getId(),
                commission.getCompany().getName(),
                commission.getCommissionRate(),
                commission.getCommissionAmount(),
                commission.getCurrency(),
                commission.getDueAt(),
                commission.getPaidAt(),
                commission.getStatus(),
                commission.getNote(),
                item == null ? null : item.getInvoice().getId(),
                item == null ? null : item.getInvoice().getInvoiceNo()
        );
    }

    public InvoiceResponse toResponse(
            Invoice invoice,
            List<InvoiceItem> items,
            List<InvoicePayment> payments
    ) {
        BigDecimal outstanding = invoice.getTotalAmount()
                .subtract(invoice.getPaidAmount())
                .max(BigDecimal.ZERO);

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNo(),
                invoice.getCompany().getId(),
                invoice.getCompany().getName(),
                invoice.getSubtotalAmount(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getPaidAmount(),
                outstanding,
                invoice.getCurrency(),
                invoice.getIssuedAt(),
                invoice.getDueAt(),
                invoice.getPaidAt(),
                invoice.getStatus(),
                invoice.getNote(),
                items == null ? List.of() : items.stream().map(this::toResponse).toList(),
                payments == null ? null : payments.stream().map(this::toResponse).toList()
        );
    }

    public InvoiceItemResponse toResponse(InvoiceItem item) {
        return new InvoiceItemResponse(
                item.getId(),
                item.getCommissionRecord() == null ? null : item.getCommissionRecord().getId(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitAmount(),
                item.getTotalAmount()
        );
    }

    public InvoicePaymentResponse toResponse(InvoicePayment payment) {
        return new InvoicePaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getTransactionReference(),
                payment.getPaidAt(),
                payment.getStatus(),
                payment.getNote()
        );
    }
}
