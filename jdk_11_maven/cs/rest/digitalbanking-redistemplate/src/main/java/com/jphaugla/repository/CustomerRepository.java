package com.jphaugla.repository;

import com.jphaugla.domain.Customer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
@Repository

public class CustomerRepository{
	private static final String KEY = "Customer";


	final Logger logger = LoggerFactory.getLogger(CustomerRepository.class);
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	@Qualifier("redisTemplateW1")
	private RedisTemplate<Object, Object> redisTemplateW1;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public CustomerRepository() {

		logger.info("CustomerRepository constructor");
	}

	public String create(Customer customer) {
		if (customer.getCreatedDatetime() == null) {
			Long currentTimeMillis = System.currentTimeMillis();
			customer.setCreatedDatetime(currentTimeMillis);
			customer.setLastUpdated(currentTimeMillis);
		}

		Map<Object, Object> customerHash = mapper.convertValue(customer, Map.class);
		redisTemplateW1.opsForHash().putAll("Customer:" + customer.getCustomerId(), customerHash);
		// redisTemplate.opsForHash().putAll("Customer:" + customer.getCustomerId(), customerHash);
		logger.info(String.format("Customer with ID %s saved", customer.getCustomerId()));
		return "Success\n";
	}

	public Customer get(String customerId) {
		logger.info("in CustomerRepository.get with customer id=" + customerId);
		String fullKey = "Customer:" + customerId;
		Map<Object, Object> customerHash = stringRedisTemplate.opsForHash().entries(fullKey);
		Customer customer = mapper.convertValue(customerHash, Customer.class);
		return (customer);
	}

}
