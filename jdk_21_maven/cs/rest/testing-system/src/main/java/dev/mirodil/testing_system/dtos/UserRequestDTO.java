package dev.mirodil.testing_system.dtos;

import dev.mirodil.testing_system.models.enums.UserGender;

public interface UserRequestDTO {
    String email();

    String password();

    String fname();

    String lname();

    UserGender gender();
}
