package com.cis357.termproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.random.Random

class GameModel {

    private var playerDecidedTrump: Int = -1
    private var orderedUp: Boolean = false
    private var pickedUp: Boolean = false
    private var trick: TrickModel = TrickModel()
    private var deck: DeckModel = DeckModel()
    private var players: Array<PlayerModel?> = arrayOfNulls(4)
    private var hands: Array<HandModel?> = arrayOfNulls(4)
    private var goneOnce: Boolean = false

    private var _kittyCard = MutableLiveData<CardModel?>(null)
    val kittyCard: LiveData<CardModel?> = _kittyCard

    private var _trump = MutableLiveData<String>("undecided")
    var trump: LiveData<String> = _trump

    private var _scores = MutableLiveData<IntArray>(IntArray(2))
    val scores: LiveData<IntArray> = _scores

    private var _dealer = MutableLiveData<Int>(0)
    val dealer: LiveData<Int> = _dealer

    private var _currentPlayerTurn = MutableLiveData<Int>(1)
    val currentPlayerTurn: LiveData<Int> = _currentPlayerTurn

    private var _tricksWon = MutableLiveData<IntArray>(IntArray(2))
    val tricksWon: LiveData<IntArray> = _tricksWon


    init {
        createPlayers()
        decideDealer()
    }

    // Deals each player a new random hand, resets their
    // trick counter, and selects the kitty card
    private fun createPlayers() {
        for (i in 0..3) {
            // Each hand gets five cards
            hands[i] = HandModel().apply { dealHand(deck) }
            // Each player gets a hand
            players[i] = PlayerModel(hands[i]!!)
        }
        // Select the first card as the kitty card
        _kittyCard.value = deck.getCards()[0]
    }

    // Decides a dealer at random by picking a number between 0 and 3.
    private fun decideDealer() {
        _dealer.value = Random.nextInt(4)
        _currentPlayerTurn.value = ((_dealer.value ?: 0) + 1) % 4
    }

    // Awards points to the winning team
    private fun scorePoints(teamNum: Int, numPoints: Int) {
        val updatedScores = scores.value?.clone() ?: IntArray(2)
        updatedScores[teamNum] += numPoints
        _scores.value = updatedScores
    }

    // Return the local player's hand
    fun getPlayerHand(): HandModel {
        return hands[0]!!
    }

    // Resets players hands
    fun newHand() {
        deck.shuffleCards()
        this.players = arrayOfNulls<PlayerModel>(4)
        createPlayers()
    }

    // Assign the cards point values
    fun assignCardPoints() {
        val trump = _trump.value ?: return
        val leftBowerSuit = when (trump) {
            "Hearts" -> "Diamonds"
            "Diamonds" -> "Hearts"
            "Clubs" -> "Spades"
            "Spades" -> "Clubs"
            else -> ""
        }
        val leadingSuit = trick.getLeadCard()?.suit

        // Assign points to each card in each player's hand
        players.forEach { player ->
            player?.hand?.getCards()?.forEach { card ->
                assignPointsToCard(card, trump, leadingSuit ?: "", leftBowerSuit)
            }
        }
    }

    // Assign card points helper
    private fun assignPointsToCard(card: CardModel, trump: String, leadingSuit: String?, leftBowerSuit: String) {
        when {
            card.suit == trump -> {
                when (card.rank) {
                    "9" -> card.pointValue = 14
                    "10" -> card.pointValue = 15
                    "Jack" -> card.pointValue = 20
                    "Queen" -> card.pointValue = 16
                    "King" -> card.pointValue = 17
                    "Ace" -> card.pointValue = 18
                }
            }
            card.suit == leftBowerSuit && card.rank == "Jack" -> {
                card.pointValue = 19  // Left bower
            }
            card.suit == leadingSuit -> {
                when (card.rank) {
                    "9" -> card.pointValue = 8
                    "10" -> card.pointValue = 9
                    "Jack" -> card.pointValue = 10
                    "Queen" -> card.pointValue = 11
                    "King" -> card.pointValue = 12
                    "Ace" -> card.pointValue = 13
                }
            }
            else -> {
                when (card.rank) {
                    "9" -> card.pointValue = 2
                    "10" -> card.pointValue = 3
                    "Jack" -> card.pointValue = 4
                    "Queen" -> card.pointValue = 5
                    "King" -> card.pointValue = 6
                    "Ace" -> card.pointValue = 7
                }
            }
        }
    }

