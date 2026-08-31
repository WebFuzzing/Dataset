package com.jphaugla.repository;

import com.jphaugla.domain.Phone;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
@Repository

public class PhoneRepository{
	@Value("${app.phoneSearchIndexName}")
	private String phoneSearchIndexName;

	final Logger logger = LoggerFactory.getLogger(com.jphaugla.repository.PhoneRepository.class);
	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public PhoneRepository() {

		logger.info("PhoneRepository constructor");
	}

	public String create(Phone phone) {

		Map<Object, Object> phoneHash = objectMapper.convertValue(phone, Map.class);
		stringRedisTemplate.opsForHash().putAll(makeKey(phone.getPhoneNumber()), phoneHash);
		// redisTemplate.opsForHash().putAll("Phone:" + phone.getPhoneId(), phoneHash);
		// logger.info(String.format("Phone with ID %s saved", phone.getPhoneNumber()));
		return "Success\n";
	}

	public Optional<Phone> get(String phoneId) {
		logger.info("in Phone Repository.get with phone id=" + phoneId);
		String fullKey = makeKey(phoneId);
		Map<Object, Object> phoneHash = stringRedisTemplate.opsForHash().entries(fullKey);
		logger.info("Full key is " + fullKey + " phoneHash is " + phoneHash);
		Phone phone = objectMapper.convertValue(phoneHash, Phone.class);
		logger.info("return phone " + phone.getPhoneNumber() + ":" + phone.getPhoneLabel() + ":" + phone.getCustomerId());
		return Optional.ofNullable((phone));
	}

    private String makeKey(String phoneId) {
		return phoneSearchIndexName + ':' + phoneId;
	}
}
