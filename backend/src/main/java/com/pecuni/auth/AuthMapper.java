package com.pecuni.auth;

import com.pecuni.auth.dto.UserResponse;
import com.pecuni.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "nome", source = "name")
    UserResponse toUserResponse(User user);
}
