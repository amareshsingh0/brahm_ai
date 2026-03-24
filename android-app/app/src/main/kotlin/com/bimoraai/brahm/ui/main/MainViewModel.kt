package com.bimoraai.brahm.ui.main

import androidx.lifecycle.ViewModel
import com.bimoraai.brahm.core.datastore.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val tokenDataStore: TokenDataStore,
) : ViewModel()
