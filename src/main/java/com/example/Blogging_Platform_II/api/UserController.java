
package com.example.Blogging_Platform_II.api;

import static com.example.Blogging_Platform_II.api.ApiEnvelope.ok;

import com.example.Blogging_Platform_II.api.Requests.UserCreate;
import com.example.Blogging_Platform_II.api.Requests.UserUpdate;
import com.example.Blogging_Platform_II.model.Blog.User;
import com.example.Blogging_Platform_II.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    ApiEnvelope<List<User>> list() {
        return ok("Users found.", users.list());
    }

    @GetMapping("/{id}")
    ApiEnvelope<User> get(@PathVariable long id) {
        return ok("User found.", users.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiEnvelope<User> create(@Valid @RequestBody UserCreate input) {
        return ok("User created.", users.create(input));
    }

    @PutMapping("/{id}")
    ApiEnvelope<User> update(@PathVariable long id, @Valid @RequestBody UserUpdate input) {
        return ok("User updated.", users.update(id, input));
    }

    @DeleteMapping("/{id}")
    ApiEnvelope<Void> delete(@PathVariable long id) {
        users.delete(id);
        return ok("User deleted.", null);
    }
}
