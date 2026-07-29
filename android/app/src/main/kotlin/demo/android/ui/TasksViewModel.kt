package demo.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import demo.android.db.Database
import demo.android.db.Priority
import demo.android.db.Task
import demo.android.db.User
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import onl.ycode.stormify.*

/**
 * Single-screen ViewModel that hands the UI a list of [Task]s plus the
 * available [User]s for the "create task" picker.
 *
 * All Stormify calls happen on [Dispatchers.IO] — Android forbids disk I/O on
 * the main thread, and Stormify is a blocking, JDBC-shaped API. The ViewModel
 * holds the [Stormify] reference, the UI never touches it directly.
 *
 * State is exposed as a single immutable [TasksUiState] [StateFlow] so the
 * Composables can render off a single source of truth and never have to worry
 * about partial loads.
 */
class TasksViewModel(app: Application) : AndroidViewModel(app) {
    // Opening the database checks the schema and may seed it — disk work, which
    // Android forbids on the main thread. A property initializer would run it there,
    // so the open is started on Dispatchers.IO and awaited by whoever needs it first.
    private val stormify: Deferred<Stormify> =
        viewModelScope.async(Dispatchers.IO) { Database.open(app) }

    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    init { refresh() }

    private fun refresh() = viewModelScope.launch {
        val db = stormify.await()
        val (tasks, users) = withContext(Dispatchers.IO) {
            // findAll loads every Task; AutoTable lazy-loads each task.user the
            // first time it's read. Touching `task.user` here forces the load
            // before we hand the data to Compose, so the UI never blocks the
            // main thread on a follow-up SELECT.
            val loadedTasks = findAll<Task>().onEach { it.user?.let { u -> db.refresh(u) } }
            val loadedUsers = findAll<User>()
            loadedTasks to loadedUsers
        }
        _state.value = TasksUiState(tasks = tasks, users = users)
    }

    fun toggleCompleted(task: Task) = viewModelScope.launch {
        val id = task.id ?: return@launch
        stormify.await()
        withContext(Dispatchers.IO) {
            // Re-read rather than mutating the instance the UI is holding: that one is
            // part of the rendered state, so writing to it changes the screen before the
            // database agrees, and a second tap would flip an already-stale copy.
            val current = findById<Task>(id) ?: return@withContext
            current.isCompleted = !current.isCompleted
            current.update()
        }
        refresh()
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        stormify.await()
        withContext(Dispatchers.IO) { task.delete() }
        refresh()
    }

    fun addTask(title: String, description: String, priority: Priority, owner: User) =
        viewModelScope.launch {
            stormify.await()
            withContext(Dispatchers.IO) {
                // A single insert is already atomic — there is no second write here that
                // would have to land with it, so there is no transaction to open.
                Task().apply {
                    this.title = title
                    this.description = description
                    this.priority = priority
                    this.user = owner
                }.create()
            }
            refresh()
        }
}

/** Immutable snapshot of what the Tasks screen needs to render. */
data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val users: List<User> = emptyList(),
)
