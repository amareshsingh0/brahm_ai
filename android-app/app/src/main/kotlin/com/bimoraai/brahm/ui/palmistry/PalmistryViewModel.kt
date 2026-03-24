package com.bimoraai.brahm.ui.palmistry

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class PalmistryViewModel @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _result = MutableStateFlow<Map<String, Any?>?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val result = _result.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val error = _error.asStateFlow()

    fun analyzePalm(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw Exception("Could not read image")
                val reqBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", "palm.jpg", reqBody)
                val res = api.analyzePalm(part)
                if (res.isSuccessful) _result.value = res.body()
                else _error.value = "Analysis failed. Try a clearer photo."
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
