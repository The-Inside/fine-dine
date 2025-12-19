package com.finedine.orderservice.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

/* * CustomAuthenticationToken is an implementation of AbstractAuthenticationToken
 * that represents an authentication token containing a user and a token string.
 * It is used to encapsulate the authentication details of a user in the security context.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class CustomAuthenticationToken extends AbstractAuthenticationToken {

    private final Jwt token;
    private final transient SecurityUser user;

    public CustomAuthenticationToken(SecurityUser user, Jwt token) {
        super(user.grantedAuthorities());
        this.token = token;
        this.user = user;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return user;
    }


}
