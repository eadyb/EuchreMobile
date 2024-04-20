package com.cis357.termproject

data class UserResponse(val message: String, val type: ResponseType)

enum class ResponseType {
    ROUND_1_DECISION, ROUND_2_DECISION, DIALOG_TRIGGER, DISCARD_DECISION, DECISION_MADE, PROCEED_TO_GAMEPLAY
}
