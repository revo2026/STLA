# STLA JavaFX Desktop Application — Integravity Prompt

## Overview

Analyze the provided STLA project documentation carefully and build a complete Desktop Application version of the system.

Project Name:
STLA - Student Learning App

Target Platform:
Desktop Application

Technology Stack:
- Frontend: JavaFX
- Backend Logic: Java
- Database: Supabase PostgreSQL
- Authentication: Supabase Auth or custom Java authentication using Supabase users/profile tables
- Storage: Supabase Storage for course thumbnails, lesson videos, resources, avatars, and certificates
- Architecture: Clean Architecture
- Code Style: Clean Code, SOLID principles, reusable components, clear package structure

IMPORTANT:
This is NOT a mobile app and NOT a web app.
Convert the system into a professional desktop application using JavaFX UI patterns.
Use desktop-friendly layouts:
- Sidebar navigation
- Top header
- Data tables
- Split panes
- Cards
- Dialogs
- Forms
- Modal windows
- Charts
- Admin panels
- Desktop dashboards

---

## 1. MAIN IDEA

Build a full online learning platform desktop system that connects:

1. Students
2. Instructors
3. Admins

The system should support:
- Course discovery
- Course enrollment
- Payment simulation
- Learning progress
- Quizzes
- Certificates
- Instructor course management
- Instructor earnings and withdrawals
- Admin user/course/payment management
- Notifications
- Reports and analytics

---

## 2. USER ROLES

The application must have role-based access.

Login flow:
User logs in using email and password.
After login, the system checks the user role:
- student → open Student Dashboard
- instructor → open Instructor Dashboard
- admin → open Admin Dashboard

Do not allow users to access screens outside their role.

---

## 3. STUDENT MODULE

Create these Student Desktop Screens:

1. Student Dashboard
- Welcome message
- Featured courses
- Continue learning
- My enrolled courses summary
- Progress overview
- Notifications preview

2. Course Catalog
- Search courses
- Filter by category
- Filter by level
- Filter by price
- Sort courses
- Course cards/grid

3. Course Details
- Course thumbnail
- Course title
- Instructor info
- Description
- Price
- Rating
- Lessons preview
- Add-ons: Certificate, Quiz, Resources, Mentor Support
- Enroll button

4. Checkout
- Course summary
- Payment method selection:
  - Visa
  - Wallet
  - PayPal
- Total price
- Confirm payment button

5. My Courses
- Enrolled courses
- Progress percentage
- Continue learning button

6. Course Player
- Lesson list
- Video placeholder/player area
- Resources section
- Mark lesson as completed
- Progress tracking
- Locked lesson UI if not enrolled

7. Quiz Screen
- Questions
- Options
- Submit answers
- Score result
- Pass/fail result

8. Certificates
- List of issued certificates
- Certificate number
- Course name
- Issue date

9. Student Profile
- Full name
- Email
- Avatar
- Interests
- Learning goals
- Payment history

10. Student Notifications
- Enrollment success
- Payment confirmation
- New lesson added
- Certificate issued

---

## 4. INSTRUCTOR MODULE

Create these Instructor Desktop Screens:

1. Instructor Dashboard
- Total courses
- Total students
- Total revenue
- Average rating
- Recent activity
- Notifications preview

2. My Courses
- List/table of instructor courses
- Course status:
  - Draft
  - Pending approval
  - Approved
  - Rejected
- Edit course
- View students
- View analytics

3. Add Course
- Course title
- Category
- Level
- Price
- Description
- Thumbnail upload
- Save draft
- Submit for approval

4. Edit Course
- Edit course details
- Manage lessons
- Manage resources
- Manage quizzes
- Manage add-ons:
  - Certificate
  - Quiz
  - Resources
  - Mentor Support

5. Lesson Manager
- Add lesson
- Edit lesson
- Delete lesson
- Reorder lessons
- Upload video/resource

6. Quiz Manager
- Add quiz
- Add questions
- Add options
- Mark correct answer
- Set passing score
- Preview quiz

7. Enrolled Students
- Table of students enrolled in instructor courses
- Student name
- Email
- Course name
- Progress
- Payment status

8. Revenue Analytics
- Revenue charts
- Enrollment charts
- Course performance
- Rating statistics

9. Wallet
- Available balance
- Pending balance
- Total earned
- Total withdrawn
- Transaction history

10. Withdraw
- Withdrawal method
- Amount
- Confirm withdraw
- Withdrawal logs

11. Instructor Notifications
- New student enrolled
- Course approved
- Course rejected
- New review
- Withdrawal update

---

## 5. ADMIN MODULE

Create these Admin Desktop Screens:

1. Admin Dashboard
- Total users
- Total students
- Total instructors
- Total courses
- Total payments
- Platform revenue
- Recent activity logs

2. Users Management
- Table of all users
- Search users
- Filter by role
- Role badge:
  - Student
  - Instructor
  - Admin
- Activate/deactivate users
- View user details

3. Courses Management
- Table of all courses
- Course title
- Instructor
- Category
- Price
- Status
- Approve course
- Reject course with reason
- Preview course
- Delete/archive course

4. Categories Management
- Add category
- Edit category
- Delete/deactivate category
- Course count per category

