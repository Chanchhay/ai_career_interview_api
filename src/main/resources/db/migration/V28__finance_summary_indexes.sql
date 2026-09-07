-- Finance reports filter successful receipts by currency and payment date.
-- These indexes add no financial data and leave earlier migrations untouched.
CREATE INDEX IF NOT EXISTS idx_invoice_payments_reporting
    ON invoice_payments (currency, status, paid_at);
CREATE INDEX IF NOT EXISTS idx_invoices_receivables
    ON invoices (currency, status, due_at);
