package com.cis357.termproject

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

// Interface to communicate purchase updates
interface PurchaseUpdateListener {
    fun onPremiumPurchased()
    fun onCardBacksPurchased()
    fun onPointsPurchased(amount: Int)
}

// Manages all billing interactions with Google Play Billing
class BillingManager(
    private val context: Context,
    private val listener: PurchaseUpdateListener
) {
    private lateinit var billingClient: BillingClient

    init {
        setupBillingClient()
    }

    // Setup the BillingClient with a listener for purchase updates
    private fun setupBillingClient() {
        val purchaseUpdateListener = PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                handlePurchases(purchases) // Handle the purchases
            }
        }
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchaseUpdateListener) // Set the listener to handle purchase updates
            .enablePendingPurchases() // Must be enabled for purchases
            .build()
        connectToBillingService() // Attempt to connect to Google Play Billing
    }

    private fun connectToBillingService() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryAvailableProducts() // Query product details once billing is setup
                    restorePurchases() // Restore any purchases upon initialization
                }
            }

            override fun onBillingServiceDisconnected() {
                connectToBillingService() // Reconnect on disconnection
            }
        })
    }

    private fun restorePurchases() {
        // Restore purchases for both in-app and subscriptions
        val types = listOf(BillingClient.SkuType.INAPP, BillingClient.SkuType.SUBS)
        types.forEach { type ->
            val result = billingClient.queryPurchasesAsync(type) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                }
            }
        }
    }

    // Initiates the purchase process for a specific SKU product
    fun initiatePurchase(skuId: String, skuType: String) {
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(listOf(skuId))
            .setType(skuType)
            .build()

        billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                skuDetailsList.firstOrNull()?.let { skuDetails ->
                    val flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails)
                        .build()
                    billingClient.launchBillingFlow(context as Activity, flowParams)
                }
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                if (isConsumable(purchase.skus.first())) {
                    consumePurchase(purchase.purchaseToken)
                } else {
                    acknowledgePurchase(purchase.purchaseToken, purchase.skus.first()) // Pass the SKU
                }
            }
        }
    }

    private fun isConsumable(skuId: String): Boolean {
        return skuId == "points_pack_small" // Example consumable ID
    }

    private fun consumePurchase(purchaseToken: String) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { consumeResult, _ ->
            if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                listener.onPointsPurchased(100) // Example points added
            }
        }
    }

    private fun acknowledgePurchase(purchaseToken: String, skuId: String) {
        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                triggerUpdateBasedOnSku(skuId) // Updates based on SKU
            }
        }
    }

    private fun triggerUpdateBasedOnSku(sku: String) {
        when (sku) {
            "premium_upgrade" -> listener.onPremiumPurchased()
            "new_card_image" -> listener.onCardBacksPurchased()
            "points_pack_small" -> listener.onPointsPurchased(100) // Correctly reflecting the number of points
        }
    }

    private fun queryAvailableProducts() {
        val skuList = listOf("premium_upgrade", "new_card_image", "points_pack_small")
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(BillingClient.SkuType.INAPP)  // Use SUBS for subscriptions
            .build()

        billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            // Here you can update your UI to display the products, currently does nothing with the results
        }
    }
}