5. Payments Management
- All payments table
- Student name
- Course name
- Payment method
- Amount
- Status
- Date

6. Transactions
- Full payment transaction log
- Filter by method/status/date

7. Withdrawal Requests
- Instructor withdrawal requests
- Approve withdrawal
- Reject withdrawal
- Show method and amount

8. Reports
- User growth report
- Course performance report
- Revenue report
- Enrollment report
- Export report button

9. Roles & Permissions
- View roles
- Manage permissions
- Restrict access by role

10. Activity Logs
- Admin actions
- Login events
- Course approvals
- Payment events
- Withdrawal events

11. Admin Notifications
- New instructor registered
- Course pending approval
- Payment issue
- Withdrawal request
- System alert

---

## 6. DATABASE REQUIREMENTS

Use Supabase PostgreSQL.

Create or use tables similar to:
- profiles
- students
- instructors
- admins
- categories
- courses
- course_lessons
- lesson_resources
- course_addons
- course_addon_map
- enrollments
- payments
- payment_methods
- student_course_progress
- lesson_progress
- quizzes
- quiz_questions
- quiz_options
- quiz_attempts
- quiz_answers
- certificates
- issued_certificates
- notifications
- instructor_wallets
- wallet_transactions
- withdrawal_requests
- reviews
- course_analytics
- activity_logs

The Java app should connect to Supabase PostgreSQL using JDBC.

Use repositories for all SQL operations.
Do not write SQL directly inside JavaFX controllers.

---

## 7. CLEAN ARCHITECTURE STRUCTURE

```text
src/main/java/com/stla/

├── app/
├── core/
├── domain/
├── data/
├── services/
├── patterns/
├── ui/
└── resources/
```

Use:
- Repository pattern
- Service layer
- Controllers
- DTOs/Mappers
- Validation layer
- Dependency separation

---

## 8. JAVA DESIGN PATTERNS

### Factory Pattern
Use for:
- Student creation
- Instructor creation
- Admin creation

### Strategy Pattern
Use for:
- Payment methods
- Withdrawal methods
- Report generation

### Observer Pattern
Use for:
- Notifications
- Enrollment events
- Course approval events

### Facade Pattern
Use for:
- Enrollment workflow
- Course publishing workflow
- Payment workflow
- Admin approval workflow

### Decorator Pattern
Use for:
- Course add-ons:
  - Certificate
  - Quiz
  - Resources
  - Mentor Support

### Proxy Pattern
Use for:
- Access control
- Locked lessons
- Instructor ownership validation
- Admin permissions

### Singleton Pattern
Use for:
- DatabaseConnection
- SessionManager
- AppConfig

### Adapter Pattern
Use for:
- Payment gateway
- Supabase storage
- Email notifications
- Report export

---

## 9. DESKTOP UI DESIGN SYSTEM

Create a modern professional JavaFX desktop UI.

Suggested colors:
- Primary:
#02262B
#011418
#0E4A54
#0F3D45
- Secondary:
#D44304
#B03803
#F06428
#E85A20
- Gradient: #0F3D45 → #A78BFA
- Background: #F8F9FB
- Card: #FFFFFF
- Text: #1F2937
- Secondary Text: #6B7280
- Input & Chips: #E8F4F6
#DFEAEC
#C5D4D7
- Accent Gold: #F59E0B
- Success: #10B981
- Danger: #EF4444
- Border: #E5E7EB

Desktop UI rules:
- Sidebar navigation
- Top header
- Dashboard cards
- Modern tables
- Charts
- Split panes
- Dialogs
- Form validation
- Loading states
- Empty states

Use JavaFX CSS files:
- app.css
- buttons.css
- tables.css
- cards.css
- forms.css
- dashboard.css

Supabase connection .env:
SUPABASE_URL=https://axtfnaqoyvpvijsuiydc.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF4dGZuYXFveXZwdmlqc3VpeWRjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgyMzgzNTAsImV4cCI6MjA5MzgxNDM1MH0.r3ny3f3CE-zvi461pncq8kfuwnkvcsBnyZp_afn9auc

SUPABASE_DB_HOST=db.axtfnaqoyvpvijsuiydc.supabase.co
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=StlA_2026@@

---

## 10. IMPORTANT CODE RULES

- Do not put database queries inside JavaFX controllers
- Controllers should call services only
- Services should call repositories
- Repositories handle SQL only
- Models should be plain Java classes
- Use interfaces where useful
- Use validation classes
- Use PreparedStatement
- Avoid duplicate code
- Follow SOLID principles

---

## 11. TESTING

Add JUnit tests for:
- Factory pattern
- Strategy pattern
- Observer pattern
- Facade pattern
- Decorator pattern
- Proxy pattern
- Singleton pattern
- Repository methods
- Service methods

---

## 12. FINAL OUTPUT REQUIRED

Generate the complete JavaFX desktop project with:
1. Clean project structure
2. Java models
3. Repositories
4. Services
5. JavaFX controllers
6. FXML views
7. CSS files
8. Supabase database connection
9. Design pattern implementations
10. Role-based navigation
11. Student module
12. Instructor module
13. Admin module
14. Error handling
15. Test files
16. README documentation

Make the project production-like, scalable, and suitable for a university Java Design Patterns project.
