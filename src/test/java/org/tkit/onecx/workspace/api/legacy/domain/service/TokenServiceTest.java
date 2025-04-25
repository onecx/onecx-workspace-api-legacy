package org.tkit.onecx.workspace.api.legacy.domain.service;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.workspace.api.legacy.AbstractTest;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TokenServiceTest extends AbstractTest {

    @Inject
    TokenService service;

    @Test
    void splitClaimPathTest() {
        var result = TokenService.splitClaimPath("one");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.length);

        result = TokenService.splitClaimPath("a/b/c");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
    }

    @Test
    void convertIdTokenTest() {
        Assertions.assertThrows(TokenException.class, () -> service.convertIdToken("000"));

        var token = createIdToken("test", false);
        Assertions.assertNotNull(service.convertIdToken(token));

        var token2 = createIdToken("test", "test");
        Assertions.assertThrows(TokenException.class, () -> service.convertIdToken(token2));
    }
}
