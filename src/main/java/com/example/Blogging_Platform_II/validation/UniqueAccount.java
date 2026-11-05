
package com.example.Blogging_Platform_II.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = UniqueAccountValidator.class)
public @interface UniqueAccount {
    String message() default "The account details are already in use.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
