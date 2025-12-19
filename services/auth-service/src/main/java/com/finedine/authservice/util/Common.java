package com.finedine.authservice.util;

import com.finedine.authservice.entity.Account;
import com.finedine.common.exception.NotFoundException;
import com.finedine.common.exception.PhoneNumberValidationException;
import com.finedine.common.exception.UnverifiedAccountException;
import com.finedine.authservice.repository.AccountRepository;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;

import static com.finedine.authservice.CustomMessages.*;
import static com.finedine.authservice.CustomMessages.USER_NOT_FOUND;

@Slf4j
@Component
@RequiredArgsConstructor
public class Common {

    private final AccountRepository accountRepository;

    public Account loadAccount(String email, Long id) {
        if (email != null) {
            return accountRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        } else if (id != null) {
            return accountRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        } else {
            throw new IllegalArgumentException("Either email or id must be provided");
        }
    }

    public Account loadAccountByEmail(String email) {
        return loadAccount(email, null);
    }

    public Account loadAccountById(Long id) {
        return loadAccount(null, id);
    }

    public Account validateAccount(Account account){

        if (!account.isVerified()) {
            throw new UnverifiedAccountException(UNVERIFIED_ACCOUNT);
        }

        if(!account.isEnabled()) {
            throw new DisabledException(ACCOUNT_DISABLED);
        }

        log.info("User {} is verified and enabled", account.getEmail());

        return account;
    }

    public String validatePhoneNumber(@NonNull String phoneNumber) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            // Remove leading zero if present and phone number doesn't start with '+'
            String normalizedNumber = phoneNumber;
            if (!phoneNumber.startsWith("+") && phoneNumber.startsWith("0")) {
                normalizedNumber = phoneNumber.substring(1);
            }

            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(normalizedNumber, "GH");

            if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
                throw new PhoneNumberValidationException(INVALID_PHONE_NUMBER);
            }

            return phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);

        } catch (NumberParseException e) {
            throw new PhoneNumberValidationException(INVALID_PHONE_NUMBER, e);
        }
    }
}
