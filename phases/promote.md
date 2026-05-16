Update the STLA JavaFX Desktop Application Student Learning Progress and Notification System.

IMPORTANT:
Do NOT rebuild the project.
Modify the existing Student Course Player, Progress, Quiz, Payment, Lesson, and Notification systems only.
Keep Clean Architecture and the current STLA premium JavaFX design.

==================================================
MAIN GOAL
==================================================

Implement a complete student learning tracking system.

Student should:
1. Watch lesson video
2. When video is fully watched, show "Complete Lesson" button
3. When clicked, mark lesson as completed
4. Update course progress percentage
5. Update My Courses progress bar
6. Create notification for completed lesson
7. Track all important student events in Notifications

==================================================
VIDEO COMPLETION LOGIC
==================================================

In Student Course Player:

Track video playback progress using JavaFX MediaPlayer.

Rules:
- If student watches 90% or more of the video:
  show button:
  "Complete Lesson"
- Button should stay hidden until video reaches completion threshold
- If lesson already completed:
  show badge:
  "Completed"

When student clicks Complete Lesson:
- Save lesson completion in DB
- Update lesson_progress.completed = true
- Set completed_at = now()
- Update watched_seconds
- Update student_course_progress
- Show success toast
- Add notification

==================================================
COURSE PROGRESS CALCULATION
==================================================

Calculate course progress:

progress_percent =
(completed_lessons / total_published_lessons) * 100

Update:
- student_course_progress.progress_percent
- student_course_progress.lessons_completed
- student_course_progress.total_lessons
- student_course_progress.last_lesson_id
- student_course_progress.last_accessed_at

If progress reaches 100%:
- Mark enrollment status = completed
- Set enrollments.completed_at = now()
- Trigger certificate if course has certificate addon
- Notify student:
  "Congratulations! You completed the course."

==================================================
QUIZ PROGRESS INTEGRATION
==================================================

If a quiz is attached to a lesson:
- Student must pass quiz if quiz is required
- After passing quiz:
  - Mark quiz as completed
  - Update progress if applicable
  - Notify student:
    "You passed the quiz: {quizTitle}"

If quiz failed:
- Notify student:
  "Quiz attempt failed. Try again if attempts remain."

==================================================
NOTIFICATION SYSTEM
==================================================

Improve Student Notifications screen.

Show all student-related events:

Payment:
- Payment successful
- Payment failed
- Refund processed if exists

Enrollment:
- Course enrollment successful
- Enrollment cancelled if exists

Learning:
- Lesson completed
- Course progress updated
- Course completed

Quiz:
- New quiz available
- Quiz passed
- Quiz failed
- Quiz attempt submitted

Course Updates:
- New lesson added
- New resource added
- New quiz added
- Course updated by instructor

Certificate:
- Certificate issued

==================================================
NOTIFICATION UI REQUIREMENTS
==================================================

Student Notifications screen should show:

- Notification title
- Body/message
- Type icon
- Time/date
- Read/unread state
- Reference course/lesson/quiz if available

Features:
- Mark as read
- Mark all as read
- Filter by type:
  All / Payment / Lesson / Quiz / Course / Certificate
- Unread counter
- Empty state
- Toast when notification opens

Use icons/badges:
- Payment success = green
- Payment failed = red
- Lesson completed = blue
- Quiz passed = green
- Quiz failed = red
- Course update = purple
- Certificate = gold

==================================================
EVENTS TO CREATE NOTIFICATIONS FOR
==================================================

Add notifications for these backend events:

1. Payment Success
2. Payment Failed
3. Enrollment Success
4. Lesson Completed
5. Course Progress Updated
6. Course Completed
7. Quiz Submitted
8. Quiz Passed
9. Quiz Failed
10. New Lesson Added
11. New Resource Added
12. New Quiz Added
13. Certificate Issued

==================================================
OBSERVER PATTERN REQUIREMENT
==================================================

Use Observer/EventBus properly.

When an event happens:
- Publish AppEvent
- Notification observer receives it
- NotificationService creates notification row in DB

Do not manually duplicate notification logic everywhere.

==================================================
DATABASE TABLES
==================================================

Use existing:
- lesson_progress
- student_course_progress
- enrollments
- notifications
- quiz_attempts
- issued_certificates

If needed, add migration:
student_progress_notifications_migration.sql

