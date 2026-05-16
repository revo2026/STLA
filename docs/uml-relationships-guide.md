# STLA Desktop — UML Relationships Guide

This guide defines **which PlantUML arrow to use** and **why**, with examples taken from the STLA codebase.

## Arrow Quick Reference

| Relationship | PlantUML | Meaning | STLA Example |
|--------------|----------|---------|--------------|
| Generalization (inheritance) | `Parent <|-- Child` | `extends` class | `CourseDecorator <|-- CertificateDecorator` |
| Realization (implements) | `Interface <|.. Class` | `implements` interface | `PaymentStrategy <|.. VisaPaymentStrategy` |
| Composition | `Whole *-- Part` | Strong ownership; part dies with whole | `Course *-- CourseSection` |
| Aggregation | `Whole o-- Part` | Weak “has-a”; part can outlive whole | `Instructor o-- Course` |
| Association | `A --> B` | Persistent reference / uses | `CourseService --> CourseRepositoryImpl` |
| Dependency | `A ..> B` | Temporary use (parameter, local) | `CheckoutController ..> PaymentStrategyFactory` |

---

## 1. Inheritance / Generalization — `<|--`

**Use when:** A Java class `extends` another class (including `abstract class`).

```plantuml
abstract class CourseDecorator
class CertificateDecorator
CourseDecorator <|-- CertificateDecorator
```

**STLA examples:**

- `CertificateDecorator extends CourseDecorator`
- `QuizDecorator extends CourseDecorator`
- `MentorSupportDecorator extends CourseDecorator`

**Not used for:** `implements` (use realization). Profile does **not** extend Student/Instructor/Admin.

---

## 2. Realization — `<|..`

**Use when:** A class `implements` an interface.

```plantuml
interface PaymentStrategy <<Strategy>>
class VisaPaymentStrategy <<ConcreteStrategy>>
PaymentStrategy <|.. VisaPaymentStrategy
```

**STLA examples:**

| Interface | Implementations |
|-----------|-----------------|
| `PaymentStrategy` | `VisaPaymentStrategy`, `WalletPaymentStrategy`, `PayPalPaymentStrategy` |
| `WithdrawStrategy` | `VisaWithdrawStrategy`, `DigitalWalletWithdrawStrategy` |
| `CourseRepository` | `CourseRepositoryImpl` |
| `EventListener` | `NotificationObserver` |
| `CourseComponent` | `BaseCourse`, `CourseDecorator` |
| `ReportExportAdapter` | `CsvReportExportAdapter` |
| `PaymentGatewayAdapter` | `SimulatedPaymentGateway` |

---

## 3. Composition — `*--`

**Use when:**

- Child cannot exist without parent in the domain model
- Deleting parent implies deleting children (DB FK `ON DELETE CASCADE` or logical containment)

```plantuml
Course "1" *-- "0..*" CourseSection
CourseSection "1" *-- "0..*" CourseLesson
Quiz "1" *-- "1..*" QuizQuestion
QuizQuestion "1" *-- "2..*" QuizOption
```

**STLA domain examples:**

| Whole | Part | Reason |
|-------|------|--------|
| `Course` | `CourseSection` | Sections belong to one course |
| `CourseSection` | `CourseLesson` | Lessons belong to one section |
| `Quiz` | `QuizQuestion` | Questions belong to one quiz |
| `QuizQuestion` | `QuizOption` | Options belong to one question |

**Note:** In Java, `CourseSection` holds `List<CourseLesson>` in memory — composition at model level.

---

## 4. Aggregation — `o--`

**Use when:**

- “Has-a” relationship
- Part has its own lifecycle (separate table/entity)
- Deleting whole does not always delete part

```plantuml
Instructor "1" o-- "0..*" Course
Student "1" o-- "0..*" Enrollment
Admin "1" o-- "0..*" ActivityLog
```

**STLA examples:**

| Whole | Part | Reason |
|-------|------|--------|
| `Instructor` | `Course` | Course tied to instructor but is independent entity |
| `Student` | `Enrollment` | Enrollment record survives as historical data |
| `Enrollment` | `Course` | Association via `courseId` (also shown as `-->`) |

---

## 5. Association — `-->`

**Use when:**

- One class holds a field of another type
- One layer routinely calls another
- Repository returns domain model

```plantuml
Course --> Category : categoryId
CourseService --> CourseRepositoryImpl
EnrollmentFacade --> PaymentService
```

**Architecture associations:**

```
UI Controller --> Service
Service --> Repository (interface or impl)
Service --> Domain Model
Facade --> Service / Repository / EventBus
```