    // Asks the AI player to decide trump. If not everyone
    // has gone, it will choose pass or pickup
    fun aiDecideTrump() {

        // Check for null hand
        val currentPlayerHand = players[currentPlayerTurn.value!!]?.hand
        if (currentPlayerHand == null) {
            println("Error: Current player hand is null")
            return
        }

        var trumpCount = 0
        val hand = players[currentPlayerTurn.value!!]!!.hand

        // If everyone has passed on trump, set it to undecided,
        // otherwise set kitty card suit as trump
        _trump.value = if (!goneOnce) {
            _kittyCard.value!!.suit
        } else {
            "undecided"
        }

        assignCardPoints()

        // increments trumpCount for each trump card in hand
        for (card in hand.getCards()) {
            if (card.suit == _trump.value) {
                trumpCount++
            }
        }

        // if not everyone has gone, use this logic to make a decision
        if (!goneOnce) {
            // if AI player has more than 3 trump, order it up. dealer must discard one.
            if (trumpCount >= 3 && currentPlayerTurn != dealer) {
                _trump.value = _kittyCard.value!!.suit
                playerDecidedTrump = _currentPlayerTurn.value!!
                orderedUp = true
                assignCardPoints()
                aiDecideDiscard()
            } else if (trumpCount >= 2 && currentPlayerTurn == dealer) {
                // if AI player has 2 or more trump in his hand,
                // and he is the dealer, pick up kitty card
                _trump.value = _kittyCard.value!!.suit
                pickedUp = true
                playerDecidedTrump = _currentPlayerTurn.value!!
                assignCardPoints()
                aiDecideDiscard()
            }
        } else {
            // check if player has good enough cards to choose trump
            // by adding point value of similar suit cards
            var highestSuitScore = 0
            val suitPoints = IntArray(4)

            for (card in hand.getCards()) {
                when (card.suit) {
                    "Hearts" -> suitPoints[0] += card.pointValue
                    "Diamonds" -> suitPoints[1] += card.pointValue
                    "Clubs" -> suitPoints[2] += card.pointValue
                    "Spades" -> suitPoints[3] += card.pointValue
                    else -> throw IllegalArgumentException("Error in checking if player has good enough cards to choose trump.")
                }

                // checks to see if the AI player has good enough
                // cards to choose when trump is undecided
                for (suitPoint in suitPoints) {
                    if (suitPoint > 20 && suitPoint > highestSuitScore) {
                        playerDecidedTrump = _currentPlayerTurn.value!!
                        highestSuitScore = suitPoint
                        _trump.value = card.suit
                        assignCardPoints()
                        orderedUp = true
                    }
                }
            }
        }
    }

    // Chooses a card for the AI to play
    fun aiDecideCard(): CardModel {
        var cardToPlay = CardModel("blank", "blank", 0)
        var highestCardInTrick = 0
        var lowestCardInHand = 21

        // find the point value of the current high card
        for (card in trick.getCardsPlayed()) {
            if (card != null && card.pointValue > highestCardInTrick) {
                highestCardInTrick = card.pointValue
            }
        }

        if (highestCardInTrick != 0) {
            for (playerCard in players[_currentPlayerTurn.value!!]!!.hand.getCards()) {

                // if no card following lead suit has been found, card has higher point
                // value than the highest card in trick, and is trump, set as card to be played
                if (playerCard.pointValue > highestCardInTrick && playerCard.suit == _trump.value
                    && cardToPlay.suit != trick.getLeadCard()?.suit) {

                    // makes sure lowest trump to win trick is played
                    if (playerCard.pointValue < cardToPlay.pointValue && cardToPlay.suit == _trump.value) {
                        cardToPlay = playerCard
                    }

                    // else if card has higher point value than the highest card in trick
                    // and is same suit as leading card, set as card to be played
                } else if (playerCard.pointValue > highestCardInTrick && playerCard.suit == trick.getLeadCard()?.suit) {
                    cardToPlay = playerCard
                } else if (playerCard.suit == trick.getLeadCard()?.suit && cardToPlay.pointValue < highestCardInTrick
                    && playerCard.pointValue < cardToPlay.pointValue) {
                    // else if card follows leading suit and current cardToPlay < highestScore
                    // if the card is lower than the current card, choose it
                    cardToPlay = playerCard
                }
            }
        }

        // if no playable card is found, choose
        // the card with the lowest point value to play
        if (cardToPlay.suit == "blank") {
            for (card in players[_currentPlayerTurn.value!!]!!.hand.getCards()) {
                if (card.pointValue < lowestCardInHand) {
                    lowestCardInHand = card.pointValue
                    cardToPlay = card
                }
            }
        }
        players[_currentPlayerTurn.value!!]!!.hand.removeCard(cardToPlay)
        return cardToPlay
    }

