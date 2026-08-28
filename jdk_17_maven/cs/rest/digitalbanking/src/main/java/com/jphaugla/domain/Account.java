package com.jphaugla.domain;

import lombok.*;

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
    private String openDatetime;
    private String lastUpdated;
    private String lastUpdatedBy;
    private String createdBy;
    private String createdDatetime;
}

