package com.finedine.authservice.service;

import com.finedine.authservice.entity.Account;
import com.finedine.authservice.entity.AuthToken;
import com.finedine.authservice.entity.RefreshToken;
import com.finedine.common.exception.NotFoundException;
import com.finedine.common.exception.UnauthorizedException;
import com.finedine.authservice.repository.AccountRepository;
import com.finedine.authservice.repository.RefreshTokenRepository;
import com.finedine.authservice.security.TokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountRepository accountRepository;
    private final TokenManager tokenManager;

    @Transactional
    public AuthToken refreshAccessToken(String refreshTokenValue) {
        UUID tokenId = UUID.fromString(refreshTokenValue);

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenId)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token expired");
        }

        Account account = accountRepository.findById(refreshToken.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        return tokenManager.issueToken(account);
    }
}
