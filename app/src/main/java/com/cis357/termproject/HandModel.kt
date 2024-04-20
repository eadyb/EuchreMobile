package com.cis357.termproject

class HandModel {

    // Creates a list to hold cards as objects
    private val cards: MutableList<CardModel> = mutableListOf()

    // Gets the cards in a given hand
    fun getCards(): List<CardModel> {
        return cards
    }

    // Adds a card to a hand
    fun addCard(card: CardModel) {
        cards.add(card)
    }

    // Remove a card from a hand
    fun removeCard(card: CardModel) {
        cards.remove(card)
    }

    // Deal 5 cards from the deck to create a hand
    fun dealHand(deck: DeckModel) {
        for (i in 0 until 5) {
            val card = deck.dealCard()
            addCard(card)
        }
    }

}