<p align="center">
  <img src="https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/JavaFX-25-007396?style=for-the-badge&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" />
  <img src="https://img.shields.io/badge/Supabase-PostgreSQL-3FCF8E?style=for-the-badge&logo=supabase&logoColor=white" />
  <img src="https://img.shields.io/badge/Architecture-Clean-blueviolet?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" />
</p>

<h1 align="center">🎓 STLA Desktop Learning Platform</h1>

<p align="center">
  <b>A modern, full-featured JavaFX desktop e-learning platform built with Clean Architecture and Software Engineering best practices.</b>
</p>

<p align="center">
  STLA (<i>Student Learning App</i>) is a comprehensive desktop application that provides <b>course management</b>, <b>secure payments</b>, <b>interactive quizzes</b>, <b>progress tracking</b>, <b>real-time notifications</b>, <b>instructor verification</b>, and <b>wallet management</b> — powered by Java 25, JavaFX 25, Supabase PostgreSQL, and a layered Clean Architecture with 8+ GoF design patterns.
</p>

---

## ✨ Features

### 👨‍🎓 Student Features

- 🔐 Secure registration & login with BCrypt password hashing
- 📚 Browse & search course catalog with category filters
- 💳 Secure enrollment via **Visa**, **Digital Wallet**, or **PayPal** payments
- ▶️ Integrated course player with video streaming
- 📊 Lesson-by-lesson progress tracking with completion percentages
- 📝 Interactive quiz solving with instant scoring
- 🔔 Real-time notifications (enrollment, payments, course updates)
- 🏆 Automated certificate generation upon course completion
- ⭐ Rate & review instructors and courses
- 👤 Full profile management

### 👩‍🏫 Instructor Features

- ✅ Instructor verification workflow (submit → admin review → approval)
- 📖 Course creation with sections, lessons, and video uploads
- 📁 Lesson resource management (PDF, DOC, ZIP, images)
- 🧩 Advanced quiz builder (single choice, multiple choice, true/false)
- 💰 Wallet system with real-time revenue tracking
- 💸 Withdrawal requests (Visa & Digital Wallet methods)
- 📈 Student enrollment analytics and progress monitoring
- 🔔 Notifications for enrollments, reviews, and approvals

### 🛡️ Admin Features

- 👥 Complete user management (students, instructors, admins)
- 🔍 Instructor verification & approval workflow
- ✔️ Course review & approval pipeline
- 💵 Payment monitoring & transaction history
- 🏦 Admin wallet with commission tracking
- 📊 Platform-wide statistics and analytics dashboards
- 🔔 System-wide notification management
- 📂 Category management for course organization

---

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| **Java 25** | Core backend logic & business rules |
| **JavaFX 25** | Modern desktop UI framework |
| **Supabase PostgreSQL** | Cloud-hosted relational database |
| **Supabase Storage** | File & video storage (thumbnails, resources) |
| **Maven 3.9+** | Dependency management & build automation |
| **HikariCP** | High-performance JDBC connection pooling |
| **BCrypt** | Secure password hashing |
| **CSS** | Custom UI styling with dark theme support |
| **PlantUML** | UML diagram generation |
| **SLF4J** | Logging framework |
| **JUnit 5** | Unit testing |
| **java-dotenv** | Environment variable management |

---

## 🏗️ Architecture

STLA follows **Clean Architecture** principles with strict **Separation of Concerns** across well-defined layers:

```
┌───────────────────────────────────────────────────┐
│                   UI Layer                        │
│   (JavaFX Controllers, FXML Views, CSS Styles)    │
├───────────────────────────────────────────────────┤
│                Service Layer                      │
│   (Business Logic, Orchestration, Validation)     │
├───────────────────────────────────────────────────┤
│           Design Patterns Layer                   │
│  (Factory, Strategy, Observer, Facade, Decorator, │
│   Proxy, Singleton, Adapter)                      │
├───────────────────────────────────────────────────┤
│              Repository Layer                     │
│   (Data Access, SQL Queries, Result Mapping)      │
├───────────────────────────────────────────────────┤
│               Domain Layer                        │
│   (Models, Enums, Interfaces)                     │
├───────────────────────────────────────────────────┤
│             Core / Infrastructure                 │
│   (Database, Session, Navigation, Config)         │
└───────────────────────────────────────────────────┘
```

