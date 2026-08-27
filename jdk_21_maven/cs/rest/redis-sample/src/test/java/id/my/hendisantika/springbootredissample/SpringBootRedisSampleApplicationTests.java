package id.my.hendisantika.springbootredissample;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class SpringBootRedisSampleApplicationTests {

    private static final String REDIS_IMAGE_NAME = "redis:7.0-alpine";
    private static final int REDIS_PORT = 6379;
    @Container
    private static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE_NAME))
                    .withExposedPorts(REDIS_PORT);
    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeAll
    static void beforeAll() {
        // No need to start the container here, @Container will handle it
    }

    @AfterAll
    static void afterAll() {
        // No need to stop the container here, @Container will handle it
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(REDIS_PORT));
    }

    @Test
    void testSaveKeyValueAndGetValue() {
        final String key = "testKey", value = "testValue";

        redisTemplate.opsForValue().set(key, value);

        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(value);
    }

}
