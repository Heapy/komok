package by.itx.std.dao;

import by.itx.std.dao.entities.Task;
import by.itx.std.dao.entities.Client;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.Ignore;
import org.junit.Test;

public class ExportDatabaseSchema {

    @Test
    @Ignore(value = "This is not test in fact. Just easy way to generate current SQL from model.")
    public void export() throws Exception {
        Configuration configuration = new Configuration();

        configuration
                .addAnnotatedClass(Task.class)
                .addAnnotatedClass(Client.class)
                .setProperty(Environment.DIALECT, "org.hibernate.dialect.PostgreSQL9Dialect");

        SchemaExport schema = new SchemaExport(configuration);
        schema.setDelimiter(";");
        schema.setFormat(true);
        schema.setOutputFile("schema.sql");
        schema.create(true, false);
    }
}
