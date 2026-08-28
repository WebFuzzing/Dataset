package com.jphaugla.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;

import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableCassandraRepositories(basePackages = {"com.jphaugla"})
@ConfigurationProperties(prefix = "spring.data.cassandra")
@Getter
@Setter
@Slf4j
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${spring.data.cassandra.keyspace-name}")
    private String keyspaceName;
    @Value("${spring.data.cassandra.contact-points}")
    private String contactPoints;
    @Value("${spring.data.cassandra.port}")
    private int port;
    @Value("${spring.data.cassandra.local-datacenter}")
    private String localDataCenter;
  //  @Value("${spring.data.cassandra.username}")
   // private String username;
  //  @Value("${spring.data.cassandra.password}")
  //  private String password;


    @Bean
    @Override
    public CqlSessionFactoryBean cassandraSession() {
        CqlSessionFactoryBean cassandraSession = super.cassandraSession();//super session should be called only once
       // cassandraSession.setUsername(username);
       // cassandraSession.setPassword(password);
        log.info("cassandra contact-points " + contactPoints);
        log.info("cassandra data center " + localDataCenter);
       // log.info("cassandra username " + username);
       // log.info("cassandra password " + password);
        log.info("cassandra keyspace " + keyspaceName);
        log.info("cassandra port " + port);
        return cassandraSession;
    }


}
