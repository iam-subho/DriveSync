package com.iamsubho.drivesync.presentation.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Lets non-Home screens (wizard, settings) surface a Home snackbar after navigation. */
@Singleton
class SnackBus @Inject constructor() {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    fun post(message: String) {
        _messages.tryEmit(message)
    }
}
