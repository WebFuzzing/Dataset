package com.example.demo.auth.model.dto.request;

import com.example.demo.auth.model.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Represents a request for user registration containing the necessary details
 * to create a new user in the system.
 * This class is used to capture the data required for registering a user, including
 * their email, password, first and last name, phone number, and user type. It also
 * enforces validation rules on the fields to ensure data integrity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @Email(message = "Please enter a valid e-mail address")
    @Size(min = 7, message = "Minimum e-mail length is 7 characters.")
    private String email;

    @Size(min = 8, message = "Minimum password length is 8 characters.")
    private String password;

    @NotBlank(message = "First name can't be blank.")
    private String firstName;

    @NotBlank(message = "Last name can't be blank.")
    private String lastName;

    @NotBlank(message = "Phone number can't be blank.")
    @Size(min = 11, max = 20, message = "Phone number must be between 11 and 20 characters.")
    private String phoneNumber;

    private UserType userType;

}
