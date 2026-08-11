package com.jphaugla.repository;
import com.jphaugla.domain.Account;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jphaugla.domain.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {
	private static final String KEY = "Account";


	final Logger logger = LoggerFactory.getLogger(AccountRepository.class);
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	@Qualifier("redisTemplateW1")
	private RedisTemplate<Object, Object> redisTemplateW1;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public AccountRepository() {

		logger.info("AccountRepository constructor");
	}

	public String create(Account account) {
		if (account.getCreatedDatetime() == null) {
			Long currentTimeMillis = System.currentTimeMillis();
			account.setCreatedDatetime(currentTimeMillis);
			account.setOpenDatetime(currentTimeMillis);
			account.setLastUpdated(currentTimeMillis);
		}

		Map<Object, Object> AccountHash = mapper.convertValue(account, Map.class);
		redisTemplateW1.opsForHash().putAll("Account:" + account.getAccountNo(), AccountHash);
		// redisTemplate.opsForHash().putAll("Account:" + Account.getAccountId(), AccountHash);
		logger.info(String.format("Account with ID %s saved", account.getAccountNo()));
		return "Success\n";
	}

	public Account get(String accountId) {
		logger.info("in AccountRepository.get with Account id=" + accountId);
		String fullKey = "Account:" + accountId;
		Map<Object, Object> AccountHash = stringRedisTemplate.opsForHash().entries(fullKey);
		Account account = mapper.convertValue(AccountHash, Account.class);
		return (account);
	}


	public void createAll(List<Account> accounts) {
		for (Account account : accounts) {
			create(account);
		}
	}
}