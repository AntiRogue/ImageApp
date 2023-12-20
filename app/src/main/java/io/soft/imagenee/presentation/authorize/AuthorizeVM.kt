package io.soft.imagenee.presentation.authorize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.soft.imagenee.data.local.LocalStorage
import io.soft.imagenee.data.network.authorize.LoginRequest
import io.soft.imagenee.data.network.authorize.SignupRequest
import io.soft.imagenee.data.repository.AuthorizeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthorizeVM @Inject constructor(
    private val repository: AuthorizeRepository,
    private val localStorage: LocalStorage
) : ViewModel() {
    private val _effect = MutableSharedFlow<Effect>()
    val effect = _effect.asSharedFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun login(
        email: String,
        password: String
    ) {
        if (
            password.isBlank()
            || email.isBlank()
            || email.contains("@").not()
        ) {
            viewModelScope.launch {
                _effect.emit(Effect.Error(message = "Invalid inputs"))
            }
            return
        }

        _loading.update { true }
        viewModelScope.launch {
            repository
                .login(LoginRequest(email, password))
                .onSuccess {
                    localStorage.put(email)
                    _effect.emit(Effect.AuthorizeSuccess)
                }
                .onFailure { _effect.emit(Effect.Error(it.message)) }
        }
        _loading.update { false }
    }

    fun signup(
        name: String,
        surname: String,
        email: String,
        password: String
    ) {
        if (
            name.isBlank()
            || surname.isBlank()
            || email.isBlank()
            || password.isBlank()
            || email.contains("@").not()
        ) {
            viewModelScope.launch {
                _effect.emit(Effect.Error("Invalid inputs"))
            }
            return
        }

        _loading.update { true }
        viewModelScope.launch {
            repository
                .signup(SignupRequest(name, surname, email, password))
                .onSuccess {
                    localStorage.put(email)
                    _effect.emit(Effect.AuthorizeSuccess)
                }
                .onFailure { _effect.emit(Effect.Error(it.message)) }
        }
        _loading.update { false }
    }
}

sealed interface Effect {
    data class Error(val message: String?) : Effect {
        fun toHumanMessage(): String =
            when {
                message == null -> "Something went wrong :("
                message.contains("404") -> "Not found error"
                message.contains("500") -> "Internal server error"
                else -> "Unknown Error"
            }
    }

    object AuthorizeSuccess : Effect
}