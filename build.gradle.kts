// Top-level build file: only declares which plugins the modules may use.
// "apply false" = plugin is made available here, but applied in app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
