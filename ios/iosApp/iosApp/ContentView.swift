import SwiftUI
import Shared

struct ContentView: View {
    @State private var tasks: [Task] = []
    @State private var users: [User] = []
    @State private var showAddSheet = false

    var body: some View {
        NavigationStack {
            Group {
                if tasks.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "checklist")
                            .font(.largeTitle)
                            .foregroundColor(.secondary)
                        Text("No tasks yet")
                            .font(.headline)
                        Text("Tap + to add one.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                } else {
                    List {
                        ForEach(tasks, id: \.id) { task in
                            TaskRow(task: task) {
                                perform { DatabaseKt.toggleCompleted(task: task) }
                            }
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    perform { DatabaseKt.deleteTask(task: task) }
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Stormify Tasks")
            .toolbar {
                Button { showAddSheet = true } label: {
                    Image(systemName: "plus")
                }
            }
            .sheet(isPresented: $showAddSheet) {
                AddTaskView(users: users) { title, desc, priority, owner in
                    perform {
                        DatabaseKt.addTask(
                            title: title, description: desc, priority: priority, owner: owner
                        )
                    }
                }
            }
            .task { await reload() }
        }
    }

    /// Runs a database write off the main thread, then reloads.
    ///
    /// Every Stormify call here is blocking disk work. Calling it straight from a
    /// SwiftUI action runs it on the main thread and stutters the UI, so the write is
    /// pushed to a background task and only the state assignment comes back to the main
    /// actor — the same split the Android demo makes with Dispatchers.IO.
    private func perform(_ work: @escaping () -> Void) {
        _Concurrency.Task.detached(priority: .userInitiated) {
            work()
            await reload()
        }
    }

    @MainActor
    private func reload() async {
        let loaded = await _Concurrency.Task.detached(priority: .userInitiated) {
            (DatabaseKt.getAllTasks(), DatabaseKt.getAllUsers())
        }.value
        tasks = loaded.0
        users = loaded.1
    }
}

// MARK: - Task Row

struct TaskRow: View {
    let task: Task
    let onToggle: () -> Void

    var body: some View {
        HStack {
            Image(systemName: task.isCompleted ? "checkmark.circle.fill" : "circle")
                .foregroundColor(task.isCompleted ? .green : .gray)

            VStack(alignment: .leading, spacing: 4) {
                Text(task.title)
                    .font(.headline)
                    .strikethrough(task.isCompleted)
                    .foregroundColor(task.isCompleted ? .secondary : .primary)

                if !task.description_.isEmpty {
                    Text(task.description_)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                HStack {
                    PriorityBadge(priority: task.priority)
                    if let user = task.user {
                        Text(user.name ?? "")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }

            Spacer()
        }
        .contentShape(Rectangle())
        .onTapGesture { onToggle() }
    }
}

// MARK: - Priority Badge

struct PriorityBadge: View {
    let priority: Priority

    var color: Color {
        switch priority {
        case .low: return .green
        case .medium: return .orange
        case .high: return .red
        default: return .gray
        }
    }

    var body: some View {
        Text(priority.name)
            .font(.caption2)
            .fontWeight(.medium)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(color.opacity(0.15))
            .foregroundColor(color)
            .cornerRadius(6)
    }
}

// MARK: - Add Task Sheet

struct AddTaskView: View {
    let users: [User]
    let onAdd: (String, String, Priority, User) -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var title = ""
    @State private var desc = ""
    @State private var priority: Priority = .medium
    @State private var selectedUser: User?

    var body: some View {
        NavigationStack {
            Form {
                TextField("Title", text: $title)
                TextField("Description", text: $desc)

                Picker("Priority", selection: $priority) {
                    Text("Low").tag(Priority.low)
                    Text("Medium").tag(Priority.medium)
                    Text("High").tag(Priority.high)
                }

                Picker("Owner", selection: $selectedUser) {
                    ForEach(users, id: \.id) { user in
                        Text(user.name ?? "").tag(Optional(user))
                    }
                }
            }
            .navigationTitle("New Task")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        if let owner = selectedUser {
                            onAdd(title, desc, priority, owner)
                        }
                        dismiss()
                    }
                    .disabled(title.isEmpty || selectedUser == nil)
                }
            }
            .onAppear {
                // Only seed the picker, never overwrite a choice already made: onAppear
                // fires again when the sheet comes back to the foreground.
                if selectedUser == nil { selectedUser = users.first }
            }
        }
    }
}
