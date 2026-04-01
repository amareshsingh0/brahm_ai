package com.bimoraai.brahm.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.network.UpdateProfileRequest
import com.bimoraai.brahm.core.network.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore,
    private val userRepository: UserRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("brahm_prefs", Context.MODE_PRIVATE)

    // Expose the shared repository's user so all screens see the same cached data
    val user: StateFlow<UserDto?> = userRepository.user

    // Local photo path — shows immediately after crop while cloud upload is in progress
    private val _localPhotoPath = MutableStateFlow(prefs.getString("profile_photo_path", null))
    val localPhotoPath: StateFlow<String?> = _localPhotoPath.asStateFlow()

    private val _photoUploadState = MutableStateFlow<PhotoUploadState>(PhotoUploadState.Idle)
    val photoUploadState: StateFlow<PhotoUploadState> = _photoUploadState.asStateFlow()

    init {
        // Re-fetch on every profile screen open
        userRepository.refresh()
    }

    /** Called after uCrop saves the cropped image to a local file. */
    fun uploadPhoto(filePath: String) {
        // Show locally first (instant feedback)
        prefs.edit().putString("profile_photo_path", filePath).apply()
        _localPhotoPath.value = filePath

        viewModelScope.launch {
            _photoUploadState.value = PhotoUploadState.Loading
            try {
                val file = File(filePath)
                val requestBody = file.asRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData("photo", "avatar.jpg", requestBody)
                val res = api.uploadAvatar(part)
                if (res.isSuccessful) {
                    userRepository.refresh()   // pull new avatar_url into UserDto
                    _photoUploadState.value = PhotoUploadState.Success
                } else {
                    _photoUploadState.value = PhotoUploadState.Error("Upload failed (${res.code()})")
                }
            } catch (e: Exception) {
                _photoUploadState.value = PhotoUploadState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun removePhoto() {
        prefs.edit().remove("profile_photo_path").apply()
        _localPhotoPath.value = null
        try { File(appContext.filesDir, "profile_photo.jpg").delete() } catch (_: Exception) {}

        viewModelScope.launch {
            try {
                api.removeAvatar()
                userRepository.refresh()
            } catch (_: Exception) {}
        }
    }

    fun resetPhotoUploadState() { _photoUploadState.value = PhotoUploadState.Idle }

    // ── City search ──────────────────────────────────────────────────────────

    val cityQuery = MutableStateFlow("")

    val citySuggestions: StateFlow<List<City>> = cityQuery
        .debounce(300)
        .map { q ->
            if (q.length < 2) return@map emptyList()
            try {
                val res = api.searchCities(q)
                if (res.isSuccessful) res.body()?.results?.take(6) ?: emptyList()
                else emptyList()
            } catch (_: Exception) { emptyList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Profile save ─────────────────────────────────────────────────────────

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired = _sessionExpired.asStateFlow()

    fun saveProfile(
        name: String,
        date: String,
        time: String,
        place: String,
        gender: String,
        lat: Double = 0.0,
        lon: Double = 0.0,
        tz: Double = 5.5,
    ) {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            try {
                val current = userRepository.user.value
                val req = UpdateProfileRequest(
                    session_id = current?.session_id ?: "",
                    name       = name,
                    date       = date,
                    time       = time,
                    place      = place,
                    gender     = gender,
                    lat        = lat,
                    lon        = lon,
                    tz         = tz,
                    rashi      = current?.rashi ?: "",
                    nakshatra  = current?.nakshatra ?: "",
                    language   = "english",
                    plan       = current?.plan ?: "free",
                    phone      = current?.phone,
                    email      = current?.email,
                )
                val res = api.updateProfile(req)
                if (res.isSuccessful) {
                    userRepository.refresh()
                    _saveState.value = SaveState.Success
                } else if (res.code() == 401) {
                    tokenDataStore.clear()
                    userRepository.clear()
                    _sessionExpired.value = true
                } else {
                    val detail = res.errorBody()?.string() ?: ""
                    _saveState.value = SaveState.Error("Save failed (${res.code()}): $detail")
                }
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetSaveState() { _saveState.value = SaveState.Idle }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) {}
            tokenDataStore.clear()
            userRepository.clear()
            prefs.edit().clear().apply()   // wipe profile_photo_path + any other prefs
            _localPhotoPath.value = null
            onDone()
        }
    }
}

sealed class SaveState {
    object Idle    : SaveState()
    object Loading : SaveState()
    object Success : SaveState()
    data class Error(val msg: String) : SaveState()
}

sealed class PhotoUploadState {
    object Idle    : PhotoUploadState()
    object Loading : PhotoUploadState()
    object Success : PhotoUploadState()
    data class Error(val msg: String) : PhotoUploadState()
}
