package co.istad.ai_interview_app.features.finance;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.finance.entity.*;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:finance_summary;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FinanceSummaryIntegrationTest {
    /** Optional real-PostgreSQL run against an explicitly supplied disposable DB. */
    @DynamicPropertySource
    static void reportingDatabase(DynamicPropertyRegistry registry) {
        String url = System.getenv("FINANCE_TEST_POSTGRES_URL");
        if (url != null && !url.isBlank()) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.datasource.username", () -> System.getenv("FINANCE_TEST_POSTGRES_USER"));
            registry.add("spring.datasource.password", () -> "");
        }
    }

    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    private FinanceProfile finance;
    private Company alpha;
    private Company beta;

    @BeforeEach
    void seed() {
        UserAccount staff = user();
        finance = new FinanceProfile(); finance.setUserAccount(staff); em.persist(finance);
        alpha = company("Alpha"); beta = company("Beta");
        Invoice first = invoice(alpha, "USD", InvoiceStatus.PAID, "120", "120", null);
        payment(first, "20", PaymentStatus.PAID, "2026-07-31T16:59:59Z");
        payment(first, "30", PaymentStatus.PAID, "2026-07-31T17:00:00Z");
        payment(first, "70", PaymentStatus.PAID, "2026-08-10T03:00:00Z");
        Invoice partial = invoice(beta, "USD", InvoiceStatus.PARTIALLY_PAID, "100", "50", "2000-01-01T00:00:00Z");
        payment(partial, "50", PaymentStatus.PAID, "2026-08-20T03:00:00Z");
        Invoice future = invoice(beta, "USD", InvoiceStatus.PAID, "500", "500", null);
        payment(future, "500", PaymentStatus.PAID, "2026-08-31T17:00:00Z");
        for (PaymentStatus status : new PaymentStatus[]{PaymentStatus.FAILED, PaymentStatus.PENDING, PaymentStatus.REFUNDED, PaymentStatus.CANCELLED}) {
            payment(first, "900", status, "2026-08-15T03:00:00Z");
        }
        payment(first, "100", PaymentStatus.PAID, null);
        payment(invoice(alpha, "USD", InvoiceStatus.CANCELLED, "600", "0", "2000-01-01T00:00:00Z"), "600", PaymentStatus.PAID, "2026-08-15T03:00:00Z");
        payment(invoice(alpha, "USD", InvoiceStatus.DRAFT, "700", "0", null), "700", PaymentStatus.PAID, "2026-08-15T03:00:00Z");
        payment(invoice(alpha, "KHR", InvoiceStatus.PAID, "100000", "100000", null), "100000", PaymentStatus.PAID, "2026-08-15T03:00:00Z");
        invoice(alpha, "USD", InvoiceStatus.ISSUED, "200", "0", "2999-01-01T00:00:00Z");
        invoice(alpha, "KHR", InvoiceStatus.ISSUED, "500000", "0", "2000-01-01T00:00:00Z");
        em.flush();
    }

    @Test
    void monthUsesPaymentDatesAndDoesNotMixCurrenciesOrPaymentStates() throws Exception {
        mvc.perform(get("/api/v1/finance/summary").with(financeJwt()).param("period", "MONTH").param("date", "2026-08-15").param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startDate").value("2026-08-01"))
                .andExpect(jsonPath("$.data.endDateExclusive").value("2026-09-01"))
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Phnom_Penh"))
                .andExpect(jsonPath("$.data.receivedAmount").value(150))
                .andExpect(jsonPath("$.data.previousReceivedAmount").value(20))
                .andExpect(jsonPath("$.data.paymentCount").value(3))
                .andExpect(jsonPath("$.data.payingCompanyCount").value(2))
                .andExpect(jsonPath("$.data.invoiceCount").value(2))
                .andExpect(jsonPath("$.data.outstandingAmount").value(250))
                .andExpect(jsonPath("$.data.overdueAmount").value(50))
                .andExpect(jsonPath("$.data.outstandingInvoiceCount").value(2))
                .andExpect(jsonPath("$.data.overdueInvoiceCount").value(1))
                .andExpect(jsonPath("$.data.trend.length()").value(31))
                .andExpect(jsonPath("$.data.trend[0].amount").value(30))
                .andExpect(jsonPath("$.data.trend[1].amount").value(0));
    }

    @Test
    void weekStartsOnMondayAndYearUsesTwelveMonthBuckets() throws Exception {
        mvc.perform(get("/api/v1/finance/summary").with(financeJwt()).param("period", "WEEK").param("date", "2026-08-02").param("currency", "USD"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.startDate").value("2026-07-27"))
                .andExpect(jsonPath("$.data.endDateExclusive").value("2026-08-03"))
                .andExpect(jsonPath("$.data.receivedAmount").value(50)).andExpect(jsonPath("$.data.trend.length()").value(7));
        mvc.perform(get("/api/v1/finance/summary").with(financeJwt()).param("period", "YEAR").param("date", "2026-08-15").param("currency", "USD"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.receivedAmount").value(670))
                .andExpect(jsonPath("$.data.trend.length()").value(12))
                .andExpect(jsonPath("$.data.trend[6].amount").value(20))
                .andExpect(jsonPath("$.data.trend[7].amount").value(150))
                .andExpect(jsonPath("$.data.trend[8].amount").value(500));
    }

    @Test
    void companiesAreAggregatedBeforePagingAndSearchCoversEveryCompany() throws Exception {
        mvc.perform(get("/api/v1/finance/summary/companies").with(financeJwt()).param("date", "2026-08-15").param("currency", "USD").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.page.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].companyName").value("Alpha"))
                .andExpect(jsonPath("$.data.content[0].receivedAmount").value(100))
                .andExpect(jsonPath("$.data.content[0].currency").value("USD"))
                .andExpect(jsonPath("$.data.content[0].paymentCount").value(2))
                .andExpect(jsonPath("$.data.content[0].invoiceCount").value(1));
        mvc.perform(get("/api/v1/finance/summary/companies").with(financeJwt()).param("date", "2026-08-15").param("currency", "USD").param("search", "bEtA").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].companyName").value("Beta"));
    }

    @Test
    void currencyAndEmptyPeriodsHaveHonestTotals() throws Exception {
        mvc.perform(get("/api/v1/finance/summary").with(financeJwt()).param("date", "2026-08-15").param("currency", "khr"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.currency").value("KHR"))
                .andExpect(jsonPath("$.data.receivedAmount").value(100000));
        mvc.perform(get("/api/v1/finance/summary").with(financeJwt()).param("date", "2024-02-15").param("currency", "USD"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.receivedAmount").value(0))
                .andExpect(jsonPath("$.data.trend.length()").value(29));
    }

    @Test
    void invoiceDrilldownKeepsCompanyAndStatusFilters() throws Exception {
        mvc.perform(get("/api/v1/finance/invoices").with(financeJwt()).param("companyId", beta.getId().toString()).param("status", "PARTIALLY_PAID"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("PARTIALLY_PAID"));
    }

    @Test
    void reportAccessAndParametersAreValidated() throws Exception {
        mvc.perform(get("/api/v1/finance/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/finance/summary").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECRUITER"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/finance/summary").with(financeJwt()).param("period", "INVALID")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/finance/summary").with(financeJwt()).param("date", "2026-02-30")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/finance/summary/companies").with(financeJwt()).param("size", "101")).andExpect(status().isBadRequest());
    }

    private RequestPostProcessor financeJwt() { return jwt().jwt(token -> token.subject("finance-test")).authorities(new SimpleGrantedAuthority("ROLE_FINANCE")); }
    private UserAccount user() { UserAccount user = new UserAccount(); user.setKeycloakUserId(UUID.randomUUID().toString()); em.persist(user); return user; }
    private Company company(String name) {
        RecruiterProfile recruiter = new RecruiterProfile(); recruiter.setUserAccount(user()); em.persist(recruiter);
        Company company = new Company(); company.setName(name); company.setRecruiterProfile(recruiter); em.persist(company); return company;
    }
    private Invoice invoice(Company company, String currency, InvoiceStatus status, String amount, String paid, String due) {
        Invoice invoice = new Invoice(); invoice.setCompany(company); invoice.setFinanceProfile(finance); invoice.setInvoiceNo(UUID.randomUUID().toString());
        invoice.setCurrency(currency); invoice.setStatus(status); invoice.setTotalAmount(new BigDecimal(amount)); invoice.setPaidAmount(new BigDecimal(paid));
        invoice.setDueAt(due == null ? null : Instant.parse(due)); em.persist(invoice); return invoice;
    }
    private void payment(Invoice invoice, String amount, PaymentStatus status, String paidAt) {
        InvoicePayment payment = new InvoicePayment(); payment.setInvoice(invoice); payment.setCurrency(invoice.getCurrency());
        payment.setAmount(new BigDecimal(amount)); payment.setStatus(status); payment.setPaidAt(paidAt == null ? null : Instant.parse(paidAt)); em.persist(payment);
    }
}
