# Metrofy porting notes

## Project identity and Android namespace

- Android application ID: `com.metrofy.app`.
- Android Gradle namespace and Kotlin package namespace: `com.metrolist.android`. Keep this namespace unchanged unless a dedicated package-migration task is approved.
- The Android app is built from the `:androidApp` module and shared code from `:shared`.

## Dependency injection

Metrofy v2 uses **Koin**, not Hilt. Any Metrofy-specific Spotify services, repositories, data sources, use cases, and ViewModels ported from the legacy `main` branch must be registered and resolved through Koin.

- Add Android bindings to the existing Koin module(s), primarily `androidApp/src/main/kotlin/com/metrolist/android/di/AppModule.kt`.
- Add shared bindings to the appropriate shared Koin module when platform-independent.
- Use Koin DSL definitions such as `single`, `factory`, `viewModel`, `get()`, and constructor parameters.
- Follow the existing `startKoin { modules(coreModule, sharedModule, appModule) }` startup flow in `App.kt`.
- Do **not** copy Hilt annotations or modules from legacy `main`: `@Inject`, `@HiltViewModel`, `@AndroidEntryPoint`, `@Module`, `@InstallIn`, `@Provides`, or Hilt scopes. Translate their lifetime and dependency semantics into Koin definitions instead.

## Upstream services

Metrofy must not use MetrolistGroup's Sentry project or updater endpoints. Sentry initialization/reporting and the upstream updater are disabled in this branch. Keep them disabled unless Metrofy-owned endpoints and an explicit opt-in decision are introduced.
