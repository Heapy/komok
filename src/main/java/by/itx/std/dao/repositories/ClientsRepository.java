package by.itx.std.dao.repositories;

import by.itx.std.dao.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientsRepository extends JpaRepository<Client, Long> {
    // Add custom methods here
}
