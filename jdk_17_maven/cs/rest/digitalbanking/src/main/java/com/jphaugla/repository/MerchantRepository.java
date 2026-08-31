package com.jphaugla.repository;

import com.jphaugla.domain.Merchant;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jphaugla.domain.Transaction;
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

public class MerchantRepository{
	@Value("${app.merchantSearchIndexName}")
	private String merchantSearchIndexName;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	public MerchantRepository() {

		log.info("MerchantRepository constructor");
	}

	public String create(Merchant merchant) {

		Map<Object, Object> merchantHash = objectMapper.convertValue(merchant, Map.class);
		stringRedisTemplate.opsForHash().putAll(makeKey( merchant.getName()), merchantHash);
		// redisTemplate.opsForHash().putAll("Merchant:" + merchant.getMerchantId(), merchantHash);
		// logger.info(String.format("Merchant with ID %s saved", merchant.getName()));
		return "Success\n";
	}

	public Merchant get(String merchantId) {
		log.info("in MerchantRepository.get with merchant id=" + merchantId);
		String fullKey = makeKey(merchantId);
		Map<Object, Object> merchantHash = stringRedisTemplate.opsForHash().entries(fullKey);
		Merchant merchant = objectMapper.convertValue(merchantHash, Merchant.class);
		return (merchant);
	}
	public String createAll(List<Merchant> merchantList) {
		for (Merchant merchant : merchantList) {
			create(merchant);
		}
		return "Success\n";
	}

    private String makeKey(String merchantId) {
		return merchantSearchIndexName + ':' + merchantId;
	}
}

