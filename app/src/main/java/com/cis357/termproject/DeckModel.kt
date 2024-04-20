package com.cis357.termproject;

import kotlin.random.Random;

class DeckModel {

    // Create structure
    private var cardList: MutableList<CardModel> = mutableListOf()

    // Initialize the Deck model
    init {
        // needs to be in constructor for game window initialization
        shuffleCards();
    }

    // Creates an un-shuffled deck of cards.
    private fun createDeck() {
        val suits = arrayOf("Hearts", "Diamonds", "Clubs", "Spades")
        val ranks = arrayOf("9", "10", "Jack", "Queen", "King", "Ace")

        for (suit in suits) {
            for (rank in ranks) {
                val card = CardModel(suit, rank, 0)
                cardList.add(card)
            }
        }
    }

    // Shuffles the deck of cards
    fun shuffleCards() {
        cardList.clear()
        createDeck()
        val shuffledDeck = mutableListOf<CardModel>()
        while (cardList.isNotEmpty()) {
            val randInt = Random.nextInt(cardList.size)
            shuffledDeck.add(cardList[randInt])
            cardList.removeAt(randInt)
        }
        cardList.addAll(shuffledDeck)
    }

    // Deals a card to a player from shuffled deck
    fun dealCard(): CardModel {
        if (cardList.isEmpty()) {
            throw IllegalStateException("Cannot deal card from an empty deck.")
        }
        return cardList.removeAt(cardList.size - 1)
    }

    // Gets the list of cards in the deck
    fun getCards(): List<CardModel> = cardList

    // Sets cards list
    fun setCards(cards: MutableList<CardModel>) {
        cardList = cards
    }

}
