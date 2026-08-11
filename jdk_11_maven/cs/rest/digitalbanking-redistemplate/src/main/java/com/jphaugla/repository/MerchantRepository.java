package com.jphaugla.repository;

import com.jphaugla.domain.Merchant;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jphaugla.domain.Transaction;
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

public class MerchantRepository{
	private static final String KEY = "Merchant";


	final Logger logger = LoggerFactory.getLogger(com.jphaugla.repository.MerchantRepository.class);
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	@Qualifier("redisTemplateW1")
	private RedisTemplate<Object, Object> redisTemplateW1;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public MerchantRepository() {

		logger.info("MerchantRepository constructor");
	}

	public String create(Merchant merchant) {

		Map<Object, Object> merchantHash = mapper.convertValue(merchant, Map.class);
		redisTemplateW1.opsForHash().putAll("Merchant:" + merchant.getName(), merchantHash);
		// redisTemplate.opsForHash().putAll("Merchant:" + merchant.getMerchantId(), merchantHash);
		logger.info(String.format("Merchant with ID %s saved", merchant.getName()));
		return "Success\n";
	}

	public Merchant get(String merchantId) {
		logger.info("in MerchantRepository.get with merchant id=" + merchantId);
		String fullKey = "Merchant:" + merchantId;
		Map<Object, Object> merchantHash = stringRedisTemplate.opsForHash().entries(fullKey);
		Merchant merchant = mapper.convertValue(merchantHash, Merchant.class);
		return (merchant);
	}
	public String createAll(List<Merchant> merchantList) {
		for (Merchant merchant : merchantList) {
			create(merchant);
		}
		return "Success\n";
	}


}

