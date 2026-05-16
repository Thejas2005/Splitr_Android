import hashlib
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session
from app.database import SessionLocal
from app import models

router = APIRouter(prefix="/api/auth")


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def hash_password(password: str) -> str:
    """SHA-256 hash — upgrade to bcrypt before going public with real users."""
    return hashlib.sha256(password.encode()).hexdigest()


class AuthRequest(BaseModel):
    username: str
    password: str


@router.post("/register")
def register(req: AuthRequest, db: Session = Depends(get_db)):
    if not req.username.strip() or not req.password:
        raise HTTPException(status_code=400, detail="Username and password required")
    existing = db.query(models.User).filter(
        models.User.username == req.username.strip()
    ).first()
    if existing:
        raise HTTPException(status_code=400, detail="Username already taken")
    user = models.User(
        username=req.username.strip(),
        password=hash_password(req.password)
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return {"message": "User created", "user_id": user.id}


@router.post("/login")
def login(req: AuthRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(
        models.User.username == req.username.strip()
    ).first()
    if not user or user.password != hash_password(req.password):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return {"message": "Login successful", "user_id": user.id, "username": user.username}


@router.get("/users")
def get_all_users(db: Session = Depends(get_db)):
    users = db.query(models.User).all()
    return [{"id": u.id, "username": u.username} for u in users]
