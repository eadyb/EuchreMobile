package com.cis357.termproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch


class MainViewModel : ViewModel() {

    // Create an instance of the game
    private val game: GameModel = GameModel()

    private val currentPlayerTurn: LiveData<Int> = game.currentPlayerTurn
    private val dealer: LiveData<Int> = game.dealer
    private val kittyCard: LiveData<CardModel?> = game.kittyCard
    private val _userPoints = MutableLiveData<Int>(0)
    val userPoints: LiveData<Int> = _userPoints
    private val _points = MutableLiveData<Int>(0)
    val points: LiveData<Int> = _points
    private val _isPremium = MutableLiveData<Boolean>(false)
    val isPremium: LiveData<Boolean> = _isPremium
    private val _cardBacksPurchased = MutableLiveData<Boolean>(false)
    val cardBacksPurchased: LiveData<Boolean> = _cardBacksPurchased
    val trump: LiveData<String> = game.trump
    val scores: LiveData<IntArray> = game.scores
    val tricksWon: LiveData<IntArray> = game.tricksWon
    // Channel for sending decisions
    private val decisionChannel = Channel<UserResponse>()
    val cardSelectionChannel = Channel<Int>(Channel.CONFLATED)
    // Channel to handle user responses
    val userResponseChannel = Channel<UserResponse>(Channel.UNLIMITED)

    // Function to trigger UI to show dialog
    private suspend fun requestUserDecision(message: String, type: ResponseType) {
        userResponseChannel.send(UserResponse(message, type))
    }

    // Function to request a user decision AND wait for a response
    private suspend fun requestAndAwaitDecision(prompt: String, type: ResponseType): UserResponse {
        requestUserDecision(prompt, type)
        return decisionChannel.receive()
    }

    // Function to send a user decision
    fun sendUserDecision(decision: UserResponse) {
        viewModelScope.launch {
            decisionChannel.send(decision)
        }
    }

    // Updates the premium status in the ViewModel.
    fun updatePremiumStatus(purchased: Boolean) {
        _isPremium.value = purchased
    }

    // Updates the status of whether card backs have been purchased.
    fun updateCardBacksPurchased(purchased: Boolean) {
        _cardBacksPurchased.value = purchased
    }

    // Adds a specified number of points to the current total points.
    fun addPoints(pointsToAdd: Int) {
        _points.value = (_points.value ?: 0) + pointsToAdd
    }

    // LiveData to emit the suit of the kitty card whenever it changes.
    val kittyCardSuit: LiveData<String?> = liveData {
        kittyCard.asFlow().collect { cardModel ->
            emit(cardModel?.suit)
        }
    }

    // Discards a card from the player's hand and replaces it with the kitty card.
    fun discardCard(index: Int) {
        val discardCard = game.getPlayerHand().getCards()[index]
        game.getPlayerHand().removeCard(discardCard) // Remove the selected card from the hand.
        game.getPlayerHand().addCard(game.getKittyCard()!!) // Add the kitty card to the hand.
        updateUserCardImages() // Update UI to reflect the new card images.
    }


    // Function to spend points and update team score
    fun spendPoints() {
        val currentPoints = _userPoints.value ?: 0
        if (currentPoints > 0) {  // Check if the user has enough points to spend
            _userPoints.value?.let { game.updateScores(it) }
            _userPoints.value = 0  // Deduct the points to spend from the current points
        }
    }

    // Get a formatted list of the users cards for displaying in popup dialog
    fun getUserCards(): List<String> {
        return game.getPlayerHand().getCards().map { card ->
            "${card.rank} of ${card.suit}"
        }
    }

    // For the 4 playable cards
    private val _playedCardsImages = List(4) { MutableLiveData<Int>(R.drawable.b1) }
    val playedCardsImages: List<LiveData<Int>> = _playedCardsImages

    // For the kitty card
    private val _kittyCardImage = MutableLiveData<Int>(R.drawable.b1)
    val kittyCardImage: LiveData<Int> = _kittyCardImage

    // For the 5 cards in the user's hand
    private val _userCardsImages = List(5) { MutableLiveData<Int>(R.drawable.b2) }
    val userCardsImages: List<LiveData<Int>> = _userCardsImages

    // Function to update images for played cards
    private fun changePlayedCardImage(cardIndex: Int, newImageResId: Int) {
        if (cardIndex in _playedCardsImages.indices) {
            _playedCardsImages[cardIndex].value = newImageResId
        }
    }

