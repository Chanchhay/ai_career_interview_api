# Finance payment summary

The existing finance screen now opens on a Summary tab, with Invoices and Hire
review alongside it. The new read-only API endpoints inherit the existing
FINANCE / SUPER_ADMIN access rule:

- `GET /api/v1/finance/summary`
- `GET /api/v1/finance/summary/companies`

Both accept `period=WEEK|MONTH|YEAR` (default MONTH), `date=YYYY-MM-DD` (any date
within the desired calendar period; defaults to today), and a three-letter
`currency` (defaults to the existing finance settings). Weeks start on Monday.
All date boundaries use Asia/Phnom_Penh. The lower boundary is inclusive and the
upper boundary exclusive; payment timestamps remain UTC instants in storage.

## What the numbers mean

Receipts are sums of PAID payment records by `InvoicePayment.paidAt`, including
partial payments. They exclude pending, failed, refunded and cancelled payment
records, payments without a payment date, and draft/cancelled invoices. An
invoice's paid status or cached paid amount alone never manufactures a receipt.
Each report covers one currency; no conversion or cross-currency total is made.

The summary includes received amount, previous-period received amount, payment
count, distinct paying-company count, distinct invoices receiving payments,
available invoice currencies, and a zero-filled payment trend. Weekly/monthly
trends use daily buckets; yearly trends use monthly buckets. Previous-period
figures cover the full previous calendar period. A current period may still be
in progress. Dates entered on payment records, including backdated payments,
determine where the money appears.

Outstanding and overdue amounts are CURRENT balances across all dates, not
historical snapshots of the selected period. They use open invoices in ISSUED,
PARTIALLY_PAID or OVERDUE status and positive `totalAmount - paidAmount`, matching
the existing maintained invoice balance. An overdue balance is included in the
outstanding balance and is identified by its actual due date, even before the
scheduled status update runs. Drafts and cancelled invoices are excluded.

## Companies and invoice drill-down

The companies endpoint also accepts `search`, `page` and `size` (maximum 100).
It returns the existing Spring paged-response shape. Aggregation happens BEFORE
pagination, with a server-side name search and a stable order: received amount
descending, then company name and ID. Each row includes its currency, amount,
payment count, distinct invoice count and latest payment timestamp.

Company search filters only that table; the summary totals still describe the
whole selected period. Opening a company's invoices shows all of its invoices,
not just those with payments in the report period. Invoice filtering now honours
both company and status together. The UI also exposes Partially paid and Overdue
invoice tabs.

## Storage and verification

No new financial tables or backfill are needed. V28 adds indexes for payment
reporting and invoice receivables through the existing Flyway startup workflow.
Deploy/start the updated backend before the frontend calls the new endpoints.

All aggregates execute in the database. The trend uses one bounded grouping
query with time-zone boundaries computed in Java, so it behaves the same on H2
and PostgreSQL and does not depend on the database server's time zone. A summary
is read in a repeatable-read transaction.

`FinanceSummaryIntegrationTest` covers calendar boundaries, partial payments,
payment statuses, multiple currencies, leap years, current receivables,
aggregation before pagination, search, invoice drill-down and access validation.

Run normally with `./gradlew test`. To run just the report tests against a
DISPOSABLE PostgreSQL database, set `FINANCE_TEST_POSTGRES_URL` and
`FINANCE_TEST_POSTGRES_USER`, then run:

```
./gradlew test --tests '*FinanceSummaryIntegrationTest' --rerun-tasks
```

The test profile uses create-drop and must never be pointed at an application
database.
