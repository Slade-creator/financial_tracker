from collections import defaultdict

from app.Model.transaction import Transaction
from typing import Dict, List


class AnalyticsService:

    @staticmethod
    def compute(transactions: List[Transaction]) -> Dict:
        approved = [t for t in transactions if t.is_approved]
        income_txns = [t for t in approved if t.is_income]
        expense_txns = [t for t in approved if not t.is_income]

        total_income = sum(t.amount for t in income_txns) / 100.0
        total_expenses = sum(t.amount for t in expense_txns) / 100.0
        net_balance = total_income - total_expenses
        savings_rate = (net_balance / total_income * 100) if total_income > 0 else 0.0

        # Category breakdown
        income_by_category: Dict[str, float] = defaultdict(float)
        expense_by_category: Dict[str, float] = defaultdict(float)
        for t in income_txns:
            income_by_category[t.category] += t.amount / 100.0
        for t in expense_txns:
            expense_by_category[t.category] += t.amount / 100.0

        # Top expense category
        top_expense_cat = (
            max(expense_by_category, key=expense_by_category.get)
            if expense_by_category else "N/A"
        )
        top_expense_amt = expense_by_category.get(top_expense_cat, 0.0)

        # Payment method split
        payment_methods: Dict[str, int] = defaultdict(int)
        for t in approved:
            payment_methods[t.paymentMethod] += 1

        # Pending transactions
        pending = [t for t in transactions if not t.is_approved]

        # Daily spending trend (last 7 days represented)
        daily_expenses: Dict[str, float] = defaultdict(float)
        for t in expense_txns:
            day = t.transactionDate[:10]  # YYYY-MM-DD
            daily_expenses[day] += t.amount / 100.0
        daily_trend = dict(sorted(daily_expenses.items()))

        return {
            "total_income": total_income,
            "total_expenses": total_expenses,
            "net_balance": net_balance,
            "savings_rate": round(savings_rate, 1),
            "transaction_count": len(approved),
            "pending_count": len(pending),
            "income_by_category": dict(income_by_category),
            "expense_by_category": dict(expense_by_category),
            "top_expense_category": top_expense_cat,
            "top_expense_amount": top_expense_amt,
            "payment_methods": dict(payment_methods),
            "daily_trend": daily_trend,
            "is_profitable": net_balance >= 0,
        }

    @staticmethod
    def format_kwacha(amount: float) -> str:
        return f"ZMW {amount:,.2f}"

    @staticmethod
    def build_ai_prompt(analytics: Dict, period: str, report_type: str) -> str:

        inc = AnalyticsService.format_kwacha(analytics["total_income"])
        exp = AnalyticsService.format_kwacha(analytics["total_expenses"])
        net = AnalyticsService.format_kwacha(analytics["net_balance"])
        sr = analytics["savings_rate"]
        top_cat = analytics["top_expense_category"]
        top_amt = AnalyticsService.format_kwacha(analytics["top_expense_amount"])
        pending = analytics["pending_count"]

        expense_breakdown = "\n".join(
            f"  - {cat}: {AnalyticsService.format_kwacha(amt)}"
            for cat, amt in sorted(
                analytics["expense_by_category"].items(),
                key=lambda x: x[1], reverse=True
            )
        ) or "  - No expenses recorded"

        income_breakdown = "\n".join(
            f"  - {cat}: {AnalyticsService.format_kwacha(amt)}"
            for cat, amt in sorted(
                analytics["income_by_category"].items(),
                key=lambda x: x[1], reverse=True
            )
        ) or "  - No income recorded"

        return f"""You are a financial analyst for ICTAZ MU Chapter, a university student association in Zambia.
      Analyze this {report_type} financial report for the period: {period}.
    
        FINANCIAL SUMMARY:
        - Total Income:    {inc}
        - Total Expenses:  {exp}
        - Net Balance:     {net}
        - Savings Rate:    {sr}%
        - Transactions:    {analytics["transaction_count"]} approved, {pending} pending
        - Status:          {"Surplus" if analytics["is_profitable"] else "Deficit"}
    
        INCOME BREAKDOWN:
        {income_breakdown}

        EXPENSE BREAKDOWN:
        {expense_breakdown}

        TOP EXPENSE CATEGORY: {top_cat} ({top_amt})
    
        Respond ONLY with valid JSON, no markdown, no explanation:
        {{
          "executiveSummary": "2-3 sentence overview of financial health",
          "insights": [
            "insight about income sources",
            "insight about spending patterns",
            "insight about savings rate or balance trend"
          ],
          "recommendations": [
            "specific actionable recommendation 1",
            "specific actionable recommendation 2",
            "specific actionable recommendation 3"
          ],
          "concerns": "one sentence about the main financial risk or concern, or 'No significant concerns' if healthy"
        }}"""