package it.pagopa.pn.mandate.model;

import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class InputSearchMandateDto {

    @NotEmpty
    private String delegateId;

    private List<Integer> statuses;

    private List<String> delegatorIds;

    private List<String> groups;

    private CxTypeAuthFleet cxType;

    private String mandateId;

    private String rootSenderId;

    private String iun;

    private Instant notificationSentAt;

    private Integer status;

    @NotNull
    @Positive
    private Integer size;

    private String nextPageKey;

    @NotNull
    @Positive
    private Integer maxPageNumber;
}
