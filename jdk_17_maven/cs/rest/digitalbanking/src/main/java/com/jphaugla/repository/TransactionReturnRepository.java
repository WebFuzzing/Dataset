package com.jphaugla.repository;

import com.jphaugla.domain.Transaction;
import com.jphaugla.domain.TransactionReturn;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
@Slf4j
@Repository

public class TransactionReturnRepository{

	@Autowired
	ObjectMapper objectMapper;

	@Value("${app.transactionReturnSearchIndexName}")
	private String transactionReturnSearchIndexName;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public TransactionReturnRepository() {

		log.info("TransactionReturnRepository constructor");
	}

	public String create(TransactionReturn transactionReturn) {
		Map<Object, Object> transactionReturnHash = objectMapper.convertValue(transactionReturn, Map.class);
		stringRedisTemplate.opsForHash().putAll(makeKey(transactionReturn.getReasonCode()), transactionReturnHash);
		// redisTemplate.opsForHash().putAll("TransactionReturn:" + transactionReturn.getTransactionReturnId(), transactionReturnHash);
		log.info(String.format("TransactionReturn with ID %s saved", transactionReturn.getReasonCode()));
		return "Success\n";
	}
	public String createAll(List<TransactionReturn> transactionReturnList) {
		for (TransactionReturn transactionReturn : transactionReturnList) {
			create(transactionReturn);
		}
		return "Success\n";
	}

	public TransactionReturn get(String transactionReturnId) {
		log.info("in TransactionReturnRepository.get with transactionReturn id=" + transactionReturnId);
		String fullKey = makeKey(transactionReturnId);
		Map<Object, Object> transactionReturnHash = stringRedisTemplate.opsForHash().entries(fullKey);
		TransactionReturn transactionReturn = objectMapper.convertValue(transactionReturnHash, TransactionReturn.class);
		return (transactionReturn);
	}
    private String makeKey(String transactionReturnId) {
		return transactionReturnSearchIndexName + ':' + transactionReturnId;
	}

}
