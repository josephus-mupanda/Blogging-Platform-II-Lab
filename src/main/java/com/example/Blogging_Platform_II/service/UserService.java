
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.api.ApiException;
import com.example.Blogging_Platform_II.api.Requests.AccountUpdate;
import com.example.Blogging_Platform_II.api.Requests.Registration;
import com.example.Blogging_Platform_II.model.Blog.Role;
import com.example.Blogging_Platform_II.model.Blog.User;
import com.example.Blogging_Platform_II.repository.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository users;
    private final Actors actors;
    private final PasswordEncoder passwords;

    public UserService(UserRepository users, Actors actors, PasswordEncoder passwords) {
        this.users = users;
        this.actors = actors;
        this.passwords = passwords;
    }

    public List<User> list() {
        actors.admin();
        return users.list();
    }

    public User get(long id) {
        actors.ownerOrAdmin(id);
        return users.find(id).orElseThrow(() -> ApiException.notFound("User"));
    }

    public User register(Registration input) {
        if (input.role() == Role.ADMIN) {
            throw ApiException.badRequest("Sign up as an author or reader.");
        }
        return users.insert(input.username(), input.email(), passwords.encode(input.password()),
                input.displayName(), input.role());
    }

    public User update(long id, AccountUpdate input) {
        actors.ownerOrAdmin(id);
        if (users.emailExists(input.email(), id)) {
            throw ApiException.conflict("This email is already registered.");
        }
        return users.update(id, input.email(), input.displayName())
                .orElseThrow(() -> ApiException.notFound("User"));
    }

    public User deactivate(long id) {
        actors.admin();
        User user = users.find(id).orElseThrow(() -> ApiException.notFound("User"));
        if (user.role() == Role.ADMIN) {
            throw ApiException.forbidden("Administrator accounts are protected.");
        }
        return users.deactivate(id).orElseThrow(() -> ApiException.notFound("User"));
    }
}