    // Function to update images for user's ImageButton cards
    private fun updateUserCardImages() {
        val userCards = game.getPlayerHand().getCards()
        for (index in userCards.indices) {
            val newImageResId = getCardImageResId(userCards[index])
            _userCardsImages[index].value = newImageResId
        }
    }

    // Convert a Card object's suit and rank into a drawable resource ID
    private fun getCardImageResId(card: CardModel): Int {
        val cardIdentifier = when (card.rank) {
            "Ace" -> "a"
            "King" -> "k"
            "Queen" -> "q"
            "Jack" -> "j"
            "10" -> "10"
            "9" -> "9"
            else -> ""
        } + when (card.suit) {
            "Hearts" -> "h"
            "Spades" -> "s"
            "Clubs" -> "c"
            "Diamonds" -> "d"
            else -> ""
        }

        // Map to drawable ID
        return when(cardIdentifier) {
            "ah" -> R.drawable.ah
            "as" -> R.drawable.aspades
            "ac" -> R.drawable.ac
            "ad" -> R.drawable.ad
            "kh" -> R.drawable.kh
            "ks" -> R.drawable.ks
            "kc" -> R.drawable.kc
            "kd" -> R.drawable.kd
            "qh" -> R.drawable.qh
            "qs" -> R.drawable.qs
            "qc" -> R.drawable.qc
            "qd" -> R.drawable.qd
            "jh" -> R.drawable.jh
            "js" -> R.drawable.js
            "jc" -> R.drawable.jc
            "jd" -> R.drawable.jd
            "10h" -> R.drawable.h10
            "10s" -> R.drawable.s10
            "10c" -> R.drawable.c10
            "10d" -> R.drawable.d10
            "9h" -> R.drawable.h9
            "9s" -> R.drawable.s9
            "9c" -> R.drawable.c9
            "9d" -> R.drawable.d9
            else -> R.drawable.b2
        }
    }


    // Function to play a card from the user's hand based on the selected card index.
    private fun playCardFromUserHand(cardIndex: Int) {
        val userCards = game.getPlayerHand().getCards() // Retrieve the cards from the player's hand.
        if (cardIndex in userCards.indices) { // Check if the index is valid.
            val cardToPlay = userCards[cardIndex] // Get the card at the specified index.
            playCard(cardToPlay, 0)  // Play the card, assuming '0' is the index for the user.
        }
    }

    // Plays a specified card for a given player number.
    private fun playCard(card: CardModel, playerNum: Int) {
        game.getTrick().addCardToTrick(card, playerNum) // Add the card to the current trick.
        game.assignCardPoints() // Assign points based on the played card.
        val newImageResId = getCardImageResId(card) // Retrieve the image resource ID for the card.
        changePlayedCardImage(playerNum, newImageResId) // Update the UI to display the card image.
    }

    // Resets the images of all played cards and the kitty card to a default state.
    private fun resetCardImages() {
        _kittyCardImage.value = R.drawable.b1 // Reset the kitty card image.
        for (i in 0..3) { // Iterate through the indices of the played cards.
            changePlayedCardImage(i, R.drawable.b1) // Reset each played card image.
        }
    }

    // Main game loop function that runs the gameplay logic.
    private suspend fun playGame() {
        if (trump.value == "undecided") { // Check if the trump has been decided.
            decideTrump() // Decide trump if not already set.
        }
        game.setCurrentPlayerTurn((game.getDealer() + 1) % 4) // Set the initial player turn.
        while (!checkGameWinner()) { // Continue playing until there's a game winner.
            playTricksUntilRoundEnds() // Play tricks until the round ends.
            if (game.getPlayerDecidedTrump() == -1) { // If no trump has been decided post-round, decide it.
                decideTrump()
            }
        }
    }

    // Play a trick until there is a winner of the hand
    private suspend fun playTricksUntilRoundEnds() {
        while (!game.getTrick().checkTrickForWin()) {
            // If its the local player's turn
            if (game.getCurrentPlayerTurn() == 0) {
                // Await user to play a card
                val cardIndex = awaitUserCardChoice()
                playCardFromUserHand(cardIndex)
            } else {
                // AI plays a card
                val card = game.aiDecideCard()
                playCard(card, game.getCurrentPlayerTurn())
            }
            // Update players turn
            game.setCurrentPlayerTurn((game.getCurrentPlayerTurn() + 1) % 4)
        }
        handleEndOfRound()
    }

