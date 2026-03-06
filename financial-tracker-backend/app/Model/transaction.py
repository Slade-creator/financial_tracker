from typing import Optional

from pydantic import BaseModel


class Transaction(BaseModel):

    id: str
    transactionType: str  # "income" or "expense"
    amount: int  # in ngwee
    memberName: Optional[str] = None
    category: str
    paymentMethod: str
    isApproved: int  # 0 or 1
    transactionDate: str  # ISO date string
    notes: Optional[str] = None
    createdAt: str
    updatedAt: str

    @property
    def amount_kwacha(self) -> float:
        return self.amount / 100.0

    @property
    def is_income(self) -> bool:
        return self.transactionType.lower() == "income"

    @property
    def is_approved(self) -> bool:
        return self.isApproved == 1
