package excel.automessage;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-api.properties")
@ActiveProfiles("test")
public abstract class BaseTest {
}
