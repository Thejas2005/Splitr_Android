from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from sqlalchemy.orm import Session
from app.database import SessionLocal
from app import models
from typing import Optional, List

router = APIRouter()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# -------------------
# SCHEMAS
# -------------------
class ExpenseCreate(BaseModel):
    amount: float
    description: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    user_id: int

class MemberSplit(BaseModel):
    user_id: Optional[int] = None
    guest_name: Optional[str] = None
    amount: float  # for dutch: what they owe / for percentage: their %

class GroupExpenseCreate(BaseModel):
    group_id: int
    amount: float
    description: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    user_id: int          # payer
    split_type: str = "equal"  # equal | percentage | dutch
    splits: Optional[List[MemberSplit]] = None


# -------------------
# ADD PERSONAL EXPENSE
# -------------------
@router.post("/api/expenses/add")
def add_expense(exp: ExpenseCreate, db: Session = Depends(get_db)):
    if exp.amount <= 0:
        raise HTTPException(status_code=400, detail="Invalid amount")
    if exp.latitude is not None and not (-90 <= exp.latitude <= 90):
        raise HTTPException(status_code=400, detail="Invalid latitude")
    if exp.longitude is not None and not (-180 <= exp.longitude <= 180):
        raise HTTPException(status_code=400, detail="Invalid longitude")

    new_expense = models.Expense(
        amount=exp.amount,
        description=exp.description,
        latitude=exp.latitude,
        longitude=exp.longitude,
        user_id=exp.user_id
    )
    db.add(new_expense)
    db.commit()
    db.refresh(new_expense)
    return {
        "message": "Expense added",
        "data": {
            "id": new_expense.id,
            "amount": new_expense.amount,
            "description": new_expense.description,
            "latitude": new_expense.latitude,
            "longitude": new_expense.longitude,
            "user_id": new_expense.user_id,
            "group_id": new_expense.group_id,
        }
    }


# -------------------
# ADD GROUP EXPENSE (equal / percentage / dutch)
# -------------------
@router.post("/api/expenses/group/add")
def add_group_expense(req: GroupExpenseCreate, db: Session = Depends(get_db)):
    if req.amount <= 0:
        raise HTTPException(status_code=400, detail="Invalid amount")
    if req.split_type not in ["equal", "percentage", "dutch"]:
        raise HTTPException(status_code=400, detail="Invalid split_type. Use: equal, percentage, dutch")

    # Get group members
    members = db.query(models.GroupMember).filter(
        models.GroupMember.group_id == req.group_id
    ).all()
    if not members:
        raise HTTPException(status_code=400, detail="No members in group")

    # Create expense record
    expense = models.Expense(
        amount=req.amount,
        description=req.description,
        latitude=req.latitude,
        longitude=req.longitude,
        user_id=req.user_id,
        group_id=req.group_id,
        split_type=req.split_type
    )
    db.add(expense)
    db.commit()
    db.refresh(expense)

    # ==================
    # EQUAL SPLIT
    # ==================
    if req.split_type == "equal":
        num_members = len(members)
        split_amount = round(req.amount / num_members, 2)

        for m in members:
            if m.user_id == req.user_id:
                continue  # payer doesn't owe themselves
            split = models.Split(
                expense_id=expense.id,
                user_id=m.user_id,
                guest_name=m.guest_name,
                amount=split_amount,
                status="pending"
            )
            db.add(split)

        db.commit()
        return {
            "message": "Group expense split equally",
            "split_type": "equal",
            "split_per_person": split_amount,
            "expense_id": expense.id
        }

    # ==================
    # PERCENTAGE SPLIT
    # ==================
    elif req.split_type == "percentage":
        if not req.splits:
            raise HTTPException(status_code=400, detail="Provide splits array for percentage split")

        total_pct = sum(s.amount for s in req.splits)
        if abs(total_pct - 100) > 0.01:
            raise HTTPException(status_code=400, detail=f"Percentages must sum to 100. Got {total_pct}")

        for s in req.splits:
            # Skip payer's own share — they don't owe themselves
            if s.user_id and s.user_id == req.user_id:
                continue

            owed = round((s.amount / 100) * req.amount, 2)
            split = models.Split(
                expense_id=expense.id,
                user_id=s.user_id,
                guest_name=s.guest_name,
                amount=owed,
                percentage=s.amount,
                status="pending"
            )
            db.add(split)

        db.commit()
        return {
            "message": "Group expense split by percentage",
            "split_type": "percentage",
            "expense_id": expense.id
        }

    # ==================
    # DUTCH SPLIT
    # Each person has a known amount; payer fronted the total.
    # Everyone except the payer owes their portion back.
    # ==================
    elif req.split_type == "dutch":
        if not req.splits:
            raise HTTPException(status_code=400, detail="Provide splits array for dutch split")

        # Total of all splits must equal the expense total (payer's share included)
        total_dutch = sum(s.amount for s in req.splits)
        if abs(total_dutch - req.amount) > 0.5:
            raise HTTPException(
                status_code=400,
                detail=f"Dutch amounts must sum to total. Got {total_dutch}, expected {req.amount}"
            )

        for s in req.splits:
            # Payer's own share: they already paid it, no split record needed
            if s.user_id and s.user_id == req.user_id:
                continue

            split = models.Split(
                expense_id=expense.id,
                user_id=s.user_id,
                guest_name=s.guest_name,
                amount=round(s.amount, 2),
                status="pending"
            )
            db.add(split)

        db.commit()
        return {
            "message": "Group expense split dutch",
            "split_type": "dutch",
            "expense_id": expense.id
        }


