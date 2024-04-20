package com.cis357.termproject

// Represents a trick in the game. It tracks the cards played
// by each player and determines the winner of the trick.
class TrickModel {
    // Array of cards for the trick
    private var cardsPlayed: Array<CardModel?> = arrayOfNulls(4)

    //Holds the leading card of the current round.
    private var leadCard: CardModel? = null

    // Determines if the trick is full and round is over
    fun checkTrickForWin(): Boolean {
        return cardsPlayed.all { it != null }
    }

    // Adds a card to the trick and sets it as the lead card if it is the first card played.
    fun addCardToTrick(card: CardModel, playerNum: Int) {
        if (cardsPlayed.all { it != null }) {
            leadCard = card
        }
        cardsPlayed[playerNum] = card
    }

    // Reset the trick
    fun clearTrick() {
        cardsPlayed.fill(null)
    }

    // Get a copy of the cards played in trick
    fun getCardsPlayed(): Array<CardModel?> {
        return cardsPlayed.copyOf()
    }

    // Get the lead card played
    fun getLeadCard(): CardModel? {
        return leadCard
    }

    // Set the lead card played
    fun setLeadCard(leadCard: CardModel?) {
        this.leadCard = leadCard
    }
}
