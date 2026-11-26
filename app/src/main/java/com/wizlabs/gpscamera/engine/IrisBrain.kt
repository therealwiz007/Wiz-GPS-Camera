package com.wizlabs.gpscamera.engine

/** Face shown by the Iris mascot. */
enum class Expression { NEUTRAL, SEARCHING, HAPPY, PROUD, WORRIED, SLEEPY }

/** Motion the mascot plays for that face. STILL is always allowed; the others need system motion. */
enum class Action { STILL, SCAN, BOUNCE, SPARKLE }

/** What the app tells Iris about. Pure data: no Android types, so it is unit-testable. */
sealed interface IrisEvent {
    data object WaitingPermission : IrisEvent
    data class SearchingGps(val accuracyM: Int?, val elapsedSec: Int = 0) : IrisEvent
    data class FixLocked(val accuracyM: Int) : IrisEvent
    data object GpsOff : IrisEvent
    data object GalleryEmpty : IrisEvent
    data object ExportDone : IrisEvent
    data object MockLocation : IrisEvent
    data class Onboarding(val page: Int) : IrisEvent
}

/** The brain's answer: face, motion and an optional short line (≤ 50 characters, technical facts only). */
data class IrisState(val expression: Expression, val action: Action, val line: String?) {
    init { require(line == null || line.length <= IrisBrain.MAX_LINE) { "line too long: $line" } }
}

object IrisBrain {
    const val MAX_LINE = 50
    /** The searching animation stops after this many seconds and settles to a still frame. */
    const val SEARCH_ANIMATION_SEC = 20

    val onboardingLines = listOf(
        "Proof of place and time",
        "Exact, fresh GPS",
        "Untouched, verifiable photos"
    )

    fun react(event: IrisEvent): IrisState = when (event) {
        IrisEvent.WaitingPermission ->
            IrisState(Expression.NEUTRAL, Action.STILL, "I need precise location to stamp photos.")
        is IrisEvent.SearchingGps -> {
            val m = event.accuracyM
            val line = if (m != null) "Finding your position… ±$m m" else "Finding your position…"
            val action = if (event.elapsedSec < SEARCH_ANIMATION_SEC) Action.SCAN else Action.STILL
            IrisState(Expression.SEARCHING, action, line)
        }
        is IrisEvent.FixLocked -> IrisState(Expression.HAPPY, Action.BOUNCE, "Locked: ±${event.accuracyM} m.")
        IrisEvent.GpsOff -> IrisState(Expression.WORRIED, Action.STILL, "Turn on GPS to continue.")
        IrisEvent.GalleryEmpty -> IrisState(Expression.SLEEPY, Action.STILL, "No photos yet.")
        IrisEvent.ExportDone -> IrisState(Expression.PROUD, Action.SPARKLE, "Report saved to Downloads.")
        IrisEvent.MockLocation -> IrisState(Expression.WORRIED, Action.STILL, "Mock location is on.")
        is IrisEvent.Onboarding -> {
            val p = event.page.coerceIn(0, 2)
            val face = when (p) { 0 -> Expression.NEUTRAL; 1 -> Expression.HAPPY; else -> Expression.PROUD }
            IrisState(face, Action.STILL, onboardingLines[p])
        }
    }
}
