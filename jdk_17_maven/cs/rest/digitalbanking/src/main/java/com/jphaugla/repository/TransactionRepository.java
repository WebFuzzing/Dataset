package com.jphaugla.repository;

import com.jphaugla.domain.Transaction;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;


import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
@Repository
@Slf4j
public class TransactionRepository{
	@Value("${app.transactionSearchIndexName}")
	private String transactionSearchIndexName;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public TransactionRepository() {

		log.info("TransactionRepository constructor");
	}

	public String create(Transaction transaction) {
		log.info("entering TransactionReposistory create transaction " + transaction.toString());
		if (transaction.getInitialdate() == null) {
			long currentTimeMillis = System.currentTimeMillis();
			transaction.setInitialdate(Long.toString(currentTimeMillis));
		}

		Map<Object, Object> transactionHash = objectMapper.convertValue(transaction, Map.class);
		//  remove null map values
		while (transactionHash.values().remove(null));
		String fullKey = makeKey(transaction.getTranid());
		log.info("full key is " + fullKey);
		stringRedisTemplate.opsForHash().putAll(fullKey, transactionHash);
		// redisTemplate.opsForHash().putAll("Transaction:" + transaction.getTransactionId(), transactionHash);
		// logger.info(String.format("Transaction with ID %s saved", transaction.getTranId()));
		return fullKey;
	}

	public String create (Transaction transaction, Boolean doExpire) {
		String returnKey = create(transaction);
		stringRedisTemplate.expire(returnKey, Duration.ofDays(2));
		return returnKey;
	}

	public String createAll(List<Transaction> transactionList) {
		for (Transaction transaction : transactionList) {
			create(transaction);
		}
		return "Success\n";
	}

	public Transaction get(String tranId) {
		log.info("in TransactionRepository.get with transaction id=" + tranId);
		String fullKey = makeKey(tranId);
		Map<Object, Object> transactionHash = stringRedisTemplate.opsForHash().entries(fullKey);
		Transaction transaction = objectMapper.convertValue(transactionHash, Transaction.class);
		return (transaction);
	}

	public String getAmount(String tranId) {
		log.info("in TransactionRepository.getAmount with transaction id=" + tranId);
		String fullKey = makeKey(tranId);
		String amount = (String) stringRedisTemplate.opsForHash().get(fullKey, "amount");
		return amount;
	}
	public void updateStatus(String tranId, String targetStatus, String timeString) {
		stringRedisTemplate.opsForHash().put(makeKey(tranId),
				"status", targetStatus);
		if (targetStatus.equals("POSTED")) {
			updateDate(tranId, "postingDate", timeString);
		} else {
			updateDate(tranId, "settlementDate", timeString);
		}
	}

	public void updateDate(String tranId, String dateField, String timeString) {
		stringRedisTemplate.opsForHash().put(makeKey(tranId), dateField,
				timeString);
	}

	public void addTags(String tranId, String tagDelimitedString) {
		stringRedisTemplate.opsForHash().put(makeKey(tranId), "transactionTags",
				tagDelimitedString);
	}
	public String getTags(String tranId) {
		log.info("in getTransactionTagList with transactionID=" + tranId);
		// hold set of transactions for a tag on an account
		String transactionKey = makeKey(tranId);
		String existingTags = (String) stringRedisTemplate.opsForHash().get(transactionKey,
				"transactionTags");
		return existingTags;
	}

	public void addDispute(String tranId, String disputeId) {
		String transactionKey = makeKey(tranId);
		stringRedisTemplate.opsForHash().put(transactionKey, "disputeid", disputeId);

	}
	 public String makeKey(String tranId) {
		return transactionSearchIndexName + ":" + tranId;
	}
}
