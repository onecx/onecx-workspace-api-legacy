package org.tkit.onecx.workspace.api.legacy.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.tkit.onecx.workspace.api.legacy.AbstractTest;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class KeyFactoryTest extends AbstractTest {

    @Test
    void createKeyFailedTest() throws NoSuchAlgorithmException {
        var mock = Mockito.mock(KeyFactory.class);
        Mockito.when(mock.createPrivateKey()).thenThrow(new NoSuchAlgorithmException());
        KeyFactory.KeyFactoryException throwable = catchThrowableOfType(KeyFactory.KeyFactoryException.class,
                () -> KeyFactory.createKey(mock));
        assertThat(throwable).isNotNull();
    }
}
