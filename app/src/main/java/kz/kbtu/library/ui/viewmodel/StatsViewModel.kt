package kz.kbtu.library.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kz.kbtu.library.data.repository.LibraryRepository
import kz.kbtu.library.model.LibraryStats

class StatsViewModel(repository: LibraryRepository) : ViewModel() {

    val stats: StateFlow<LibraryStats> = repository.library
        .map { it.stats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryStats())
}
