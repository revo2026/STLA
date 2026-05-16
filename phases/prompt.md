Update the STLA JavaFX Desktop Application Notification System for Admin and Instructor roles.

IMPORTANT:
Do NOT rebuild the project.
Extend and improve the existing Notification system only.
Keep Clean Architecture, JavaFX architecture, Supabase PostgreSQL integration, EventBus/Observer pattern, and STLA premium desktop UI.

Do NOT put SQL inside controllers.
Controllers must call Services only.
Services must call Repositories only.

==================================================
MAIN GOAL
==================================================

Create a FULL professional Notification Center for:
1. Admin
2. Instructor

All important system events must generate notifications automatically.

Notifications must:
- Save in database
- Display correctly in UI
- Support read/unread state
- Support filtering
- Support real-time refresh if possible
- Support notification counter badge

==================================================
ADMIN NOTIFICATIONS
==================================================

Admin should receive notifications for:

==================================================
PAYMENTS
==================================================

- New course payment received
- Payment failed
- Refund processed
- Platform commission added

Examples:
"New payment received for course Java Masterclass."
"Payment failed for student Ahmed."
"Platform commission added: $20."

==================================================
INSTRUCTOR EVENTS
==================================================

- New instructor registration
- Instructor submitted verification documents
- Instructor updated verification documents
- Instructor withdrawal completed
- Instructor account verified
- Instructor account rejected

==================================================
COURSE EVENTS
==================================================

- New course submitted for review
- Course updated by instructor
- Course approved
- Course rejected
- Course deleted

==================================================
STUDENT EVENTS
==================================================

- Student enrolled in course
- Student completed course
- Student reported issue if support exists

==================================================
SYSTEM EVENTS
==================================================

- New user registered
- Database/storage error if logging exists
- New category added
- High revenue milestone reached

==================================================
ADMIN NOTIFICATION UI
==================================================

Admin Notification Center should show:

- Notification icon
- Title
- Message
- Time/date
- Type badge
- Read/unread state

Actions:
- Mark as read
- Mark all as read
- Open related entity
- Delete notification optional

Filters:
- All
- Payments
- Courses
- Instructors
- Students
- System

==================================================
INSTRUCTOR NOTIFICATIONS
==================================================

Instructor should receive notifications for:

==================================================
REVENUE & WALLET
==================================================

- Revenue added to wallet
- Withdrawal completed
- New course purchase
- Student enrolled in course

Examples:
"You earned $80 from Java Course."
"Withdrawal completed successfully."

==================================================
COURSE EVENTS
==================================================

- Course approved
- Course rejected with reason
- Course review pending
- Course published successfully

==================================================
CONTENT EVENTS
==================================================

- Student completed lesson
- Student completed course
- Student passed quiz
- Student failed quiz
- New review/rating added

==================================================
QUIZ EVENTS
==================================================

- Quiz submitted
- Quiz attempts completed
- New quiz available if collaborative

==================================================
SYSTEM EVENTS
==================================================

- Verification approved
- Verification rejected
- Profile updated successfully

==================================================
INSTRUCTOR NOTIFICATION UI
==================================================

Instructor Notification Center should show:

- Notification icon
- Title
- Message
- Course reference if available
- Student reference if available
- Amount if financial
- Time/date
- Read/unread state

Filters:
- Revenue
- Courses
- Students
- Quizzes
- Wallet
- System

==================================================
DATABASE REQUIREMENTS
==================================================

Use existing notifications table.

Ensure fields exist:
- id
- profile_id
- title
- message
- notification_type
- is_read
- created_at
- action_url optional
- metadata jsonb optional

If missing:
Create migration:
notifications_upgrade.sql

==================================================
OBSERVER / EVENTBUS
==================================================

Use Observer Pattern properly.

When events happen:
- Publish AppEvent
- NotificationObserver listens
- NotificationService creates notification row

Do NOT manually insert notifications in every controller.

==================================================
EVENT TYPES
==================================================

