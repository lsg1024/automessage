package excel.automessage;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-api.properties")
@ActiveProfiles("test")
@Transactional
public abstract class BaseTest {
}
