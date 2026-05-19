from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session
from app.database import SessionLocal
from app import models
from typing import List, Optional

router = APIRouter(prefix="/api/groups", tags=["Groups"])

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

class CreateGroupRequest(BaseModel):
    name: str
    user_id: int

class AddMemberRequest(BaseModel):
    group_id: int
    user_id: Optional[int] = None
    guest_name: Optional[str] = None

class MemberSpec(BaseModel):
    user_id: Optional[int] = None
    guest_name: Optional[str] = None  # for non-DB members

class FindOrCreateGroupRequest(BaseModel):
    """
    Given a list of members (mix of registered user_ids and guest names),
    find an existing group with exactly those members or create a new one.
    The payer (user_id) is always included automatically.
    """
    payer_id: int
    members: List[MemberSpec]
    group_name: Optional[str] = None  # fallback name if creating new group

@router.post("/create")
def create_group(req: CreateGroupRequest, db: Session = Depends(get_db)):
    group = models.Group(name=req.name)
    db.add(group)
    db.commit()
    db.refresh(group)

    member = models.GroupMember(user_id=req.user_id, group_id=group.id)
    db.add(member)
    db.commit()

    return {"message": "Group created", "group_id": group.id, "name": group.name}

@router.post("/add-member")
def add_member(req: AddMemberRequest, db: Session = Depends(get_db)):
    if not req.user_id and not req.guest_name:
        raise HTTPException(status_code=400, detail="Provide user_id or guest_name")

    # Check not already a member
    if req.user_id:
        existing = db.query(models.GroupMember).filter(
            models.GroupMember.group_id == req.group_id,
            models.GroupMember.user_id == req.user_id
        ).first()
        if existing:
            raise HTTPException(status_code=400, detail="Already a member")
    else:
        existing = db.query(models.GroupMember).filter(
            models.GroupMember.group_id == req.group_id,
            models.GroupMember.guest_name == req.guest_name
        ).first()
        if existing:
            raise HTTPException(status_code=400, detail="Guest already a member")

    member = models.GroupMember(
        user_id=req.user_id,
        group_id=req.group_id,
        guest_name=req.guest_name
    )
    db.add(member)
    db.commit()
    return {"message": "Member added"}

@router.post("/find-or-create")
def find_or_create_group(req: FindOrCreateGroupRequest, db: Session = Depends(get_db)):
    """
    Find an existing group that has exactly the given set of members,
    or create a new group with those members. The payer is always included.
    Returns group_id and name.
    """
    # Build a canonical set of member "keys"
    # registered users: "u:{id}", guests: "g:{name_lower}"
    desired_keys = set()
    desired_keys.add(f"u:{req.payer_id}")
    for m in req.members:
        if m.user_id:
            desired_keys.add(f"u:{m.user_id}")
        elif m.guest_name:
            desired_keys.add(f"g:{m.guest_name.strip().lower()}")

    # Fetch all groups the payer belongs to
    payer_memberships = db.query(models.GroupMember).filter(
        models.GroupMember.user_id == req.payer_id
    ).all()

    for pm in payer_memberships:
        gid = pm.group_id
        all_members = db.query(models.GroupMember).filter(
            models.GroupMember.group_id == gid
        ).all()

        group_keys = set()
        for gm in all_members:
            if gm.user_id:
                group_keys.add(f"u:{gm.user_id}")
            elif gm.guest_name:
                group_keys.add(f"g:{gm.guest_name.strip().lower()}")

        if group_keys == desired_keys:
            # Found an exact match
            group = db.query(models.Group).filter(models.Group.id == gid).first()
            return {"group_id": gid, "name": group.name if group else f"Group {gid}", "created": False}

    # No match found — create new group
    # Auto-generate name from members if not provided
    if not req.group_name:
        names = []
        for m in req.members:
            if m.user_id:
                u = db.query(models.User).filter(models.User.id == m.user_id).first()
                if u:
                    names.append(u.username)
            elif m.guest_name:
                names.append(m.guest_name.strip())
        me = db.query(models.User).filter(models.User.id == req.payer_id).first()
        if me:
            names.insert(0, me.username)
        group_name = " & ".join(names[:3]) + (" +more" if len(names) > 3 else "")
    else:
        group_name = req.group_name

    group = models.Group(name=group_name)
    db.add(group)
    db.commit()
    db.refresh(group)

    # Add payer
    db.add(models.GroupMember(user_id=req.payer_id, group_id=group.id))

    # Add other members
    for m in req.members:
        if m.user_id and m.user_id != req.payer_id:
            db.add(models.GroupMember(user_id=m.user_id, group_id=group.id))
        elif m.guest_name:
            db.add(models.GroupMember(group_id=group.id, guest_name=m.guest_name.strip()))

    db.commit()
    return {"group_id": group.id, "name": group_name, "created": True}

