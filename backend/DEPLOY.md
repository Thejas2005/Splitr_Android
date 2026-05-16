# Deploying Splitr Backend to Railway

## File structure in your repo

```
Splitr_Android/              ← your GitHub repo root
├── android/                 ← Android Studio project
├── frontend/                ← HTML files
└── backend/                 ← THIS folder (FastAPI)
    ├── app/
    │   ├── __init__.py
    │   ├── main.py
    │   ├── database.py
    │   ├── models.py
    │   └── routes/
    │       ├── __init__.py
    │       ├── auth.py
    │       ├── expenses.py
    │       └── groups.py
    ├── requirements.txt
    ├── Procfile
    ├── runtime.txt
    ├── railway.json
    └── .env.example
```

---

## Step 1 — Push backend to GitHub

```bash
# From your repo root
git add backend/
git commit -m "add: FastAPI backend ready for Railway"
git push
```

---

## Step 2 — Create a Railway account

Go to https://railway.app and sign up with your GitHub account.

---

## Step 3 — Create a new project on Railway

1. Click **New Project**
2. Choose **Deploy from GitHub repo**
3. Select your **Splitr_Android** repo
4. Railway will detect it — click **Add service**

---

## Step 4 — Set the root directory to backend/

This is critical. Railway will try to deploy the whole repo otherwise.

1. Click your service → **Settings** tab
2. Find **Root Directory**
3. Set it to: `backend`
4. Click **Save** — Railway will redeploy

---

## Step 5 — Add a PostgreSQL database

1. In your Railway project, click **+ New** → **Database** → **PostgreSQL**
2. Railway creates the DB and **automatically** sets `DATABASE_URL` in your backend service
3. You don't need to copy anything — it's injected automatically

---

## Step 6 — Set environment variables

In your backend service → **Variables** tab, add:

| Variable | Value |
|----------|-------|
| `ALLOWED_ORIGINS` | (leave blank for now — add your frontend URL later) |

Railway already sets `DATABASE_URL`, `PORT` automatically. You don't touch those.

---

## Step 7 — Get your public URL

1. Go to your backend service → **Settings** tab
2. Under **Networking** → click **Generate Domain**
3. You'll get a URL like: `https://splitr-backend-production.up.railway.app`

**Test it:** open `https://your-url.railway.app/api` in your browser.
You should see: `{"status":"ok","message":"Splitr API running"}`

Also check: `https://your-url.railway.app/docs` — FastAPI's auto-generated API docs.

---

## Step 8 — Update your Android app

Open `RetrofitClient.java` and change ONE line:

```java
// BEFORE (local emulator)
private static final String BASE_URL = "http://10.0.2.2:8000/";

// AFTER (Railway)
private static final String BASE_URL = "https://your-app.up.railway.app/";
```

Also remove `android:usesCleartextTraffic="true"` from `AndroidManifest.xml`
since you're now using HTTPS.

---

## Step 9 — Update your frontend HTML files

In `login.html` and `dashboard.html`, change:

```javascript
// BEFORE
const API = 'http://127.0.0.1:8000';

// AFTER
const API = 'https://your-app.up.railway.app';
```

Then add your frontend's URL to ALLOWED_ORIGINS on Railway (Step 6).

---

## Step 10 — Build and install the Android APK

In Android Studio:
1. **Build** → **Generate Signed Bundle / APK** → **APK**
2. Create a keystore if you don't have one (just follow the wizard)
3. Choose **release** build
4. Find the APK in `android/app/release/app-release.apk`
5. Send it to your phone via WhatsApp, email, or USB
6. On your phone: Settings → Install unknown apps → allow your file manager → install

---

## Checking logs on Railway

If something breaks, click your service → **Deployments** → click the latest deployment → **View Logs**.

Common errors:
- `relation "users" does not exist` → tables not created yet, check if `create_all` ran
- `connection refused` → DATABASE_URL not set, check Variables tab
- `ModuleNotFoundError` → requirements.txt missing a package

---

## Local development (after Railway setup)

```bash
cd backend

# Create virtual environment
python -m venv venv
source venv/bin/activate        # Mac/Linux
# venv\Scripts\activate         # Windows

# Install dependencies
pip install -r requirements.txt

# Copy env file
cp .env.example .env
# Edit .env — either point DATABASE_URL at your Railway Postgres
# or leave it blank to use local SQLite

# Run
uvicorn app.main:app --reload --port 8000
```

---

## Free tier limits on Railway

| Resource | Free allowance |
|----------|----------------|
| Execution hours | 500 hrs/month (~20 days of 24/7) |
| RAM | 512 MB |
| Postgres storage | 1 GB |
| Bandwidth | 100 GB |

More than enough for a personal expense app. If you need more, $5/month Hobby plan gives unlimited hours.

---

## Security notes

- Passwords are currently hashed with SHA-256. Good enough for personal use,
  upgrade to `bcrypt` before sharing with many users (`pip install bcrypt`).
- Your Railway URL is public — anyone can call your API.
  Add API key auth or rate limiting if needed.
- Never commit `.env` to git — it's in `.gitignore`.
