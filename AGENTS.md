# OmniHub Development Guidelines (AGENTS.md)

You are a senior engineer specializing in Kotlin Multiplatform (KMP) and Compose Multiplatform. Please follow these guidelines when assisting with the development of the OmniHub project.

## 1. Architectural Standards
This project strictly adheres to **Clean Architecture** and the **MVI (Model-View-Intent)** pattern:

- **Clean Architecture Layers:**
    - **Data Layer:** Responsible for Repository implementations, API calls (Ktor), and Data Mappers.
    - **Domain Layer:** Contains Use Cases, Domain Models, and Repository interfaces. This layer must be free of any platform-specific dependencies.
    - **UI/Presentation Layer:** Includes ViewModels, Contracts (State/Event/Effect), and Compose UI components.

- **MVI Pattern Implementation:**
    - Every screen must define a `Contract` interface containing:
        - `State`: The single source of truth for the UI.
        - `Event`: User actions or intents.
        - `Effect`: One-time side effects (e.g., Navigation, SnackBar).
    - ViewModels must handle logic through an `onEvent` function and update the `State` accordingly.

## 2. Tech Stack
- **Multiplatform:** Kotlin Multiplatform (KMP)
- **UI Framework:** Compose Multiplatform (targeting Android, iOS, Desktop, Web)
- **Concurrency:** Kotlin Coroutines & Flow
- **Dependency Injection:** Koin (configured in `commonMain`)
- **Networking:** Ktor
- **Image Loading:** Coil3
- **Navigation:** Compose Navigation

## 3. Coding Style & Rules
- **Naming Conventions:**
    - ViewModels: `[Feature]ViewModel`
    - UseCases: `[Action][Entity]UseCase` (e.g., `GetPhotosUseCase`)
- **Compose Best Practices:**
    - Keep UI components in `commonMain` as much as possible for maximum reuse.
    - Follow the State Hoisting principle.
    - Use `composeResources` for multi-language support.
- **Language Preference:**
    - While code is in English, please provide explanations and comments in **Traditional Chinese** (繁體中文).

## 4. Project Specifics
- **Android XR Support:** The project includes XR-specific layouts (e.g., `XrAppLayout` in `androidMain`). Be mindful of platform compatibility when modifying navigation or top-level UI.
- **Entry Points:** Note the initialization differences across platforms (`androidApp`, `iosApp`, `composeApp`).

## 5. AI Interaction Instructions
- When asked to implement a new feature, start by designing the **Domain Layer (UseCase)**, then move to the **ViewModel Contract**, and finally the **UI**.
- If my code violates the Unidirectional Data Flow (UDF) of MVI, please point it out and suggest a refactor.

## 6. Project Directory Structure & Context

### Directory Map
- `composeApp/` - Main UI implementation layer (KMP Compose)
    - `src/androidMain/` - Android-specific platform code
        - `kotlin/.../layout/` - **XR Layout Hub**: Defines Subspace, SpatialPanel, and Orbiter.
        - `kotlin/.../navigation/` - **Spatial Navigation Control**: Manages ActivityPanelEntity and spatial offsets.
        - `kotlin/.../activities/` - Entry points for various XR panels (e.g., `PhotoStackActivity`).
    - `src/commonMain/` - Cross-platform shared UI components and base logic.
- `shared/` - Core business logic module (KMP Shared)
- `docs/` - Project documentation and media assets
    - `images/gallery/` - Contains demonstration assets (e.g., `OmniHub_Gallery_AndroidXR.webp`).
- `gradle/` - Dependency management
    - `libs.versions.toml` - **Version Catalog**: Contains definitions for Android XR Alpha versions.

### Development Context (Key Principles)
- **Spatial Unit:** Strictly adhere to the **1dp = 1mm** spatial conversion logic.
- **Perceived Consistency:** To address scaling issues introduced in Alpha 11, physical dimensions for side panels (`ActivityPanelEntity`) are typically set to `1.5f x 1.6f` meters to maintain visual consistency with the main panel.
- **State-Driven Navigation:** Navigation is driven via global `StateFlow` updates; persistent Activities observe these states to avoid redundant panel creation and flickering.
- **XR API Version:** Currently targeting Android XR **Alpha 13+**, using `AnchorPoint` and `DpVolumeOffset` APIs for spatial positioning.

## 7. AI Commit Message Generation Rules
When generating commit messages (via the AI "Pencil" icon or Gemini chat), strictly follow these rules:

- **Single Source of Truth:** Strictly follow the conventions defined in `CONTRIBUTING.md`.
- **Mandatory Structure:** You must always use the **Why/What** structure as defined in the contribution guide.
- **Language:** The subject line, Why section, and What section must all be in **English**.
- **Context Awareness:** Refer to the "Development Context" in Section 6 (e.g., mention Alpha 13 migration, 1dp=1mm logic, or perceived consistency if relevant).
