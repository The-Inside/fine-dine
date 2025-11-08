package com.finedine.authservice.security;

import com.finedine.authservice.entity.Account;
import com.finedine.authservice.entity.AuthToken;
import com.finedine.authservice.entity.RefreshToken;
import com.finedine.common.exception.NotFoundException;
import com.finedine.common.exception.UnauthorizedException;
import com.finedine.authservice.repository.AccountRepository;
import com.finedine.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenManager {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public AuthToken issueToken (Account account){
        Instant currentTime = Instant.now();
        Instant accessExpiry = currentTime.plus(30, ChronoUnit.MINUTES);
        Instant refreshExpiry = currentTime.plus(7, ChronoUnit.DAYS);

        JwsHeader jwsHeader = buildJwsHeader();
        JwtClaimsSet claimSet = buildJwtClaimSet(account, currentTime);

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimSet)).getTokenValue();
        String refreshToken = saveRefreshToken(account, currentTime);

        return AuthToken.builder()
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .refreshTokenExpiresAt(refreshExpiry)
                .accessTokenExpiresAt(accessExpiry)
                .build();
    }

    public AuthToken refreshAccessToken(String refreshTokenValue) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(UUID.fromString(refreshTokenValue))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        Account account = accountRepository.findById(refreshToken.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        return issueToken(account);
    }

    private JwsHeader buildJwsHeader() {
        return JwsHeader.with(SignatureAlgorithm.RS256)
                .header("type", "JWT")
                .build();
    }

    private JwtClaimsSet buildJwtClaimSet(Account account, Instant currentTime) {
        return JwtClaimsSet.builder()
                .subject(account.getEmail())
                .issuer("http://localhost:8081")
                .issuedAt(currentTime)
                .expiresAt(currentTime.plus(30, ChronoUnit.MINUTES))
                .claim("email", account.getEmail())
                .claim("role", List.of(account.getRole().name()))
                .claim("externalId", account.getExternalId())
                .build();
    }

    private String saveRefreshToken(Account account, Instant currentTime) {
        UUID refreshTokenId = UUID.randomUUID();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenId)
                .accountId(account.getId())
                .issuedAt(currentTime)
                .expiresAt(currentTime.plus(7, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);
        return refreshTokenId.toString();
    }
}
