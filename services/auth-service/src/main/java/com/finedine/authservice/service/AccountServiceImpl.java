package com.finedine.authservice.service;

import com.finedine.authservice.dto.AccountDetails;
import com.finedine.authservice.dto.GenericMessageResponse;
import com.finedine.authservice.dto.VerifiedUsers;
import com.finedine.authservice.entity.Account;
import com.finedine.authservice.repository.AccountRepository;
import com.finedine.authservice.security.SecurityUser;
import com.finedine.authservice.util.AccountMapper;
import com.finedine.authservice.util.Common;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static com.finedine.authservice.CustomMessages.ACCOUNT_DELETED_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final Common common;
    private final AccountMapper accountMapper;

    /**
     {@inheritDoc}
     */
    @Override
    public Account getAccount(Long id) {
        Account account = common.loadAccountById(id);
        return common.validateAccount(account);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public Page<VerifiedUsers> getAllVerifiedUsers(Pageable pageable) {
        return accountRepository.findAllVerified(pageable);
    }

    @Override
    public Account findAccountByEmail(String email) {
        Account account = common.loadAccountByEmail(email);
        return common.validateAccount(account);
    }

    @Override
    public AccountDetails myAccount(SecurityUser securityUser) {
        Account account = common.loadAccountByEmail(securityUser.getUsername());
        var validAccount = common.validateAccount(account);
        return accountMapper.toDto(validAccount);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public GenericMessageResponse deleteAccount(Long id) {
        Account account = common.loadAccountById(id);
        accountRepository.delete(account);
        return new GenericMessageResponse(ACCOUNT_DELETED_SUCCESS);
    }

}
