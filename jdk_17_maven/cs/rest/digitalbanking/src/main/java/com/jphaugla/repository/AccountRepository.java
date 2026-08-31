package com.jphaugla.repository;
import com.jphaugla.domain.Account;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jphaugla.domain.Account;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class AccountRepository {

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	@Qualifier("redisTemplateW1")
	private RedisTemplate<Object, Object> redisTemplateW1;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	@Value("${app.accountSearchIndexName}")
	private String accountSearchIndexName;

	public AccountRepository() {

		log.info("AccountRepository constructor");
	}

	public String create(Account account) {
		log.info("AccountRepository create with index=" + accountSearchIndexName);
		log.info(" account " + account.toString());
		if (account.getCreatedDatetime() == null) {
			long currentTimeMillis = System.currentTimeMillis();
			String stringMillis = Long.toString(currentTimeMillis);
			account.setCreatedDatetime(stringMillis);
			account.setOpenDatetime(stringMillis);
			account.setLastUpdated(stringMillis);
		}

		Map<Object, Object> AccountHash = objectMapper.convertValue(account, Map.class);
		// while (AccountHash.values().remove(null));
		// logger.info( "before null removal with AccountHash " + AccountHash.toString());
		AccountHash.values().removeIf(Objects::isNull);

        // logger.info( "before putall with AccountHash " + AccountHash.toString());
		stringRedisTemplate.opsForHash().putAll(makeKey(account.getAccountNo()), AccountHash);
		// redisTemplate.opsForHash().putAll("Account:" + Account.getAccountId(), AccountHash);
		// logger.info(String.format("Account with ID %s saved", account.getAccountNo()));
		return "Success\n";
	}

	public Account get(String accountId) {
		log.info("in AccountRepository.get with Account id=" + accountId);
		String fullKey = makeKey(accountId);
		Map<Object, Object> AccountHash = stringRedisTemplate.opsForHash().entries(fullKey);
		Account account = objectMapper.convertValue(AccountHash, Account.class);
		return (account);
	}


	public void createAll(List<Account> accounts) {
		for (Account account : accounts) {
			create(account);
		}
	}

	private String makeKey(String accountId) {
		return accountSearchIndexName + ':' + accountId;
	}
}