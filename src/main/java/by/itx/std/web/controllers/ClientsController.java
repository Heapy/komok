package by.itx.std.web.controllers;

import by.itx.std.dao.entities.Client;
import by.itx.std.dao.repositories.ClientsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientsController extends BaseRestController<Client, Long> {

    @Autowired
    public ClientsController(ClientsRepository repository) {
        super(repository);
    }

    @Override
    protected String getName() {
        return "ClientsController";
    }
}
