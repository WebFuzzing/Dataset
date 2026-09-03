package com.example.joinUs.mapping;

import com.example.joinUs.dto.CategoryDTO;
import com.example.joinUs.model.mongodb.Category;
import org.mapstruct.Mapper;

import java.util.List;


@Mapper(config = CentralMappingConfig.class)
public interface CategoryMapper {

    CategoryDTO toDTO(Category category);

    List<CategoryDTO> toDTOs(List<Category> categories);

    Category toEntity(CategoryDTO dto);

}
