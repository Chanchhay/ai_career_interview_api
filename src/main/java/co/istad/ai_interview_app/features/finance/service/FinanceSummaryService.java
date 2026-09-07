package co.istad.ai_interview_app.features.finance.service;

import co.istad.ai_interview_app.features.finance.dto.FinanceSummaryResponse;
import co.istad.ai_interview_app.features.finance.dto.FinanceSummaryResponse.PaidCompany;
import co.istad.ai_interview_app.features.finance.dto.FinanceSummaryResponse.Trend;
import co.istad.ai_interview_app.features.finance.repository.FinanceSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class FinanceSummaryService {
    public static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Phnom_Penh");
    private final FinanceSummaryRepository reports;
    private final FinanceSettingsService settings;

    public FinanceSummaryResponse summary(FinanceReportPeriod period, LocalDate date, String requestedCurrency) {
        LocalDate start = start(period, date);
        LocalDate end = period.next(start);
        LocalDate previous = period.previous(start);
        String currency = currency(requestedCurrency);
        Instant now = Instant.now();
        Object[] totals = reports.totals(instant(start), instant(end), currency);
        Object[] prior = reports.totals(instant(previous), instant(start), currency);
        Object[] owed = reports.receivables(currency, now);
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate day = start; day.isBefore(end); day = period == FinanceReportPeriod.YEAR ? day.plusMonths(1) : day.plusDays(1)) {
            dates.add(day);
        }
        List<Instant> boundaries = new ArrayList<>(dates.stream().map(this::instant).toList());
        boundaries.add(instant(end));
        List<Trend> trend = new ArrayList<>(dates.stream().map(day -> new Trend(day, BigDecimal.ZERO, 0)).toList());
        for (Object[] row : reports.trend(boundaries, currency)) {
            int index = ((Number) row[0]).intValue();
            trend.set(index, new Trend(dates.get(index), amount(row[1]), count(row[2])));
        }
        TreeSet<String> currencies = new TreeSet<>(reports.currencies());
        currencies.add(currency);
        return new FinanceSummaryResponse(period.name(), start, end, previous, REPORT_ZONE.getId(), currency,
                List.copyOf(currencies), now, amount(totals[0]), amount(prior[0]), count(totals[1]), count(totals[2]), count(totals[3]),
                amount(owed[0]), count(owed[1]), amount(owed[2]), count(owed[3]), trend);
    }

    public Page<PaidCompany> companies(FinanceReportPeriod period, LocalDate date, String requestedCurrency,
                                       String search, Pageable page) {
        if (page.getPageSize() > 100 || page.getOffset() > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose at most 100 companies per page.");
        }
        if (search != null && search.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company search must be 200 characters or fewer.");
        }
        LocalDate start = start(period, date);
        return reports.companies(instant(start), instant(period.next(start)), currency(requestedCurrency),
                search == null ? "" : search.trim().toLowerCase(Locale.ROOT), page);
    }

    private LocalDate start(FinanceReportPeriod period, LocalDate date) {
        LocalDate selected = date == null ? LocalDate.now(REPORT_ZONE) : date;
        if (selected.getYear() < 1900 || selected.getYear() > 9998) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a reporting date between 1900 and 9998.");
        }
        return period.start(selected);
    }

    private String currency(String requested) {
        String currency = requested == null || requested.isBlank() ? settings.getSettings().currency() : requested;
        currency = currency.trim().toUpperCase(Locale.ROOT);
        if (!currency.matches("[A-Z]{3}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a three-letter currency code, such as USD.");
        }
        return currency;
    }

    private Instant instant(LocalDate date) { return date.atStartOfDay(REPORT_ZONE).toInstant(); }
    private BigDecimal amount(Object value) { return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()); }
    private long count(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
}
