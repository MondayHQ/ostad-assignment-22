package com.zubayer.customauthentication.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.mail.MessagingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Local Imports
import com.zubayer.customauthentication.dto.*;
import com.zubayer.customauthentication.services.AuthService;
import com.zubayer.customauthentication.exceptions.UserNotFoundException;
import com.zubayer.customauthentication.exceptions.EmailNotVerifiedException;
import com.zubayer.customauthentication.exceptions.InvalidVerificationTokenException;
import com.zubayer.customauthentication.exceptions.NewVerificationTokenGeneratedException;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<AuthResponseEmailNotVerified> createNewAccount(@Valid @RequestBody CreateAccountRequest createAccountRequest) throws MessagingException {
        AuthResponseEmailNotVerified authResponseEmailNotVerified = authService.addNewUser(createAccountRequest);

        return new ResponseEntity<>(authResponseEmailNotVerified, HttpStatus.CREATED);
    }

    @GetMapping(path = "/verify")
    public ResponseEntity<EmailVerifiedResponse> verifyEmail(
            @RequestParam String email,
            @RequestParam String token
    ) throws InvalidVerificationTokenException, UserNotFoundException {
        EmailVerifiedResponse emailVerifiedResponse = authService.verifyEmail(email, token);

        return new ResponseEntity<>(emailVerifiedResponse, HttpStatus.OK);
    }

    @PostMapping(path = "/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) throws UserNotFoundException, NewVerificationTokenGeneratedException, MessagingException, EmailNotVerifiedException {
        AuthResponse authResponse = authService.login(authRequest);

        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }

}
