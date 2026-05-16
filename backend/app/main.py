import os
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from dotenv import load_dotenv

from app.database import engine
from app import models
from app.routes import expenses, auth, groups

load_dotenv()

app = FastAPI(title="Splitr API", version="1.0.0")

# ─── CORS ─────────────────────────────────────────────────────────────────────
# ALLOWED_ORIGINS is a comma-separated env var you set on Railway.
# Example: https://yourapp.vercel.app,https://yourapp.netlify.app
# Falls back to localhost for local development.

_raw_origins = os.environ.get("ALLOWED_ORIGINS", "")
ALLOWED_ORIGINS = [o.strip() for o in _raw_origins.split(",") if o.strip()] or [
    "http://127.0.0.1:5500",
    "http://localhost:5500",
    "http://127.0.0.1:5501",
    "http://localhost:5501",
    "http://127.0.0.1:3000",
    "http://localhost:3000",
    "http://127.0.0.1:8000",
    "http://localhost:8000",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"],
    allow_headers=["*"],
    expose_headers=["*"],
)


def cors_headers(request: Request) -> dict:
    origin = request.headers.get("origin", "")
    allow_origin = origin if origin in ALLOWED_ORIGINS else (ALLOWED_ORIGINS[0] if ALLOWED_ORIGINS else "*")
    return {
        "Access-Control-Allow-Origin": allow_origin,
        "Access-Control-Allow-Credentials": "true",
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS, PATCH",
        "Access-Control-Allow-Headers": "*",
    }


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={"detail": f"Internal server error: {str(exc)}"},
        headers=cors_headers(request),
    )


# ─── DB setup ─────────────────────────────────────────────────────────────────
models.Base.metadata.create_all(bind=engine)

# ─── Routes ───────────────────────────────────────────────────────────────────
app.include_router(expenses.router)
app.include_router(auth.router)
app.include_router(groups.router)


# ─── Health check ─────────────────────────────────────────────────────────────
@app.get("/api")
def health():
    return {"status": "ok", "message": "Splitr API running"}


@app.get("/")
def root():
    return {"status": "ok"}
