package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MandateCreationResponse;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MandateDto;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import org.springframework.stereotype.Component;

@Component
public class MandateEntityAppIoMandateDtoMapper implements BaseMapperInterface<MandateCreationResponse, MandateEntity>{
    @Override
    public MandateEntity toEntity(MandateCreationResponse source) {throw new UnsupportedOperationException();}

    @Override
    public MandateCreationResponse toDto(MandateEntity entity) {
        final MandateCreationResponse target = new MandateCreationResponse();
        MandateDto mandateDto = new MandateDto();
        mandateDto.setMandateId(entity.getMandateId());
        mandateDto.dateTo(entity.getValidto() != null ? entity.getValidto().toString() : null);
        mandateDto.setVerificationCode(entity.getValidationcode());
        target.setMandate(mandateDto);
        target.setRequestTTL(entity.getValidto() != null ? (int) entity.getValidto().getEpochSecond() : null);
        return target;
    }
}