    // Removes the lowest card from the AI's hand if ordered up or picked up
    private fun aiDecideDiscard() {
        assignCardPoints()

        var lowestCard = 20
        var discardCard: CardModel? = null
        val hand = players[_dealer.value!!]!!.hand

        // this for loop finds the lowest card in
        // hand based off trump suit and discards it
        for (card in hand.getCards()) {
            if (card.pointValue <= lowestCard) {
                lowestCard = card.pointValue
                discardCard = card
            }
        }
        assignCardPoints()

        discardCard?.let { card ->
            hand.removeCard(card)
        }

        _kittyCard.value?.let { card ->
            hand.addCard(card)
        }

    }

    // Increments the team trick score for the winning team of the trick
    fun awardTrickPoints(): Int {
        var highestCard = 0
        var winningTeam = 0
        var playerWonTrick = 0

        var trickCard: CardModel?

        for (i in 0..3) {
            trickCard = trick.getCardsPlayed()[i]
            if (trickCard != null) {
                if (trickCard.pointValue > highestCard) {
                    highestCard = trickCard.pointValue
                    winningTeam = i % 2
                    playerWonTrick = i
                }
            } else {
                throw IllegalArgumentException("Null card found in trick")
            }
        }
        _tricksWon.value!![winningTeam]++
        return playerWonTrick
    }

    // Awards the winning team of the hand points and returns the winning team
    fun awardTeamPoints(): Int {
        if (_tricksWon.value!![0] >= 3) {
            return when {
                // if all 5 tricks were taken by team 0
                _tricksWon.value!![0] == 5 -> {
                    scorePoints(0, 2)
                    0
                }
                // else if opposite team decided trump
                playerDecidedTrump % 2 != 0 -> {
                    scorePoints(0, 2)
                    0
                }
                else -> {
                    scorePoints(0, 1)
                    0
                }
            }
        } else if (_tricksWon.value!![1] == 5) {
            scorePoints(1, 2)
            return 1
        } else if (playerDecidedTrump % 2 != 1) {
            scorePoints(1, 2)
            return 1
        } else {
            scorePoints(1, 1)
            return 1
        }
    }

    // Getters and setters to use in MainViewModel
    fun getDealer(): Int { return _dealer.value ?: 0 }
    fun setDealer(dealer: Int) { _dealer.value = dealer }

    fun getScores(): IntArray { return _scores.value ?: IntArray(2) }

    fun getTrick() = trick
    fun setTrick(trick: TrickModel) { this.trick = trick }

    fun getKittyCard(): CardModel? { return _kittyCard.value }
    fun setKittyCard(kittyCard: CardModel?) { _kittyCard.value = kittyCard }

    fun getCurrentPlayerTurn(): Int { return _currentPlayerTurn.value ?: 0 }
    fun setCurrentPlayerTurn(currentPlayerTurn: Int) { _currentPlayerTurn.value = currentPlayerTurn }

    fun getTrump(): String { return _trump.value ?: "undecided" }
    fun setTrump(trump: String) { _trump.value = trump }

    fun isGoneOnce() = goneOnce
    fun setGoneOnce(goneOnce: Boolean) { this.goneOnce = goneOnce }

    fun isOrderedUp() = orderedUp
    fun setOrderedUp(orderedUp: Boolean) { this.orderedUp = orderedUp }

    fun isPickedUp() = pickedUp
    fun setPickedUp(pickedUp: Boolean) { this.pickedUp = pickedUp }

    fun getTricksWon(): IntArray { return _tricksWon.value ?: IntArray(2) }
    fun setTricksWon(tricksWon: IntArray) { _tricksWon.value = tricksWon }

    fun getPlayerDecidedTrump() = playerDecidedTrump
    fun setPlayerDecidedTrump(playerDecidedTrump: Int) { this.playerDecidedTrump = playerDecidedTrump }

    // Updates team 1's score when player spends points
    fun updateScores(additionalPoints: Int) {
        val updatedScores = _scores.value ?: intArrayOf(0, 0)
        updatedScores[0] += additionalPoints
        _scores.value = updatedScores
    }

}
