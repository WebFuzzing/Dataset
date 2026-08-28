package com.jphaugla.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;


import java.io.Serializable;

@Data
@Table
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter


public class Transaction implements Serializable {
    @PrimaryKey
    private String tranid;
    private String accountno;
    // debit or credit
    private String amounttype;
    private String merchant;
    private String referencekeytype;
    private String referencekeyvalue;
    private String originalamount;
    private String amount;
    private String trancd;
    private String description;
    private String initialdate;
    private String settlementdate;
    private String postingdate;
    //  this is authorized, posted, settled
    private String status   ;
    private String disputeid;
    private String transactionreturn;
    private String location;
    private String transactiontags;

}
