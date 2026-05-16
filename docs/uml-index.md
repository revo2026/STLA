# STLA Desktop — UML Documentation Index

This folder contains **read-only architecture and design-pattern documentation** for the STLA JavaFX Desktop project (`com.stla`). No source code was modified to produce these files.

## Files in This Documentation Set

| File | Purpose |
|------|---------|
| [uml-relationships-guide.md](./uml-relationships-guide.md) | Reference for PlantUML arrow types, when to use each, and STLA examples |
| [design-patterns-uml.md](./design-patterns-uml.md) | Deep UML for each GoF-style pattern found in `com.stla.patterns` and related UI factories |
| [full-project-uml.md](./full-project-uml.md) | Layered architecture, package map, domain model, controllers, services, repositories |

## Quick Navigation

- **Patterns** → [design-patterns-uml.md](./design-patterns-uml.md)
- **Architecture** → [full-project-uml.md](./full-project-uml.md)
- **Arrow rules** → [uml-relationships-guide.md](./uml-relationships-guide.md)

## Patterns Found (Summary)

| Pattern | Status in Codebase | Primary Package |
|---------|-------------------|-----------------|
| Factory | ✅ Implemented & used | `com.stla.patterns.factory`, `com.stla.ui.components`, `com.stla.patterns.strategy` |
| Strategy | ✅ Implemented & used | `com.stla.patterns.strategy` |
| Observer | ✅ Implemented & used | `com.stla.patterns.observer` |
| Facade | ✅ Implemented & used | `com.stla.patterns.facade` |
| Decorator | ✅ Classes present; UI uses `Course` boolean flags | `com.stla.patterns.decorator` |
| Proxy | ✅ Implemented & used (`CourseAccessProxy`) | `com.stla.patterns.proxy` |
| Singleton | ✅ Multiple classes | `com.stla.app`, `com.stla.core`, `com.stla.patterns.observer` |
| Adapter | ✅ Interfaces + impls; some unused in UI | `com.stla.patterns.adapter`, `com.stla.services.SupabaseStorageAdapter` |

## Classes per Pattern (Count)

| Pattern | Class / Interface Count |
|---------|-------------------------|
| Factory | `UserFactory`, `PaymentStrategyFactory`, `WithdrawStrategyFactory`, `ComponentFactory`, `CourseCardFactory`, `ChartFactory` |
| Strategy | `PaymentStrategy` + 3 impls; `WithdrawStrategy` + 2 impls |
| Observer | `EventBus`, `EventListener`, `AppEvent`, `NotificationObserver` |
| Facade | `EnrollmentFacade`, `CoursePublishFacade` |
| Decorator | `CourseComponent`, `BaseCourse`, `CourseDecorator`, 3 concrete decorators |
| Proxy | `CourseAccessProxy`, `AccessControlProxy` |
| Singleton | `AppConfig`, `DatabaseConnection`, `SessionManager`, `EventBus`, `NavigationManager`, `ThemeManager`, `SupabaseStorageService`, `SingletonRegistry` |
| Adapter | `ReportExportAdapter`, `PaymentGatewayAdapter`, `CsvReportExportAdapter`, `SimulatedPaymentGateway`, `SupabaseStorageAdapter` |

## UML Diagrams Generated

- **8 pattern sections** with PlantUML in [design-patterns-uml.md](./design-patterns-uml.md)
- **Layered architecture** diagram in [full-project-uml.md](./full-project-uml.md)
- **Domain model** diagram in [full-project-uml.md](./full-project-uml.md)
- **Repository layer** diagram in [full-project-uml.md](./full-project-uml.md)
- **Student enrollment flow** diagram in [full-project-uml.md](./full-project-uml.md)
- **Relationship reference** tables in [uml-relationships-guide.md](./uml-relationships-guide.md)

## Relationship Types Used

| UML Arrow | PlantUML | Used For |
|-----------|----------|----------|
| Generalization | `<\|--` | Decorator hierarchy (`CourseDecorator` extends abstract decorator) |
| Realization | `<\|..` | Strategy, Adapter, Observer listener, Repository interfaces |
| Composition | `*--` | Course → Section → Lesson; Quiz → Question → Option |
| Aggregation | `o--` | Instructor → Course; Student → Enrollment |
| Association | `-->` | Controller → Service; Service → Repository |
| Dependency | `..>` | Facade ..> Strategy; Checkout ..> PaymentStrategyFactory |

## Missing / Unclear (Manual Review)

| Item | Notes |
|------|-------|
| `AccessControlProxy` | Defined in `patterns.proxy`; **no references** found outside its file |
| `CsvReportExportAdapter` | Implemented; **no UI/service caller** found |
| `SimulatedPaymentGateway` | Implemented; **not wired** into `PaymentService` (strategies simulate payment directly) |
| `PayPalPaymentStrategy` | Class exists; **not exposed** by `PaymentStrategyFactory` |
| Decorator runtime stack | Pattern classes exist; **AddCourseController** persists add-ons as `Course.hasCertificate` etc. |
| `StudentCourseProgress` | **No domain model class** — progress via `StudentCourseProgressRepository` SQL only |
| `Profile` inheritance | **No** `extends` — Profile associates to Student/Instructor/Admin via `profileId` |
| Repository interfaces | Only **6** interfaces in `domain.interfaces`; many repos are concrete-only (`EnrollmentRepository`, `WalletRepository`, etc.) |

## Project Statistics (Approximate)

| Layer | Java Files (under `src/main/java/com/stla`) |
|-------|---------------------------------------------|
| `ui` | ~80+ (controllers + components) |
| `services` | 22 |
| `data` | ~15 repositories + mappers |
| `domain` | ~55 models/enums/interfaces |
| `patterns` | 29 |
| `core` | navigation, session, database |
| `app` | Launcher, StlaApplication, AppConfig |

## How to Render PlantUML

Paste any `@startuml` block into:

- [PlantUML Online](https://www.plantuml.com/plantuml/uml/)
- VS Code extension: **PlantUML**
- IntelliJ plugin: **PlantUML integration**

---

*Generated from static analysis of `d:\STLA_Desktop\src\main\java` — May 2026.*
