
package com.example.Blogging_Platform_II.validation;

import com.example.Blogging_Platform_II.dto.UserCreate;
import com.example.Blogging_Platform_II.service.UserService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UniqueAccountValidator implements ConstraintValidator<UniqueAccount, UserCreate> {
    private final UserService users;

    public UniqueAccountValidator(UserService users) {
        this.users = users;
    }

    @Override
    public boolean isValid(UserCreate input, ConstraintValidatorContext context) {
        if (input == null) {
            return true;
        }
        boolean usernameTaken = input.username() != null && !input.username().isBlank()
                && users.usernameExists(input.username());
        boolean emailTaken = input.email() != null && !input.email().isBlank() && users.emailExists(input.email());
        if (usernameTaken || emailTaken) {
            context.disableDefaultConstraintViolation();
            if (usernameTaken) {
                context.buildConstraintViolationWithTemplate("This username is already taken.")
                        .addPropertyNode("username").addConstraintViolation();
            }
            if (emailTaken) {
                context.buildConstraintViolationWithTemplate("This email is already registered.")
                        .addPropertyNode("email").addConstraintViolation();
            }
        }
        return !usernameTaken && !emailTaken;
    }
}