@router.get("/members/{group_id}")
def get_members(group_id: int, db: Session = Depends(get_db)):
    members = db.query(models.GroupMember).filter(
        models.GroupMember.group_id == group_id
    ).all()

    result = []
    for m in members:
        if m.user_id:
            user = db.query(models.User).filter(models.User.id == m.user_id).first()
            result.append({
                "member_id": m.id,
                "user_id": m.user_id,
                "group_id": m.group_id,
                "username": user.username if user else "Unknown",
                "is_guest": False
            })
        else:
            result.append({
                "member_id": m.id,
                "user_id": None,
                "group_id": m.group_id,
                "username": m.guest_name or "Guest",
                "is_guest": True
            })
    return result

@router.get("/user/{user_id}")
def get_user_groups(user_id: int, db: Session = Depends(get_db)):
    memberships = db.query(models.GroupMember).filter(
        models.GroupMember.user_id == user_id
    ).all()

    result = []
    for m in memberships:
        group = db.query(models.Group).filter(models.Group.id == m.group_id).first()
        result.append({
            "group_id": m.group_id,
            "name": group.name if group else f"Group {m.group_id}"
        })
    return result

@router.get("/info/{group_id}")
def get_group_info(group_id: int, db: Session = Depends(get_db)):
    group = db.query(models.Group).filter(models.Group.id == group_id).first()
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")
    return {"id": group.id, "name": group.name}
# =========================
# UPDATE GROUP NAME
# =========================

class UpdateGroupRequest(BaseModel):
    name: str


@router.put("/{group_id}")
def update_group(group_id: int, req: UpdateGroupRequest, db: Session = Depends(get_db)):
    group = db.query(models.Group).filter(
        models.Group.id == group_id
    ).first()

    if not group:
        raise HTTPException(status_code=404, detail="Group not found")

    new_name = req.name.strip()

    if not new_name:
        raise HTTPException(status_code=400, detail="Group name cannot be empty")

    group.name = new_name
    db.commit()
    db.refresh(group)

    return {
        "message": "Group updated successfully",
        "group_id": group.id,
        "name": group.name
    }


# =========================
# REMOVE MEMBER
# =========================

@router.delete("/members/{member_id}")
def remove_member(member_id: int, db: Session = Depends(get_db)):
    member = db.query(models.GroupMember).filter(
        models.GroupMember.id == member_id
    ).first()

    if not member:
        raise HTTPException(status_code=404, detail="Member not found")

    db.delete(member)
    db.commit()

    return {"message": "Member removed successfully"}


# =========================
# DELETE GROUP
# =========================

@router.delete("/{group_id}")
def delete_group(group_id: int, db: Session = Depends(get_db)):
    group = db.query(models.Group).filter(
        models.Group.id == group_id
    ).first()

    if not group:
        raise HTTPException(status_code=404, detail="Group not found")

    # Delete all group members
    db.query(models.GroupMember).filter(
        models.GroupMember.group_id == group_id
    ).delete()

    # Delete all expenses linked to this group
    db.query(models.Expense).filter(
        models.Expense.group_id == group_id
    ).delete()

    # Delete the group itself
    db.delete(group)
    db.commit()

    return {"message": "Group deleted successfully"}
