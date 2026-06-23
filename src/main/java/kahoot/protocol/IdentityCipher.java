package kahoot.protocol;

public final class IdentityCipher implements MessageCipher {

    @Override
    public byte[] encrypt(byte[] plaintext) {
        return plaintext;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        return ciphertext;
    }
}