| Layer | Package | Responsibility |
|-------|---------|----------------|
| **UI** | `com.stla.ui.*` | JavaFX controllers, FXML views, reusable components |
| **Services** | `com.stla.services` | Business logic, workflow orchestration |
| **Patterns** | `com.stla.patterns.*` | GoF design patterns implementation |
| **Data** | `com.stla.data.*` | Repository implementations, result mappers |
| **Domain** | `com.stla.domain.*` | Models, enums, repository interfaces |
| **Core** | `com.stla.core.*` | Database connection, session, navigation |
| **App** | `com.stla.app` | Application entry point, configuration |

---

## 🧩 Design Patterns

STLA implements **8 Gang of Four (GoF) design patterns** with real, production-grade usage:

| Pattern | Purpose | Key Classes |
|---------|---------|-------------|
| **Factory** | Creates role-specific user records (Student, Instructor, Admin) from a common Profile | `UserFactory`, `PaymentStrategyFactory`, `WithdrawStrategyFactory` |
| **Strategy** | Interchangeable payment & withdrawal algorithms | `PaymentStrategy`, `VisaPaymentStrategy`, `WalletPaymentStrategy`, `PayPalPaymentStrategy`, `WithdrawStrategy`, `VisaWithdrawStrategy`, `DigitalWalletWithdrawStrategy` |
| **Observer** | Event-driven notification system via publish/subscribe | `EventBus`, `EventListener`, `AppEvent`, `NotificationObserver` |
| **Facade** | Simplifies complex multi-step workflows | `EnrollmentFacade` (payment → enrollment → wallet → notifications), `CoursePublishFacade` (draft → submit → approve/reject) |
| **Decorator** | Dynamic course pricing with stackable add-ons | `CourseComponent`, `BaseCourse`, `CourseDecorator`, `CertificateDecorator`, `QuizDecorator`, `MentorSupportDecorator` |
| **Proxy** | Access control for screens & course content | `AccessControlProxy` (role-based navigation), `CourseAccessProxy` (enrollment-based lesson access) |
| **Singleton** | Single-instance application-wide services | `AppConfig`, `DatabaseConnection`, `SessionManager`, `EventBus` |
| **Adapter** | Unified interfaces for external services | `PaymentGatewayAdapter`, `SimulatedPaymentGateway`, `ReportExportAdapter`, `CsvReportExportAdapter` |

---

## 📁 Project Structure

```text
src/
 ├── main/
 │   ├── java/com/stla/
 │   │   ├── app/                          # Application entry point
 │   │   │   ├── StlaApplication.java      # JavaFX Application class
 │   │   │   ├── Launcher.java             # Main launcher
 │   │   │   └── AppConfig.java            # Singleton configuration
 │   │   ├── core/                         # Infrastructure
 │   │   │   ├── database/                 # HikariCP database connection
 │   │   │   ├── navigation/               # Screen navigation manager
 │   │   │   └── session/                  # User session management
 │   │   ├── data/                         # Data access layer
 │   │   │   ├── mappers/                  # ResultSet → Model mappers
 │   │   │   └── repositories/             # Repository implementations
 │   │   ├── domain/                       # Domain layer
 │   │   │   ├── enums/                    # AppRole, CourseStatus, PaymentStatus, etc.
 │   │   │   ├── interfaces/               # Repository contracts
 │   │   │   └── models/                   # Entity models (30+ classes)
 │   │   ├── patterns/                     # Design patterns
 │   │   │   ├── adapter/                  # PaymentGateway & ReportExport adapters
 │   │   │   ├── decorator/                # Course pricing decorators
 │   │   │   ├── facade/                   # Enrollment & CoursePublish facades
 │   │   │   ├── factory/                  # UserFactory
 │   │   │   ├── observer/                 # EventBus & NotificationObserver
 │   │   │   ├── proxy/                    # Access control proxies
 │   │   │   ├── singleton/                # SingletonRegistry documentation
 │   │   │   └── strategy/                 # Payment & Withdrawal strategies
 │   │   ├── services/                     # Business logic (20+ services)
 │   │   └── ui/                           # JavaFX controllers
 │   │       ├── admin/                    # Admin dashboard & management
 │   │       ├── auth/                     # Login & registration
 │   │       ├── components/               # Reusable UI components
 │   │       ├── instructor/               # Instructor dashboard & tools
 │   │       └── student/                  # Student dashboard & learning
 │   └── resources/com/stla/
 │       ├── css/                          # Stylesheets (25+ CSS files)
 │       │   ├── app.css                   # Global application styles
 │       │   ├── dark-theme.css            # Dark mode theme
 │       │   ├── theme.css                 # Theme variables
 │       │   ├── animations.css            # UI animations
 │       │   └── ...                       # Component-specific styles
 │       ├── data/                         # Static data files
 │       └── views/                        # FXML view files
 │           ├── admin/                    # 12 admin views
 │           ├── auth/                     # Login & register views
 │           ├── components/               # Reusable component views
 │           ├── instructor/               # 11 instructor views
 │           └── student/                  # 14 student views
 └── test/                                 # JUnit 5 test suite
```

