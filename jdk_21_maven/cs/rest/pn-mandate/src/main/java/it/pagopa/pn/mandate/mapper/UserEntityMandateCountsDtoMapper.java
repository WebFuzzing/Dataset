package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.middleware.db.entities.DelegateEntity;
import org.springframework.stereotype.Component;

import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateCountsDto;

@Component
public class UserEntityMandateCountsDtoMapper implements BaseMapperInterface<MandateCountsDto, DelegateEntity> {
 
    private UserEntityMandateCountsDtoMapper(){
        super();
    }

    @Override
    public DelegateEntity toEntity(MandateCountsDto source) {
        throw new UnsupportedOperationException();
    }


    @Override
    public MandateCountsDto toDto(DelegateEntity ent) {
        final MandateCountsDto mDto = new MandateCountsDto();
        mDto.setValue(ent == null?0:ent.getPendingcount());
        return mDto;
    }
}
