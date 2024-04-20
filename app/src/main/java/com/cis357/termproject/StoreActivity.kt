package com.cis357.termproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import com.android.billingclient.api.BillingClient

class StoreActivity : AppCompatActivity(), PurchaseUpdateListener {
    private lateinit var viewModel: MainViewModel
    private lateinit var billingManager: BillingManager

    private var isPremiumPurchased: Boolean = false
    private var areCardBacksPurchased: Boolean = false
    private var userPoints: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Initialize BillingManager with this class as the listener
        billingManager = BillingManager(this, this)

        isPremiumPurchased = intent.getBooleanExtra("PremiumStatus", false)
        areCardBacksPurchased = intent.getBooleanExtra("CardBacksPurchased", false)
        userPoints = intent.getIntExtra("Points", 0)

        setupButtons()
    }

    // Initialize buttons and their click listeners
    private fun setupButtons() {
        val buyCardBacksButton = findViewById<Button>(R.id.buyCardBacksBtn)
        val buyPremiumButton = findViewById<Button>(R.id.buyPremiumBtn)
        val buyPointsButton = findViewById<Button>(R.id.buyPointsBtn)
        val backToGameButton = findViewById<Button>(R.id.backToGameBtn)

        buyCardBacksButton.isEnabled = !areCardBacksPurchased
        buyPremiumButton.isEnabled = !isPremiumPurchased

        buyCardBacksButton.setOnClickListener { billingManager.initiatePurchase("new_card_image", BillingClient.SkuType.INAPP) }
        buyPremiumButton.setOnClickListener { billingManager.initiatePurchase("premium_upgrade", BillingClient.SkuType.SUBS) }
        buyPointsButton.setOnClickListener { billingManager.initiatePurchase("points_pack_small", BillingClient.SkuType.INAPP) }

        backToGameButton.setOnClickListener { finish() }
    }

    override fun onPremiumPurchased() {
        viewModel.updatePremiumStatus(true)
    }

    override fun onCardBacksPurchased() {
        viewModel.updateCardBacksPurchased(true)
    }

    override fun onPointsPurchased(amount: Int) {
        viewModel.addPoints(amount)
    }
}
