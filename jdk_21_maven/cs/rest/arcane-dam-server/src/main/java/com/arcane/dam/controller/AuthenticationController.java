package com.arcane.dam.controller;

import com.arcane.dam.dto.AuthRequest;
import com.arcane.dam.dto.AuthResponseDTO;
import com.arcane.dam.dto.OTPVerificationRequest;
import com.arcane.dam.dto.UsersDTO;
import com.arcane.dam.exception.IllegalStateException;
import com.arcane.dam.security.CognitoService;
import com.arcane.dam.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/auth")
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final UserService userService;
    private final EmailValidator emailValidator;
    private final CognitoService cognitoService;

    @GetMapping ("/test")
    public ResponseEntity<String> login() {
        return new ResponseEntity<>("Welcome", HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequest authRequest) {
        boolean  isValid = emailValidator.isValid(authRequest.getEmail(), null);
        if(!isValid) {
            throw new IllegalStateException("Invalid email");
        }
        return new ResponseEntity<>(cognitoService.loginUser(authRequest.getEmail(), authRequest.getPassword()), HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<UsersDTO> register(@RequestBody UsersDTO user) {
        return new ResponseEntity<>(cognitoService.registerUser(user), HttpStatus.CREATED);
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponseDTO> verify(@RequestBody OTPVerificationRequest otpVerificationRequest) {
        return new ResponseEntity<>(cognitoService.verifyUser(otpVerificationRequest), HttpStatus.OK);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Boolean> resendOtp(@RequestBody OTPVerificationRequest otpVerificationRequest) {
        return new ResponseEntity<>(cognitoService.resendOtp(otpVerificationRequest.getUserName()), HttpStatus.OK);
    }
}
