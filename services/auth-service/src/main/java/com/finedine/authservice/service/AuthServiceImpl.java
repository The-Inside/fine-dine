package com.finedine.authservice.service;

import com.finedine.authservice.async.CustomerCreationQueueObject;
import com.finedine.authservice.async.RestaurantQueueObject;
import com.finedine.authservice.async.RiderRegistrationQueue;
import com.finedine.authservice.dto.*;
import com.finedine.authservice.dto.signup.CustomerRegistrationRequest;
import com.finedine.authservice.dto.signup.RestaurantRegistrationRequest;
import com.finedine.authservice.dto.signup.RiderRegistrationRequest;
import com.finedine.authservice.entity.Account;
import com.finedine.authservice.entity.AuthToken;
import com.finedine.authservice.entity.OtpType;
import com.finedine.authservice.enums.AccountStatus;
import com.finedine.authservice.enums.Role;
import com.finedine.common.exception.NotFoundException;
import com.finedine.common.exception.VerificationFailedException;
import com.finedine.authservice.repository.AccountRepository;
import com.finedine.authservice.security.SecurityUser;
import com.finedine.authservice.security.TokenManager;
import com.finedine.authservice.util.Common;
import com.finedine.authservice.util.config.SqsProducer;
import com.finedine.common.validators.CustomValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.finedine.authservice.CustomMessages.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenManager tokenManager;
    private final Common common;
    private final ImageService imageService;
    private final SqsProducer sqsProducer;
    private final OtpService otpService;
    private static final String EXTERNAL_ID_FORMAT = "account-%s";

    @Value("${aws.sqs.restaurant.restaurant-registration-queue}")
    private String restaurant_registration_queue;

    @Value("${aws.sqs.customer.customer-registration-queue}")
    private String customer_registration_queue;

    @Value("${aws.sqs.rider.rider-registration-queue}")
    private String rider_registration_queue;

    /**
     {@inheritDoc}
     */
    @Override
    public LoginResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        Account account = principal.account();
        common.validateAccount(account);

        AuthToken authToken = tokenManager.issueToken(account);

        log.info("User {} logged in successfully", account.getEmail());

        return LoginResponse.builder()
                .email(account.getEmail())
                .accessToken(authToken.getAccessToken())
                .refreshToken(authToken.getRefreshToken())
                .role(account.getRole().name())
                .isEnabled(account.isEnabled())
                .isVerified(account.isVerified())
                .createdAt(account.getCreatedAt())
                .build();
    }

    @Transactional
    public GenericMessageResponse registerCustomer(CustomerRegistrationRequest request) {

        var account = createAndSaveAccount(request.email(), request.password(), request.firstName(),
                request.lastName(), request.phoneNumber(), Role.CUSTOMER);

        String validatedPhoneNumber = common.validatePhoneNumber(request.phoneNumber());
        String profilePictureUrl = null;

        if (request.image() != null) {
            profilePictureUrl = uploadImage(request.image());
        }

        CustomerCreationQueueObject queueObject = CustomerCreationQueueObject.builder()
                .accountId(account.getId())
                .email(account.getEmail())
                .externalId(account.getExternalId())
                .address(request.address())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .phoneNumber(validatedPhoneNumber)
                .profilePictureUrl(profilePictureUrl)
                .build();

        otpService.generateAndSendOtp(account.getEmail(), OtpType.CREATE);

        sqsProducer.produce(customer_registration_queue, queueObject);

        log.info("Sending customer registration data to SQS for account: {}", account.getEmail());
        return new GenericMessageResponse(TOKEN_SENT_MSG);
    }

    @Transactional
    public GenericMessageResponse registerRider(RiderRegistrationRequest request) {
        String imageUrl = uploadImage(request.image());
        String validatedPhoneNumber = common.validatePhoneNumber(request.phoneNumber());

        var account =createAndSaveAccount(request.email(), request.password(), request.firstName(),
                request.lastName(), request.phoneNumber(), Role.RIDER);

        RiderRegistrationQueue queueObject = RiderRegistrationQueue.builder()
                .accountId(account.getId())
                .email(account.getEmail())
                .externalId(account.getExternalId())
                .address(request.address())
                .phoneNumber(validatedPhoneNumber)
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .profilePictureUrl(imageUrl)
                .vehicleType(request.vehicleType())
                .vehicleColor(request.vehicleColor())
                .build();

        otpService.generateAndSendOtp(account.getEmail(), OtpType.CREATE);

        sqsProducer.produce(rider_registration_queue, queueObject);

        log.info("Sending rider registration data to SQS for account: {}", account.getEmail());
        return new GenericMessageResponse(TOKEN_SENT_MSG);
    }

    @Transactional
    public GenericMessageResponse registerRestaurant(RestaurantRegistrationRequest request) {
        String imageUrl = uploadImage(request.image());
        String validatedPhoneNumber = common.validatePhoneNumber(request.phoneNumber());
        var account = createAndSaveAccount(request.email(), request.password(), request.ownerFirstName(),
                request.ownerLastName(), request.phoneNumber(), Role.RESTAURANT);

        RestaurantQueueObject queueObject = RestaurantQueueObject.builder()
                .email(account.getEmail())
                .accountId(account.getId())
                .externalId(account.getExternalId())
                .phone(validatedPhoneNumber)
                .restaurantName(request.restaurantName())
                .address(request.address())
                .cuisine(request.cuisine())
                .description(request.description())
                .imageUrl(imageUrl)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .openTime(request.openTime())
                .closeTime(request.closeTime())
                .build();

        otpService.generateAndSendOtp(account.getEmail(), OtpType.CREATE);

        sqsProducer.produce(restaurant_registration_queue, queueObject);
        log.info("Sending restaurant registration data to SQS for account: {}", account.getEmail());
        return new GenericMessageResponse(TOKEN_SENT_MSG);

    }

    @Override
    public SignUpSuccessResponse verifyAndSignUpUser(VerifyOtpDto otp) {
        boolean otpValid = otpService.isValidOtp(otp.code(), otp.email());

        if(!otpValid) {
            throw new VerificationFailedException(OTP_NOT_VERIFIED);
        }
        Account account = accountRepository.findByEmail(otp.email()).orElseThrow(()-> new VerificationFailedException(OTP_NOT_VERIFIED));
        account.setVerified(true);
        account.setEnabled(true);
        account.setAccountStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        otpService.invalidateOtp(otp.code());

        AuthToken authToken = tokenManager.issueToken(account);

        return SignUpSuccessResponse.builder()
                .email(account.getEmail())
                .firstname(account.getFirstName())
                .lastname(account.getLastName())
                .accessToken(authToken.getAccessToken())
                .refreshToken(authToken.getRefreshToken())
                .build();

    }

    @Override
    public GenericMessageResponse resendOtp(ResendOtpRequest request) {
        Account account = accountRepository.findByEmail(request.email()).orElseThrow(()-> new NotFoundException(USER_NOT_FOUND));

        otpService.generateAndSendOtp(account.getEmail(), OtpType.CREATE);

        return new GenericMessageResponse(TOKEN_SENT_MSG);
    }

    @Override
    public GenericMessageResponse resetPasswordRequest(PasswordResetRequest request) {
        Optional<Account> byEmail = accountRepository.findByEmail(request.email());

        if(byEmail.isEmpty()) throw new NotFoundException(USER_NOT_FOUND);

        otpService.generateAndSendOtp(request.email(), OtpType.RESET);
        return new GenericMessageResponse(TOKEN_SENT_MSG);
    }

    @Override
    public GenericMessageResponse resetPassword(PasswordReset request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new VerificationFailedException(PASSWORD_MISMATCH);
        }

        boolean otpValid = otpService.isValidOtp(request.code(), String.valueOf(request.email()));

        if (!otpValid) throw new VerificationFailedException(OTP_VERIFICATION_FAILED_MESSAGE);

        Account account = accountRepository.findByEmail(String.valueOf(request.email())).orElseThrow(()-> new NotFoundException(USER_NOT_FOUND));
        account.setPassword(passwordEncoder.encode(request.password()));
        accountRepository.save(account);

        otpService.invalidateOtp(request.code());

        return new GenericMessageResponse(RESET_PASSWORD_SUCC);

    }

    private Account createAndSaveAccount(String email, String rawPassword, String firstName, String lastName, String phone, Role role) {
        Optional<Account> byEmail = accountRepository.findByEmail(email);

        if(byEmail.isPresent()){
            throw new DuplicateKeyException(EMAIL_ALREADY_EXISTS);
        }

        String passwordError = CustomValidator.validatePassword(rawPassword);
        String emailError = CustomValidator.validateEmail(email);
        String phoneError = CustomValidator.validatePhoneNumber(phone);



        if (passwordError != null) {
            throw new IllegalArgumentException(passwordError);
        }
        if (emailError != null){
            throw new IllegalArgumentException(emailError);
        }
        if (phoneError != null){
            throw new IllegalArgumentException(phoneError);
        }

        CustomValidator.validatePassword(rawPassword);
        Account account = Account.builder()
                .externalId(generateExternalId())
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phone)
                .lastEmailUpdate(Instant.now())
                .password(passwordEncoder.encode(rawPassword))
                .accountStatus(AccountStatus.INACTIVE)
                .role(role)
                .build();

        return accountRepository.save(account);
    }

    private String uploadImage(MultipartFile image) {
        String imageUrl;

        try {
            imageUrl = imageService.uploadImage(image);
        } catch (Exception e) {
            log.error("Image upload failed: {}", e.getMessage());

            throw new IllegalArgumentException("Image upload failed");
        }
        return imageUrl;
    }
    private String generateExternalId() {
        return String.format(EXTERNAL_ID_FORMAT, UUID.randomUUID().toString().replace("-", ""));
    }

}
