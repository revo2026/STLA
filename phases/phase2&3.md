Continue the STLA JavaFX Desktop project from Phase 1.

Phase 1 is already complete and includes:
- JavaFX 25 + Java 25
- Maven project
- Clean Architecture structure
- Supabase PostgreSQL connection with JDBC + HikariCP
- AuthService and DashboardService
- Repository interfaces and JDBC implementations
- All 8 design patterns implemented:
  Factory, Strategy, Observer, Facade, Decorator, Proxy, Singleton, Adapter
- Initial FXML views and CSS design system

Now build Phase 2 and Phase 3.

IMPORTANT:
Do not rebuild the project from scratch.
Continue on the existing structure.
Keep Clean Architecture.
Do not put SQL inside JavaFX controllers.
Controllers must call Services only.
Services must call Repositories.
Repositories handle SQL only.

==================================================
MAIN GOAL
==================================================

Complete the real desktop application screens and connect them to the backend logic.

Make the JavaFX design modern, interactive, elegant, and visually attractive.

The UI should feel like a professional desktop SaaS dashboard, not a basic JavaFX form.

==================================================
DESIGN REQUIREMENTS
==================================================

Improve the whole JavaFX UI to be modern and interactive:

Use:
- Sidebar navigation
- Top header with search, profile avatar, notification icon
- Rounded cards
- Smooth hover effects
- Modern buttons
- Gradient CTA buttons
- Clean tables
- Charts
- Modal dialogs
- Empty states
- Loading states
- Error messages
- Success messages
- Form validation
- Modern spacing and alignment

Use the existing color palette:
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
Add interactive CSS effects:
- Button hover
- Card hover
- Sidebar selected item
- Table row hover
- Input focus
- Animated loading spinner
- Smooth transitions where JavaFX supports it

==================================================
STUDENT MODULE
==================================================

Complete Student UI screens:

1. Student Dashboard
- Welcome card
- Featured courses
- Continue learning
- My progress summary
- Recent notifications

2. Course Catalog
- Search input
- Category filter
- Level filter
- Price filter
- Course cards/grid
- Course details button

3. Course Details
- Thumbnail
- Title
- Instructor info
- Description
- Price
- Rating
- Lessons preview
- Add-ons:
  Certificate, Quiz, Resources, Mentor Support
- Enroll Now button

4. Checkout
- Course summary
- Payment method cards:
  Visa / Wallet / PayPal
- Total price
- Confirm payment
- Use PaymentStrategy

5. My Courses
- Enrolled course cards
- Progress bars
- Continue learning button

6. Course Player
- Lesson list
- Video placeholder
- Resources list
- Mark lesson completed
- Locked lesson state using Proxy Pattern

7. Quiz Screen
- Questions
- Options
- Submit quiz
- Score result

8. Certificates
- Certificate list
- Course name
- Certificate number
- Issue date

9. Notifications
- Notification list
- Read/unread state

10. Student Profile
- Profile info
- Interests
- Learning goals
- Payment history

==================================================
INSTRUCTOR MODULE
==================================================

Complete Instructor UI screens:

1. Instructor Dashboard
- Total courses
- Total students
- Total revenue
- Average rating
- Recent activity

2. My Courses
- Courses table/cards
- Status: Draft / Pending / Approved / Rejected
- Edit button
- View students button
- Analytics button

3. Add Course
- Course title
- Category
- Level
- Price
- Description
- Thumbnail upload field
- Save draft
- Submit for approval
- Use CoursePublishFacade

4. Edit Course
- Edit course data
- Manage lessons
- Manage resources
- Manage quiz
- Manage add-ons using Decorator Pattern

5. Lesson Manager
- Add lesson
- Edit lesson
- Delete lesson
- Reorder lessons

6. Quiz Manager
- Create quiz
- Add questions
- Add options
- Mark correct answer
- Set passing score

7. Enrolled Students
- Table:
  Student name, email, course, progress, payment status

