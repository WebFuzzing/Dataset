package dev.mirodil.testing_system.validations;

import dev.mirodil.testing_system.services.UserService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserService userService;

    public UniqueEmailValidator(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return false;
        }
        try {
            userService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            return true;
        }

        return false;
    }
}
