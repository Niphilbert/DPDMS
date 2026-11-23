package com.itzetech.dpdms.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itzetech.dpdms.user.UserDto;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response shapes of the sign-in and registration endpoints. */
final class AuthDtos {

    private AuthDtos() {
    }

    record LoginRequest(
            @NotBlank(message = "Enter your email.") String email,
            @NotBlank(message = "Enter your password.") String password) {
    }

    record CodeRequest(
            @NotBlank String challenge,
            @NotBlank(message = "Enter the 6-digit code.") String code) {
    }

    record ChangePasswordRequest(
            @NotBlank String challenge,
            @NotBlank(message = "Choose a new password.") @Size(max = 100) String newPassword) {
    }

    record RegisterRequest(
            @NotBlank(message = "Enter your company or organisation.") @Size(max = 200) String companyName,
            @NotBlank(message = "Enter your full name.") @Size(max = 150) String fullName,
            @NotBlank(message = "Enter your email.") @Email(message = "Enter a valid email address.") @Size(max = 254) String email,
            @Size(max = 40) String phone,
            @NotBlank(message = "Choose a password.") @Size(max = 100) String password,
            @AssertTrue(message = "Please accept the terms of use to continue.") boolean acceptTerms) {
    }

    record VerifyEmailRequest(
            @NotBlank @Email String email,
            @NotBlank(message = "Enter the 6-digit code.") String code) {
    }

    record ResendRequest(@NotBlank @Email String email) {
    }

    /**
     * What the frontend should do next. status is one of AUTHENTICATED (token and user set),
     * CODE_REQUIRED (ask for the e-mailed code, send back the challenge),
     * PASSWORD_CHANGE_REQUIRED (ask for a new password, send back the challenge)
     * or VERIFICATION_REQUIRED (a new client must enter the e-mailed code).
     * devCode is only present when app.expose-secrets is on (development).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AuthResult(String status, String token, String challenge, UserDto user, String email, String devCode) {

        static AuthResult authenticated(String token, UserDto user) {
            return new AuthResult("AUTHENTICATED", token, null, user, null, null);
        }

        static AuthResult codeRequired(String challenge, String devCode) {
            return new AuthResult("CODE_REQUIRED", null, challenge, null, null, devCode);
        }

        static AuthResult passwordChangeRequired(String challenge) {
            return new AuthResult("PASSWORD_CHANGE_REQUIRED", null, challenge, null, null, null);
        }

        static AuthResult verificationRequired(String email, String devCode) {
            return new AuthResult("VERIFICATION_REQUIRED", null, null, null, email, devCode);
        }
    }
}
