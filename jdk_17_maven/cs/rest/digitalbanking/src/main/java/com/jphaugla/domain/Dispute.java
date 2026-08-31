package com.jphaugla.domain;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Dispute {
    private String disputeId;
    private String tranId;
    private String filingDate;
    private String reviewDate;
    private String reasonCode;
    private String acceptanceChargeBackDate;
    private String resolutionDate;
    private String lastUpdateDate;
    private String status;
    private String chargeBackAmount;

}
