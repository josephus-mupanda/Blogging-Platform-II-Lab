
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
        boolean usernameTaken = input.getUsername() != null && !input.getUsername().isBlank()
                && users.usernameExists(input.getUsername());
        boolean emailTaken = input.getEmail() != null && !input.getEmail().isBlank() && users.emailExists(input.getEmail());
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