Create/extend AppEventType:

Admin events:
- PAYMENT_RECEIVED
- PAYMENT_FAILED
- PLATFORM_COMMISSION_ADDED
- INSTRUCTOR_REGISTERED
- INSTRUCTOR_VERIFICATION_SUBMITTED
- COURSE_SUBMITTED
- COURSE_APPROVED
- COURSE_REJECTED
- STUDENT_REGISTERED
- STUDENT_ENROLLED

Instructor events:
- COURSE_PURCHASED
- REVENUE_ADDED
- WITHDRAWAL_COMPLETED
- COURSE_APPROVED
- COURSE_REJECTED
- COURSE_COMPLETED_BY_STUDENT
- QUIZ_PASSED
- QUIZ_FAILED
- PROFILE_UPDATED
- VERIFICATION_APPROVED
- VERIFICATION_REJECTED

==================================================
NOTIFICATION BADGES & ICONS
==================================================

Use professional icons/colors:

Payments:
- Green money icon

Rejected:
- Red warning icon

Courses:
- Purple course icon

Students:
- Blue student icon

Wallet:
- Gold wallet icon

Verification:
- Orange shield/check icon

==================================================
REAL-TIME UPDATE
==================================================

If possible:
- Refresh notification badge automatically
- Update unread count dynamically
- Show popup toast for new notifications

==================================================
TOP BAR NOTIFICATION ICON
==================================================

Admin and Instructor top navigation bar should show:
- Bell icon
- Unread counter badge
- Dropdown preview
- Click opens Notification Center

Dropdown should show:
- Latest 5 notifications
- Mark all read button
- View all notifications button

==================================================
NOTIFICATION ACTIONS
==================================================

Notifications can optionally open:
- Course page
- Payment page
- Instructor profile
- Wallet page
- Quiz page
- Student profile

Use:
action_url or route metadata

==================================================
BACKEND REQUIREMENTS
==================================================

Use:
- NotificationService
- EventBus
- NotificationObserver
- AppEvent
- AppEventType

Repositories:
- NotificationRepository
- NotificationRepositoryImpl

Methods:
- createNotification(...)
- getNotifications(profileId)
- getUnreadNotifications(profileId)
- markAsRead(notificationId)
- markAllAsRead(profileId)
- deleteNotification(notificationId)

==================================================
UI/UX REQUIREMENTS
==================================================

Use STLA premium desktop design:
- Rounded cards
- Purple gradients
- Smooth hover animations
- Notification badges
- Unread highlight
- Toast popup notifications
- Dark mode support
- Skeleton loading states
- Empty states

Notification cards should feel modern like:
- Discord
- LinkedIn
- Facebook
- Coursera

==================================================
FILES TO CREATE/UPDATE
==================================================

Create/Update:

FXML:
- AdminNotifications.fxml
- InstructorNotifications.fxml
- NotificationDropdown.fxml

Controllers:
- AdminNotificationsController.java
- InstructorNotificationsController.java
- NotificationDropdownController.java

Backend:
- NotificationService.java
- NotificationRepository.java
- NotificationRepositoryImpl.java
- EventBus.java
- AppEvent.java
- AppEventType.java
- NotificationObserver.java

Models:
- Notification.java

CSS:
- notifications.css
- cards.css
- topbar.css
- animations.css
- dark-theme.css

==================================================
FINAL CHECK
==================================================

Before finishing:

Admin:
- Payment notifications work
- Course notifications work
- Instructor notifications work
- Unread counter works
- Mark as read works

Instructor:
- Revenue notifications work
- Course approval/rejection notifications work
- Student progress notifications work
- Wallet notifications work

UI:
- Notification dropdown works
- Dark mode works
- Toasts work
- No broken layout

Run:
mvn clean compile
mvn test
mvn javafx:run

Final output:
1. Files created
2. Files updated
3. Admin notification workflow
4. Instructor notification workflow
5. EventBus flow explanation
6. Database fields used
7. Build result