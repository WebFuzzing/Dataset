package com.jphaugla.repository;

import com.jphaugla.domain.Customer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository

public class CustomerRepository{


    final Logger logger = LoggerFactory.getLogger(CustomerRepository.class);
    @Autowired
    ObjectMapper objectMapper;

    @Value("${app.customerSearchIndexName}")
    private String customerSearchIndexName;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public CustomerRepository() {

        logger.info("CustomerRepository constructor");
    }

    public String create(Customer customer) {
        if (customer.getCreatedDatetime() == null) {
            Long currentTimeMillis = System.currentTimeMillis();
            customer.setCreatedDatetime(Long.toString(currentTimeMillis));
            customer.setLastUpdated(Long.toString(currentTimeMillis));
        }

        Map<Object, Object> customerHash = objectMapper.convertValue(customer, Map.class);
        customerHash.values().removeIf(Objects::isNull);

        stringRedisTemplate.opsForHash().putAll(makeKey(customer.getCustomerId()), customerHash);
        // redisTemplate.opsForHash().putAll("Customer:" + customer.getCustomerId(), customerHash);
        // logger.info(String.format("Customer with ID %s saved", customer.getCustomerId()));
        return "Success\n";
    }

    public Customer get(String customerId) {
        // logger.info("in CustomerRepository.get with customer id=" + customerId);
        String fullKey = makeKey(customerId);
        Map<Object, Object> customerHash = stringRedisTemplate.opsForHash().entries(fullKey);
        Customer customer = objectMapper.convertValue(customerHash, Customer.class);
        return (customer);
    }
    private String makeKey(String customerId) {
        return customerSearchIndexName + ':' + customerId;
    }

}
