package com.itzetech.dpdms.auth;

import com.itzetech.dpdms.auth.AuthDtos.AuthResult;
import com.itzetech.dpdms.auth.AuthDtos.ChangePasswordRequest;
import com.itzetech.dpdms.auth.AuthDtos.CodeRequest;
import com.itzetech.dpdms.auth.AuthDtos.LoginRequest;
import com.itzetech.dpdms.auth.AuthDtos.RegisterRequest;
import com.itzetech.dpdms.auth.AuthDtos.ResendRequest;
import com.itzetech.dpdms.auth.AuthDtos.VerifyEmailRequest;
import com.itzetech.dpdms.security.AuthUser;
import com.itzetech.dpdms.user.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    AuthResult login(@Valid @RequestBody LoginRequest r) {
        return auth.login(r.email(), r.password());
    }

    /** Administrator's second step: the code that was e-mailed after the password was accepted. */
    @PostMapping("/verify-code")
    AuthResult verifyCode(@Valid @RequestBody CodeRequest r) {
        return auth.verifySignInCode(r.challenge(), r.code());
    }

    /** First sign-in of an invited staff member: replace the temporary password. */
    @PostMapping("/change-password")
    AuthResult changePassword(@Valid @RequestBody ChangePasswordRequest r) {
        return auth.changePassword(r.challenge(), r.newPassword());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResult register(@Valid @RequestBody RegisterRequest r) {
        return auth.register(r);
    }

    @PostMapping("/verify-email")
    AuthResult verifyEmail(@Valid @RequestBody VerifyEmailRequest r) {
        return auth.verifyEmail(r.email(), r.code());
    }

    /** Always 202. In development (app.expose-secrets) the body is {"devCode": "..."}; otherwise it is empty. */
    @PostMapping("/resend-code")
    ResponseEntity<Map<String, String>> resend(@Valid @RequestBody ResendRequest r) {
        String devCode = auth.resendVerification(r.email());
        return devCode == null ? ResponseEntity.accepted().build() : ResponseEntity.accepted().body(Map.of("devCode", devCode));
    }

    @GetMapping("/me")
    UserDto me(@AuthenticationPrincipal AuthUser me) {
        return auth.me(me.id());
    }
}