# -------------------
# GET ALL EXPENSES
# -------------------
@router.get("/api/expenses")
def get_expenses(db: Session = Depends(get_db)):
    expenses = db.query(models.Expense).all()
    result = []
    for e in expenses:
        user = db.query(models.User).filter(models.User.id == e.user_id).first()
        result.append({
            "id": e.id,
            "amount": e.amount,
            "description": e.description,
            "latitude": e.latitude,
            "longitude": e.longitude,
            "user_id": e.user_id,
            "group_id": e.group_id,
            "split_type": e.split_type,
            "username": user.username if user else "Unknown"
        })
    return result


# -------------------
# GET USER EXPENSES
# -------------------
@router.get("/api/expenses/user/{user_id}")
def get_user_expenses(user_id: int, db: Session = Depends(get_db)):
    expenses = db.query(models.Expense).filter(models.Expense.user_id == user_id).all()
    result = []
    for e in expenses:
        result.append({
            "id": e.id,
            "amount": e.amount,
            "description": e.description,
            "latitude": e.latitude,
            "longitude": e.longitude,
            "user_id": e.user_id,
            "group_id": e.group_id,
            "split_type": e.split_type,
        })
    return result


# -------------------
# GET MAP LOCATIONS (user-filtered)
# -------------------
@router.get("/api/expenses/locations")
def get_locations(user_id: Optional[int] = None, db: Session = Depends(get_db)):
    q = db.query(models.Expense).filter(
        models.Expense.latitude != None,
        models.Expense.longitude != None
    )
    expenses = q.all()

    result = []
    for e in expenses:
        is_mine = (user_id is not None and e.user_id == user_id)
        # For group expenses: find what this user owes/paid
        my_amount = e.amount if is_mine else 0
        if e.group_id and user_id:
            splits = db.query(models.Split).filter(
                models.Split.expense_id == e.id,
                models.Split.user_id == user_id
            ).all()
            my_amount += sum(s.amount for s in splits)

        result.append({
            "lat": e.latitude,
            "lng": e.longitude,
            "description": e.description,
            "amount": e.amount,
            "my_amount": my_amount,
            "user_id": e.user_id,
            "group_id": e.group_id,
            "is_mine": is_mine,
            "split_type": e.split_type,
        })
    return result


# -------------------
# GET SPLITS
# -------------------
@router.get("/api/expenses/splits")
def get_splits(db: Session = Depends(get_db)):
    return db.query(models.Split).all()


