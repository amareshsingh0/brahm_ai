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

    // Single-hand (legacy / manual flow)
    private val _result    = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)

    val result    = _result.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val error     = _error.asStateFlow()

    // Dual-hand flow
    private val _domResult    = MutableStateFlow<JsonObject?>(null)
    private val _nonDomResult = MutableStateFlow<JsonObject?>(null)
    private val _combined     = MutableStateFlow<JsonObject?>(null)
    private val _domLoading   = MutableStateFlow(false)
    private val _nonDomLoading= MutableStateFlow(false)
    private val _domError     = MutableStateFlow<String?>(null)
    private val _nonDomError  = MutableStateFlow<String?>(null)

    val domResult     = _domResult.asStateFlow()
    val nonDomResult  = _nonDomResult.asStateFlow()
    val combined      = _combined.asStateFlow()
    val domLoading    = _domLoading.asStateFlow()
    val nonDomLoading = _nonDomLoading.asStateFlow()
    val domError      = _domError.asStateFlow()
    val nonDomError   = _nonDomError.asStateFlow()

    /** Creates a temp URI for camera capture via FileProvider */
    fun createCameraUri(): Uri {
        val file = File(context.cacheDir, "palm_capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun uriToBytes(uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.readBytes()
            ?: throw Exception("Could not read image")

    private fun makePart(bytes: ByteArray, name: String): MultipartBody.Part {
        val req = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(name, "palm.jpg", req)
    }

    /** Legacy single-hand analyze (used by manual flow) */
    fun analyzePalm(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val bytes = uriToBytes(uri)
                val part  = makePart(bytes, "file")
                val res   = api.analyzePalm(part, "dominant")
                if (res.isSuccessful) {
                    _result.value = res.body()
                } else {
                    val errBody = res.errorBody()?.string()
                    _error.value = if (errBody?.contains("GEMINI_API_KEY") == true)
                        "AI service not configured on server."
                    else "Analysis failed (${res.code()}). Try a clearer photo."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Step 1: analyze dominant hand */
    fun analyzeDominant(uri: Uri) {
        viewModelScope.launch {
            _domLoading.value = true
            _domError.value   = null
            try {
                val bytes = uriToBytes(uri)
                val part  = makePart(bytes, "file")
                val res   = api.analyzePalm(part, "dominant")
                if (res.isSuccessful) _domResult.value = res.body()
                else _domError.value = "Dominant hand analysis failed (${res.code()})."
            } catch (e: Exception) {
                _domError.value = e.message ?: "Network error"
            } finally {
                _domLoading.value = false
            }
        }
    }

    /** Step 2: analyze non-dominant hand, then combine both */
    fun analyzeNonDominantAndCombine(domUri: Uri, nonDomUri: Uri, dominantHand: String) {
        viewModelScope.launch {
            _nonDomLoading.value = true
            _nonDomError.value   = null
            try {
                val domBytes    = uriToBytes(domUri)
                val nonDomBytes = uriToBytes(nonDomUri)
                val domPart     = makePart(domBytes, "dominant")
                val nonDomPart  = makePart(nonDomBytes, "non_dominant")
                val res = api.analyzeBothPalms(domPart, nonDomPart, dominantHand)
                if (res.isSuccessful) {
                    val body = res.body()
                    _nonDomResult.value = body?.get("non_dominant")?.let { it as? JsonObject }
                    _combined.value     = body   // full response: {dominant, non_dominant, combined}
                } else {
                    _nonDomError.value = "Combined analysis failed (${res.code()})."
                }
            } catch (e: Exception) {
                _nonDomError.value = e.message ?: "Network error"
            } finally {
                _nonDomLoading.value = false
            }
        }
    }

    fun clearResult() {
        _result.value     = null
        _error.value      = null
        _domResult.value  = null
        _nonDomResult.value = null
        _combined.value   = null
        _domError.value   = null
        _nonDomError.value = null
    }
}
