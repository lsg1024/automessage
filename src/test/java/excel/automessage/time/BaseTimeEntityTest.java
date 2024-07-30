package excel.automessage.time;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class BaseTimeEntityTest {

    @Autowired
    private TestEntityRepository testEntityRepository;

    @Test
    public void testCreateDate() {
        // Given
        TestEntity testEntity = new TestEntity();

        // When
        TestEntity savedEntity = testEntityRepository.save(testEntity);

        // Then
        assertThat(savedEntity.getCreateDate()).isNotNull();
        System.out.println("Create Date: " + savedEntity.getCreateDate());
    }

    @Test
    @Transactional
    public void testLastModifiedDate() throws InterruptedException {
        // Given
        TestEntity testEntity = new TestEntity();
        TestEntity savedEntity = testEntityRepository.save(testEntity);

        // When
        Thread.sleep(1000); // Modify time delay
        savedEntity = testEntityRepository.save(savedEntity);

        // Then
        assertThat(savedEntity.getLastModifiedDate()).isNotNull();
        System.out.println("Last Modified Date: " + savedEntity.getLastModifiedDate());
    }

//    interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
//    }
}

