import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from dotenv import load_dotenv

load_dotenv()

# Railway injects DATABASE_URL automatically when you add a Postgres plugin.
# Locally it falls back to SQLite so you don't need Postgres installed.
DATABASE_URL = os.environ.get("DATABASE_URL")

if DATABASE_URL:
    # Railway (and most PaaS) still give postgres:// but SQLAlchemy 1.4+ requires postgresql://
    if DATABASE_URL.startswith("postgres://"):
        DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)
    engine = create_engine(DATABASE_URL)
else:
    # Local development fallback — SQLite
    BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    SQLITE_URL = f"sqlite:///{os.path.join(BASE_DIR, 'splitr.db')}"
    engine = create_engine(SQLITE_URL, connect_args={"check_same_thread": False})
    print("⚠️  No DATABASE_URL found — using local SQLite")

SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)
Base = declarative_base()
