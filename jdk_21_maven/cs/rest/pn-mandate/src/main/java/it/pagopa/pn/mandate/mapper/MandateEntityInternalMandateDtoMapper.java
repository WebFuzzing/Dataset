package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.WorkflowType;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.InternalMandateDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MandateEntityInternalMandateDtoMapper implements BaseMapperInterface<InternalMandateDto, MandateEntity> {

    private MandateEntityInternalMandateDtoMapper() {
        super();
    }     

    @Override
    public MandateEntity toEntity(InternalMandateDto source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InternalMandateDto toDto(MandateEntity entity) {
        final InternalMandateDto target = new InternalMandateDto();
        target.setMandateId(entity.getMandateId());
        target.setDatefrom(entity.getValidfrom().toString());
        target.setDateto(entity.getValidto().toString());
        target.setDelegate(entity.getDelegate());
        target.setDelegator(entity.getDelegator());
        target.setGroups(entity.getGroups() != null ? List.copyOf(entity.getGroups()) : null);
        if (entity.getVisibilityIds() != null) {
            target.setVisibilityIds(new ArrayList<>(entity.getVisibilityIds()));
        } else {
            target.setVisibilityIds(new ArrayList<>());
        }
        if(entity.getWorkflowType() != null) {
            target.setWorkflowType(WorkflowType.fromValue(entity.getWorkflowType().name()));
        }
        if(entity.getIuns() != null) {
            target.setIuns(new ArrayList<>(entity.getIuns()));
        } else {
            target.setIuns(new ArrayList<>());
        }

        return target;
    } 
 
}
