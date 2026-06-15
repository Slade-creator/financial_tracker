from pydantic import BaseModel
from typing import List, Optional
from Model.transaction import Transaction


class ReportRequest(BaseModel):
    period: str
    reportType: str = "monthly"
    transactions: List[Transaction]


class AIInsights(BaseModel):
    executiveSummary: str
    insights: List[str]
    recommendations: List[str]
    concerns: str


class ReportResponse(BaseModel):
    success: bool
    reportUrl: Optional[str] = None
    summary: str
    insights: Optional[AIInsights] = None
    generatedAt: str
