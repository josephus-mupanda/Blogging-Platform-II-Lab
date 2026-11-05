
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.api.ApiException;
import com.example.Blogging_Platform_II.api.Requests.UserCreate;
import com.example.Blogging_Platform_II.api.Requests.UserUpdate;
import com.example.Blogging_Platform_II.model.Blog.User;
import com.example.Blogging_Platform_II.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) {
        this.users = users;
    }

    public List<User> list() {
        return users.list();
    }

    public User get(long id) {
        return users.find(id).orElseThrow(() -> ApiException.notFound("User"));
    }

    public User create(UserCreate input) {
        return users.insert(input.username(), input.email(), input.displayName());
    }

    public User update(long id, UserUpdate input) {
        if (users.emailExists(input.email(), id)) {
            throw ApiException.conflict("This email is already registered.");
        }
        return users.update(id, input.email(), input.displayName())
                .orElseThrow(() -> ApiException.notFound("User"));
    }

    public void delete(long id) {
        if (!users.delete(id)) {
            throw ApiException.notFound("User");
        }
    }
}
