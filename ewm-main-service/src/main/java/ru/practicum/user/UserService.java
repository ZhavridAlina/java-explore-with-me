package ru.practicum.user;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest request);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long userId);

    User getUserOrThrow(Long userId);
}
