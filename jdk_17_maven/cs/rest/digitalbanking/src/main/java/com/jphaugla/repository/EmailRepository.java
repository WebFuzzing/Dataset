package com.jphaugla.repository;

import com.jphaugla.domain.Email;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;


import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;


@Slf4j
@Repository

public class EmailRepository{

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	@Qualifier("redisTemplateW1")
	private RedisTemplate<Object, Object> redisTemplateW1;
	@Value("${app.emailSearchIndexName}")
	private String emailSearchIndexName;
	@Value("${app.customerSearchIndexName}")
	private String customerSearchIndexName;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public EmailRepository() {

		log.info("EmailRepository constructor");
	}

	public String create(Email email) {

		Map<Object, Object> emailHash = objectMapper.convertValue(email, Map.class);
		stringRedisTemplate.opsForHash().putAll(makeKey( email.getEmailAddress()), emailHash);
		// for demo purposed add a member to the set for the Customer
		stringRedisTemplate.opsForSet().add("CustEmail:" + email.getCustomerId(), email.getEmailAddress());
		// redisTemplate.opsForHash().putAll("Email:" + email.getEmailId(), emailHash);
		// logger.info(String.format("Email with ID %s saved", email.getEmailAddress()));
		return "Success\n";
	}

	public Email get(String emailId) {
		log.info("in EmailRepository.get with email id=" + emailId);
		String fullKey = makeKey(emailId);
		Map<Object, Object> emailHash = stringRedisTemplate.opsForHash().entries(fullKey);
		Email email = objectMapper.convertValue(emailHash, Email.class);
		return (email);
	}
    //  this is sample code demonstrating removing all the emails for a customer without using redisearch
	public void delete(String emailId) {
		log.info("in emailrepository.delete with emailId " + emailId);
		String fullKey = makeKey(emailId);
		stringRedisTemplate.delete(fullKey);
	}

	public int deleteCustomerEmails (String customerId) {
		log.info("in EmailRepository.deleteCustomerEmails with custid " + customerId);
		String custEmailKey = "CustEmail" + ':' + customerId;
		String fullEmailKey;
		Set<String> emailsToDelete = stringRedisTemplate.opsForSet().members(custEmailKey);
		int emailCount = emailsToDelete.size();
		for (String emailKey : emailsToDelete) {
			fullEmailKey = makeKey(emailKey);
			log.info("emailKey to delete is " + fullEmailKey);
			stringRedisTemplate.delete(fullEmailKey);
		}
		stringRedisTemplate.delete(custEmailKey);
		return emailCount;
	}
    private String makeKey(String emailKey) {
		return emailSearchIndexName + ':' + emailKey;
	}

}
