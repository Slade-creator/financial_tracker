"""Unit tests for AnalyticsService.compute (services/analytics.py).

Amounts are stored in ngwee (1 ZMW = 100 ngwee); the service divides by 100
to produce kwacha floats. No HTTP layer involved — plain pytest.
"""

import pytest

from Model.transaction import Transaction
from services.analytics import AnalyticsService


def make_txn(
    id="t1",
    transactionType="expense",
    amount=1000,
    category="Stationery",
    paymentMethod="CASH",
    isApproved=1,
    transactionDate="2026-03-15T10:00:00Z",
):
    return Transaction(
        id=id,
        transactionType=transactionType,
        amount=amount,
        category=category,
        paymentMethod=paymentMethod,
        isApproved=isApproved,
        transactionDate=transactionDate,
        createdAt="2026-03-15T09:00:00Z",
        updatedAt="2026-03-15T09:00:00Z",
    )


@pytest.fixture
def mixed_transactions():
    """2 approved income, 2 approved expenses, 1 pending expense."""
    return [
        make_txn("i1", "income", 50000, "Member Fees", "CASH", 1,
                 "2026-03-01T08:00:00Z"),
        make_txn("i2", "income", 25000, "Donations", "MOBILE_MONEY", 1,
                 "2026-03-05T08:00:00Z"),
        make_txn("e1", "expense", 20000, "Stationery", "CASH", 1,
                 "2026-03-10T08:00:00Z"),
        make_txn("e2", "expense", 5000, "Transport", "MOBILE_MONEY", 1,
                 "2026-03-10T09:00:00Z"),
        make_txn("p1", "expense", 10000, "Equipment", "CASH", 0,
                 "2026-03-12T09:00:00Z"),
    ]


class TestComputeTotals:
    def test_totals_and_counts(self, mixed_transactions):
        result = AnalyticsService.compute(mixed_transactions)

        assert result["total_income"] == pytest.approx(750.0)   # 75000 ngwee
        assert result["total_expenses"] == pytest.approx(250.0)  # 25000 ngwee
        assert result["net_balance"] == pytest.approx(500.0)
        assert result["transaction_count"] == 4  # pending excluded
        assert result["pending_count"] == 1

    def test_savings_rate_rounded_to_one_decimal(self, mixed_transactions):
        result = AnalyticsService.compute(mixed_transactions)
        # 500 / 750 * 100 = 66.666... -> 66.7
        assert result["savings_rate"] == 66.7

    def test_pending_transactions_excluded_from_totals(self, mixed_transactions):
        result = AnalyticsService.compute(mixed_transactions)
        # The pending 100 ngwee Equipment expense must not appear anywhere
        assert "Equipment" not in result["expense_by_category"]
        assert result["total_expenses"] == pytest.approx(250.0)

    def test_is_profitable_positive(self, mixed_transactions):
        assert AnalyticsService.compute(mixed_transactions)["is_profitable"] is True

    def test_is_profitable_deficit(self):
        txns = [
            make_txn("i1", "income", 10000, "Fees", "CASH", 1),
            make_txn("e1", "expense", 20000, "Rent", "CASH", 1),
        ]
        result = AnalyticsService.compute(txns)
        assert result["net_balance"] == pytest.approx(-100.0)
        assert result["is_profitable"] is False

    def test_savings_rate_zero_when_no_income(self):
        txns = [make_txn("e1", "expense", 20000, "Rent", "CASH", 1)]
        result = AnalyticsService.compute(txns)
        assert result["savings_rate"] == 0.0
        assert result["total_income"] == 0.0


class TestCategoryBreakdown:
    def test_income_by_category(self, mixed_transactions):
        result = AnalyticsService.compute(mixed_transactions)
        assert result["income_by_category"] == {
            "Member Fees": 500.0,
            "Donations": 250.0,
        }

    def test_expense_by_category(self, mixed_transactions):
        result = AnalyticsService.compute(mixed_transactions)
        assert result["expense_by_category"] == {
            "Stationery": 200.0,
            "Transport": 50.0,
        }

    def test_top_expense_category(self, mixed_transactions):
        result = AnalyticsService.compute(mixed_transactions)
        assert result["top_expense_category"] == "Stationery"
        assert result["top_expense_amount"] == pytest.approx(200.0)

    def test_top_expense_category_when_no_expenses(self):
        txns = [make_txn("i1", "income", 50000, "Fees", "CASH", 1)]
        result = AnalyticsService.compute(txns)
        assert result["top_expense_category"] == "N/A"
        assert result["top_expense_amount"] == 0.0


class TestOtherOutputs:
    def test_payment_method_counts_only_approved(self, mixed_transactions):
        result = AnalyticsService.compute(mixed_transactions)
        # Pending CASH transaction must not be counted
        assert result["payment_methods"] == {"CASH": 2, "MOBILE_MONEY": 2}

    def test_daily_trend_groups_by_day_sorted(self):
        txns = [
            make_txn("e1", "expense", 2000, "Transport", "CASH", 1,
                     "2026-03-02T08:00:00Z"),
            make_txn("e2", "expense", 3000, "Transport", "CASH", 1,
                     "2026-03-01T08:00:00Z"),
            make_txn("e3", "expense", 1000, "Stationery", "CASH", 1,
                     "2026-03-01T12:00:00Z"),
        ]
        result = AnalyticsService.compute(txns)
        assert result["daily_trend"] == {
            "2026-03-01": 40.0,   # 3000 + 1000 ngwee
            "2026-03-02": 20.0,
        }
        assert list(result["daily_trend"]) == ["2026-03-01", "2026-03-02"]

    def test_empty_transaction_list(self):
        result = AnalyticsService.compute([])
        assert result["total_income"] == 0.0
        assert result["total_expenses"] == 0.0
        assert result["net_balance"] == 0.0
        assert result["savings_rate"] == 0.0
        assert result["transaction_count"] == 0
        assert result["pending_count"] == 0
        assert result["income_by_category"] == {}
        assert result["expense_by_category"] == {}
        assert result["top_expense_category"] == "N/A"
        assert result["payment_methods"] == {}
        assert result["daily_trend"] == {}
        assert result["is_profitable"] is True  # 0 >= 0


class TestHelpers:
    def test_format_kwacha(self):
        assert AnalyticsService.format_kwacha(1234.5) == "ZMW 1,234.50"

    def test_build_ai_prompt_contains_summary(self, mixed_transactions):
        analytics = AnalyticsService.compute(mixed_transactions)
        prompt = AnalyticsService.build_ai_prompt(analytics, "March 2026", "monthly")

        assert "March 2026" in prompt
        assert "ZMW 750.00" in prompt       # total income
        assert "ZMW 250.00" in prompt       # total expenses
        assert "66.7%" in prompt            # savings rate
        assert "Surplus" in prompt
        assert "Stationery" in prompt       # top expense category
        assert "4 approved, 1 pending" in prompt

    def test_build_ai_prompt_deficit_status(self):
        txns = [
            make_txn("i1", "income", 10000, "Fees", "CASH", 1),
            make_txn("e1", "expense", 20000, "Rent", "CASH", 1),
        ]
        analytics = AnalyticsService.compute(txns)
        prompt = AnalyticsService.build_ai_prompt(analytics, "Q1", "quarterly")
        assert "Deficit" in prompt
