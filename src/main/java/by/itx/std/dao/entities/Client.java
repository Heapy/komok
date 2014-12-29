package by.itx.std.dao.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    private String login;

    private String password;

    @OneToMany
    private List<Task> tasks;

    public Long getId() {
        return id;
    }

    public Client setId(Long id) {
        this.id = id;
        return this;
    }

    public String getLogin() {
        return this.login;
    }

    public Client setLogin(String login) {
        this.login = login;
        return this;
    }

    public Client setPassword(String password) {
        this.password = password;
        return this;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public Client setTasks(List<Task> tasks) {
        this.tasks = tasks;
        return this;
    }
}
