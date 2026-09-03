package com.example.joinUs.controller;

import com.example.joinUs.dto.LoginForm;
import com.example.joinUs.dto.ResponseMessage;
import com.example.joinUs.dto.UserDTO;
import com.example.joinUs.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Tag(name = "Auth")
@RestController
public class AuthController {

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    UserService userService;

    @Autowired
    AuthenticationManager authManager;

    @GetMapping("/")
    public void redirect(HttpServletResponse response) {

        try {
            //            response.sendRedirect("/login");
            response.sendRedirect("/swagger-ui/index.html");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginForm req, HttpServletRequest request) {

        SecurityContext securityContext = userService.loginUser(req);
        if (securityContext != null) {
            request.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
            return ResponseEntity.ok(new ResponseMessage("success", "Login of the user was successful "));
        } else {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseMessage("failure",
                    "Your login failed "));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new ResponseMessage("success", "Your logout was successful"));
    }

    @PostMapping("/register")
    public ResponseEntity register(HttpServletRequest request, @RequestBody UserDTO userDTO) {
        SecurityContext securityContext = userService.registerUser(userDTO);
        request.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        return ResponseEntity.ok(new ResponseMessage("success", "Your registration was successful "));
    }

}
