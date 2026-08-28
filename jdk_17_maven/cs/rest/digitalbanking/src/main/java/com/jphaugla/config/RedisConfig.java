package com.jphaugla.config;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import redis.clients.jedis.JedisPoolConfig;


import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.core.env.Environment;

import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@EnableAsync
@EnableRedisRepositories
@EnableAutoConfiguration
@ComponentScan("com.jphaugla")
@Slf4j
public class RedisConfig {
    @Autowired
    private Environment env;
    private @Value("${spring.redis.timeout}")
    Duration redisCommandTimeout;
    

    @Bean(name = "redisConnectionFactory")
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(50);
        poolConfig.setMaxTotal(50);
        JedisClientConfiguration.JedisClientConfigurationBuilder clientConfig = JedisClientConfiguration.builder();
        clientConfig.usePooling().poolConfig(poolConfig);
        JedisConnectionFactory jedisConnectionFactory;
        RedisStandaloneConfiguration redisServerConf = new RedisStandaloneConfiguration();
        String hostname = env.getProperty("spring.redis.host", "localhost");
        String port = env.getProperty("spring.redis.port", "6379");
        String password = env.getProperty("spring.redis.password");
        redisServerConf.setHostName(hostname);
        redisServerConf.setPort(Integer.parseInt(port));
        log.info("hostname is " + hostname + " port is " + port);
        if(password != null && !password.isEmpty()) {
            redisServerConf.setPassword(RedisPassword.of(password));
            log.info("password is " + password);
        }
        jedisConnectionFactory = new JedisConnectionFactory(redisServerConf, clientConfig.build());
        return jedisConnectionFactory;
    }

    @Bean
    @Primary
    public RedisTemplate<Object, Object> redisTemplateW1(@Qualifier("redisConnectionFactory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // redisTemplate.setHashValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        // redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    @Bean("threadPoolTaskExecutor")
    public TaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        on large 64 core machine, drove setCorePoolSize to 200 to really spike performance
        executor.setCorePoolSize(100);
        executor.setMaxPoolSize(1000);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("Async-");
        return executor;
    }
    /* @Bean
    public KafkaAdmin admin()
    {
        Map<String, Object> configs = new HashMap<>();
        String kafkaBootstrap = env.getProperty("spring.kafka.producer.bootstrap-servers");
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaBootstrap);
        return new KafkaAdmin(configs);
    }
    @Bean
    public NewTopic topic1()
    {
        String topic_name=env.getProperty("topic.name.producer");
        return TopicBuilder.name(topic_name)
                .partitions(1)
                .replicas(1)
                .build();
    }

     */
}