---

## 🗄️ Database Overview

STLA uses **Supabase PostgreSQL** with **25+ tables**, custom enums, row-level security (RLS), triggers, and indexes:

### Core User Tables

| Table | Description |
|-------|-------------|
| `profiles` | Shared user profile (name, email, avatar, role, password hash) |
| `students` | Student-specific data (headline, interests, learning goals) |
| `instructors` | Instructor data (expertise, rating, verification status) |
| `admins` | Admin data (admin level, permissions) |

### Course Domain

| Table | Description |
|-------|-------------|
| `categories` | Course categories with slug and icon |
| `courses` | Course catalog (title, price, status, ratings, enrollment count) |
| `course_lessons` | Ordered lessons with video URLs and preview flags |
| `lesson_resources` | Downloadable resources (PDF, DOC, ZIP, images) |
| `course_addons` | Optional add-ons (certificate, quiz, mentor support) |

### Enrollment & Payment

| Table | Description |
|-------|-------------|
| `enrollments` | Student ↔ Course enrollment records |
| `payments` | Payment transactions with gateway metadata |
| `payment_methods` | Saved payment methods (masked details, tokens) |

### Progress & Quizzes

| Table | Description |
|-------|-------------|
| `student_course_progress` | Per-course completion percentage |
| `lesson_progress` | Per-lesson watch time and completion |
| `quizzes` | Quiz definitions (time limit, passing score, attempts) |
| `quiz_questions` | Questions (single choice, multiple choice, true/false) |
| `quiz_options` | Answer options with correctness flags |
| `quiz_attempts` | Student quiz attempts with scores |

### Financial

| Table | Description |
|-------|-------------|
| `instructor_wallets` | Instructor balance (pending, available, total earned) |
| `wallet_transactions` | Earning & withdrawal transaction ledger |
| `withdrawal_requests` | Withdrawal requests with admin review workflow |

### Social & Notifications

| Table | Description |
|-------|-------------|
| `notifications` | System notifications with read status |
| `reviews` | Course ratings and comments |
| `certificates` / `issued_certificates` | Certificate templates and issued records |

---

## 💳 Payment & Wallet Flow