    // Handles logic at the end of a round within the game.
    private suspend fun handleEndOfRound() {
        game.setCurrentPlayerTurn(game.awardTrickPoints()) // Update the current player turn to the winner of the trick.
        game.getTrick().clearTrick() // Clear the current trick.
        requestAndAwaitDecision("Player ${game.getCurrentPlayerTurn()} won the trick!", ResponseType.DIALOG_TRIGGER) // Prompt user that a player won the trick.
        resetCardImages() // Reset the images of all cards.
        if (game.getTricksWon().sum() == 5) { // Check if all tricks for the hand have been won.
            val winningTeam = game.awardTeamPoints() + 1 // Award points to the winning team.
            requestAndAwaitDecision("Team $winningTeam won the hand!", ResponseType.DIALOG_TRIGGER) // Notify users which team won the hand.
            prepareNewHand() // Prepare for a new hand.
        }
        checkGameWinner() // Check if there is a game winner.
    }

    // Prepares a new hand by resetting various game state properties.
    private fun prepareNewHand() {
        game.setPickedUp(false) // Reset the 'picked up' status.
        game.setOrderedUp(false) // Reset the 'ordered up' status.
        game.setPlayerDecidedTrump(-1) // Reset the player who decided trump.
        game.getTrick().clearTrick() // Clear the current trick.
        game.newHand() // Start a new hand.
        game.setTricksWon(intArrayOf(0, 0)) // Reset the tricks won count.
        resetCardImages() // Reset all card images.
        updateUserCardImages() // Update card images in the UI.
        game.getTrick().setLeadCard(null) // Clear the lead card.
        game.assignCardPoints() // Assign point values based on the new trump.
        game.setDealer((game.getDealer() + 1) % 4) // Set the next dealer.
        game.setCurrentPlayerTurn((game.getDealer() + 1) % 4) // Set the player turn to follow the dealer.
    }

    // Checks if there is a winner in the game based on team scores.
    private suspend fun checkGameWinner() : Boolean {
        val teamScores = game.getScores() // Get the current scores of both teams.
        if (teamScores[0] >= 10 || teamScores[1] >= 10) { // Check if either team has reached or surpassed the winning score.
            val winningTeam = if (teamScores[0] >= 10) 0 else 1 // Determine which team won.
            requestAndAwaitDecision("Team $winningTeam won the game!", ResponseType.DIALOG_TRIGGER) // Notify users which team won the game.
            endGame() // Perform end-of-game cleanup.
            return true // Return true to indicate that the game has a winner.
        }
        return false // Return false to indicate that the game continues.
    }

    // Waits for the user to select a card to play.
    private suspend fun awaitUserCardChoice(): Int {
        return cardSelectionChannel.receive() // Suspend execution until a card index is sent through the channel.
    }


    private suspend fun decideTrump() {
        do {
            requestAndAwaitDecision("Deal goes to player ${dealer.value}", ResponseType.DIALOG_TRIGGER)
            initializeTrumpRound()
            showKittyCard()
            game.setTrump(game.getKittyCard()!!.suit)
            game.assignCardPoints()

            // Process turn for either AI or User until a trump decision is made
            while (game.getPlayerDecidedTrump() == -1) {

                processPlayerTurn()

                // Break out if gameplay is to proceed, checking if a decision was made or both rounds are over

                if (game.getPlayerDecidedTrump() != -1 || (game.isGoneOnce() && dealer.value == currentPlayerTurn.value)) {
                    break
                }
            }

            // If no decision on trump was made after all players' input, reset and try again
            if (game.getPlayerDecidedTrump() == -1) {
                game.setDealer((game.getCurrentPlayerTurn() + 1) % 4)
                game.setCurrentPlayerTurn((game.getDealer() + 1) % 4)
                requestAndAwaitDecision("No one decided on trump. Deal goes to player ${game.getDealer()}.", ResponseType.DIALOG_TRIGGER)
                game.newHand()
                //game.setGoneOnce(false)
                // Loop will continue, and a new hand setup starts
            } else {
                // Proceed to gameplay if trump has been decided
                game.assignCardPoints()
                hideKittyCard()
                break
            }

            // This part only executes if a new hand is required
            hideKittyCard()
        } while (game.getPlayerDecidedTrump() == -1)  // Continue if no trump decision was made
    }

    // Reset the deciding trump attributes
    private fun initializeTrumpRound() {
        game.setOrderedUp(false)
        game.setPickedUp(false)
        game.setGoneOnce(false)
        game.setPlayerDecidedTrump(-1)
    }

