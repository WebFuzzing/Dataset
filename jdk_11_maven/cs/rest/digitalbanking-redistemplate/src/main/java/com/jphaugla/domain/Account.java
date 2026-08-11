package com.jphaugla.domain;

import lombok.*;
import org.springframework.data.annotation.Reference;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter


public class Account implements Serializable {
    private String accountNo;
    private String customerId;
    private String accountType;
    private String accountOriginSystem;
    private String accountStatus;
    private String cardNum;
    private Long openDatetime;
    private Long lastUpdated;
    private String lastUpdatedBy;
    private String createdBy;
    private Long createdDatetime;
}
