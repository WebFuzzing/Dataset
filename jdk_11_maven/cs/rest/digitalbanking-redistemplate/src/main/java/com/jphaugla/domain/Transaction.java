package com.jphaugla.domain;

import lombok.*;
import org.springframework.data.annotation.Id;


import java.io.Serializable;
import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Transaction implements Serializable {
    private String tranId;
    private String accountNo;
    // debit or credit
    private String amountType;
    private String merchant;
    private String referenceKeyType;
    private String referenceKeyValue;
    private String originalAmount;
    private String amount;
    private String tranCd ;
    private String description;
    private Long initialDate;
    private Long settlementDate;
    private Long postingDate;
    //  this is authorized, posted, settled
    private String status   ;
    private String transactionReturn;
    private String location;
    private String transactionTags;

}
