package by.itx.std.web.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.Serializable;
import java.util.List;

public abstract class BaseRestController<T, ID extends Serializable> {

    private final static Logger LOGGER = LoggerFactory.getLogger(BaseRestController.class);

    private JpaRepository<T, ID> repository;

    protected abstract String getName();

    public BaseRestController(JpaRepository<T, ID> repository) {
        this.repository = repository;
    }

    @RequestMapping(value = {"/", ""}, method = RequestMethod.GET)
    public List<T> list() {
        LOGGER.trace("List items {}.", getName());
        return repository.findAll();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public T getOne(@PathVariable("id") ID id) {
        LOGGER.trace("Get item from {} for id={}.", getName(), id);
        return repository.getOne(id);
    }

    @RequestMapping(value = {"/", ""}, method = {RequestMethod.POST, RequestMethod.PUT})
    public T save(T entity) {
        LOGGER.trace("Update item from {}.", getName());
        return repository.save(entity);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("id") ID id) {
        repository.delete(id);
    }
}
