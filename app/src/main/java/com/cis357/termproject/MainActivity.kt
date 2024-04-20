package com.cis357.termproject

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.consumeAsFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Declare necessary properties for use in the activity.
    private lateinit var viewModel: MainViewModel
    private lateinit var storeActivityResultLauncher: ActivityResultLauncher<Intent>
    private var currentKittySuit: String? = null

    // Called when the activity is starting.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // Set the UI layout for this activity.

        // Initialize the ViewModel associated with this activity.
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Set up the activity result launcher to handle returning data from StoreActivity.
        storeActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Process the result from StoreActivity.
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                intent?.let {
                    // Update the ViewModel based on the results.
                    val premiumUpdated = it.getBooleanExtra("PremiumStatusUpdated", false)
                    val cardBacksUpdated = it.getBooleanExtra("CardBacksPurchasedUpdated", false)
                    val pointsAdded = it.getIntExtra("PointsUpdated", 0)

                    // Apply the updates from the store activity to the ViewModel.
                    if (premiumUpdated) {
                        viewModel.updatePremiumStatus(true)
                    }
                    if (cardBacksUpdated) {
                        viewModel.updateCardBacksPurchased(true)
                    }
                    viewModel.addPoints(pointsAdded)
                }
            }
        }

        setupUI()  // Set up user interface elements and their interactions.
        observeGameUpdates()  // Set up observers for game-related LiveData.
        handleUserResponses()  // Set up handling for user interactions based on game responses.
    }

    // Set up user interface components and their event listeners.
    private fun setupUI() {
        val playGameButton = findViewById<ImageButton>(R.id.playGameBtn)
        val goToStoreButton = findViewById<Button>(R.id.toStoreBtn)
        val resetGameButton = findViewById<Button>(R.id.resetGameBtn)

        // Set an onClick listener on the play game button to start the game.
        playGameButton.setOnClickListener {
            lifecycleScope.launch {
                // Start the game via the ViewModel
                viewModel.startGame()
                // Disable the button after starting the game
                it.isEnabled = false
            }
        }

        resetGameButton.setOnClickListener {
                playGameButton.isEnabled = true
        }

        // Set an onClick listener on the go to store button to navigate to the store.
        goToStoreButton.setOnClickListener {
            val intent = Intent(this, StoreActivity::class.java).apply {
                // Pass current state values to the StoreActivity.
                putExtra("PremiumStatus", viewModel.isPremium.value ?: false)
                putExtra("CardBacksPurchased", viewModel.cardBacksPurchased.value ?: false)
                putExtra("Points", viewModel.userPoints.value ?: 0)
            }
            storeActivityResultLauncher.launch(intent)  // Launch StoreActivity and expect a result.
        }

        // Observe changes in the premium status to update UI accordingly.
        viewModel.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // Change the background to a premium-specific background when the status is premium.
                findViewById<ConstraintLayout>(R.id.mainLayout).setBackgroundResource(R.drawable.premium_background)
            } else {
                // Use a standard background when the status is not premium.
                findViewById<ConstraintLayout>(R.id.mainLayout).setBackgroundResource(R.drawable.normal_background)
            }
        }

        // Observe changes in the card backs purchase status to update the card images.
        viewModel.cardBacksPurchased.observe(this) { purchased ->
            val cardBackResId = if (purchased) R.drawable.b3 else R.drawable.b1  // Choose the resource ID based on purchase status.
            setupCardButtons(cardBackResId)  // Setup card buttons with the correct card back image.
        }
    }

    // Initialize buttons
    private fun setupCardButtons(imageResId: Int) {
        val cardButtons = arrayOf(
            findViewById<ImageButton>(R.id.p1card1Btn),
            findViewById<ImageButton>(R.id.p1card2Btn),
            findViewById<ImageButton>(R.id.p1card3Btn),
            findViewById<ImageButton>(R.id.p1card4Btn),
            findViewById<ImageButton>(R.id.p1card5Btn)
        )

        cardButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.cardSelectionChannel.send(index)
                    button.setImageResource(imageResId)
                    button.isEnabled = false
                    // Check if all buttons are disabled
                    if (cardButtons.all { !it.isEnabled }) {
                        // If all buttons are disabled, enable them again
                        cardButtons.forEach { button ->
                            button.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    // Observe LiveData
    private fun observeGameUpdates() {
        val t1score = findViewById<TextView>(R.id.team1scoreLbl)
        val t2score = findViewById<TextView>(R.id.team2scoreLbl)
        val t1tricks = findViewById<TextView>(R.id.team1tricksLbl)
        val t2tricks = findViewById<TextView>(R.id.team2tricksLbl)
        val currentTrump = findViewById<TextView>(R.id.trumpLbl)
        val kittyImageView = findViewById<ImageView>(R.id.kittyCardImg)
        val userPointsButton = findViewById<Button>(R.id.usePointsBtn)

        viewModel.scores.observe(this) { scores ->
            t1score.text = getString(R.string.team_1_score_label, scores[0])
            t2score.text = getString(R.string.team_2_score_label, scores[1])
        }

        viewModel.tricksWon.observe(this) { tricks ->
            t1tricks.text = getString(R.string.team_1_tricks_label, tricks[0])
            t2tricks.text = getString(R.string.team_2_tricks_label, tricks[1])
        }

        viewModel.trump.observe(this) {
            currentTrump.text = getString(R.string.trump_label, it)
        }

        viewModel.kittyCardImage.observe(this) {
            kittyImageView.setImageResource(it)
        }

        viewModel.playedCardsImages.forEachIndexed { index, liveData ->
            liveData.observe(this) { imageResId ->
                findViewById<ImageView>(
                    resources.getIdentifier(
                        "p${index + 1}playCardImg",
                        "id",
                        packageName
                    )
                )?.setImageResource(imageResId)
            }
        }

        viewModel.userCardsImages.forEachIndexed { index, liveData ->
            liveData.observe(this) { imageResId ->
                findViewById<ImageButton>(
                    resources.getIdentifier(
                        "p1card${index + 1}Btn",
                        "id",
                        packageName
                    )
                )?.setImageResource(imageResId)
            }
        }

        // Observe the kitty card suit live data
        viewModel.kittyCardSuit.observe(this) { suit ->
            currentKittySuit = suit
        }

        viewModel.userPoints.observe(this) { points ->
            userPointsButton.text = getString(R.string.use_points_button, points)
        }

        // Setting up click listener for the user points button
        userPointsButton.setOnClickListener {
            viewModel.spendPoints()
        }
    }

    // Determine what dialog to show
    private fun handleUserResponses() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userResponseChannel.consumeAsFlow().collect { userResponse ->
                    when (userResponse.type) {
                        ResponseType.ROUND_1_DECISION -> showTrumpDialog1(userResponse)
                        ResponseType.ROUND_2_DECISION -> showTrumpDialog2()
                        ResponseType.DIALOG_TRIGGER -> showAlertDialog(userResponse.message)
                        ResponseType.DISCARD_DECISION -> showDiscardDialog()
                        ResponseType.DECISION_MADE -> showAlertDialog(userResponse.message)
                        ResponseType.PROCEED_TO_GAMEPLAY -> showAlertDialog(userResponse.message)
                    }
                }
            }
        }
    }

    // Dialog for the first round of trump
    private fun showTrumpDialog1(userResponse: UserResponse) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Decision Needed")
            .setMessage(userResponse.message)
            .setPositiveButton("Yes") { dialog, _ ->
                viewModel.sendUserDecision(UserResponse("Yes", ResponseType.DECISION_MADE))
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                viewModel.sendUserDecision(UserResponse("No", ResponseType.DECISION_MADE))
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    // Dialog for the second round of trump
    private fun showTrumpDialog2() {
        val availableSuits = arrayOf("Hearts", "Spades", "Diamonds", "Clubs")
            .filter { it != currentKittySuit }  // Use the stored suit value
            .plus("Pass")

        AlertDialog.Builder(this)
            .setTitle("Select Trump Suit")
            .setItems(availableSuits.toTypedArray()) { dialog, which ->
                val decision =
                    if (which < availableSuits.size - 1) availableSuits[which] else "Pass"
                lifecycleScope.launch {
                    viewModel.sendUserDecision(UserResponse(decision, ResponseType.DECISION_MADE))
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // Dialog for local player choosing a card to discard
    private fun showDiscardDialog() {
        val userCards = viewModel.getUserCards()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select a Card to Discard")
            .setItems(userCards.toTypedArray()) { _, which ->
                // 'which' is the index of the selected item
                viewModel.discardCard(which)
            }
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()
        dialog.show()
    }

    // Show general notice dialog for game events
    private fun showAlertDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                // Send a simple user response indicating the message was acknowledged
                viewModel.sendUserDecision(UserResponse("Acknowledged", ResponseType.DECISION_MADE))
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close the channel to free resources
        viewModel.userResponseChannel.close()
    }

}
