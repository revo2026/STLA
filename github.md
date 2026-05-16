````text
Analyze the entire STLA JavaFX Desktop project and generate a COMPLETE professional GitHub README.md file.

IMPORTANT:
Do NOT modify source code.
Only create/update:
README.md

The README must look professional and production-quality, suitable for:
- GitHub portfolio
- Graduation project
- Software Engineering project
- Resume showcase

==================================================
MAIN GOAL
==================================================

Generate a modern, professional, highly organized README.md for the STLA Desktop Learning Platform.

The README should explain:
- What the project is
- Features
- Technologies
- Architecture
- Design Patterns
- Setup
- Database
- Screens
- Project structure
- Future improvements

Use Markdown professionally.

==================================================
README STRUCTURE
==================================================

# STLA Desktop Learning Platform

Add a professional project description.

Example style:
"STLA is a modern JavaFX-based desktop e-learning platform that provides course management, payments, quizzes, progress tracking, notifications, instructor verification, and wallet management using Java, JavaFX, Supabase PostgreSQL, and Clean Architecture."

==================================================
1. PROJECT PREVIEW
==================================================

Add placeholders:

## Screenshots

Example:
- Login Screen
- Student Dashboard
- Instructor Dashboard
- Admin Dashboard
- Course Player
- Quiz Builder

Use markdown image placeholders:

![Login Screen](docs/screenshots/login.png)

==================================================
2. FEATURES
==================================================

Create professional categorized feature lists.

==================================================
STUDENT FEATURES
==================================================

- Register/Login
- Browse courses
- Secure course enrollment
- Visa & Wallet payments
- Course player
- Lesson progress tracking
- Quiz solving
- Notifications
- Certificates
- Profile management

==================================================
INSTRUCTOR FEATURES
==================================================

- Instructor verification
- Course creation
- Lesson/video uploads
- Quiz builder
- Wallet system
- Revenue tracking
- Student analytics
- Notifications

==================================================
ADMIN FEATURES
==================================================

- User management
- Instructor verification
- Course approval
- Payment monitoring
- Wallet analytics
- Notifications
- Platform statistics

==================================================
3. TECHNOLOGIES USED
==================================================

Create markdown table:

| Technology | Purpose |
|------------|---------|
| Java | Backend Logic |
| JavaFX | Desktop UI |
| Supabase PostgreSQL | Database |
| Supabase Storage | File Storage |
| Maven | Dependency Management |
| CSS | Styling |
| PlantUML | UML Diagrams |

==================================================
4. ARCHITECTURE
==================================================

Explain:
- Clean Architecture
- Layered Architecture
- Separation of Concerns

Explain layers:
- UI Layer
- Service Layer
- Repository Layer
- Database Layer
- Core Utilities

==================================================
5. DESIGN PATTERNS
==================================================

Create markdown table:

| Pattern | Purpose | Main Classes |
|---------|----------|--------------|

Include:
- Factory
- Strategy
- Observer
- Facade
- Decorator
- Proxy
- Singleton
- Adapter

Use real class names from project only.

==================================================
6. PROJECT STRUCTURE
==================================================

Generate tree structure.

Example:

```text
src/
 ├── main/
 │   ├── java/com/stla/
 │   │   ├── core/
 │   │   ├── domain/
 │   │   ├── repositories/
 │   │   ├── services/
 │   │   ├── patterns/
 │   │   ├── ui/
 │   │   └── utils/
 │   └── resources/
 │       ├── fxml/
 │       ├── css/
 │       └── assets/
````

==================================================
7. DATABASE OVERVIEW
====================

Explain important tables:

* profiles
* students
* instructors
* admins
* courses
* lessons
* quizzes
* payments
* enrollments
* notifications
* instructor_wallets

==================================================
8. PAYMENT & WALLET FLOW
========================

Explain:

* Student payment
* Commission deduction
* Admin revenue
* Instructor revenue
* Withdrawal flow

==================================================
9. QUIZ SYSTEM
==============

Explain:

* Supported question types
* Quiz attempts
* Scoring system
* Progress integration

==================================================
10. NOTIFICATION SYSTEM
=======================

Explain:

* EventBus
* Observer Pattern
* Real-time notifications
* Student/Admin/Instructor notifications

==================================================
11. UI/UX
=========

Explain:

* Modern desktop UI
* Dark mode
* Animations
* Responsive layouts
* Professional dashboards

==================================================
12. INSTALLATION
================

Generate professional setup instructions.

Include:

## Requirements

* Java 21+
* Maven
* Supabase project
* JavaFX SDK

## Clone Repository

```bash
git clone https://github.com/your-username/STLA_Desktop.git
cd STLA_Desktop
```

## Install Dependencies

```bash
mvn clean install
```

## Run Project

```bash
mvn javafx:run
```

==================================================
13. ENVIRONMENT VARIABLES
=========================

Create example:

```env
SUPABASE_URL=
SUPABASE_ANON_KEY=
DB_HOST=
DB_PORT=
DB_NAME=
DB_USER=
DB_PASSWORD=
```

==================================================
14. UML DIAGRAMS
================

Mention:

* PlantUML diagrams available in docs/plantuml/

==================================================
15. FUTURE IMPROVEMENTS
=======================

Examples:

* Mobile app
* AI recommendations
* Live streaming
* Chat system
* Real payment gateways
* Gamification

==================================================
16. CONTRIBUTORS
================

Add placeholder section.

==================================================
17. LICENSE
===========

Add MIT License placeholder.

==================================================
README STYLE REQUIREMENTS
=========================

The README should:

* Look modern and professional
* Use markdown professionally
* Use badges if possible
* Use tables
* Use emojis moderately
* Be highly organized
* Be readable on GitHub

==================================================
OPTIONAL GITHUB BADGES
======================

Add badges like:

* Java
* JavaFX
* Maven
* Supabase
* License
* Build status

==================================================
FINAL OUTPUT
============

Generate:
README.md

At the end report:

* Sections generated
* Tables generated
* Project structure included
* Design patterns documented

```
```