```
Student Payment Flow:
┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Student  │───▶│  Checkout    │───▶│  Payment     │───▶│  Enrollment  │
│ selects  │    │  (Strategy)  │    │  Processing  │    │  Created     │
│ course   │    │  Visa/Wallet │    │  via Gateway │    │              │
└──────────┘    └──────────────┘    └──────────────┘    └──────┬───────┘
                                                               │
                                                               ▼
                                                    ┌──────────────────┐
                                                    │  Revenue Split   │
                                                    │  ├─ 70% → Instructor Wallet
                                                    │  └─ 30% → Admin Commission
                                                    └──────────────────┘

Instructor Withdrawal Flow:
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Instructor  │───▶│  Withdrawal  │───▶│ Admin Review │───▶│  Completed   │
│  requests    │    │  Request     │    │ (approve/    │    │  Payout      │
│  withdrawal  │    │  (Visa/Wallet)    │  reject)     │    │              │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

- **Payment Methods**: Visa Credit/Debit, Digital Wallets (Vodafone Cash, Orange Cash, Etisalat Cash, InstaPay, Fawry), PayPal
- **Commission**: Platform deducts a configurable commission (default 30%) from each course sale
- **Wallet**: Instructor revenue automatically credited to wallet with full transaction history
- **Withdrawals**: Instructors request withdrawals → Admin reviews → Approved or Rejected

---

## 📝 Quiz System

| Feature | Details |
|---------|---------|
| **Question Types** | Single Choice, Multiple Choice, True/False |
| **Configuration** | Time limit, passing score, max attempts, question shuffling |
| **Scoring** | Points per question, automatic grading, pass/fail determination |
| **Attempts** | Configurable max attempts per quiz with attempt history |
| **Integration** | Quiz completion contributes to overall course progress |
| **Builder** | Visual quiz builder for instructors with drag-and-drop ordering |
| **Results** | Detailed results view with correct answers shown post-submission |

---

## 🔔 Notification System

STLA uses an **event-driven notification system** built on the **Observer Pattern**:

```
┌─────────────┐     publish()     ┌──────────┐     notify()     ┌──────────────────┐
│  Any Module │────────────────▶│ EventBus │────────────────▶│ NotificationObserver │
│  (Service,  │                  │(Singleton)│                  │ (persists to DB)    │
│  Controller)│                  └──────────┘                  └─────────────────────┘
└─────────────┘
```

### Supported Event Types

- `ENROLLMENT_CREATED` · `PAYMENT_COMPLETED` · `PAYMENT_FAILED`
- `COURSE_SUBMITTED` · `COURSE_APPROVED` · `COURSE_REJECTED`
- `LESSON_COMPLETED` · `COURSE_COMPLETED` · `COURSE_PROGRESS_UPDATED`
- `QUIZ_SUBMITTED` · `QUIZ_PASSED` · `QUIZ_FAILED`
- `WITHDRAWAL_COMPLETED` · `CERTIFICATE_ISSUED`
- `INSTRUCTOR_VERIFIED` · `INSTRUCTOR_REJECTED`
- `STUDENT_REVIEW_ADDED` · And more...

### Per-Role Notifications

| Role | Notification Examples |
|------|----------------------|
| **Student** | Enrollment confirmation, payment receipts, new lessons, quiz results, certificates |
| **Instructor** | New enrollments, revenue updates, course approval/rejection, withdrawal status, student reviews |
| **Admin** | New course submissions, verification requests, withdrawal requests, platform alerts |

---

## 🎨 UI / UX

- 🌗 **Dark Mode** — Full dark theme support with smooth toggle transitions
- ✨ **Animations** — Fade-in, slide, and scale transitions for a polished feel
- 📐 **Responsive Layouts** — Adaptive UI that scales across different screen sizes
- 🧩 **Reusable Components** — Course cards, notification bells, avatar components, video players
- 📊 **Professional Dashboards** — Rich chart-based analytics for all roles
- 🎬 **Integrated Video Player** — In-app course video playback with progress tracking
- 🖼️ **Modern Design System** — 25+ dedicated CSS stylesheets for consistent theming

---

## 🚀 Installation

### Requirements

| Prerequisite | Version |
|---|---|
| Java JDK | 21+ (project uses Java 25) |
| Maven | 3.9+ |
| Supabase Project | With PostgreSQL database |
| JavaFX SDK | 25 (managed via Maven) |

### Clone Repository

```bash
git clone https://github.com/revo2026/STLA_Desktop.git
cd STLA_Desktop
```

### Configure Environment

Create a `.env` file in the project root:

```env
# Supabase API
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key

# Supabase PostgreSQL JDBC
SUPABASE_DB_HOST=aws-0-region.pooler.supabase.com
SUPABASE_DB_PORT=6543
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres.your-project-ref
SUPABASE_DB_PASSWORD=your-db-password

# Application Settings
APP_TITLE=STLA - Student Learning App
APP_WIDTH=1400
APP_HEIGHT=900
```

### Setup Database

Run the SQL schema file against your Supabase PostgreSQL database:

```bash
# Via Supabase SQL Editor or psql
psql -h your-db-host -U your-db-user -d postgres -f stla.sql
```

### Install Dependencies

```bash
mvn clean install
```

### Run Project

```bash
mvn javafx:run
```

---


## 🔮 Future Improvements

- 📱 **Mobile App** — Cross-platform mobile companion (Android/iOS)
- 🤖 **AI Recommendations** — Personalized course suggestions using ML
- 🎥 **Live Streaming** — Real-time instructor-led sessions
- 💬 **Chat System** — Student-instructor direct messaging
- 💳 **Real Payment Gateways** — Stripe, PayPal, and regional payment integration
- 🎮 **Gamification** — Badges, streaks, leaderboards, and XP system
- 🌍 **Multi-language Support** — i18n for Arabic, French, and more
- 📊 **Advanced Analytics** — Learning path analytics with AI insights
- 🔄 **Offline Mode** — Downloaded content for offline learning
- 🧑‍🤝‍🧑 **Discussion Forums** — Per-course discussion boards

---

## 👥 Contributors

<table>
  <tr>
    <td align="center">
      <b>Your Name</b><br/>
      <sub>Full Stack Developer</sub>
    </td>
  </tr>
</table>

> 💡 *Contributions are welcome! Feel free to open issues and submit pull requests.*

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2026 revo2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<p align="center">
  Built with ❤️ using Java, JavaFX & Supabase
</p>
