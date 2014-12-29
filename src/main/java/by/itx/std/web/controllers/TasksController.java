package by.itx.std.web.controllers;

import by.itx.std.dao.entities.Task;
import by.itx.std.dao.repositories.TasksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TasksController extends BaseRestController<Task, Long> {

    @Autowired
    public TasksController(TasksRepository repository) {
        super(repository);
    }

    @Override
    protected String getName() {
        return "TasksController";
    }
}
