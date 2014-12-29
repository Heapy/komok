package by.itx.std.dao.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    private String title;

    @ManyToOne
    private Client client;

    public Long getId() {
        return id;
    }

    public Task setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return this.title;
    }

    public Task setTitle(String title) {
        this.title = title;
        return this;
    }

    public Client getClient() {
        return client;
    }

    public Task setClient(Client client) {
        this.client = client;
        return this;
    }
}
