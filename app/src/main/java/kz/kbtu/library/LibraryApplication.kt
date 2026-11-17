package kz.kbtu.library

import android.app.Application
import kz.kbtu.library.data.local.LibraryDatabase
import kz.kbtu.library.data.repository.LibraryRepository
import kz.kbtu.library.data.repository.RoomLibraryRepository
import kz.kbtu.library.ui.Messenger

// Created objects live as long as the process: one database, one repository
class AppContainer(application: Application) {
    val database: LibraryDatabase = LibraryDatabase.create(application)
    val repository: LibraryRepository = RoomLibraryRepository(database)
    val messenger = Messenger()
}

class LibraryApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
