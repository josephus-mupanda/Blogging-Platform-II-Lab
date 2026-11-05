
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.exception.BlogException;
import com.example.Blogging_Platform_II.model.User;
import com.example.Blogging_Platform_II.repository.UserRepository;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
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
        return users.find(id).orElseThrow(() -> BlogException.notFound("User"));
    }

    public User create(String username, String email, String displayName) {
        try {
            return users.insert(username, email, displayName);
        } catch (DuplicateKeyException exception) {
            throw BlogException.duplicateValue();
        }
    }

    public User update(long id, String email, String displayName) {
        if (users.emailExists(email, id)) {
            throw BlogException.conflict("This email is already registered.");
        }
        try {
            return users.update(id, email, displayName).orElseThrow(() -> BlogException.notFound("User"));
        } catch (DuplicateKeyException exception) {
            throw BlogException.duplicateValue();
        }
    }

    public boolean usernameExists(String username) {
        return users.usernameExists(username);
    }

    public boolean emailExists(String email) {
        return users.emailExists(email, 0);
    }

    public void delete(long id) {
        if (!users.delete(id)) {
            throw BlogException.notFound("User");
        }
    }
}
