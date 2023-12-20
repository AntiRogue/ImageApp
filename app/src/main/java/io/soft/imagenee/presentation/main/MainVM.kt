package io.soft.imagenee.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.soft.imagenee.data.local.LocalStorage
import io.soft.imagenee.data.model.Image
import io.soft.imagenee.data.network.image.AddImageRequest
import io.soft.imagenee.data.repository.ImageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainVM @Inject constructor(
    private val repository: ImageRepository,
    private val localStorage: LocalStorage
) : ViewModel() {
    private val _effect = MutableSharedFlow<UIEffect>()
    val effect = _effect.asSharedFlow()

    private val _state = MutableStateFlow<ImagesState>(ImagesState.NotLoading)
    val state = _state.asStateFlow()

    private val _deleteMode = MutableStateFlow<String?>(null)
    val deleteMode = _deleteMode.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName = _userName.asStateFlow()

    init {
        getUserName()
        getAllImages()
    }

    private fun getUserName() {
        viewModelScope.launch {
            localStorage.getEmail.collectLatest {
                _userName.update { it }
            }
        }
    }

    private fun getAllImages() {
        _state.update { ImagesState.Loading }
        viewModelScope.launch {
            repository
                .getAllImages()
                .onSuccess { list -> _state.update { ImagesState.Success(list) } }
                .onFailure { error ->
                    _state.update {
                        ImagesState.Error(error.message)
                    }
                }
        }
        _state.update { ImagesState.NotLoading }
    }

    fun uploadImage(file: File, imageTitle: String, userId: String) {
        viewModelScope.launch {
            _effect.emit(UIEffect.FileUploading)
            repository.uploadImage(AddImageRequest(file, imageTitle))
                .onFailure {
                    Timber.d(it.message)
                    _effect.emit(
                        UIEffect.FileUploadFailure(
                            it.message ?: "Unknown error"
                        )
                    )
                }
            _effect.emit(UIEffect.Processed)

            // refresh
            getAllImages()
        }
    }

    fun select(id: String) {
        val state = _state.value

        if (state !is ImagesState.Success || state.images == null) {
            return
        }

        val newImages = state.images.map {
            if (it.id == id) {
                it.isSelected = it.isSelected.not()
            } else {
                it.isSelected = false
            }
            it
        }

        _deleteMode.update {
            newImages.find {
                it.isSelected
            }?.id
        }

        _state.update {
            state.copy(
                images = newImages
            )
        }
    }

    fun logOut() {
        viewModelScope.launch {
            localStorage.clear()
            _effect.emit(UIEffect.LogOut)
        }
    }

    fun deleteImage() {
        _deleteMode.value ?: return
        viewModelScope.launch {
            repository
                .deleteImage(_deleteMode.value!!)
                .onSuccess { getAllImages() }
                .onFailure {
                    _effect.emit(
                        UIEffect.FileUploadFailure(
                            it.message ?: "Unknown error"
                        )
                    )
                }
        }
    }
}

sealed interface ImagesState {
    object Loading : ImagesState
    object NotLoading : ImagesState
    data class Success(val images: List<Image>? = null) : ImagesState
    data class Error(val message: String?) : ImagesState {
        fun toHumanMessage(): String =
            when {
                message == null -> "Something went wrong :("
                message.contains("404") -> "Not found error"
                message.contains("500") -> "Internal server error"
                else -> "Unknown Error"
            }
    }
}

sealed interface UIEffect {
    object FileUploading : UIEffect
    object Processed : UIEffect
    data class FileUploadFailure(val failureReason: String) : UIEffect
    object LogOut : UIEffect
}