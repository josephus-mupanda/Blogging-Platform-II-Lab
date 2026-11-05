
package com.example.Blogging_Platform_II.api;

import static com.example.Blogging_Platform_II.api.ApiEnvelope.ok;

import com.example.Blogging_Platform_II.api.Requests.Registration;
import com.example.Blogging_Platform_II.api.Requests.AccountUpdate;
import com.example.Blogging_Platform_II.api.Requests.AccountStatus;
import com.example.Blogging_Platform_II.model.Blog.User;
import com.example.Blogging_Platform_II.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<List<User>> list() {
        return ok("Users found.", users.list());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<User> me() {
        return ok("Account found.", users.me());
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<User> get(@PathVariable long id) {
        return ok("User found.", users.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiEnvelope<User> create(@Valid @RequestBody Registration input) {
        return ok("User registered.", users.register(input));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<User> update(@PathVariable long id, @Valid @RequestBody AccountUpdate input) {
        return ok("User updated.", users.update(id, input));
    }

    @PatchMapping("/{id}/status")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<User> status(@PathVariable long id, @Valid @RequestBody AccountStatus input) {
        return ok("Account status updated.", users.setActive(id, input.active()));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<User> deactivate(@PathVariable long id) {
        return ok("User deactivated.", users.setActive(id, false));
    }
}
