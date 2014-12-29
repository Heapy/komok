package by.itx.std.dao.repositories;

import by.itx.std.SpringTestBase;
import by.itx.std.dao.entities.Task;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

@Transactional
public class TasksRepositoryTest extends SpringTestBase {

    @Autowired
    private TasksRepository repository;

    @Test
    public void testCRD() throws Exception {
        Task task = new Task();
        task.setTitle("Test");
        Long id = repository.save(task).getId();
        assertNotNull(task.getId());
        repository.delete(id);
        assertFalse(repository.exists(id));
    }

}