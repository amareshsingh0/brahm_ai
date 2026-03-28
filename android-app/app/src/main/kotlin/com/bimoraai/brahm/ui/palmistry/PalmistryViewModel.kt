package com.bimoraai.brahm.ui.palmistry

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PalmistryViewModel @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _result    = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)

    val result    = _result.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val error     = _error.asStateFlow()

    /** Creates a temp URI for camera capture via FileProvider */
    fun createCameraUri(): Uri {
        val file = File(context.cacheDir, "palm_capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun analyzePalm(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw Exception("Could not read image")
                val reqBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                // Backend FastAPI parameter is named "file" — must match exactly
                val part = MultipartBody.Part.createFormData("file", "palm.jpg", reqBody)
                val res = api.analyzePalm(part)
                if (res.isSuccessful) {
                    _result.value = res.body()
                } else {
                    val errBody = res.errorBody()?.string()
                    _error.value = if (errBody?.contains("GEMINI_API_KEY") == true)
                        "AI service not configured on server."
                    else
                        "Analysis failed (${res.code()}). Try a clearer photo."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearResult() {
        _result.value = null
        _error.value  = null
    }
}
