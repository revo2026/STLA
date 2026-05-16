Continue the STLA JavaFX Desktop Application project.

Phase 1, 2, 3, and 4 are complete with BUILD SUCCESS.

Now start Phase 5:
Testing, Bug Fixing, Demo Data, and Final Presentation Readiness.

Do NOT rebuild the project from scratch.
Do NOT change the architecture unless necessary.
Focus on stability, testing, final polish, and making the app ready for university presentation.

Required tasks:

1. Full Flow Testing
Test and verify these flows:
- Login as Student
- Login as Instructor
- Login as Admin
- Logout
- Role-based navigation
- Sidebar navigation for all roles
- Dark/light theme switching
- Toast notifications
- Charts loading
- Form validation
- Error handling

2. Student Flow
Verify:
- Dashboard loads
- Catalog search/filter works
- Course cards display correctly
- Course details opens
- Checkout payment method selection works
- Enrollment flow uses EnrollmentFacade
- Payment uses Strategy Pattern
- My Courses loads enrolled courses
- Progress bars display correctly
- Certificates screen works
- Notifications read/unread works
- Profile loads correctly

3. Instructor Flow
Verify:
- Dashboard stats load
- My courses load
- Course status badges work
- Add/edit course forms validate inputs
- Course publishing uses CoursePublishFacade
- Enrolled students table works
- Wallet and withdraw screens work
- Analytics charts load
- Notifications work

4. Admin Flow
Verify:
- Dashboard KPIs load
- Users table search/filter works
- Activate/deactivate user works
- Courses approve/reject works using CourseApprovalFacade
- Categories add/deactivate works
- Payments table loads
- Withdrawals approve/reject works
- Activity logs load
- Reports/charts work

5. JUnit Tests
Add or complete tests for:
- Factory Pattern
- Strategy Pattern
- Observer Pattern
- Facade Pattern
- Decorator Pattern
- Proxy Pattern
- Singleton Pattern
- Adapter Pattern
- AuthService
- CourseService
- DashboardService
- Repository mapping methods

6. Demo Data
Create a demo seed SQL file:
demo_seed.sql

It should include:
- 2 students
- 2 instructors
- 1 admin
- 5 categories
- 8 courses
- lessons for each course
- enrollments
- payments
- notifications
- reviews
- certificates
- wallet transactions
- activity logs

Make sure demo data matches the existing schema.

7. README Final Update
Update README with:
- Project overview
- Tech stack
- Architecture
- How to configure .env
- How to run with Maven
- How to seed demo data
- User roles
- Screens/features
- Design patterns table
- Folder structure
- Troubleshooting section
- Presentation notes

8. UI Bug Fixing
Fix:
- Any overflow
- Broken layouts
- Missing CSS classes
- Bad spacing
- Table width issues
- Dark mode styling issues
- Empty states
- Missing icons
- Broken buttons

9. Final Build Check
Run:
mvn clean compile
mvn test
mvn javafx:run

Fix all errors before finishing.

Final output must include:
- Files created
- Files updated
- Tests added
- Demo data summary
- Bugs fixed
- Final build result
- Remaining TODOs if any