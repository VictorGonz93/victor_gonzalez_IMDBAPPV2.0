package edu.pmdm.gonzalez_victorimdbapp.utils;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class KeystoreManager {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12; // Tamaño recomendado para GCM (12 bytes)
    private static final int TAG_LENGTH = 128; // Longitud de la etiqueta en bits
    private final SecretKey secretKey; // Clave de cifrado generada desde el userId

    /**
     * Constructor: Genera la clave secreta basada en el userId.
     * @param userId ID único del usuario, utilizado para generar la clave de cifrado.
     * @throws Exception Si ocurre un error al generar la clave.
     */
    public KeystoreManager(String userId) throws Exception {
        this.secretKey = generateKeyFromUserId(userId);
    }

    /**
     * Genera una clave AES basada en el userId del usuario.
     * @param userId ID único del usuario.
     * @return Clave secreta AES derivada del userId.
     * @throws Exception Si ocurre un error al generar la clave.
     */
    private SecretKey generateKeyFromUserId(String userId) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(userId.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(keyBytes, 16), "AES"); // Usa solo 16 bytes para AES-128
    }

    /**
     * Cifra el texto plano utilizando AES/GCM y lo devuelve codificado en Base64.
     * @param plainText El texto a cifrar.
     * @return Texto cifrado en Base64 (incluye IV en los primeros 12 bytes).
     * @throws Exception Si ocurre un error durante el cifrado.
     */
    public String encryptData(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Combinar IV + datos cifrados (IV || ciphertext)
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    /**
     * Descifra el texto cifrado (codificado en Base64) utilizando AES/GCM.
     * @param cipherText El texto cifrado en Base64 (debe incluir IV).
     * @return Texto plano descifrado.
     * @throws Exception Si ocurre un error durante el descifrado.
     */
    public String decryptData(String cipherText) throws Exception {
        byte[] combined = Base64.decode(cipherText, Base64.DEFAULT);

        // Extraer IV y datos cifrados
        byte[] iv = Arrays.copyOfRange(combined, 0, IV_SIZE);
        byte[] encryptedBytes = Arrays.copyOfRange(combined, IV_SIZE, combined.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
