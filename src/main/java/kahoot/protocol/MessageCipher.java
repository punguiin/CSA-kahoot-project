package kahoot.protocol;

import java.security.GeneralSecurityException;

public interface MessageCipher {

    byte[] encrypt(byte[] plaintext) throws GeneralSecurityException;

    byte[] decrypt(byte[] ciphertext) throws GeneralSecurityException;
}
