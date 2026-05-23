package com.store.bytestore.dto;

import com.store.bytestore.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    User toEntity(UserDto userDto);

    User UserToSaveDtoToEntity(UserToSaveDto userToSaveDto);

    UserDto toDto(User user);

}