---

## 6. Dependency — `..>`

**Use when:**

- Used only inside a method (parameter, local variable, factory call)
- No long-lived field reference

```plantuml
CheckoutController ..> PaymentStrategyFactory
CheckoutController ..> EnrollmentFacade
EnrollmentFacade ..> PaymentStrategy
```

**Why not association?** `CheckoutController` creates `PaymentStrategy` at pay time via factory; it does not store strategy as a field permanently.

---

## 7. Facade Relationships

Facade **coordinates** subsystems — typically **association** arrows to services/repos, **dependency** to strategies.

```plantuml
class EnrollmentFacade <<Facade>>
EnrollmentFacade --> PaymentService
EnrollmentFacade ..> PaymentStrategy
```

```plantuml
class CoursePublishFacade <<Facade>>
CoursePublishFacade --> CourseRepositoryImpl
CoursePublishFacade --> InstructorRepositoryImpl
CoursePublishFacade --> NotificationService
CoursePublishFacade --> EventBus
```

---

## 8. Decorator Relationships

```plantuml
interface CourseComponent <<Component>>
class BaseCourse <<ConcreteComponent>>
abstract class CourseDecorator <<Decorator>>
class CertificateDecorator <<ConcreteDecorator>>

CourseComponent <|.. BaseCourse
CourseComponent <|.. CourseDecorator
CourseDecorator o-- CourseComponent : wrapped
CourseDecorator <|-- CertificateDecorator
```

| Arrow | Why |
|-------|-----|
| `<\|..` | Component interface realized by base and decorator |
| `o--` | Decorator holds wrapped `CourseComponent` |
| `<\|--` | Concrete decorator extends abstract decorator |

---

## 9. Observer Relationships

```plantuml
class EventBus <<Subject>>
interface EventListener <<Observer>>
class NotificationObserver <<ConcreteObserver>>
record AppEvent

EventBus o-- EventListener : subscribers
EventListener <|.. NotificationObserver
EventBus ..> AppEvent : publish
NotificationObserver --> NotificationService
```

| Arrow | Why |
|-------|-----|
| `o--` | EventBus holds list of listeners |
| `<\|..` | NotificationObserver implements EventListener |
| `..>` | `publish(AppEvent)` is operation dependency |

---

## 10. Singleton — Notes (No Special Arrow)

Document with PlantUML `note`:

```plantuml
class SessionManager <<Singleton>>
note right of SessionManager
  - private static volatile instance
  - private constructor
  - getInstance()
  - double-checked locking
end note
```

**STLA singletons:**

| Class | Package |
|-------|---------|
| `AppConfig` | `com.stla.app` |
| `DatabaseConnection` | `com.stla.core.database` |
| `SessionManager` | `com.stla.core.session` |
| `NavigationManager` | `com.stla.core.navigation` |
| `EventBus` | `com.stla.patterns.observer` |
| `ThemeManager` | `com.stla.ui.components` |
| `SupabaseStorageService` | `com.stla.services` |

---

## Common Mistakes

| Wrong | Correct | Reason |
|-------|---------|--------|
| `Service <|-- Repository` | `RepositoryImpl <|.. Repository` | Repository is interface, not parent class |
| `Course o-- Lesson` (skip section) | `Course *-- Section *-- Lesson` | Lessons belong to sections |
| `Controller <|-- Service` | `Controller --> Service` | Controller does not extend service |
| `Profile <\|-- Student` | `Profile <-- Student : profileId` | No inheritance in code |
| Composition for Instructor–Course | Aggregation | Course is separate aggregate root |

---

## Stereotypes Used in STLA Diagrams

| Stereotype | Applied To |
|------------|------------|
| `<<Facade>>` | `EnrollmentFacade`, `CoursePublishFacade` |
| `<<Strategy>>` | `PaymentStrategy`, `WithdrawStrategy` |
| `<<ConcreteStrategy>>` | `VisaPaymentStrategy`, etc. |
| `<<Decorator>>` | `CourseDecorator` |
| `<<Observer>>` | `EventListener`, `NotificationObserver` |
| `<<Subject>>` | `EventBus` |
| `<<Singleton>>` | Session/config/DB classes |
| `<<Adapter>>` | `ReportExportAdapter`, `SupabaseStorageAdapter` |
| `<<Proxy>>` | `CourseAccessProxy` |

---

*See [design-patterns-uml.md](./design-patterns-uml.md) for pattern-specific diagrams and [full-project-uml.md](./full-project-uml.md) for architecture.*
