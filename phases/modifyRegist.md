Update the STLA JavaFX Desktop Application Registration system.

IMPORTANT:
Do NOT rebuild the project.
Modify the existing registration flow and backend integration only.

==================================================
GOAL
==================================================

Improve the Registration system so users can register ONLY as:

1. Student
2. Instructor

Admin registration must NOT be available publicly.

The registration process must save data correctly into the related database tables in Supabase PostgreSQL.

==================================================
ROLE SELECTION
==================================================

In the Registration screen:

Replace any old role system with ONLY:

[ I'm a Student ]
[ I'm an Instructor ]

Use modern selectable cards/buttons.

Selected role should:
- Highlight with STLA primary gradient
- Animate slightly on selection
- Update registration form dynamically

Default selection:
Student

==================================================
IMPORTANT DATABASE RULE
==================================================

When registering:

If role = Student:
→ Save user data into:
- profiles table
- students table

If role = Instructor:
→ Save user data into:
- profiles table
- instructors table

DO NOT insert instructors into students table.
DO NOT insert students into instructors table.

==================================================
DATABASE STRUCTURE EXPECTED
==================================================

Use the existing Supabase PostgreSQL schema.

Expected logic:

1. profiles table:
Stores:
- id
- full_name
- email
- password_hash
- role
- avatar_url
- created_at

2. students table:
Stores:
- profile_id
- interests
- learning_goals
- skill_level

3. instructors table:
Stores:
- profile_id
- title
- expertise
- years_experience
- bio
- rating
- total_students

==================================================
REGISTRATION FLOW
==================================================

Step 1:
Choose Role:
- Student
- Instructor

Step 2:
Basic Information:
- Full Name
- Email
- Password
- Confirm Password

Step 3:
Dynamic Role-Specific Form

==================================================
STUDENT REGISTRATION FORM
==================================================

If role = Student:

Show fields:
- Interests
- Learning Goals
- Skill Level:
  Beginner / Intermediate / Advanced

After submit:
1. Create profile
2. Create student record linked using profile_id
3. Role in profile = STUDENT

==================================================
INSTRUCTOR REGISTRATION FORM
==================================================

If role = Instructor:

Show fields:
- Professional Title
- Expertise
- Years of Experience
- Short Bio

Optional:
- Upload avatar/profile image

After submit:
1. Create profile
2. Create instructor record linked using profile_id
3. Role in profile = INSTRUCTOR

==================================================
ADMIN RULE
==================================================

Completely REMOVE Admin from public registration UI.

Admins must only be created manually through:
- Database
OR
- Admin management system

==================================================
BACKEND REQUIREMENTS
==================================================

Use:
- AuthService
- UserFactory
- Repository layer
- PreparedStatement
- Existing Clean Architecture

Do NOT:
- Put SQL inside controllers
- Put registration logic inside UI

==================================================
FACTORY PATTERN USAGE
==================================================

Use Factory Pattern properly:

- StudentFactory creates Student registration flow
- InstructorFactory creates Instructor registration flow

Factory should:
- Validate role
- Build correct model
- Save correct DB records

==================================================
VALIDATION RULES
==================================================

Validate:
- Email uniqueness
- Password length
- Password confirmation
- Required fields
- Experience numeric value

Show:
- Inline validation messages
- Toast notifications
- Success animation

==================================================
UI/UX REQUIREMENTS
==================================================

Use the STLA desktop modern UI style:
- Rounded cards
- Purple gradients
- Soft shadows
- Smooth animations
- Modern forms
- Hover effects
- Fade transitions

Role selection cards should feel interactive and modern.

==================================================
SUCCESS FLOW
==================================================

After successful registration:

If Student:
→ Redirect to Student Dashboard

If Instructor:
→ Redirect to Instructor Dashboard

Also:
- Initialize SessionManager
- Store logged-in user session

==================================================
FILES TO UPDATE
==================================================

Update/Create:
- Register.fxml
- RegisterController.java
- AuthService.java
- StudentRepositoryImpl.java
- InstructorRepositoryImpl.java
- ProfileRepositoryImpl.java
- UserFactory.java
- StudentFactory.java
- InstructorFactory.java
- Related DTOs/models if needed

==================================================
FINAL CHECK
==================================================

Before finishing:
- Ensure Student registration inserts correctly
- Ensure Instructor registration inserts correctly
- Ensure profile role saved correctly
- Ensure Admin registration removed
- Ensure role-based redirect works
- Ensure build success

Run:
mvn clean compile
mvn javafx:run