Possible additions:
- lesson_progress.watched_percent
- lesson_progress.completed_by_student
- notifications.action_url
- notifications.metadata jsonb

==================================================
BACKEND REQUIREMENTS
==================================================

Use:
- LearningProgressService
- LessonService
- QuizService
- NotificationService
- EnrollmentService
- CertificateService
- EventBus
- CourseAccessProxy

Add/update methods:

LearningProgressService:
- recordWatchProgress(studentId, lessonId, watchedSeconds, totalSeconds)
- canCompleteLesson(studentId, lessonId)
- markLessonCompleted(studentId, lessonId)
- recalculateCourseProgress(enrollmentId)
- completeCourseIfEligible(enrollmentId)

NotificationService:
- createStudentNotification(...)
- getStudentNotifications(studentProfileId)
- markAsRead(notificationId)
- markAllAsRead(studentProfileId)
- countUnread(studentProfileId)

LessonService:
- getLessonProgress(studentId, lessonId)
- getTotalPublishedLessons(courseId)

==================================================
STUDENT COURSE PLAYER UI
==================================================

Improve UI:

Video Area:
- Progress watched indicator
- "Complete Lesson" button appears when eligible
- Completed badge
- Next lesson button
- Quiz required warning if needed

Lesson Sidebar:
- Completed lessons show check icon
- Current lesson highlighted
- Locked lessons show lock icon
- Progress percent per lesson if available

Course Header:
- Course progress bar
- Completed lessons count
- Total lessons count

==================================================
MY COURSES PAGE
==================================================

Update My Courses:

Each enrolled course card shows:
- Progress bar
- Completed lessons / total lessons
- Last accessed lesson
- Continue Learning button
- Completed badge if 100%

==================================================
PAYMENT NOTIFICATIONS
==================================================

In PaymentService / EnrollmentFacade:

If payment succeeds:
- Create payment success notification
- Create enrollment success notification

If payment fails:
- Create payment failed notification with failure reason

==================================================
INSTRUCTOR COURSE UPDATE NOTIFICATIONS
==================================================

When instructor adds:
- New lesson
- New resource
- New quiz

Notify all enrolled students in that course.

Use EventBus:
- NEW_LESSON_ADDED
- NEW_RESOURCE_ADDED
- NEW_QUIZ_ADDED

==================================================
CERTIFICATE NOTIFICATIONS
==================================================

When course completed and certificate issued:
- Create notification:
  "Your certificate is ready."

Show:
- Certificate button/link in notification if possible

==================================================
UI/UX STYLE
==================================================

Use STLA premium style:
- Rounded cards
- Purple gradients
- Smooth hover
- Progress animations
- Toast notifications
- Success animations
- Dark mode support
- Modern badges

==================================================
FILES TO CREATE/UPDATE
==================================================

Update/Create:

Student UI:
- StudentCoursePlayer.fxml
- StudentCoursePlayerController.java
- MyCourses.fxml
- MyCoursesController.java
- Notifications.fxml
- NotificationsController.java

Backend:
- LearningProgressService.java
- LessonService.java
- NotificationService.java
- EnrollmentService.java
- PaymentService.java
- QuizService.java
- CertificateService.java

Repositories:
- LessonProgressRepository.java
- StudentCourseProgressRepository.java
- NotificationRepository.java
- EnrollmentRepository.java

Patterns:
- EventBus.java
- AppEvent.java
- NotificationObserver.java

CSS:
- player.css
- progress.css
- notifications.css
- cards.css
- buttons.css
- animations.css
- dark-theme.css

==================================================
FINAL CHECK
==================================================

Before finishing:

Video:
- Watched progress is tracked
- Complete Lesson button appears after 90%
- Completed lesson saves in DB
- Course progress updates correctly

Progress:
- My Courses progress updates
- Course Player progress updates
- Course completion works

Notifications:
- Payment success notification works
- Payment failed notification works
- Lesson completed notification works
- Quiz passed/failed notification works
- New lesson/resource/quiz notifications work
- Certificate notification works
- Read/unread works

Access:
- Locked content stays locked until enrollment
- Completed lessons show correct state

Final output:
1. Files created
2. Files updated
3. Video completion logic
4. Progress calculation flow
5. Notification event flow
6. Database fields used
7. Build result