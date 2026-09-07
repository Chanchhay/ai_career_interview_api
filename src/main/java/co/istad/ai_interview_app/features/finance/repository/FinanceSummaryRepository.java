package co.istad.ai_interview_app.features.finance.repository;

import co.istad.ai_interview_app.features.finance.dto.FinanceSummaryResponse.PaidCompany;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Aggregates in the database, without fetching invoices or payments per row. */
@Repository
@RequiredArgsConstructor
public class FinanceSummaryRepository {
    private final EntityManager em;
    private static final String RECEIPTS = """
             from InvoicePayment p join p.invoice i join i.company c
             where p.status = :paid and p.currency = :currency
               and p.paidAt >= :start and p.paidAt < :end
               and i.status not in :excluded
            """;

    private <T> TypedQuery<T> receipts(String select, String suffix, Class<T> type,
                                      Instant start, Instant end, String currency) {
        return em.createQuery(select + RECEIPTS + suffix, type)
                .setParameter("paid", PaymentStatus.PAID).setParameter("currency", currency)
                .setParameter("start", start).setParameter("end", end)
                .setParameter("excluded", List.of(InvoiceStatus.DRAFT, InvoiceStatus.CANCELLED));
    }

    public Object[] totals(Instant start, Instant end, String currency) {
        return receipts("select coalesce(sum(p.amount), 0), count(p), count(distinct c.id), count(distinct i.id)",
                "", Object[].class, start, end, currency).getSingleResult();
    }

    public Object[] receivables(String currency, Instant now) {
        return em.createQuery("""
                select coalesce(sum(i.totalAmount - i.paidAmount), 0), count(i),
                       coalesce(sum(case when i.dueAt < :now then i.totalAmount - i.paidAmount else 0 end), 0),
                       coalesce(sum(case when i.dueAt < :now then 1 else 0 end), 0)
                from Invoice i where i.currency = :currency and i.status in :statuses
                  and i.totalAmount > i.paidAmount
                """, Object[].class)
                .setParameter("currency", currency).setParameter("now", now)
                .setParameter("statuses", List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE))
                .getSingleResult();
    }

    public List<String> currencies() {
        return em.createQuery("select distinct i.currency from Invoice i order by i.currency", String.class).getResultList();
    }

    public Page<PaidCompany> companies(Instant start, Instant end, String currency, String search, Pageable page) {
        // The search is parameterised and covers all matching companies, not one page.
        String filter = " and (:search = '' or locate(:search, lower(c.name)) > 0)";
        long total = receipts("select count(distinct c.id)", filter, Long.class, start, end, currency)
                .setParameter("search", search).getSingleResult();
        List<Object[]> rows = receipts("select c.id, c.name, sum(p.amount), count(p), count(distinct i.id), max(p.paidAt)",
                filter + " group by c.id, c.name order by sum(p.amount) desc, c.name asc, c.id asc",
                Object[].class, start, end, currency)
                .setParameter("search", search).setFirstResult(Math.toIntExact(page.getOffset()))
                .setMaxResults(page.getPageSize()).getResultList();
        return new PageImpl<>(rows.stream().map(row -> new PaidCompany((UUID) row[0], (String) row[1], currency,
                (BigDecimal) row[2], ((Number) row[3]).longValue(), ((Number) row[4]).longValue(), (Instant) row[5])).toList(), page, total);
    }

    public List<Object[]> trend(List<Instant> boundaries, String currency) {
        // Each boundary is computed in the reporting time zone by the service.
        // A bounded CASE (7/28-31/12 buckets) groups in ONE query and works on
        // both PostgreSQL and H2 without relying on either server's time zone.
        StringBuilder bucket = new StringBuilder("case");
        for (int index = 1; index < boundaries.size() - 1; index++) {
            bucket.append(" when p.paidAt < :boundary").append(index).append(" then ").append(index - 1);
        }
        bucket.append(" else ").append(boundaries.size() - 2).append(" end");
        TypedQuery<Object[]> query = receipts("select " + bucket + " as bucketIndex, sum(p.amount), count(p)",
                " group by bucketIndex", Object[].class, boundaries.getFirst(), boundaries.getLast(), currency);
        for (int index = 1; index < boundaries.size() - 1; index++) {
            query.setParameter("boundary" + index, boundaries.get(index));
        }
        return query.getResultList();
    }
}
