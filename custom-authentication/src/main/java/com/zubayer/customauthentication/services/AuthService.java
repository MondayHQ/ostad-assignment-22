package com.zubayer.customauthentication.services;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

// Local Imports
import com.zubayer.customauthentication.dto.*;
import com.zubayer.customauthentication.enums.Role;
import com.zubayer.customauthentication.models.UserEntity;
import com.zubayer.customauthentication.utils.TokenGenerator;
import com.zubayer.customauthentication.security.jwt.JWTUtils;
import com.zubayer.customauthentication.repositories.AuthRepository;
import com.zubayer.customauthentication.enums.VerificationTokenStatus;
import com.zubayer.customauthentication.models.VerificationTokenEntity;
import com.zubayer.customauthentication.exceptions.UserNotFoundException;
import com.zubayer.customauthentication.exceptions.EmailNotVerifiedException;
import com.zubayer.customauthentication.repositories.VerificationTokenRepository;
import com.zubayer.customauthentication.exceptions.InvalidVerificationTokenException;
import com.zubayer.customauthentication.exceptions.NewVerificationTokenGeneratedException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JWTUtils jwtUtils;
    private final MailService mailService;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final VerificationTokenRepository verificationTokenRepository;

    @Transactional
    public AuthResponseEmailNotVerified addNewUser(CreateAccountRequest createAccountRequest) throws MessagingException {

        String password = createAccountRequest.getPassword();
        String hashedPassword = passwordEncoder.encode(password);

        UserEntity userEntity = UserEntity.builder()
                .email(createAccountRequest.getEmail())
                .password(hashedPassword)
                .role(Role.USER)
                .build();

        String verificationToken = TokenGenerator.generateToken();
        String hashedVerificationToken = passwordEncoder.encode(verificationToken);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMinute = now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime expiration = nextMinute.plusMinutes(10);

        VerificationTokenEntity verificationTokenEntity = VerificationTokenEntity.builder()
                .token(hashedVerificationToken)
                .expiresAt(expiration)
                .verificationTokenStatus(VerificationTokenStatus.CREATED)
                .userEntity(userEntity)
                .build();

        authRepository.save(userEntity);
        verificationTokenRepository.save(verificationTokenEntity);

        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();

        String emailReceiver = createAccountRequest.getEmail();
        String emailSubject = "Verify Email";
        String verificationLink = baseUrl + "/auth/verify?email=" + createAccountRequest.getEmail() + "&token=" + verificationToken;

        String emailBody = "<html><p>Click the link to verify your email</p><a href='" + verificationLink + "'>" + verificationLink + "</a></html>";

        mailService.sendHtml(emailReceiver, emailSubject, emailBody);

        return AuthResponseEmailNotVerified.builder()
                .email(createAccountRequest.getEmail())
                .message("Please, verify your account. A link was sent to your registered email. The link will expire at " + expiration)
                .build();
    }

    @Transactional
    public EmailVerifiedResponse verifyEmail(String email, String token) throws InvalidVerificationTokenException, UserNotFoundException {

        Optional<UserEntity> userEntity = authRepository.findByEmail(email);
        if (userEntity.isEmpty()) throw new UserNotFoundException("User not found");

        List<VerificationTokenEntity> verificationTokens = userEntity.get().getVerificationTokens();
        if (verificationTokens == null || verificationTokens.isEmpty())
            throw new InvalidVerificationTokenException("Invalid token");

        for (VerificationTokenEntity verificationToken : verificationTokens) {
            if (verificationToken.getVerificationTokenStatus().equals(VerificationTokenStatus.CREATED) && passwordEncoder.matches(token, verificationToken.getToken())) {
                if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
                    throw new InvalidVerificationTokenException("Verification url expired");
                }

                verificationToken.setVerificationTokenStatus(VerificationTokenStatus.VERIFIED);

                return EmailVerifiedResponse.builder()
                        .email(email)
                        .message("Congratulations! Your Email is verified. You can now login with username and password.")
                        .build();
            }
        }

        throw new InvalidVerificationTokenException("Invalid verification link");

    }

    @Transactional
    public AuthResponse login(AuthRequest authRequest) throws UserNotFoundException, MessagingException, NewVerificationTokenGeneratedException, EmailNotVerifiedException {

        Optional<UserEntity> userEntity = authRepository.findByEmail(authRequest.getEmail());
        if (userEntity.isEmpty()) throw new UserNotFoundException("User not found");

        Optional<VerificationTokenEntity> verificationTokenEntity = verificationTokenRepository.findByUserEntity_IdAndVerificationTokenStatus(
                userEntity.get(), VerificationTokenStatus.VERIFIED
        );

        if (verificationTokenEntity.isEmpty()) {
            Optional<VerificationTokenEntity> verificationTokenEntityLatest = verificationTokenRepository.findByUserEntity_IdAndVerificationTokenStatus(
                    userEntity.get(), VerificationTokenStatus.CREATED
            );

            if (verificationTokenEntityLatest.isPresent()) {
                String verificationToken = TokenGenerator.generateToken();
                String hashedVerificationToken = passwordEncoder.encode(verificationToken);

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextMinute = now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
                LocalDateTime expiration = nextMinute.plusMinutes(10);

                VerificationTokenEntity newVerificationTokenEntity = VerificationTokenEntity.builder()
                        .token(hashedVerificationToken)
                        .expiresAt(expiration)
                        .verificationTokenStatus(VerificationTokenStatus.CREATED)
                        .userEntity(userEntity.get())
                        .build();

                if (verificationTokenEntityLatest.get().getExpiresAt().isBefore(LocalDateTime.now())) {
                    verificationTokenEntityLatest.get().setVerificationTokenStatus(VerificationTokenStatus.EXPIRED);

                    verificationTokenRepository.save(newVerificationTokenEntity);

                    String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .build()
                            .toUriString();

                    String emailSubject = "Verify Email";
                    String verificationLink = baseUrl + "/auth/verify?email=" + authRequest.getEmail() + "&token=" + verificationToken;

                    String emailBody = "<html><p>Click the link to verify your email</p><a href='" + verificationLink + "'>" + verificationLink + "</a></html>";

                    mailService.sendHtml(authRequest.getEmail(), emailSubject, emailBody);

                    throw new NewVerificationTokenGeneratedException("Please, verify your account. A link was sent to your registered email. The link will expire at " + expiration);
                }

                if (verificationTokenEntityLatest.get().getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
                    verificationTokenEntityLatest.get().setVerificationTokenStatus(VerificationTokenStatus.INVALID);

                    verificationTokenRepository.save(newVerificationTokenEntity);

                    String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .build()
                            .toUriString();

                    String emailSubject = "Verify Email";
                    String verificationLink = baseUrl + "/auth/verify?email=" + authRequest.getEmail() + "&token=" + verificationToken;

                    String emailBody = "<html><p>Click the link to verify your email</p><a href='" + verificationLink + "'>" + verificationLink + "</a></html>";

                    mailService.sendHtml(authRequest.getEmail(), emailSubject, emailBody);

                    throw new NewVerificationTokenGeneratedException("Please, verify your account. A link was sent to your registered email. The link will expire at " + expiration);

                }

                if (!verificationTokenEntityLatest.get().getExpiresAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                    throw new EmailNotVerifiedException("Please, verify your account. A link was sent to your registered email. The link will expire at " + verificationTokenEntityLatest.get().getExpiresAt());
                }
            }

        }

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(),
                        authRequest.getPassword()
                ));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserEntity savedUserEntity = (UserEntity) userDetails;

        String authToken = jwtUtils.generateToken(authentication);

        return AuthResponse
                .builder()
                .email(savedUserEntity.getEmail())
                .token(authToken)
                .build();
    }

}
