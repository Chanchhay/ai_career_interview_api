package co.istad.ai_interview_app.features.finance;

import co.istad.ai_interview_app.features.application.entity.JobApplication;
import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.finance.entity.CommissionRecord;
import co.istad.ai_interview_app.features.finance.entity.FinanceProfile;
import co.istad.ai_interview_app.features.finance.entity.HiringRecord;
import co.istad.ai_interview_app.features.finance.entity.Invoice;
import co.istad.ai_interview_app.features.finance.entity.InvoiceItem;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.seeker.entity.JobSeekerProfile;
import co.istad.ai_interview_app.shared.enums.finance.HiringRecordStatus;
import co.istad.ai_interview_app.shared.enums.finance.InvoiceStatus;
import co.istad.ai_interview_app.shared.enums.finance.PaymentStatus;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The finance desk's "who can I bill" list.
 *
 * <p>The aggregate behind it groups and sums in the database, so the numbers it
 * reports are worth checking against real rows rather than trusting the query
 * to say what it reads like it says.
 *
 * <p>Assertions select their own company by id: this suite shares one database,
 * and any other test that confirms a hire would otherwise change the length of
 * this list.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillableCompaniesIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /*
     * Built here rather than injected: reading three fields out of a response
     * body needs no application configuration, and Boot 4 does not expose a
     * databind ObjectMapper bean to autowire.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void unbilledCommissionsAreGroupedPerCompanyAndSummed() throws Exception {
        Instant older = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant newer = Instant.now().plus(10, ChronoUnit.DAYS);

        Long companyId = transactionTemplate.execute(status -> {
            Company company = seedCompany("Billable Co");
            commission(company, new BigDecimal("100.00"), newer, false);
            commission(company, new BigDecimal("250.00"), older, false);
            return company.getId();
        });

        JsonNode row = billableRow(companyId);

        assertThat(row).isNotNull();
        assertThat(row.get("commissionCount").asInt()).isEqualTo(2);
        assertThat(row.get("totalAmount").decimalValue()).isEqualByComparingTo("350.00");
        assertThat(row.get("currency").asText()).isEqualTo("USD");
        // The oldest of the two due dates, not the newest and not the sum.
        assertThat(Instant.parse(row.get("oldestDueAt").asText()))
                .isCloseTo(older, within(1, ChronoUnit.SECONDS));
    }

    @Test
    void aCommissionAlreadyOnAnInvoiceLeavesTheList() throws Exception {
        Long companyId = transactionTemplate.execute(status -> {
            Company company = seedCompany("Already Billed Co");
            commission(company, new BigDecimal("400.00"), Instant.now(), true);
            return company.getId();
        });

        assertThat(billableRow(companyId)).isNull();
    }

    /**
     * A cancelled invoice returns its commissions to the pool, so the company it
     * was drawn against becomes billable again.
     */
    @Test
    void aCancelledInvoiceReturnsItsCompanyToTheList() throws Exception {
        Long companyId = transactionTemplate.execute(status -> {
            Company company = seedCompany("Cancelled Invoice Co");
            InvoiceItem item = commission(company, new BigDecimal("75.00"), Instant.now(), true);
            item.getInvoice().setStatus(InvoiceStatus.CANCELLED);
            return company.getId();
        });

        JsonNode row = billableRow(companyId);

        assertThat(row).isNotNull();
        assertThat(row.get("totalAmount").decimalValue()).isEqualByComparingTo("75.00");
    }

    /**
     * This company's row in the billable list, or null if it has none.
     *
     * <p>Reading the row out rather than asserting on a position: the suite
     * shares one database, so anything else that confirms a hire lands in the
     * same list.
     */
    private JsonNode billableRow(Long companyId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/finance/billable-companies").with(financeJwt()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (JsonNode row : objectMapper.readTree(body).get("data")) {
            if (row.get("companyId").asLong() == companyId) return row;
        }

        return null;
    }

    /* ------------------------------------------------------------ seed --- */

    private Company seedCompany(String name) {
        int suffix = SEQUENCE.incrementAndGet();

        UserAccount recruiterUser = new UserAccount();
        recruiterUser.setKeycloakUserId("billable-recruiter-" + suffix);
        entityManager.persist(recruiterUser);

        RecruiterProfile recruiterProfile = new RecruiterProfile();
        recruiterProfile.setUserAccount(recruiterUser);
        entityManager.persist(recruiterProfile);

        Company company = new Company();
        company.setRecruiterProfile(recruiterProfile);
        company.setName(name + " " + suffix);
        entityManager.persist(company);

        return company;
    }

    /**
     * One confirmed hire and the commission it produced, optionally already
     * picked up by a draft invoice.
     *
     * @return the invoice item, so a caller can reach the invoice it sits on
     */
    private InvoiceItem commission(Company company, BigDecimal amount, Instant dueAt, boolean billed) {
        int suffix = SEQUENCE.incrementAndGet();

        UserAccount seekerUser = new UserAccount();
        seekerUser.setKeycloakUserId("billable-seeker-" + suffix);
        entityManager.persist(seekerUser);

        JobSeekerProfile profile = new JobSeekerProfile();
        profile.setUserAccount(seekerUser);
        profile.setHeadline("Hired Developer");
        entityManager.persist(profile);

        JobPost jobPost = new JobPost();
        jobPost.setCompany(company);
        jobPost.setRecruiterProfile(company.getRecruiterProfile());
        jobPost.setTitle("Billable Job " + suffix);
        jobPost.setDescription("Billable job description");
        jobPost.setStatus(JobStatus.PUBLISHED);
        jobPost.setPublishedAt(Instant.now());
        entityManager.persist(jobPost);

        JobApplication application = new JobApplication();
        application.setJobPost(jobPost);
        application.setJobSeekerProfile(profile);
        entityManager.persist(application);

        HiringRecord hire = new HiringRecord();
        hire.setApplication(application);
        hire.setJobPost(jobPost);
        hire.setCompany(company);
        hire.setJobSeekerProfile(profile);
        hire.setStatus(HiringRecordStatus.CONFIRMED);
        entityManager.persist(hire);

        CommissionRecord commission = new CommissionRecord();
        commission.setHiringRecord(hire);
        commission.setCompany(company);
        commission.setCommissionRate(new BigDecimal("10.00"));
        commission.setCommissionAmount(amount);
        commission.setDueAt(dueAt);
        commission.setStatus(PaymentStatus.PENDING);
        entityManager.persist(commission);

        if (!billed) return null;

        UserAccount financeUser = new UserAccount();
        financeUser.setKeycloakUserId("billable-finance-" + suffix);
        entityManager.persist(financeUser);

        FinanceProfile financeProfile = new FinanceProfile();
        financeProfile.setUserAccount(financeUser);
        entityManager.persist(financeProfile);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setFinanceProfile(financeProfile);
        invoice.setInvoiceNo("INV-TEST-" + suffix);
        invoice.setSubtotalAmount(amount);
        invoice.setTotalAmount(amount);
        invoice.setStatus(InvoiceStatus.DRAFT);
        entityManager.persist(invoice);

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setCommissionRecord(commission);
        item.setDescription("Placement commission");
        item.setUnitAmount(amount);
        item.setTotalAmount(amount);
        entityManager.persist(item);

        return item;
    }

    private static RequestPostProcessor financeJwt() {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("billable-finance-caller")
                        .claim("realm_access", Map.of("roles", List.of("FINANCE"))))
                .authorities(new SimpleGrantedAuthority("ROLE_FINANCE"));
    }
}
