package by.itx.std;

import by.itx.std.config.LoggingInitializer;
import by.itx.std.utils.Profiles;
import by.itx.std.dao.config.DaoConfig;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ActiveProfiles(Profiles.TEST)
@ContextConfiguration(classes = { DaoConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class SpringTestBase {

    @BeforeClass
    public static void init() {
        new LoggingInitializer().contextInitialized(null);
    }
}
