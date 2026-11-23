package com.itzetech.dpdms.security;

import com.itzetech.dpdms.user.UserAccount;
import com.itzetech.dpdms.user.UserAccountRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Turns a valid access token into the signed-in user. The account is read from the database on every request,
 * so deactivating someone or changing their role takes effect immediately instead of when their token expires.
 */
@Component
class ActiveUserJwtConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final UserAccountRepository users;

    ActiveUserJwtConverter(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        UserAccount u = users.findById(UUID.fromString(jwt.getSubject()))
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new InvalidBearerTokenException("The account is no longer active."));
        AuthUser principal = new AuthUser(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.companyId());
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, authorities);
    }
}
