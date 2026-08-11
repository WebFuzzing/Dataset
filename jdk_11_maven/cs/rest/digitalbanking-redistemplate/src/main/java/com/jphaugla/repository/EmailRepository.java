package com.jphaugla.repository;

import com.jphaugla.domain.Email;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
@Repository

public class EmailRepository{
	private static final String KEY = "Email";


	final Logger logger = LoggerFactory.getLogger(com.jphaugla.repository.EmailRepository.class);
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	@Qualifier("redisTemplateW1")
	private RedisTemplate<Object, Object> redisTemplateW1;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public EmailRepository() {

		logger.info("EmailRepository constructor");
	}

	public String create(Email email) {

		Map<Object, Object> emailHash = mapper.convertValue(email, Map.class);
		redisTemplateW1.opsForHash().putAll("Email:" + email.getEmailAddress(), emailHash);
		// for demo purposed add a member to the set for the Customer
		stringRedisTemplate.opsForSet().add("CustEmail:" + email.getCustomerId(), email.getEmailAddress());
		// redisTemplate.opsForHash().putAll("Email:" + email.getEmailId(), emailHash);
		logger.info(String.format("Email with ID %s saved", email.getEmailAddress()));
		return "Success\n";
	}

	public Email get(String emailId) {
		logger.info("in EmailRepository.get with email id=" + emailId);
		String fullKey = "Email:" + emailId;
		Map<Object, Object> emailHash = stringRedisTemplate.opsForHash().entries(fullKey);
		Email email = mapper.convertValue(emailHash, Email.class);
		return (email);
	}
    //  this is sample code demonstrating removing all the emails for a customer without using redisearch
	public void delete(String emailId) {
		logger.info("in emailrepository.delete with emailId " + emailId);
		String fullKey = "Email:" + emailId;
		stringRedisTemplate.delete(fullKey);
	}

	public int deleteCustomerEmails (String customerId) {
		logger.info("in EmailRepository.deleteCustomerEmails with custid " + customerId);
		String custEmailKey = "CustEmail:"+ customerId;
		String fullEmailKey;
		Set<String> emailsToDelete = stringRedisTemplate.opsForSet().members(custEmailKey);
		int emailCount = emailsToDelete.size();
		for (String emailKey : emailsToDelete) {
			fullEmailKey = "Email:" + emailKey;
			logger.info("emailKey to delete is " + fullEmailKey);
			stringRedisTemplate.delete(fullEmailKey);
		}
		stringRedisTemplate.delete(custEmailKey);
		return emailCount;
	}


}
