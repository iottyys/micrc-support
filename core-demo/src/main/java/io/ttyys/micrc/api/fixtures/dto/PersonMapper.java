package io.ttyys.micrc.api.fixtures.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PersonMapper {
    PersonMapper INSTANCE = Mappers.getMapper(PersonMapper.class);

    @Mapping(source = "id", target = "id")
    PersonCommand dtoToCommand(PersonIDto personDto);

    @Mapping(source = "name", target = "allName")
    PersonODto toData(PersonCommand person);
}