8. Revenue Analytics
- Charts
- Revenue summary
- Enrollment performance

9. Wallet & Withdraw
- Available balance
- Pending balance
- Transaction history
- Withdraw request form
- Use Withdraw Strategy if implemented

10. Instructor Notifications
- New enrollments
- Course approval/rejection
- New reviews
- Withdrawal updates

==================================================
ADMIN MODULE
==================================================

Complete Admin UI screens:

1. Admin Dashboard
- Total users
- Total students
- Total instructors
- Total courses
- Total payments
- Platform revenue
- Activity logs preview

2. Users Management
- Data table
- Search users
- Filter by role
- Role badges
- Activate/deactivate user
- View details

3. Courses Management
- Courses table
- Filter by status/category
- Preview course
- Approve course
- Reject course with reason
- Archive/delete course
- Use CourseApprovalFacade

4. Categories Management
- Add category
- Edit category
- Delete/deactivate category

5. Payments Management
- Payments table
- Filter by method/status/date
- Amount
- Student
- Course
- Payment method

6. Withdrawal Requests
- Instructor withdrawal requests
- Approve/reject withdrawal
- Show amount and method

7. Reports
- User growth report
- Revenue report
- Course performance report
- Enrollment report
- Export report button
- Use Report Strategy

8. Roles & Permissions
- View roles
- Show permissions
- Restrict access using Proxy Pattern

9. Activity Logs
- Login events
- Admin actions
- Course approvals
- Payment events

10. Admin Notifications
- Pending courses
- Payment issues
- New instructors
- System alerts

==================================================
BACKEND CONNECTION
==================================================

Connect the UI to the existing backend layers.

Required:
- Add missing repository methods
- Add missing service methods
- Connect all buttons to real services
- Load real data from Supabase PostgreSQL
- Use PreparedStatement
- Use ResultSetMapper
- Use custom exceptions or Result style
- Show loading and errors in UI

Do not hardcode data except temporary fallback demo data if database is empty.

==================================================
DESIGN PATTERNS USAGE
==================================================

Use existing pattern classes in real flows:

Factory:
- Registration/Login role creation and role loading

Strategy:
- Checkout payment method selection
- Reports type selection if available

Observer:
- Notifications after enrollment, payment, course approval, course rejection

Facade:
- Enrollment flow
- Course publish flow
- Course approval flow
- Payment flow

Decorator:
- Course add-ons pricing:
  Certificate + Quiz + Resources + Mentor Support

Proxy:
- Course lesson access
- Instructor course ownership
- Admin permissions

Singleton:
- DatabaseConnection
- SessionManager
- AppConfig

Adapter:
- PaymentGatewayAdapter
- Storage/File upload adapter
- Report export adapter

==================================================
FILES TO CREATE OR UPDATE
==================================================

Create/update:
- FXML files for all missing screens
- Controllers for all screens
- Services for missing features
- Repository methods
- CSS files
- Reusable UI components
- Dialog components
- Table components
- Card components

Suggested reusable UI components:
- DashboardCard
- CourseCard
- UserBadge
- StatusBadge
- LoadingOverlay
- EmptyState
- ConfirmDialog
- NotificationItem
- SidebarItem
- ModernTable

==================================================
QUALITY RULES
==================================================

Before finishing:
- Run mvn clean compile
- Fix all compile errors
- Ensure JavaFX app launches
- Ensure navigation works
- Ensure Login redirects by role
- Ensure each sidebar item opens its screen
- Ensure controllers do not contain SQL
- Ensure UI is not plain/default JavaFX
- Ensure CSS is applied everywhere
- Ensure README is updated with Phase 2 details

==================================================
FINAL OUTPUT
==================================================

At the end, provide:
1. List of files created
2. List of files updated
3. Screens completed
4. Backend services completed
5. Design patterns connected to real flows
6. Any remaining TODOs
7. Build result