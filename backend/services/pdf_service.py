import os
import uuid
import logging
from datetime import datetime
from typing import Dict, List

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (
    Spacer, Table,
    TableStyle, Paragraph, HRFlowable
)
from reportlab.platypus import SimpleDocTemplate

from Model.report import AIInsights
from config import get_settings

logger = logging.getLogger(__name__)

# ── Brand colours ────────────────────────────────────────────────────────────
BRAND_BLUE   = colors.HexColor("#1565C0")
BRAND_LIGHT  = colors.HexColor("#E3F2FD")
ACCENT_GREEN = colors.HexColor("#2E7D32")
ACCENT_RED   = colors.HexColor("#C62828")
ACCENT_AMBER = colors.HexColor("#F57F17")
TEXT_DARK    = colors.HexColor("#212121")
TEXT_MUTED   = colors.HexColor("#757575")
DIVIDER      = colors.HexColor("#BDBDBD")
WHITE        = colors.white


def _zmw(amount: float) -> str:
    return f"ZMW {amount:,.2f}"


def _pct(value: float) -> str:
    return f"{value:.1f}%"


class PDFService:

    def __init__(self):
        self.settings = get_settings()
        os.makedirs(self.settings.reports_dir, exist_ok=True)

    # ── Public entry point ───────────────────────────────────────────────────

    def generate(
        self,
        analytics: Dict,
        insights: AIInsights,
        period: str,
        report_type: str,
    ) -> str:
        filename = f"report_{uuid.uuid4().hex[:10]}.pdf"
        filepath = os.path.join(self.settings.reports_dir, filename)

        doc = SimpleDocTemplate(
            filepath,
            pagesize=A4,
            leftMargin=20 * mm,
            rightMargin=20 * mm,
            topMargin=15 * mm,
            bottomMargin=20 * mm,
        )

        styles = self._styles()
        story  = []

        story += self._header(styles, period, report_type)
        story += self._summary_cards(styles, analytics)
        story += self._category_table(styles, analytics)
        story += self._payment_methods_table(styles, analytics)
        story += self._ai_section(styles, insights)
        story += self._footer_note(styles)

        doc.build(story)
        logger.info(f"PDF generated: {filepath}")
        return f"/reports/{filename}"

    # ── Styles ───────────────────────────────────────────────────────────────

    def _styles(self) -> Dict:
        base = getSampleStyleSheet()
        return {
            "title": ParagraphStyle(
                "title", parent=base["Title"],
                fontSize=20, textColor=WHITE,
                alignment=TA_CENTER, spaceAfter=2
            ),
            "subtitle": ParagraphStyle(
                "subtitle", parent=base["Normal"],
                fontSize=10, textColor=colors.HexColor("#BBDEFB"),
                alignment=TA_CENTER, spaceAfter=0
            ),
            "section": ParagraphStyle(
                "section", parent=base["Heading2"],
                fontSize=12, textColor=BRAND_BLUE,
                spaceBefore=10, spaceAfter=4,
                borderPad=2,
            ),
            "body": ParagraphStyle(
                "body", parent=base["Normal"],
                fontSize=9, textColor=TEXT_DARK, spaceAfter=3
            ),
            "muted": ParagraphStyle(
                "muted", parent=base["Normal"],
                fontSize=8, textColor=TEXT_MUTED, spaceAfter=2
            ),
            "bullet": ParagraphStyle(
                "bullet", parent=base["Normal"],
                fontSize=9, textColor=TEXT_DARK,
                leftIndent=12, spaceAfter=3,
                bulletIndent=4,
            ),
            "concern": ParagraphStyle(
                "concern", parent=base["Normal"],
                fontSize=9, textColor=ACCENT_RED, spaceAfter=3,
                leftIndent=6,
            ),
            "card_label": ParagraphStyle(
                "card_label", parent=base["Normal"],
                fontSize=8, textColor=TEXT_MUTED, alignment=TA_CENTER
            ),
            "card_value": ParagraphStyle(
                "card_value", parent=base["Normal"],
                fontSize=14, textColor=TEXT_DARK,
                alignment=TA_CENTER, fontName="Helvetica-Bold"
            ),
            "card_value_green": ParagraphStyle(
                "card_value_green", parent=base["Normal"],
                fontSize=14, textColor=ACCENT_GREEN,
                alignment=TA_CENTER, fontName="Helvetica-Bold"
            ),
            "card_value_red": ParagraphStyle(
                "card_value_red", parent=base["Normal"],
                fontSize=14, textColor=ACCENT_RED,
                alignment=TA_CENTER, fontName="Helvetica-Bold"
            ),
            "footer": ParagraphStyle(
                "footer", parent=base["Normal"],
                fontSize=7, textColor=TEXT_MUTED, alignment=TA_CENTER
            ),
        }

    # ── Header banner ────────────────────────────────────────────────────────

    def _header(self, styles, period: str, report_type: str) -> list:
        now = datetime.now().strftime("%d %B %Y, %H:%M")
        title_text = f"ICTAZ MU Chapter — {report_type.capitalize()} Financial Report"

        header_table = Table(
            [[
                Paragraph(title_text, styles["title"]),
            ], [
                Paragraph(f"Period: {period}  •  Generated: {now}", styles["subtitle"]),
            ]],
            colWidths=["100%"],
        )
        header_table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), BRAND_BLUE),
            ("TOPPADDING",    (0, 0), (-1, -1), 10),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 10),
            ("LEFTPADDING",   (0, 0), (-1, -1), 12),
            ("RIGHTPADDING",  (0, 0), (-1, -1), 12),
            ("ROUNDEDCORNERS", [4]),
        ]))

        return [header_table, Spacer(1, 6 * mm)]

    # ── Summary cards ────────────────────────────────────────────────────────

    def _summary_cards(self, styles, analytics: Dict) -> list:
        net = analytics["net_balance"]
        net_style = styles["card_value_green"] if net >= 0 else styles["card_value_red"]
        net_label = "✓ Surplus" if net >= 0 else "✗ Deficit"

        def card(label: str, value: str, val_style) -> List:
            return [
                Paragraph(value, val_style),
                Paragraph(label, styles["card_label"]),
            ]

        data = [[
            card("Total Income",    _zmw(analytics["total_income"]),    styles["card_value_green"]),
            card("Total Expenses",  _zmw(analytics["total_expenses"]),  styles["card_value_red"]),
            card(net_label,         _zmw(net),                          net_style),
            card("Savings Rate",    _pct(analytics["savings_rate"]),    styles["card_value"]),
        ]]

        tbl = Table(data, colWidths=["25%", "25%", "25%", "25%"])
        tbl.setStyle(TableStyle([
            ("BACKGROUND",    (0, 0), (-1, -1), BRAND_LIGHT),
            ("BOX",           (0, 0), (-1, -1), 0.5, DIVIDER),
            ("INNERGRID",     (0, 0), (-1, -1), 0.5, DIVIDER),
            ("VALIGN",        (0, 0), (-1, -1), "MIDDLE"),
            ("TOPPADDING",    (0, 0), (-1, -1), 8),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
            ("ALIGN",         (0, 0), (-1, -1), "CENTER"),
        ]))

        txn_info = Paragraph(
            f"<font color='#757575'>Approved transactions: "
            f"<b>{analytics['transaction_count']}</b>  •  "
            f"Pending approval: <b>{analytics['pending_count']}</b></font>",
            styles["muted"],
        )

        return [
            Paragraph("Financial Overview", styles["section"]),
            HRFlowable(width="100%", thickness=1, color=BRAND_BLUE, spaceAfter=4),
            tbl,
            Spacer(1, 2 * mm),
            txn_info,
            Spacer(1, 5 * mm),
        ]

    # ── Category breakdown table ─────────────────────────────────────────────

    def _category_table(self, styles, analytics: Dict) -> list:
        income_cats  = analytics.get("income_by_category", {})
        expense_cats = analytics.get("expense_by_category", {})

        all_cats = sorted(
            set(income_cats.keys()) | set(expense_cats.keys())
        )

        if not all_cats:
            return []

        header_row = [
            Paragraph("<b>Category</b>",  styles["body"]),
            Paragraph("<b>Income</b>",    styles["body"]),
            Paragraph("<b>Expenses</b>",  styles["body"]),
            Paragraph("<b>Net</b>",       styles["body"]),
        ]
        rows = [header_row]

        for cat in all_cats:
            inc = income_cats.get(cat, 0.0)
            exp = expense_cats.get(cat, 0.0)
            net = inc - exp
            net_str = _zmw(net)
            net_color = "#2E7D32" if net >= 0 else "#C62828"
            rows.append([
                Paragraph(cat, styles["body"]),
                Paragraph(_zmw(inc) if inc else "—", styles["body"]),
                Paragraph(_zmw(exp) if exp else "—", styles["body"]),
                Paragraph(f'<font color="{net_color}"><b>{net_str}</b></font>', styles["body"]),
            ])

        tbl = Table(rows, colWidths=["40%", "20%", "20%", "20%"])
        tbl.setStyle(TableStyle([
            ("BACKGROUND",    (0, 0), (-1, 0),  BRAND_BLUE),
            ("TEXTCOLOR",     (0, 0), (-1, 0),  WHITE),
            ("ROWBACKGROUNDS",(0, 1), (-1, -1), [WHITE, BRAND_LIGHT]),
            ("BOX",           (0, 0), (-1, -1), 0.5, DIVIDER),
            ("INNERGRID",     (0, 0), (-1, -1), 0.3, DIVIDER),
            ("TOPPADDING",    (0, 0), (-1, -1), 5),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ("LEFTPADDING",   (0, 0), (-1, -1), 6),
        ]))

        return [
            Paragraph("Category Breakdown", styles["section"]),
            HRFlowable(width="100%", thickness=1, color=BRAND_BLUE, spaceAfter=4),
            tbl,
            Spacer(1, 5 * mm),
        ]

    # ── Payment methods table ────────────────────────────────────────────────

    def _payment_methods_table(self, styles, analytics: Dict) -> list:
        methods = analytics.get("payment_methods", {})
        if not methods:
            return []

        total = sum(methods.values())
        rows  = [[
            Paragraph("<b>Payment Method</b>", styles["body"]),
            Paragraph("<b>Transactions</b>",   styles["body"]),
            Paragraph("<b>Share</b>",          styles["body"]),
        ]]
        for method, count in sorted(methods.items(), key=lambda x: x[1], reverse=True):
            pct = (count / total * 100) if total else 0
            rows.append([
                Paragraph(method, styles["body"]),
                Paragraph(str(count), styles["body"]),
                Paragraph(_pct(pct), styles["body"]),
            ])

        tbl = Table(rows, colWidths=["50%", "25%", "25%"])
        tbl.setStyle(TableStyle([
            ("BACKGROUND",    (0, 0), (-1, 0),  BRAND_BLUE),
            ("TEXTCOLOR",     (0, 0), (-1, 0),  WHITE),
            ("ROWBACKGROUNDS",(0, 1), (-1, -1), [WHITE, BRAND_LIGHT]),
            ("BOX",           (0, 0), (-1, -1), 0.5, DIVIDER),
            ("INNERGRID",     (0, 0), (-1, -1), 0.3, DIVIDER),
            ("TOPPADDING",    (0, 0), (-1, -1), 5),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ("LEFTPADDING",   (0, 0), (-1, -1), 6),
        ]))

        return [
            Paragraph("Payment Methods", styles["section"]),
            HRFlowable(width="100%", thickness=1, color=BRAND_BLUE, spaceAfter=4),
            tbl,
            Spacer(1, 5 * mm),
        ]

    # ── AI Insights section ──────────────────────────────────────────────────

    def _ai_section(self, styles, insights: AIInsights) -> list:
        elements = [
            Paragraph("AI-Powered Insights", styles["section"]),
            HRFlowable(width="100%", thickness=1, color=BRAND_BLUE, spaceAfter=4),
            Paragraph(insights.executiveSummary, styles["body"]),
            Spacer(1, 3 * mm),
        ]

        if insights.insights:
            elements.append(Paragraph("<b>Key Insights</b>", styles["body"]))
            for item in insights.insights:
                elements.append(Paragraph(f"• {item}", styles["bullet"]))
            elements.append(Spacer(1, 3 * mm))

        if insights.recommendations:
            elements.append(Paragraph("<b>Recommendations</b>", styles["body"]))
            for i, rec in enumerate(insights.recommendations, 1):
                elements.append(Paragraph(f"{i}. {rec}", styles["bullet"]))
            elements.append(Spacer(1, 3 * mm))

        if insights.concerns and insights.concerns.lower() != "no significant concerns":
            elements.append(Paragraph("<b>⚠ Concerns</b>", styles["body"]))
            elements.append(Paragraph(insights.concerns, styles["concern"]))
        else:
            elements.append(
                Paragraph("✓ No significant financial concerns identified.", styles["body"])
            )

        elements.append(Spacer(1, 5 * mm))
        return elements

    # ── Footer ───────────────────────────────────────────────────────────────

    def _footer_note(self, styles) -> list:
        return [
            HRFlowable(width="100%", thickness=0.5, color=DIVIDER, spaceAfter=3),
            Paragraph(
                "This report was generated automatically by the ICTAZ MU Chapter "
                "Financial Tracker. AI insights are advisory only and should be "
                "reviewed by the Treasurer before acting.",
                styles["footer"],
            ),
        ]