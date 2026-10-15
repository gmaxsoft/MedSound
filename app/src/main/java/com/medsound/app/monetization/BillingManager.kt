package com.medsound.app.monetization

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.medsound.app.BuildConfig
import com.medsound.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillingUiState(
    val isPremium: Boolean = false,
    val premiumPriceLabel: String? = null,
    val isConnecting: Boolean = true,
    val canPurchase: Boolean = false,
    val lastError: String? = null,
)

/**
 * Jednorazowy produkt in-app [BuildConfig.PREMIUM_PRODUCT_ID] (np. do 9 USD — cena w Play Console).
 * Premium trwale usuwa reklamy i odblokowuje ekstra content.
 */
class BillingManager(
    context: Context,
    private val premiumPreferences: PremiumPreferences,
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private var productDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        scope.launch {
            premiumPreferences.isPremium.collect { cached ->
                _state.update { it.copy(isPremium = cached || it.isPremium) }
            }
        }
        startConnection()
    }

    private fun startConnection() {
        _state.update { it.copy(isConnecting = true) }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        refreshPurchases()
                        queryProductDetails()
                        _state.update { it.copy(isConnecting = false) }
                    }
                } else {
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            canPurchase = false,
                            lastError = result.debugMessage,
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.update { it.copy(isConnecting = true, canPurchase = false) }
            }
        })
    }

    private suspend fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update {
                it.copy(
                    canPurchase = false,
                    lastError = result.billingResult.debugMessage,
                )
            }
            return
        }

        val details = result.productDetailsList?.firstOrNull()
        productDetails = details
        val offer = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()
            ?: details?.oneTimePurchaseOfferDetails
        val price = offer?.formattedPrice

        _state.update {
            it.copy(
                premiumPriceLabel = price,
                canPurchase = details != null && !it.isPremium,
                lastError = if (details == null) {
                    appContext.getString(R.string.billing_error_missing_product)
                } else {
                    null
                },
            )
        }
    }

    suspend fun refreshPurchases() {
        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return

        val owned = result.purchasesList.any { purchase ->
            purchase.products.contains(BuildConfig.PREMIUM_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (owned) {
            result.purchasesList
                .filter { it.products.contains(BuildConfig.PREMIUM_PRODUCT_ID) }
                .forEach { acknowledgeIfNeeded(it) }
            grantPremium()
        }
    }

    fun launchPremiumPurchase(activity: Activity) {
        val details = productDetails
        if (details == null) {
            _state.update {
                it.copy(lastError = appContext.getString(R.string.billing_error_unavailable))
            }
            return
        }
        val offer = details.oneTimePurchaseOfferDetailsList?.firstOrNull()
            ?: details.oneTimePurchaseOfferDetails
        val offerToken = offer?.offerToken
        if (offerToken.isNullOrBlank()) {
            _state.update {
                it.copy(lastError = appContext.getString(R.string.billing_error_no_offer))
            }
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch {
                    purchases.orEmpty().forEach { purchase ->
                        if (purchase.products.contains(BuildConfig.PREMIUM_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        ) {
                            acknowledgeIfNeeded(purchase)
                            grantPremium()
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> {
                _state.update { it.copy(lastError = result.debugMessage) }
            }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params)
    }

    private suspend fun grantPremium() {
        premiumPreferences.setPremium(true)
        _state.update {
            it.copy(
                isPremium = true,
                canPurchase = false,
                lastError = null,
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
