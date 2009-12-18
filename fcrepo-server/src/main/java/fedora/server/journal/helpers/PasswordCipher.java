/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.helpers;

import fedora.utilities.Base64;

/**
 * Encipher the password so we aren't writing it to the file in clear.
 * <p>
 * The encipher method does not allow options for cipher type. It should always
 * use the latest and greatest. The decipher method accepts different cipher
 * types so it can be used to read older Journal Files. Known types include:
 * <ul>
 * <li>"" or null - empty cipher, clear text is the same as cipher text</li>
 * <li>"1" - rotating key XOR cipher</li>
 * </ul>
 *
 * @author Jim Blake
 */
public class PasswordCipher {

    /**
     * Two methods: String encode(String key, String clearText); String
     * decode(String key, String decodedText); encoding: for each character in
     * the clearText, get the byte from the char, get a byte from the key, XOR,
     * and write to the byte array. Base64 encode and return. decoding: base64
     * decode. for each byte in the result, XOR with a byte from the key to
     * yield a char. combine into a string. return.
     */

    /**
     * Use the key to produce a ciphered String from the clearText.
     */
    public static String encipher(String key, String clearText) {
        if (key == null) {
            throw new NullPointerException("key may not be null");
        }

        if (key == "") {
            return clearText;
        }

        if (clearText == null) {
            return null;
        }

        byte[] keyBytes = convertKeyToByteArray(key);
        byte[] clearTextBytes = convertClearTextToByteArray(clearText);
        byte[] cipherBytes = applyCipher(keyBytes, clearTextBytes);
        return Base64.encodeToString(cipherBytes);
    }

    /**
     * Use the key to produce a clear text String from the cipherText. If no key
     * is provided, or if no type is specified, just return the text as is.
     */
    public static String decipher(String key,
                                  String cipherText,
                                  String cipherType) {
        if (key == null || key == "") {
            return cipherText;
        }

        if (cipherText == null) {
            return null;
        }

        if (cipherType == null || cipherType == "") {
            return cipherText;
        } else if ("1".equalsIgnoreCase(cipherType)) {
            byte[] keyBytes = convertKeyToByteArray(key);
            byte[] cipherBytes = Base64.decode(cipherText);
            sanityCheckOnCipherBytes(cipherText, cipherBytes);
            byte[] clearTextBytes = applyCipher(keyBytes, cipherBytes);
            return convertByteArrayToClearText(clearTextBytes);
        } else {
            throw new IllegalArgumentException("Unrecognized cipher type: '"
                    + cipherType + "'");
        }
    }

    /**
     * Convert the key to a byte array by compressing the 16-bit characters to
     * bytes. This XOR compression insures that the top 8 bits are still
     * significant, so a key of "\u0020" yields a different cipher than a key of
     * "\u0120".
     */
    private static byte[] convertKeyToByteArray(String key) {
        byte[] result = new byte[key.length()];
        for (int i = 0; i < result.length; i++) {
            char thisChar = key.charAt(i);
            result[i] = (byte) (thisChar >>> 8 & 0xFF ^ thisChar & 0xFF);
        }
        return result;
    }

    /**
     * Convert the clear text to a byte array by splitting each 16-bit character
     * into 2 bytes. This insures that no data is lost.
     */
    private static byte[] convertClearTextToByteArray(String clearText) {
        byte[] result = new byte[clearText.length() * 2];
        for (int i = 0; i < clearText.length(); i++) {
            char thisChar = clearText.charAt(i);
            int pos = i * 2;
            result[pos] = (byte) (thisChar >>> 8 & 0xFF);
            result[pos + 1] = (byte) (thisChar & 0xFF);
        }
        return result;
    }

    /**
     * If the cipher text decodes to an odd number of bytes, we can't go on!
     */
    private static void sanityCheckOnCipherBytes(String cipherText,
                                                 byte[] cipherBytes) {
        if (cipherBytes.length % 2 != 0) {
            throw new IllegalStateException("Ciphered text decodes to an odd number of bytes! Text='"
                    + cipherText
                    + "', decodes to "
                    + cipherBytes.length
                    + " bytes.");
        }
    }

    /**
     * Convert a byte array to text by joining each two bytes into a 16-bit
     * character.
     */
    private static String convertByteArrayToClearText(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < bytes.length; i += 2) {
            char thisChar = (char) (bytes[i] << 8 | bytes[i + 1]);
            result.append(thisChar);
        }
        return result.toString();
    }

    /**
     * The same algorithm applies for enciphering or deciphering. Go through the
     * text, XORing with successive bytes of the key. If you apply it twice, you
     * get the original text back.
     */
    private static byte[] applyCipher(byte[] keyBytes, byte[] textBytes) {
        byte[] result = new byte[textBytes.length];
        for (int i = 0; i < result.length; i++) {
            int keyPos = i % keyBytes.length;
            result[i] = (byte) (textBytes[i] ^ keyBytes[keyPos]);
        }
        return result;
    }
}
