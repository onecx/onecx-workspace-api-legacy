package org.tkit.onecx.workspace.api.legacy.domain.service;

import java.util.Map;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwx.JsonWebStructure;
import org.tkit.onecx.workspace.api.legacy.domain.config.ApiLegacyConfig;

import io.smallrye.jwt.build.Jwt;

@Singleton
public class TokenService {

    @SuppressWarnings("java:S5998")
    private static final Pattern CLAIM_PATH_PATTERN = Pattern.compile("\\/(?=(?:(?:[^\"]*\"){2})*[^\"]*$)");

    @Inject
    ApiLegacyConfig config;

    private static String[] accessTokenClaimPath;

    @PostConstruct
    @SuppressWarnings("java:S2696")
    public void init() {
        accessTokenClaimPath = splitClaimPath(config.token().accessTokenRolesPath());
    }

    public String convertIdToken(String token) {

        JwtClaims jwtClaims;

        try {
            var jws = (JsonWebSignature) JsonWebStructure.fromCompactSerialization(token);
            jwtClaims = JwtClaims.parse(jws.getUnverifiedPayload());
        } catch (Exception ex) {
            throw new TokenException("Error parse token", ex);
        }

        // check for claim name
        if (!jwtClaims.hasClaim(config.token().idTokenRoles())) {
            return token;
        }

        // check for list
        if (!jwtClaims.isClaimValueStringList(config.token().idTokenRoles())) {
            throw new TokenException("Roles claim ist not valid list");
        }

        Object item = jwtClaims.getClaimValue(config.token().idTokenRoles());
        for (var i = (accessTokenClaimPath.length - 1); 0 < i; i--) {
            item = Map.of(accessTokenClaimPath[i], item);
        }
        jwtClaims.setClaim(accessTokenClaimPath[0], item);

        var data = jwtClaims.getClaimsMap();
        data.remove(config.token().idTokenRoles());
        return Jwt.claims(data).sign(KeyFactory.PRIVATE_KEY);
    }

    @SuppressWarnings("java:S2692")
    static String[] splitClaimPath(String claimPath) {
        return claimPath.indexOf('/') > 0 ? CLAIM_PATH_PATTERN.split(claimPath) : new String[] { claimPath };
    }
}