    // Iterate though a players turn for deciding trump
    private suspend fun processPlayerTurn() {
        // If its the user's turn
        if (game.currentPlayerTurn.value == 0) {
            awaitUserDecision()
            if (game.isPickedUp()) {
                requestAndAwaitDecision("Select a card to discard", ResponseType.DISCARD_DECISION)
            }
        } else {  // AI's turn
            game.aiDecideTrump()
            // Display AI's decision
        }
        displayTrumpMessage()
        updateTurn()
    }

    // Updates the players turn for deciding trump only and sets the goneOnce flag
    private fun updateTurn() {
        if (currentPlayerTurn.value == dealer.value) {
            game.setGoneOnce(true)
        }
        game.setCurrentPlayerTurn((game.getCurrentPlayerTurn() + 1) % 4)
    }


    // Suspends the current coroutine to await a decision from the user based on the game state.
    private suspend fun awaitUserDecision() {
        val prompt = when {
            // Prompt to pick up the kitty card if it's the first round and the current player is the dealer.
            !game.isGoneOnce() && game.dealer.value == game.currentPlayerTurn.value ->
                "Would you like to pick up the ${game.getKittyCard()?.rank} of ${game.getKittyCard()?.suit}?"
            // Prompt to order up the kitty card if it's the first round and the current player is not the dealer.
            !game.isGoneOnce() ->
                "Would you like to order up the ${game.getKittyCard()?.rank} of ${game.getKittyCard()?.suit} to player ${game.dealer.value}?"
            // Prompt to choose the trump suit if it's the second round.
            else ->
                "Choose the trump suit"
        }

        // Determine the response type based on whether it's the first or second round.
        val responseType = if (game.isGoneOnce()) ResponseType.ROUND_2_DECISION else ResponseType.ROUND_1_DECISION
        // Request and wait for the user's decision.
        val decision = requestAndAwaitDecision(prompt, responseType)
        // Apply the user's decision to the game state.
        applyUserDecision(decision)
    }

    // Applies the user's decision to the game state.
    private fun applyUserDecision(decision: UserResponse) {
        when (decision.message) {
            // If the user says "Yes", set the trump to the kitty card's suit and mark the decision maker.
            "Yes" -> {
                game.setTrump(game.getKittyCard()!!.suit)
                game.setPlayerDecidedTrump(0)
                // If the dealer is the current player, mark as picked up; otherwise, mark as ordered up.
                if (game.dealer.value == 0) {
                    game.setPickedUp(true)
                } else {
                    game.setOrderedUp(true)
                }
            }
            // If the user selects a suit, set that as trump and mark the player as having decided and picked up.
            "Clubs", "Spades", "Hearts", "Diamonds" -> {
                game.setTrump(decision.message)
                game.setPlayerDecidedTrump(0)
                game.setPickedUp(true)
            }
            // If the user says "No", "Pass", or acknowledges the prompt, take no action.
            "No", "Pass", "Acknowledged" -> {}
            // Throw an exception if an unexpected decision is received.
            else -> throw IllegalArgumentException("Unexpected decision: ${decision.message}")
        }
    }

    // Display a message of who picked or ordered up trump
    private suspend fun displayTrumpMessage() {
        val message = when {
            game.isPickedUp() -> "Player ${game.getCurrentPlayerTurn()} picked up the ${game.getKittyCard()?.rank} of ${game.getKittyCard()?.suit}. ${game.getTrump()} is now Trump."
            game.isOrderedUp() -> "Player ${game.getCurrentPlayerTurn()} ordered up the ${game.getKittyCard()?.rank} of ${game.getKittyCard()?.suit} to the dealer. ${game.getTrump()} is now Trump."
            else -> "Player ${game.getCurrentPlayerTurn()} passed."
        }
        requestAndAwaitDecision(message, ResponseType.DIALOG_TRIGGER)
    }

    // Show kitty card
    private fun showKittyCard() {
        _kittyCardImage.value = getCardImageResId(game.getKittyCard()!!)
    }

    // Hide kitty card
    private fun hideKittyCard() {
        _kittyCardImage.value = R.drawable.b1
    }

    // Start the game loop
    fun startGame() {
        viewModelScope.launch {
            updateUserCardImages()
            playGame()
        }
    }

    // Resets the game state
    private fun endGame() {
        game.newHand()
        resetCardImages()
        updateUserCardImages()
        showKittyCard()
    }

}
