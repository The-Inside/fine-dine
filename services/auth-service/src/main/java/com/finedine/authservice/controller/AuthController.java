package com.finedine.authservice.controller;

import com.finedine.authservice.dto.ResendOtpRequest;
import com.finedine.authservice.dto.*;
import com.finedine.authservice.dto.signup.CustomerRegistrationRequest;
import com.finedine.authservice.dto.signup.RestaurantRegistrationRequest;
import com.finedine.authservice.dto.signup.RiderRegistrationRequest;
import com.finedine.authservice.entity.AuthToken;
import com.finedine.authservice.service.AuthService;
import com.finedine.authservice.service.RefreshTokenService;
import com.finedine.common.ratelimit.RateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RateLimit(limit = 20, windowSeconds = 300)
    public AuthToken refresh(@RequestParam("refreshToken") String refreshToken) {
        return refreshTokenService.refreshAccessToken(refreshToken);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RateLimit(limit = 5, windowSeconds = 300)
    public LoginResponse login(@RequestBody LoginRequest request){
        return authService.login(request);
    }

    @PostMapping("/signup/customer")
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimit(limit = 3, windowSeconds = 3600)
    public GenericMessageResponse signUpCustomer(@RequestBody @ModelAttribute @Valid CustomerRegistrationRequest request){
        return authService.registerCustomer(request);
    }

    @PostMapping(value = "/signup/restaurant", consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimit(limit = 4, windowSeconds = 180)
    public GenericMessageResponse signUpRestaurant(@RequestBody @ModelAttribute @Valid RestaurantRegistrationRequest request){
        return authService.registerRestaurant(request);
    }

    @PostMapping("/signup/rider")
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimit(limit = 2, windowSeconds = 3600)
    public GenericMessageResponse signUpRider(@RequestBody @ModelAttribute RiderRegistrationRequest request){
        return authService.registerRider(request);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/verify-otp")
    @RateLimit(limit = 5, windowSeconds = 600)
    public SignUpSuccessResponse verifyUser(@RequestBody VerifyOtpDto otp){
        return authService.verifyAndSignUpUser(otp);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/resend-otp")
    @RateLimit(limit = 3, windowSeconds = 600)
    public GenericMessageResponse resendOtp (@RequestBody ResendOtpRequest request){
        return authService.resendOtp(request);
    }

    @PostMapping("/request-password-reset")
    @RateLimit(limit = 3, windowSeconds = 3600)
    public GenericMessageResponse resetPasswordRequest(@RequestBody PasswordResetRequest request){
        return authService.resetPasswordRequest(request);
    }

    @PostMapping("/reset-password")
    @RateLimit(limit = 5, windowSeconds = 1800)
    public GenericMessageResponse resetPassword(@RequestBody PasswordReset request){
        return authService.resetPassword(request);
    }
}