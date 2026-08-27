package id.my.hendisantika.springbootredissample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

@EnableCaching // Enables Spring’s annotation-driven cache management capability
@SpringBootApplication
public class SpringBootRedisSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootRedisSampleApplication.class, args);
    }

    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<?, ?> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * Creates and configures a RedisCacheManager bean.
     * RedisCacheManager: Manages caching operations and configurations for Redis.
     * RedisCacheConfiguration: Configures caching behavior, including cache name prefixes, TTL, and whether null values should be cached.
     *
     * @param connectionFactory the RedisConnectionFactory used to connect to the Redis server.
     * @return a configured RedisCacheManager instance.
     * - **Prefix Cache Names:** Cache names are prefixed with the package name of the class to avoid naming collisions.
     * - **Entry TTL (Time-to-Live):** Cache entries will expire after 1 hour.
     * - **Disable Caching Null Values:** Null values are not cached.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(this.getClass().getPackageName() + ".")
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
    }

}
