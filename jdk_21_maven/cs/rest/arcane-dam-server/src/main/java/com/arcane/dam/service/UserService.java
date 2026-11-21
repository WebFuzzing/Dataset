package com.arcane.dam.service;

import com.arcane.dam.dto.AuthRequest;
import com.arcane.dam.dto.AuthResponseDTO;
import com.arcane.dam.dto.UsersDTO;
import com.arcane.dam.entity.Space;

import java.util.List;

public interface UserService {
    List<UsersDTO> getAllUsers();
    UsersDTO getUserById(String id);
    UsersDTO getUserByEmail(String email);
    UsersDTO addUser(UsersDTO usersDTO);
    boolean updateUser(String id, UsersDTO usersDTO);
    boolean deleteUser(String id);
    AuthResponseDTO verify(AuthRequest authRequest);
    UsersDTO updateSpace(String userId, Space updatedSpace);
}
