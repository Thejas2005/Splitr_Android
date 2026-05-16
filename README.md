<<<<<<< HEAD
# Splitr — Android App

Native Android (Java) client for the Splitr expense-splitting backend.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1+ |
| Java | 1.8 (set in build.gradle) |
| Android SDK | API 26+ (minSdk) / 34 (targetSdk) |
| Google Maps API key | Required |

---

## Quick Start

### 1. Clone / copy the project
Open Android Studio → **Open** → select the `splitr-android/` folder.

### 2. Add your Google Maps API key
Edit `app/src/main/AndroidManifest.xml` and replace:
```xml
android:value="YOUR_MAPS_API_KEY"
```
with your actual key from the [Google Cloud Console](https://console.cloud.google.com/).

Enable these APIs on your key:
- Maps SDK for Android
- Places API (optional, for future address search)

### 3. Configure backend URL
Edit `app/src/main/java/com/splitr/app/api/RetrofitClient.java`:

```java
// Android emulator → your machine's localhost:
private static final String BASE_URL = "http://10.0.2.2:8000/";

// Real Android device on the same WiFi:
private static final String BASE_URL = "http://192.168.1.X:8000/";
```

### 4. Run the FastAPI backend
```bash
cd your-backend-folder
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 5. Build & run
Press **Run ▶** in Android Studio, targeting an emulator or physical device.

---

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/splitr/app/
│   ├── activities/
│   │   ├── LoginActivity.java          Sign in / Register
│   │   ├── DashboardActivity.java      Expenses + Balance tabs
│   │   ├── MapActivity.java            Full-screen expense map ★
│   │   ├── GroupActivity.java          Group management
│   │   └── AddExpenseActivity.java     Personal & group expense form
│   ├── adapters/
│   │   ├── ExpenseAdapter.java         Expense list rows
│   │   ├── GroupAdapter.java           Group list rows
│   │   ├── SplitItemAdapter.java       I Owe / Owed to Me rows
│   │   └── LocationExpenseAdapter.java Bottom sheet cluster list
│   ├── api/
│   │   ├── ApiService.java             Retrofit interface (all endpoints)
│   │   └── RetrofitClient.java         Singleton Retrofit client
│   ├── models/                         POJO models matching FastAPI schemas
│   │   ├── AuthRequest / AuthResponse
│   │   ├── Expense / ExpenseCreate
│   │   ├── GroupExpenseCreate / GroupExpenseResponse
│   │   ├── MemberSplit / MemberSpec
│   │   ├── LocationExpense
│   │   ├── MyBalances / SplitItem
│   │   ├── Group / GroupMember / GroupCreate
│   │   ├── AddMemberRequest
│   │   ├── FindOrCreateGroupRequest / Response
│   │   └── GenericResponse
│   └── utils/
│       ├── SessionManager.java         SharedPreferences login state
│       ├── ExpenseClusterer.java       Haversine ~100m grouping
│       └── MarkerClusterGroup.java     Cluster data + heat ratio
└── res/
    ├── layout/                         All XML layouts
    ├── drawable/                       Shapes, vectors, selectors
    ├── menu/bottom_nav.xml
    ├── color/nav_icon_color.xml
    ├── values/colors, strings, themes, dimens
    └── raw/map_style_dark.json         Custom dark map style
```

---

## Map Feature (Core)

The `MapActivity` implements the full geo-expense map:

| Feature | Implementation |
|---------|---------------|
| Dark map style | `R.raw.map_style_dark` JSON |
| Expense fetch | `GET /api/expenses/locations?user_id=X` |
| ~100m clustering | `ExpenseClusterer.cluster()` — Haversine distance |
| Heat coloring | `myAmount / totalAmount` ratio → Blue/Red/Orange/Purple |
| Marker icon | Custom `Bitmap` drawn on `Canvas` with shadow + border |
| Cluster tap | `BottomSheetDialog` with totals + expense list |
| Add from map | Tap map → pin lat/lng → open `AddExpenseActivity` |
| My location | `FusedLocationProviderClient` + `ACCESS_FINE_LOCATION` |

### Marker color legend
| Color | Meaning |
|-------|---------|
| 🔵 Blue | No user spend at this location |
| 🔴 Red | Low user ratio (< 33%) |
| 🟠 Orange | Medium user ratio (33–66%) |
| 🟣 Purple | High user ratio (> 66%) |

---

## Splitting Logic

| Type | API field | Behavior |
|------|-----------|---------|
| Equal | `split_type: "equal"` | Backend divides by member count |
| Percentage | `split_type: "percentage"` | Pass `splits[]` with `amount` = % share |
| Dutch | `split_type: "dutch"` | Pass `splits[]` with `amount` = each person's actual share |

---

## API Endpoints Used

```
POST /api/auth/login
POST /api/auth/register
GET  /api/auth/users

POST /api/expenses/add
POST /api/expenses/group/add
GET  /api/expenses/user/{user_id}
GET  /api/expenses/locations?user_id=X
GET  /api/expenses/my-balances/{user_id}
POST /api/expenses/settle?split_id=X
DELETE /api/expenses/{expense_id}

POST /api/groups/create
POST /api/groups/add-member
POST /api/groups/find-or-create
GET  /api/groups/members/{group_id}
GET  /api/groups/user/{user_id}
GET  /api/groups/info/{group_id}
```

---

## Dependencies (app/build.gradle)

```gradle
// UI
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.coordinatorlayout:coordinatorlayout:1.2.0'

// Maps + Location
implementation 'com.google.android.gms:play-services-maps:18.2.0'
implementation 'com.google.android.gms:play-services-location:21.1.0'

// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
```

---

## Notes

- **Passwords** are sent in plain text matching the existing backend (`auth.py`). Add hashing (bcrypt) on both sides before production use.
- `usesCleartextTraffic="true"` is set in the manifest for local HTTP dev. Remove this and use HTTPS for production.
- The `10.0.2.2` IP is Android's emulator alias for your host machine's localhost. Change to your LAN IP for physical device testing.
=======
# Splitr_Android
Splitr
>>>>>>> 0ee5ab4dc21124a61b6fadccfcf9d4502b8f0273
