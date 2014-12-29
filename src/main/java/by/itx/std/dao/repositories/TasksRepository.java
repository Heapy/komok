package by.itx.std.dao.repositories;

import by.itx.std.dao.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TasksRepository extends JpaRepository<Task, Long> {
    // Add custom methods here
}
