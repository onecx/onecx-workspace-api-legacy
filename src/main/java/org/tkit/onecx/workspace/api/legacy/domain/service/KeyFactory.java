package org.tkit.onecx.workspace.api.legacy.domain.service;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

import io.smallrye.jwt.util.KeyUtils;

public class KeyFactory {

    static final PrivateKey PRIVATE_KEY = createKey();

    static PrivateKey createKey() {
        return createKey(new KeyFactory());
    }

    static PrivateKey createKey(KeyFactory kf) {
        try {
            return kf.createPrivateKey();
        } catch (NoSuchAlgorithmException ex) {
            throw new KeyFactoryException(ex);
        }
    }

    PrivateKey createPrivateKey() throws NoSuchAlgorithmException {
        return KeyUtils.generateKeyPair(2048).getPrivate();
    }

    public static class KeyFactoryException extends RuntimeException {

        public KeyFactoryException(Throwable ex) {
            super(ex);
        }
    }
}
