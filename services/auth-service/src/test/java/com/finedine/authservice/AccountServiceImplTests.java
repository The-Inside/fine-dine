package com.finedine.authservice;


import com.finedine.authservice.dto.AccountDetails;
import com.finedine.authservice.entity.Account;
import com.finedine.authservice.enums.AccountStatus;
import com.finedine.authservice.repository.AccountRepository;
import com.finedine.authservice.security.SecurityUser;
import com.finedine.authservice.service.AccountServiceImpl;
import com.finedine.authservice.util.AccountMapper;
import com.finedine.authservice.util.Common;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.finedine.authservice.CustomMessages.ACCOUNT_DELETED_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTests {
    @InjectMocks
    private AccountServiceImpl accountService;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private Common common;
    @Mock
    private AccountMapper accountMapper;


    @Test
    void testGetAccountById() {
        Account testAccount = createTestAccount();
        when(common.loadAccountById(1L)).thenReturn(testAccount);
        when(common.validateAccount(testAccount)).thenReturn(testAccount);

        Account account = accountService.getAccount(1L);

        assertNotNull(account);
        assertEquals(1L,account.getId());
        assertEquals(testAccount.getEmail(), account.getEmail());

        verify(common, times(1)).loadAccountById(1L);
        verify(common, times(1)).validateAccount(account);
    }

    @Test
    void testFindByEmail(){
        Account testAccount = createTestAccount();
        when(common.loadAccountByEmail(testAccount.getEmail())).thenReturn(testAccount);
        when(common.validateAccount(testAccount)).thenReturn(testAccount);

        Account account = accountService.findAccountByEmail("test@example.com");

        assertNotNull(account);
        assertEquals(testAccount.getId(), account.getId());
        assertEquals(testAccount.getEmail(), account.getEmail());

        verify(common, times(1)).loadAccountByEmail("test@example.com");
        verify(common, times(1)).validateAccount(account);
    }

    @Test
    void testGetMyAccount(){
        Account account = createTestAccount();

        SecurityUser securityUser = createTestSecurityUser(account);
        AccountDetails accountDetails = createAccountDetails(account);

        when(common.loadAccountByEmail(securityUser.getUsername())).thenReturn(account);
        when(common.validateAccount(account)).thenReturn(account);
        when(accountMapper.toDto(account)).thenReturn(accountDetails);

        AccountDetails result = accountService.myAccount(securityUser);

        assertNotNull(result);
        assertEquals(account.getEmail(), securityUser.getUsername());
        assertEquals(securityUser.getUsername(), result.email());
        verify(common, times(1)).loadAccountByEmail(securityUser.getUsername());
        verify(common, times(1)).validateAccount(account);
    }

    @Test
    void testGetVerifiedUsers(){
        Pageable pageable = PageRequest.of(0,10);
        Account account1 = createTestAccount();
        Account account2 = createTestAccount();

        List<Account> accounts = List.of(account1, account2);

    }

    @Test
    void testDeleteAccount(){
        Account account = createTestAccount();
        when(common.loadAccountById(account.getId())).thenReturn(account);

        var result = accountService.deleteAccount(1L);

        assertNotNull(result);
        assertEquals(ACCOUNT_DELETED_SUCCESS, result.message());
        verify(accountRepository, times(1)).delete(account);
    }

    private SecurityUser createTestSecurityUser(Account account){
        return new SecurityUser(account);
    }

    private Account createTestAccount() {
        return Account.builder()
                .id(1L)
                .email("test@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .firstName("John")
                .lastName("Doe")
                .isVerified(true)
                .isEnabled(true)
                .build();
    }

    private AccountDetails createAccountDetails(Account account){
        return AccountDetails.builder()
                .email(account.getEmail())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .enabled(account.isEnabled())
                .verified(account.isVerified())
                .build();
    }

}
