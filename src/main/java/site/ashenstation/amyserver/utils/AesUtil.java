package site.ashenstation.amyserver.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AesUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16; // 128 bit
    private static final int AES_KEY_SIZE = 128;

    /**
     * 生成 AES 密钥
     *
     * @param keySize 128, 192 或 256
     */
    /**
     * 生成随机 AES 密钥（128位）
     */
    public static String generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(AES_KEY_SIZE, new SecureRandom());
        return Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
    }

    public static byte[] encrypt(byte[] bytes, String key) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(key);

        SecretKeySpec secretKeySpec = new SecretKeySpec(decodedKey, ALGORITHM);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        // 生成随机 IV
        byte[] iv = new byte[IV_SIZE];
        SecureRandom random = new SecureRandom();

        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
        byte[] cipherText = cipher.doFinal(bytes);

        // 将 IV 和密文合并
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return combined;
    }

    public static byte[] decrypt(byte[] bytes, String key) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(key);

        SecretKeySpec secretKeySpec = new SecretKeySpec(decodedKey, ALGORITHM);

        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(bytes, 0, iv, 0, iv.length);
        // 提取密文
        byte[] cipherText = new byte[bytes.length - iv.length];
        System.arraycopy(bytes, iv.length, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

        return cipher.doFinal(cipherText);
    }

    public static void main(String[] args) throws Exception {
        File file = new File("C:\\Users\\ashen\\Desktop\\ic_9-3.txt");
        File tfile = new File("C:\\Users\\ashen\\Desktop\\ic_9-3.enc");

        String key = generateKey();

        System.out.println("key:" + key);

//        try (FileInputStream fin = new FileInputStream(file); FileOutputStream fout = new FileOutputStream(tfile)) {
//            byte[] bytes = fin.readAllBytes();
//            byte[] encrypt = encrypt(bytes, key);
//
//            fout.write(encrypt);
//        }
//
//        try (FileInputStream fin = new FileInputStream(tfile); FileOutputStream fout = new FileOutputStream("C:\\Users\\ashen\\Desktop\\ic_9-3.enc.txt")) {
//            byte[] bytes = fin.readAllBytes();
//
//            byte[] decrypt = decrypt(bytes, "3Jk4tiBhmKGiDKrhyCi/3g==");
//
//            fout.write(decrypt);
//        }

    }

}






