# -------------------
# GET BALANCES FOR GROUP
# -------------------
@router.get("/api/expenses/balances/{group_id}")
def get_balances(group_id: int, db: Session = Depends(get_db)):
    splits = db.query(models.Split).join(models.Expense).filter(
        models.Expense.group_id == group_id,
        models.Split.status == "pending"
    ).all()

    result = []
    for split in splits:
        expense = db.query(models.Expense).filter(models.Expense.id == split.expense_id).first()
        creditor = db.query(models.User).filter(models.User.id == expense.user_id).first()

        if split.user_id:
            debtor = db.query(models.User).filter(models.User.id == split.user_id).first()
            from_name = debtor.username if debtor else f"User {split.user_id}"
        else:
            from_name = split.guest_name or "Guest"

        result.append({
            "split_id": split.id,
            "from_user_id": split.user_id,
            "from_username": from_name,
            "is_guest": split.user_id is None,
            "to_user_id": expense.user_id,
            "to_username": creditor.username if creditor else f"User {expense.user_id}",
            "amount": split.amount,
            "description": expense.description,
            "status": split.status,
            "split_type": expense.split_type,
            "percentage": split.percentage
        })

    return result


# -------------------
# GET MY BALANCES
# -------------------
@router.get("/api/expenses/my-balances/{user_id}")
def get_my_balances(user_id: int, db: Session = Depends(get_db)):
    i_owe = db.query(models.Split).filter(
        models.Split.user_id == user_id,
        models.Split.status == "pending"
    ).all()

    my_expenses = db.query(models.Expense).filter(models.Expense.user_id == user_id).all()
    my_expense_ids = [e.id for e in my_expenses]
    owed_to_me = db.query(models.Split).filter(
        models.Split.expense_id.in_(my_expense_ids),
        models.Split.status == "pending"
    ).all()

    i_owe_result = []
    for split in i_owe:
        expense = db.query(models.Expense).filter(models.Expense.id == split.expense_id).first()
        creditor = db.query(models.User).filter(models.User.id == expense.user_id).first()
        i_owe_result.append({
            "split_id": split.id,
            "to_username": creditor.username if creditor else "Unknown",
            "to_user_id": expense.user_id,
            "amount": split.amount,
            "description": expense.description,
            "group_id": expense.group_id
        })

    owed_result = []
    for split in owed_to_me:
        expense = db.query(models.Expense).filter(models.Expense.id == split.expense_id).first()
        if split.user_id:
            debtor = db.query(models.User).filter(models.User.id == split.user_id).first()
            from_name = debtor.username if debtor else "Unknown"
        else:
            from_name = split.guest_name or "Guest"

        owed_result.append({
            "split_id": split.id,
            "from_username": from_name,
            "from_user_id": split.user_id,
            "is_guest": split.user_id is None,
            "amount": split.amount,
            "description": expense.description,
            "group_id": expense.group_id
        })

    return {
        "i_owe": i_owe_result,
        "owed_to_me": owed_result,
        "total_i_owe": round(sum(s["amount"] for s in i_owe_result), 2),
        "total_owed_to_me": round(sum(s["amount"] for s in owed_result), 2),
        "net": round(sum(s["amount"] for s in owed_result) - sum(s["amount"] for s in i_owe_result), 2)
    }


# -------------------
# SETTLE PAYMENT
# -------------------
@router.post("/api/expenses/settle")
def settle_payment(split_id: int, db: Session = Depends(get_db)):
    split = db.query(models.Split).filter(models.Split.id == split_id).first()
    if not split:
        raise HTTPException(status_code=404, detail="Split not found")
    split.status = "paid"
    db.commit()
    return {"message": "Payment settled", "split_id": split_id}


# -------------------
# DELETE EXPENSE
# -------------------
@router.delete("/api/expenses/{expense_id}")
def delete_expense(expense_id: int, db: Session = Depends(get_db)):
    expense = db.query(models.Expense).filter(models.Expense.id == expense_id).first()
    if not expense:
        raise HTTPException(status_code=404, detail="Expense not found")
    db.query(models.Split).filter(models.Split.expense_id == expense_id).delete()
    db.delete(expense)
    db.commit()
    return {"message": "Expense deleted"}
