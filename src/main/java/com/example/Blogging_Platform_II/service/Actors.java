
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.api.ApiException;
import com.example.Blogging_Platform_II.model.Blog.Role;
import com.example.Blogging_Platform_II.model.Blog.User;
import com.example.Blogging_Platform_II.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Actors {
    private final UserRepository users;

    public Actors(UserRepository users) {
        this.users = users;
    }

    public User current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            throw ApiException.unauthorized();
        }
        return users.account(auth.getName()).map(account -> account.user()).filter(User::active)
                .orElseThrow(ApiException::unauthorized);
    }

    public User admin() {
        User actor = current();
        if (actor.role() != Role.ADMIN) {
            throw ApiException.forbidden("Administrator access is required.");
        }
        return actor;
    }

    public void ownerOrAdmin(long ownerId) {
        User actor = current();
        if (actor.id() != ownerId && actor.role() != Role.ADMIN) {
            throw ApiException.forbidden("Only the owner or an administrator can change this content.");
        }
    }
}
