package com.finedine.authservice.util;

import com.finedine.authservice.entity.Account;
import com.finedine.authservice.enums.AccountStatus;
import com.finedine.authservice.exception.NotFoundException;
import com.finedine.authservice.exception.UnverifiedAccountException;
import com.finedine.authservice.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;

import java.util.Optional;

import static com.finedine.authservice.CustomMessages.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CommonTest {

    @InjectMocks
    private Common common;

    @Mock
    private AccountRepository accountRepository;


    @Test
    void testLoadAccountByEmail_Success(){
        Account account = createTestAccount();
        when(accountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));

        Account result = common.loadAccountByEmail(account.getEmail());

        assertNotNull(result);
        assertEquals(account.getId(), result.getId());
        assertEquals(account.getEmail(), result.getEmail());

        verify(accountRepository, times(1)).findByEmail(account.getEmail());
    }

    @Test
    void testLoadAccountByEmail_NotFound(){
        String email = "notfound@gmail.com";
        when(accountRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> common.loadAccountByEmail(email)
        );

        assertEquals(USER_NOT_FOUND, exception.getMessage());
        verify(accountRepository, times(1)).findByEmail(email);
    }

    @Test
    void testLoadAccountById_Success(){
        Account account = createTestAccount();
        when(accountRepository.findById(account.getId()))
                .thenReturn(Optional.of(account));

        Account result = common.loadAccountById(account.getId());

        assertNotNull(result);
        assertEquals(account.getId(), result.getId());
        assertEquals(account.getEmail(), result.getEmail());

        verify(accountRepository, times(1)).findById(account.getId());
    }

    @Test
    void testLoadAccountById_NotFound(){
        Long id = 999L;
        when(accountRepository.findById(id))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> common.loadAccountById(id)
        );

        assertEquals(USER_NOT_FOUND, exception.getMessage());
        verify(accountRepository, times(1)).findById(id);
    }

    @Test
    void testLoadAccount_WithEmail_Success(){
        Account account = createTestAccount();
        when(accountRepository.findByEmail(account.getEmail()))
                .thenReturn(Optional.of(account));

        Account result = common.loadAccount(account.getEmail(), null);

        assertNotNull(result);
        assertEquals(account.getId(), result.getId());
        verify(accountRepository, times(1)).findByEmail(account.getEmail());
        verify(accountRepository, never()).findById(anyLong());
    }

    @Test
    void testLoadAccount_WithId_Success(){
        Account account = createTestAccount();
        when(accountRepository.findById(account.getId()))
                .thenReturn(Optional.of(account));

        Account result = common.loadAccount(null, account.getId());

        assertNotNull(result);
        assertEquals(account.getId(), result.getId());
        verify(accountRepository, times(1)).findById(account.getId());
        verify(accountRepository, never()).findByEmail(anyString());
    }

    @Test
    void testLoadAccount_BothNull_ThrowsException(){
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> common.loadAccount(null, null)
        );

        assertEquals("Either email or id must be provided", exception.getMessage());
    }

    @Test
    void testValidateAccount_Success(){
        Account account = createTestAccount();

        Account result = common.validateAccount(account);

        assertNotNull(result);
        assertEquals(account, result);
    }

    @Test
    void testValidateAccount_NotVerified_ThrowsException(){
        Account account = createTestAccount();
        account.setVerified(false);

        assertThrows(UnverifiedAccountException.class,
                () -> common.validateAccount(account)
        );
    }

    @Test
    void testValidateAccount_NotEnabled_ThrowsException(){
        Account account = createTestAccount();
        account.setEnabled(false);

        assertThrows(DisabledException.class,
                () -> common.validateAccount(account)
        );
    }


    private Account createTestAccount(){
        return Account.builder()
                .id(1L)
                .email("testuser@gmail.com")
                .accountStatus(AccountStatus.ACTIVE)
                .isEnabled(true)
                .isVerified(true)
                .build();
    }
}