package com.v2ex.idea.model

sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
    data object Empty : LoadState<Nothing>
    data class Error(val message: String, val retryable: Boolean = true) : LoadState<Nothing>
}
