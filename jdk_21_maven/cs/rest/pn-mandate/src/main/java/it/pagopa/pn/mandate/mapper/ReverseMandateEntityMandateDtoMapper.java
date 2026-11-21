package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDtoRequest;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.utils.DateUtils;
import it.pagopa.pn.mandate.utils.MandateUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ReverseMandateEntityMandateDtoMapper {

    private final MandateUtils mandateUtils;

    public MandateEntity toEntity(MandateDtoRequest dto) {
        MandateEntity target = new MandateEntity();
        if (dto.getDateto() != null)
            target.setValidto(DateUtils.atStartOfDay(DateUtils.parseDate(dto.getDateto()).toInstant()).plusDays(1).minusSeconds(1).toInstant());
        target.setValidationcode(mandateUtils.generateRandomCode());
        if (dto.getDelegator() != null)
            target.setDelegatorisperson(dto.getDelegator().getPerson());
        return target;
    }

}
