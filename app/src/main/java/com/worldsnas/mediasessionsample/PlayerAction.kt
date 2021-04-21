package com.worldsnas.mediasessionsample

enum class PlayerAction(
    val action: String,
    val requestCode: Int
) {
    OpenApp("player_action_open_app", 1000094),
    Play("player_action_play", 1000097),
    Pause("player_action_pause", 1000096),
    Stop("player_action_stop", 1000099),
    NextAya("player_action_next_aya", 1000098),
    PreviousAya("player_action_previous_aya", 1000095),
    ;

    companion object {
        operator fun invoke(action: String) =
            values().find { it.action == action } ?: error("action= $action is not supported")
    }
}