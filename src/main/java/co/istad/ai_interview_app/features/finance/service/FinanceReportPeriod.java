package co.istad.ai_interview_app.features.finance.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum FinanceReportPeriod {
    WEEK, MONTH, YEAR;

    public LocalDate start(LocalDate date) {
        return switch (this) {
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    public LocalDate next(LocalDate start) {
        return switch (this) {
            case WEEK -> start.plusWeeks(1);
            case MONTH -> start.plusMonths(1);
            case YEAR -> start.plusYears(1);
        };
    }

    public LocalDate previous(LocalDate start) {
        return switch (this) {
            case WEEK -> start.minusWeeks(1);
            case MONTH -> start.minusMonths(1);
            case YEAR -> start.minusYears(1);
        };
    }
}
