package kz.kbtu.library.ui.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.kbtu.library.AppContainer
import kz.kbtu.library.LibraryApplication

// Tells Android how to create each ViewModel with its dependencies
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { CatalogViewModel(container().repository, container().messenger) }
        initializer { ItemDetailViewModel(container().repository, container().messenger, createSavedStateHandle()) }
        initializer { ItemFormViewModel(container().repository, container().messenger, createSavedStateHandle()) }
        initializer { MembersViewModel(container().repository, container().messenger) }
        initializer { LoansViewModel(container().repository, container().messenger) }
        initializer { StatsViewModel(container().repository) }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as LibraryApplication).container
