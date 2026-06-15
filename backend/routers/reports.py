import logging
from datetime import datetime, timezone

from fastapi import APIRouter, status, HTTPException
from starlette.responses import FileResponse

from Model.report import ReportResponse, ReportRequest
from services.ai_service import generate_insights
from services.analytics import AnalyticsService
from services.pdf_service import PDFService
from config import get_settings

logger = logging.getLogger(__name__)
router = APIRouter()
pdf_service = PDFService()


@router.post("/api/generate-report", response_model=ReportResponse)
async def generate_report(request: ReportRequest) -> ReportResponse:
    settings = get_settings()

    if not request.transactions:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="No transactions provided")

    if len(request.transactions) > settings.max_transactions_per_request:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Too many transactions. Max allowed:{settings.max_transactions_per_request}",
        )

    logger.info(
        f"Generating {request.reportType} report for period '{request.period}' "
        f"with {len(request.transactions)} transactions"
    )

    try:
        analytics = AnalyticsService.compute(request.transactions)

        prompt = AnalyticsService.build_ai_prompt(analytics, request.period, request.reportType)
        insights = await generate_insights(prompt)

        report_url = pdf_service.generate(
            analytics=analytics,
            insights=insights,
            period=request.period,
            report_type=request.reportType,
        )

        generated_at = datetime.now(timezone.utc).isoformat()

        net = analytics["net_balance"]
        status1 = "surplus" if net >= 0 else "deficit"
        summary = (
            f"{request.reportType.capitalize()} report for {request.period}. "
            f"Income: ZMW {analytics['total_income']:,.2f} | "
            f"Expenses: ZMW {analytics['total_expenses']:,.2f} | "
            f"Net: ZMW {net:,.2f} ({status1}). "
            f"Savings rate: {analytics['savings_rate']}%."
        )

        return ReportResponse(
            success=True,
            reportUrl=report_url,
            summary=summary,
            insights=insights,
            generatedAt=generated_at,
        )


    except RuntimeError as e:
        logger.error(f"Report generation failed: {e}")

        raise HTTPException(status_code=502, detail=str(e))

    except Exception as e:

        logger.exception(f"Unexpected error: {e}")

        raise HTTPException(status_code=500, detail="Internal server error")


@router.get("/api/health")
async def health_check():

    return {"status": "ok", "service": "ICTAZ MU Financial Tracker Backend"}

@router.get("/reports/{filename}")
async def download_report(filename: str):

    settings = get_settings()
    import os
    filepath = os.path.join(settings.reports_dir, filename)

    if not os.path.exists(filepath) or not filepath.endswith(".pdf"):
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Report not found")

    return FileResponse(
        path=filepath,
        media_type="application/pdf",
        filename=filename,
    )