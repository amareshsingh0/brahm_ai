package com.bimoraai.brahm.ui.subscription

import androidx.lifecycle.ViewModel
import com.bimoraai.brahm.core.data.SubscriptionInfo
import com.bimoraai.brahm.core.data.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Shared ViewModel for subscription state.
 * Inject via hiltViewModel() in any screen that needs feature gating.
 *
 * Usage:
 *   val subVm: SubscriptionViewModel = hiltViewModel()
 *   if (!subVm.hasFeature("kundali")) { UpgradeSheet(...) }
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repo: SubscriptionRepository,
) : ViewModel() {

    val info: StateFlow<SubscriptionInfo> = repo.info

    fun hasFeature(featureKey: String): Boolean = repo.hasFeature(featureKey)

    val isPaid:    Boolean get() = repo.isPaid
    val isExpired: Boolean get() = repo.isExpired
    val messagesRemaining: Int? get() = repo.messagesRemaining

    /** Call on login, app foreground, and after each chat message. */
    fun refresh() = repo.refresh()
}
