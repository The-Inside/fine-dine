package com.finedine.orderservice.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;


/**
 * Represents a user in the security context with an outlet ID, email, and granted authorities.
 */
public record SecurityUser(String email,
                           String externalId,
                           Collection<SimpleGrantedAuthority> grantedAuthorities) {

}
