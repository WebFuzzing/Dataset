package com.example.joinUs.mapping;

import com.example.joinUs.dto.TopicDTO;
import com.example.joinUs.model.mongodb.Topic;
import org.mapstruct.Mapper;

import java.util.List;


@Mapper(config = CentralMappingConfig.class)
public interface TopicMapper {

    TopicDTO toDTO(Topic topic);

    //    @Mapping(target = "id", ignore = true)
    Topic toEntity(TopicDTO dto);

    List<TopicDTO> toDTOs(List<Topic> topics);

}
