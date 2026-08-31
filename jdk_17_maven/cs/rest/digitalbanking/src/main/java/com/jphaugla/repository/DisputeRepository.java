package com.jphaugla.repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jphaugla.domain.Dispute;
import lombok.extern.slf4j.Slf4j;


import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository

public class DisputeRepository{


    @Autowired
    ObjectMapper objectMapper;
    
    @Value("${app.disputeSearchIndexName}")
    private String disputeSearchIndexName;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public DisputeRepository() {

        log.info("DisputeRepository constructor");
    }

    private String makeKey(String disputeId) {
        return (disputeSearchIndexName + ':' + disputeId);
    }

    public String create(Dispute dispute) {
        if (dispute.getFilingDate() == null) {
            Long currentTimeMillis = System.currentTimeMillis();
            dispute.setFilingDate(Long.toString(currentTimeMillis));
            dispute.setLastUpdateDate(Long.toString(currentTimeMillis));
        }

        Map<Object, Object> DisputeHash = objectMapper.convertValue(dispute, Map.class);
        DisputeHash.values().removeIf(Objects::isNull);
        String fullKey = makeKey(dispute.getDisputeId());
        stringRedisTemplate.opsForHash().putAll(makeKey(dispute.getDisputeId()), DisputeHash);
        log.info(String.format("Dispute with ID %s saved", fullKey));
        return "Success\n";
    }

    public Dispute get(String disputeId) {
        // logger.info("in DisputeRepository.get with Dispute id=" + DisputeId);
        Map<Object, Object> DisputeHash = stringRedisTemplate.opsForHash().entries(makeKey(disputeId));
        Dispute dispute = objectMapper.convertValue(DisputeHash, Dispute.class);
        return (dispute);
    }

    public void setChargeBackReason(String disputeId, String chargeBackReason) {
        Long currentTimeMillis = System.currentTimeMillis();
        stringRedisTemplate.opsForHash().put(makeKey(disputeId),"lastUpdateDate", Long.toString(currentTimeMillis));
        stringRedisTemplate.opsForHash().put(makeKey(disputeId),"reasonCode", chargeBackReason);
        stringRedisTemplate.opsForHash().put(makeKey(disputeId),"reviewDate", Long.toString(currentTimeMillis));
        stringRedisTemplate.opsForHash().put(makeKey(disputeId),"status", "Investigate");
    }

    public void acceptChargeBack(String disputeId) {
        Long currentTimeMillis = System.currentTimeMillis();
        stringRedisTemplate.opsForHash().put(makeKey(disputeId),"lastUpdateDate", Long.toString(currentTimeMillis));
        stringRedisTemplate.opsForHash().put(makeKey(disputeId),"acceptChargeBackDate", Long.toString(currentTimeMillis));
        stringRedisTemplate.opsForHash().put(makeKey(disputeId),"status", "ChargedBack");
    }
    public void resolved(String disputeId) {
        Long currentTimeMillis = System.currentTimeMillis();
        stringRedisTemplate.opsForHash().put(makeKey(disputeId),"lastUpdateDate", Long.toString(currentTimeMillis));
        stringRedisTemplate.opsForHash().put(makeKey(disputeId),"resolutionDate", Long.toString(currentTimeMillis));
        stringRedisTemplate.opsForHash().put(makeKey(disputeId),"status", "Resolved");
    }

}
