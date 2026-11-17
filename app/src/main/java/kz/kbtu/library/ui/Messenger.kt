package kz.kbtu.library.ui

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

// Short feedback for the user ("Added ...", "Limit reached ..."). ViewModels send, the root
// Scaffold shows it in a Snackbar, so a message survives leaving the screen that produced it.
class Messenger {
    private val channel = Channel<String>(Channel.BUFFERED)

    val messages: Flow<String> = channel.receiveAsFlow()

    fun show(text: String) {
        channel.trySend(text)
    }
}
