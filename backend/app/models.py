from sqlalchemy import Column, Integer, String, Float, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base


class User(Base):
    __tablename__ = "users"
    id       = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, nullable=False)
    password = Column(String, nullable=False)


class Group(Base):
    __tablename__ = "groups"
    id   = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)


class GroupMember(Base):
    __tablename__ = "group_members"
    id         = Column(Integer, primary_key=True, index=True)
    user_id    = Column(Integer, ForeignKey("users.id"), nullable=True)   # null for guests
    group_id   = Column(Integer, ForeignKey("groups.id"), nullable=False)
    guest_name = Column(String, nullable=True)


class Expense(Base):
    __tablename__ = "expenses"
    id          = Column(Integer, primary_key=True, index=True)
    amount      = Column(Float, nullable=False)
    description = Column(String, nullable=False)
    latitude    = Column(Float, nullable=True)
    longitude   = Column(Float, nullable=True)
    user_id     = Column(Integer, ForeignKey("users.id"), nullable=False)
    group_id    = Column(Integer, ForeignKey("groups.id"), nullable=True)
    split_type  = Column(String, default="equal")  # equal | percentage | dutch


class Split(Base):
    __tablename__ = "splits"
    id         = Column(Integer, primary_key=True, index=True)
    expense_id = Column(Integer, ForeignKey("expenses.id"), nullable=False)
    user_id    = Column(Integer, ForeignKey("users.id"), nullable=True)
    guest_name = Column(String, nullable=True)
    amount     = Column(Float, nullable=False)
    percentage = Column(Float, nullable=True)
    status     = Column(String, default="pending")  # pending | paid
