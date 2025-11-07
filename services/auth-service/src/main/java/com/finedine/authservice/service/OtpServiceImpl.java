package com.finedine.authservice.service;

import com.finedine.authservice.dto.OtpEmailTemplate;
import com.finedine.authservice.entity.Otp;
import com.finedine.authservice.entity.OtpType;
import com.finedine.common.exception.VerificationFailedException;
import com.finedine.authservice.repository.OtpRepository;
import com.finedine.authservice.util.OtpMailSender;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;



@Slf4j
@Service
@AllArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final OtpMailSender sender;

    private static final int OTP_LENGTH = 6;


    @Transactional
    @Override
    public void generateAndSendOtp(String email, OtpType type) {
        String code = generator();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);
        LocalDateTime now = LocalDateTime.now();

        Otp otp = Otp.builder()
                .code(code)
                .type(type)
                .expiry(expiry)
                .email(email)
                .createdAt(now)
                .build();

        Otp savedOtp = otpRepository.save(otp);
        log.info("Saved OTP for email: {}, code: {}, expiry: {}", email, savedOtp.getCode(), savedOtp.getExpiry());

        OtpEmailTemplate template = OtpEmailTemplate.builder()
                .to(email)
                .otpType(type)
                .otp(code)
                .build();

        sender.send(template);
    }

    @Override
    public boolean isValidOtp(String code, String email) {
        Optional<Otp> otpOptional = otpRepository.findFirstByEmailOrderByCreatedAtDesc(email);
        if (otpOptional.isEmpty()) {
            throw new VerificationFailedException("OTP not found");
        }

        Otp otp = otpOptional.get();
        if (!Objects.equals(otp.getCode(), code)) {
            throw new VerificationFailedException("Invalid OTP code");
        }

        if (!Objects.equals(otp.getEmail(), email)) {
            throw new VerificationFailedException("Email does not match OTP");
        }

        boolean isOtpExpired = isOtpExpired(otp);
        if (isOtpExpired) {
            throw new VerificationFailedException("OTP has expired");
        }

        return true;
    }

    @Override
    public void invalidateOtp(String code) {
        Otp otp = otpRepository.findByCode(code).orElseThrow(() -> new VerificationFailedException("OTP not found"));
        otp.setExpiry(LocalDateTime.now());
        otpRepository.save(otp);
    }

    private boolean isOtpExpired(Otp otp) {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime expiredAt = otp.getExpiry();

        return currentTime.isAfter(expiredAt);
    }

    private String generator(){
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < OTP_LENGTH; i ++){
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }

}