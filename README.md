# 📚 STLA Desktop — Student Learning Application

A premium JavaFX desktop application for online learning, built with **Clean Architecture**, **8 Design Patterns**, and a modern SaaS-style UI connected to **Supabase PostgreSQL**.

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 25 LTS |
| **UI Framework** | JavaFX 25 |
| **Database** | Supabase PostgreSQL |
| **Connection Pool** | HikariCP 6.2 |
| **Password Hashing** | BCrypt |
| **Environment Config** | java-dotenv |
| **Build Tool** | Maven 3.9+ |
| **Testing** | JUnit 5 |

---

## 🏗 Architecture

```
Clean Architecture (4 Layers)
┌───────────────────────────────────┐
│  UI Layer (JavaFX Controllers)    │
│  └─ FXML Views + CSS Stylesheets  │
├───────────────────────────────────┤
│  Service Layer (Business Logic)   │
│  └─ AuthService, CourseService    │
├───────────────────────────────────┤
│  Domain Layer (Models + Enums)    │
│  └─ Profile, Course, Enrollment   │
├───────────────────────────────────┤
│  Data Layer (Repositories + DB)   │
│  └─ JDBC PreparedStatement queries│
└───────────────────────────────────┘
```

---

## 📐 Design Patterns (8 Patterns)

| # | Pattern | Implementation | Usage |
|---|---------|---------------|-------|
| 1 | **Factory** | `UserFactory` | Creates Student/Instructor/Admin from Profile |
| 2 | **Strategy** | `PaymentStrategy` | Visa, PayPal, Wallet payment methods |
| 3 | **Observer** | `EventBus` | Enrollment, payment, course approval events |
| 4 | **Facade** | `EnrollmentFacade`, `CoursePublishFacade` | Orchestrates enrollment + publish workflows |
| 5 | **Decorator** | `CourseDecorator` | Certificate, Quiz, Mentor pricing add-ons |
| 6 | **Proxy** | `AccessControlProxy` | Role-based screen access control |
| 7 | **Singleton** | `DatabaseConnection`, `SessionManager`, `EventBus`, `AppConfig` | Single instance services |
| 8 | **Adapter** | `ReportExportAdapter`, `CsvReportExportAdapter` | Report export in different formats |

---

## ⚙️ Configuration

### 1. Create `.env` file in project root:

```env
SUPABASE_URL=your-supabase-url
SUPABASE_DB_HOST=your-host.supabase.co
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=your-password
SUPABASE_ANON_KEY=your-anon-key
```

### 2. Apply database schema:

```sql
-- Run stla.sql first (creates tables)
-- Then run demo_seed.sql (inserts demo data)
```

---

## 🚀 How to Run

```powershell
# Compile
mvn clean compile

# Run tests
mvn test

# Run application
mvn javafx:run
```

---

## 🔐 Demo Login Credentials

| Role | Email | Password |
|------|-------|----------|
| Student 1 | `student1@stla.com` | `password123` |
| Student 2 | `student2@stla.com` | `password123` |
| Instructor 1 | `instructor1@stla.com` | `password123` |
| Instructor 2 | `instructor2@stla.com` | `password123` |
| Admin | `admin@stla.com` | `password123` |

---

## 👤 User Roles & Features

### Student
- 📊 Dashboard with enrollment stats
- 📖 Course catalog with search & filter
- 📚 My Courses with progress tracking
- 🏆 Certificates
- 🔔 Notifications (read/unread)
- 👤 Profile

### Instructor
- 📊 Dashboard with revenue & enrollment charts
- 📚 Course management
- 👥 Enrolled students table
- 💰 Wallet & withdrawal requests
- 📈 Revenue analytics

### Admin
- 📊 Dashboard with KPI cards & charts
- 👥 User management (search, filter, activate/deactivate)
- 📚 Course approval/rejection
- 📁 Category management
- 💳 Payment records
- 💰 Withdrawal approval
- 📝 Activity logs
- 🌙 Dark mode toggle

---

## 📁 Project Structure

