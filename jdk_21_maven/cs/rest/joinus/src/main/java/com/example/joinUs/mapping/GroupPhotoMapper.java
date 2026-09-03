package com.example.joinUs.mapping;

import com.example.joinUs.dto.GroupPhotoDTO;
import com.example.joinUs.model.mongodb.GroupPhoto;
import org.mapstruct.Mapper;


@Mapper(config = CentralMappingConfig.class)
public interface GroupPhotoMapper {

    GroupPhotoDTO toDTO(GroupPhoto groupPhoto);

    GroupPhoto toEntity(GroupPhotoDTO dto);

}
