package com.example.joinUs.dto.summary;

import com.example.joinUs.model.embedded.GroupEmbedded;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


//        return "{'event_id':1,'event_name':1,'venue.city.name':1,
//        'member_count':1,'creator_group':1,'event_time':1}";
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventSummaryDTO {

    private String id;
    private String eventName;
    private Date eventTime;
    private Date updated;
    private Integer memberCount;
    private Integer feeAmount;
    private String venueCityName;
    private String venueAddress1;
    //TODO add venueAddress1 in the mapper
    private GroupEmbedded creatorGroup;
}