```
STLA_Desktop/
├── src/main/java/com/stla/
│   ├── app/                    # Application entry, config
│   ├── core/
│   │   ├── database/           # DatabaseConnection (Singleton)
│   │   ├── navigation/         # NavigationManager
│   │   └── session/            # SessionManager (Singleton)
│   ├── data/repositories/      # JDBC repository implementations
│   ├── domain/
│   │   ├── enums/              # AppRole, CourseStatus, etc.
│   │   └── models/             # Profile, Course, Enrollment, etc.
│   ├── patterns/
│   │   ├── adapter/            # ReportExportAdapter
│   │   ├── decorator/          # CourseComponent decorators
│   │   ├── facade/             # EnrollmentFacade, CoursePublishFacade
│   │   ├── factory/            # UserFactory
│   │   ├── observer/           # EventBus, AppEvent
│   │   ├── proxy/              # AccessControlProxy
│   │   └── strategy/           # PaymentStrategy implementations
│   ├── services/               # AuthService, CourseService, DashboardService
│   └── ui/
│       ├── admin/              # Admin controllers
│       ├── auth/               # Login/Register controllers
│       ├── components/         # ComponentFactory, ChartFactory, AnimationUtils, ThemeManager, ToastNotification
│       ├── instructor/         # Instructor controllers
│       └── student/            # Student controllers
├── src/main/resources/com/stla/
│   ├── css/                    # 9 CSS files (app, buttons, cards, forms, tables, dashboard, animations, charts, dark-theme)
│   └── views/                  # FXML files (auth, student, instructor, admin)
├── src/test/java/com/stla/     # JUnit 5 tests
├── .env                        # Database credentials
├── stla.sql                    # Database schema
├── demo_seed.sql               # Demo data
├── pom.xml                     # Maven build config
└── README.md
```

---

## 🎨 UI Features

- **Modern SaaS Design** — Dark gradient sidebar, clean white content area
- **Dark Mode** — Full dark theme toggle (🌙 button)
- **Animations** — Card hover lifts, stagger fade-in, shake on errors, pulse on notifications
- **Toast Notifications** — Success/error/warning/info toasts
- **Professional Charts** — Line, area, bar, pie charts with STLA color palette
- **Responsive Layout** — Minimum 1100x700, scales gracefully

---

## 🧪 Testing

```powershell
mvn test
```

Tests cover:
- ✅ Factory Pattern (UserFactory)
- ✅ Strategy Pattern (Visa, PayPal, Wallet)
- ✅ Observer Pattern (EventBus subscribe/publish/unsubscribe)
- ✅ Decorator Pattern (price stacking)
- ✅ Facade Pattern (EnrollmentFacade)
- ✅ Adapter Pattern (CSV export)
- ✅ Singleton Pattern (EventBus, AppConfig)
- ✅ Domain Enums mapping

---

## 🔧 Troubleshooting

| Issue | Solution |
|-------|---------|
| `Connection refused` | Check `.env` credentials and Supabase project status |
| `FXML not found` | Run `mvn clean compile` first |
| JavaFX module errors | Ensure `--add-opens` flags in pom.xml javafx plugin |
| `class not found` | Delete `target/` folder and recompile |
| Dark mode not applying | Ensure `dark-theme.css` exists in `css/` folder |

---

## 🎓 Presentation Notes

1. **Start with Login** — Show role-based routing (student/instructor/admin)
2. **Student Flow** — Dashboard → Catalog → Enroll → My Courses → Certificates
3. **Instructor Flow** — Dashboard → Courses → Students → Wallet → Analytics
4. **Admin Flow** — Dashboard → Users → Courses (approve/reject) → Payments → Logs
5. **Dark Mode** — Toggle with 🌙 button in admin dashboard
6. **Design Patterns** — Highlight each pattern in code during Q&A
7. **Architecture** — Show Clean Architecture layers
8. **Charts** — Revenue trends, enrollment distribution, user pie chart

---

## 📊 Build Status

```
✅ BUILD SUCCESS
✅ 96 source files compiled
✅ 9 test classes with 40+ test cases
✅ Java 25 + JavaFX 25
✅ 9 CSS stylesheets
✅ 15+ FXML views
```
