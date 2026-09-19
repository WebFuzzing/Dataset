package dev.mirodil.testing_system.services;

import dev.mirodil.testing_system.dtos.*;
import dev.mirodil.testing_system.exceptions.GenericValidationError;
import dev.mirodil.testing_system.exceptions.ResourceNotFoundException;
import dev.mirodil.testing_system.models.Role;
import dev.mirodil.testing_system.models.User;
import dev.mirodil.testing_system.models.enums.UserRole;
import dev.mirodil.testing_system.repositories.RoleRepository;
import dev.mirodil.testing_system.repositories.UserRepository;
import dev.mirodil.testing_system.utils.AuthUtil;
import dev.mirodil.testing_system.utils.PageWithFilterRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    private static User prepareNewUser(UserRequestDTO request, Role role) {
        User user = new User(
                request.email(),
                AuthUtil.encodePassword(request.password()),
                role.getId(),
                request.fname(),
                request.lname(),
                request.gender()
        );
        user.setUserRole(role);
        if (role.isPermissionsSet()) {
            user.setPermissionNames(role.getPermissionNames());
        }
        return user;
    }

    public UserResponseDTO getProfile() {
        return AuthUtil.getAuthenticatedUserDTO();
    }

    // TODO: change user profile (details, password)

    public AuthResponseDTO createTestTaker(TestTakerRegisterRequestDTO request, HttpServletRequest servletRequest) {
        Role defaultRole = roleRepository.findRoleWithPermissionsByName(UserRole.TEST_TAKER)
                .orElseThrow(() -> new GenericValidationError("Role not found: " + UserRole.TEST_TAKER.name()));

        User user = saveNewUser(request, defaultRole);
        UserResponseDTO userDTO = new UserResponseDTO(user);
        AuthUtil.setAuthenticationToSecurityContext(user, servletRequest);

        return convertToAuthResponse(userDTO);
    }

    public UserResponseDTO createUser(UserCreateRequestDTO request) {
        Role role = roleRepository.findRoleWithPermissionsByName(request.role())
                .orElseThrow(() -> new GenericValidationError("Role not found: " + request.role().name()));

        return new UserResponseDTO(saveNewUser(request, role));
    }

    private User saveNewUser(UserRequestDTO request, Role role) {
        User user = prepareNewUser(request, role);
        return userRepository.save(user);
    }

    private AuthResponseDTO convertToAuthResponse(UserResponseDTO userDTO) {
        Map<String, Object> jwtTokenDetails = AuthUtil.generateTokenDetails(userDTO.email());
        return new AuthResponseDTO(userDTO, jwtTokenDetails);
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findUserById(id).orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + id)
        );
        return new UserResponseDTO(user);
    }

    public PagedResponse<UserResponseDTO> getUsers(PageWithFilterRequest pageable, String fullUrl) {
        List<User> users = userRepository.findAndSortUsersWithPagination(pageable);
        long totalElements = userRepository.countFilteredUsers(pageable);

        List<UserResponseDTO> usersDTO = users.stream()
                .map(UserResponseDTO::new)
                .toList();
        Page<UserResponseDTO> page = new PageImpl<>(usersDTO, pageable, totalElements);

        return new PagedResponse<>(page, fullUrl);
    }

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findUserByEmail(username).orElseThrow(
                () -> new UsernameNotFoundException("User with " + username + " username doesn't exist")
        );
    }

    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findUserByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found with email: " + email)
        );
        return new UserResponseDTO(user);
    }
}
