package demo;

import onl.ycode.stormify.AutoTable;
import onl.ycode.stormify.DbTable;
import onl.ycode.stormify.DbField;
import onl.ycode.stormify.DbValue;

/**
 * Task entity using Stormify annotations and AutoTable for lazy-loaded references.
 * The {@code user} field is a reference to a User entity — Stormify resolves it
 * automatically from the {@code user_id} foreign key column.
 *
 * <p>Non-primary-key getters/setters call {@code hydrate()} to trigger lazy loading
 * when the entity was obtained as a reference (e.g. from another entity's foreign key).
 */
@DbTable
public class Task extends AutoTable {

    @DbField(primaryKey = true, autoIncrement = true)
    private Integer id;

    private String title;

    private String description;

    private boolean isCompleted;

    private Priority priority;

    @DbField(name = "user_id")
    private User user;

    public Task() {
    }

    public Task(Integer id, String title, String description, boolean isCompleted, Priority priority, User user) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isCompleted = isCompleted;
        this.priority = priority;
        this.user = user;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { hydrate(); return title; }
    public void setTitle(String title) { hydrate(); this.title = title; }

    public String getDescription() { hydrate(); return description; }
    public void setDescription(String description) { hydrate(); this.description = description; }

    public boolean isCompleted() { hydrate(); return isCompleted; }
    public void setCompleted(boolean completed) { hydrate(); isCompleted = completed; }

    public Priority getPriority() { hydrate(); return priority; }
    public void setPriority(Priority priority) { hydrate(); this.priority = priority; }

    public User getUser() { hydrate(); return user; }
    public void setUser(User user) { hydrate(); this.user = user; }

    @Override
    public String toString() {
        return "Task(id=" + id + ", title=" + getTitle() + ", completed=" + isCompleted() +
                ", priority=" + getPriority() + ", user=" + getUser() + ")";
    }
}
