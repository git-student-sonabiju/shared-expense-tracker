package com.sharedexpenses.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.Session register(@Valid @RequestBody AuthDtos.Register request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.Session login(@Valid @RequestBody AuthDtos.Login request) {
        return authService.login(request);
    }

    /** Protected by AuthInterceptor, so the token is known to be valid here. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        authService.logout(AuthInterceptor.extractToken(request));
    }

    @GetMapping("/me")
    public AuthDtos.UserInfo me() {
        return authService.me(CurrentUser.id());
    }
}
