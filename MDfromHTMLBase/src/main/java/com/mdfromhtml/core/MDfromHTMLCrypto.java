/**
 * (c) Copyright 2020 IBM Corporation
 * 1 New Orchard Road, 
 * Armonk, New York, 10504-1722
 * United States
 * +1 914 499 1900
 * support: Nathaniel Mills wnm3@us.ibm.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.mdfromhtml.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Utility to mask / unmask text. While there is a well defined key used for
 * masking, a different key can be set as either an environment variable or
 * passed as a system property (e.g., -Dname=value on Java command line) using
 * the name defined by {@link MDfromHTMLMasker#ENV_MASKER_KEY}
 */

public class MDfromHTMLCrypto {

   private static final String REVERSE = "d3cry9t:";

   public static final byte[] SALT = {
      (byte) 0xfe, (byte) 0x31, (byte) 0x12, (byte) 0x17, (byte) 0xde,
      (byte) 0x53, (byte) 0x16, (byte) 0x25,
   };

   private static byte[] base64Decode(String property) throws IOException {
      return Base64.getUrlDecoder().decode(property);
   }

   private static String base64Encode(byte[] bytes) {
      return Base64.getUrlEncoder().encodeToString(bytes);
   }

   /**
    * Unmasks the text using the salt and key
    * 
    * @param text
    *           string to be masked
    * @param key
    *           character phrase used to mask the text
    * @param salt
    *           bytes used to initialize the cryptographic environment
    * @return unmasked version of the masked text
    * @throws Exception
    *            if there is a problem with the cryptographic environment
    */
   public static String decrypt(String text, char[] key, byte[] salt)
      throws Exception {
      SecretKeyFactory keyFactory;
      try {
         keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
         SecretKey secretKey = keyFactory.generateSecret(new PBEKeySpec(key));
         Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
         pbeCipher.init(Cipher.DECRYPT_MODE, secretKey,
            new PBEParameterSpec(salt, 20));
         return new String(pbeCipher.doFinal(base64Decode(text)), "UTF-8");
      } catch (NoSuchAlgorithmException e) {
         throw new Exception(
            "Can not find needed algorithm: " + e.getLocalizedMessage(), e);
      } catch (InvalidKeySpecException e) {
         throw new Exception(
            "Invalid key specification: " + e.getLocalizedMessage(), e);
      } catch (NoSuchPaddingException e) {
         throw new Exception("Missing padding: " + e.getLocalizedMessage(), e);
      } catch (InvalidKeyException e) {
         throw new Exception("Invalid key: " + e.getLocalizedMessage(), e);
      } catch (InvalidAlgorithmParameterException e) {
         throw new Exception(
            "Invalid algorithm parameter: " + e.getLocalizedMessage(), e);
      } catch (UnsupportedEncodingException e) {
         throw new Exception("Unsupported encoding: " + e.getLocalizedMessage(),
            e);
      } catch (IllegalBlockSizeException e) {
         throw new Exception("Illegal block size: " + e.getLocalizedMessage(),
            e);
      } catch (BadPaddingException e) {
         throw new Exception("Bad padding: " + e.getLocalizedMessage(), e);
      } catch (IOException e) {
         throw new Exception("IO Exception: " + e.getLocalizedMessage(), e);
      }
   }

   /**
    * Unmasks the text using the key and system supplied cryptographic
    * initialization bytes
    * 
    * @param text
    *           string to be unmasked
    * @param key
    *           character phrase used to mask the text
    * @return unmasked version of the masked text
    * @throws Exception
    *            if there is a problem with the cryptographic environment
    */
   public static String decrypt(String text, String key) throws Exception {
      if (new String(MDfromHTMLUtils.getKey()).equals(key) == false) {
         throw new Exception("Property or Password is invalid");
      }
      return decrypt(text, MDfromHTMLUtils.getKey(), SALT);
   }

   /**
    * Masks the supplied text
    * 
    * @param text
    *           string to be masked
    * @return masked version of the text
    * @throws Exception
    *            if there is a problem with the cryptographic environment
    */
   public static String encrypt(String text) throws Exception {
      return encrypt(text, MDfromHTMLUtils.getKey(), SALT);
   }

   /**
    * Masks the supplied text using the key and salt bytes
    * 
    * @param text
    *           string to be masked
    * @param key
    *           character phrase used to mask the text
    * @param salt
    *           bytes used to initialize the cryptographic environment
    * @return masked version of the text
    * @throws Exception
    *            if there is a problem with the cryptographic environment
    */
   public static String encrypt(String text, char[] key, byte[] salt)
      throws Exception {
      SecretKeyFactory keyFactory;
      try {
         keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
         SecretKey secretKey = keyFactory.generateSecret(new PBEKeySpec(key));
         Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
         pbeCipher.init(Cipher.ENCRYPT_MODE, secretKey,
            new PBEParameterSpec(salt, 20));
         return base64Encode(pbeCipher.doFinal(text.getBytes("UTF-8")));
      } catch (NoSuchAlgorithmException e) {
         throw new Exception(
            "Can not find needed algorithm: " + e.getLocalizedMessage(), e);
      } catch (InvalidKeySpecException e) {
         throw new Exception(
            "Invalid key specification: " + e.getLocalizedMessage(), e);
      } catch (NoSuchPaddingException e) {
         throw new Exception("Missing padding: " + e.getLocalizedMessage(), e);
      } catch (InvalidKeyException e) {
         throw new Exception("Invalid key: " + e.getLocalizedMessage(), e);
      } catch (InvalidAlgorithmParameterException e) {
         throw new Exception(
            "Invalid algorithm parameter: " + e.getLocalizedMessage(), e);
      } catch (UnsupportedEncodingException e) {
         throw new Exception("Unsupported encoding: " + e.getLocalizedMessage(),
            e);
      } catch (IllegalBlockSizeException e) {
         throw new Exception("Illegal block size: " + e.getLocalizedMessage(),
            e);
      } catch (BadPaddingException e) {
         throw new Exception("Bad padding: " + e.getLocalizedMessage(), e);
      }
   }

   public static void main(String[] args) throws Exception {
      String originalText = "secret";
      while (true) {
         originalText = MDfromHTMLUtils
            .prompt("Enter text to be masked, or nothing to quit:", true);
         if (originalText.length() == 0) {
            break;
         }
         if (originalText.startsWith(REVERSE) == false) {
            System.out.println("Original text: " + originalText);
            String maskedText = encrypt(originalText);
            System.out.println("Masked text: " + maskedText);
            String unmaskedText = decrypt(maskedText,
               MDfromHTMLUtils.getKey().toString());
            System.out.println("Unmasked text: " + unmaskedText);
         } else {
            originalText = originalText.substring(REVERSE.length());
            System.out.println("Masked text: " + originalText);
            String unmaskedText = decrypt(originalText,
               MDfromHTMLUtils.getKey().toString());
            System.out.println("Unmasked text: " + unmaskedText);
         }
      }
      System.out.println("Goodbye");
   }

}
