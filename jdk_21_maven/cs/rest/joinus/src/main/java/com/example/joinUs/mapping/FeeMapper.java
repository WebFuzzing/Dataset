package com.example.joinUs.mapping;

import com.example.joinUs.dto.FeeDTO;
import com.example.joinUs.model.mongodb.Fee;
import org.mapstruct.Mapper;


@Mapper(config = CentralMappingConfig.class)
public interface FeeMapper {

    FeeDTO toDTO(Fee fee);

    Fee toEntity(FeeDTO dto);
}
