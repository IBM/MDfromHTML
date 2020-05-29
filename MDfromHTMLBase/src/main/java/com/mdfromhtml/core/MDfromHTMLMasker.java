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

import java.util.Set;

import com.api.json.JSONArray;
import com.api.json.JSONObject;

/**
 * Utility class to provide obfuscation of text and JSON objects. Content that
 * is masked can also be unmasked using the complementary functions.
 */
public class MDfromHTMLMasker {

   /**
    * Environment variable used to define the environment variable holding the
    * masking key
    */
   static public final String ENV_MASKER_KEY = "MDfromHTML_MaskerKey";

   // s_debug = false turns off logging responses (normal state)
   // s_debug = true turns on logging responses (s_debug state)
   static boolean s_debug = false; // true;

   public static void main(String[] args) {
      try {
         while (true) {
            String value = MDfromHTMLUtils.prompt("Enter value to be masked or q to quit:");
            if (value.length() == 0 || "q".equalsIgnoreCase(value)) {
               break;
            }
            if (value.startsWith("d3cry9t:")) {
               System.out.println("Unmasked version of "+value+" is:\n"+unmask(value.substring(8)));
            } else {
               System.out.println("Masked version of "+value+" is:\n"+mask(value));
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      System.out.println("Goodbye");
   }

   /**
    * Masks a text string
    * 
    * @param unmaskedText
    *           text to be masked
    * @return masked version of supplied text
    * @throws Exception
    *            if there is a problem with the cryptographic environment
    */
   public static String mask(String unmaskedText) throws Exception {
      return MDfromHTMLCrypto.encrypt(unmaskedText);
   }

   /**
    * Masks all string values found in the supplied array
    * 
    * @param array
    *           Array of elements whose string values are to be masked
    * @return Array of masked elements
    * @throws Exception
    *            if there is a problem with the cryptographic environment
    */
   public static JSONArray maskArray(JSONArray array) throws Exception {
      if (array == null) {
         return null;
      }
      JSONArray newArray = new JSONArray();
      for (int i = 0; i < array.size(); i++) {
         Object value = array.get(i);
         if (value instanceof String) {
            value = mask((String) value);
         } else if (value instanceof JSONObject) {
            value = maskObject((JSONObject) value);
         } else if (value instanceof JSONArray) {
            value = maskArray((JSONArray) value);
         }
         // else fall through to save value as is
         newArray.add(value);
         if (s_debug) {
            System.out.println("pushed =" + value);
         }
      }
      return newArray;
   };

   /**
    * Masks all string values found in the supplied object
    * 
    * @param object
    *           the JSON object whose string values are to be masked
    * @return the masked JSON object
    * @throws Exception
    *            if there is a problem with the cryptographic environment
    */
   public static JSONObject maskObject(JSONObject object) throws Exception {
      if (object == null) {
         return null;
      }
      Set<String> keyset = object.keySet();
      String[] keys = keyset.toArray(new String[0]);
      JSONObject newObj = new JSONObject();
      for (int i = 0; i < keys.length; i++) {
         Object value = object.get(keys[i]);
         if (value instanceof String) {
            value = mask((String) value);
         } else if (value instanceof JSONObject) {
            value = maskObject((JSONObject) value);
         } else if (value instanceof JSONArray) {
            value = maskArray((JSONArray) value);
         }
         // else fall through to save value as is
         newObj.put(keys[i], value);
         if (s_debug) {
            System.out.println("processed " + keys[i] + "=" + value);
         }
      }
      return newObj;
   };

   /**
    * Used to unmask masked content
    * 
    * @param maskedText
    *           masked text to be unmasked
    * @throws Exception
    *            if there is a problem with the cryptographic environment
    * @return unmasked version of input text
    */
   public static String unmask(String maskedText) throws Exception {
      return MDfromHTMLCrypto.decrypt(maskedText, new String(MDfromHTMLUtils.getKey()));
   };

   /**
    * Unmasks any string values found in the array elements
    * 
    * @param array
    *           Array whose string elements are to be unmasked
    * @return Array with unmasked elements
    * @throws Exception
    *            if there is a problem with the cryptographic environment
    */
   public static JSONArray unmaskArray(JSONArray array) throws Exception {
      if (array == null) {
         return null;
      }
      JSONArray newArray = new JSONArray();
      for (int i = 0; i < array.size(); i++) {
         Object value = array.get(i);
         if (value instanceof String) {
            value = unmask((String) value);
         } else if (value instanceof JSONObject) {
            value = unmaskObject((JSONObject) value);
         } else if (value instanceof JSONArray) {
            value = unmaskArray((JSONArray) value);
         }
         // else fall through to save value as is
         newArray.add(value);
         if (s_debug) {
            System.out.println("pushed =" + value);
         }
      }
      return newArray;
   };

   /**
    * Unmasks all string values found within the supplied object
    * 
    * @param object
    *           JSON object to be unmasked
    * @return unmasked JSON object
    * @throws Exception
    *            if there is a problem with the cryptographic environment
    */
   public static JSONObject unmaskObject(JSONObject object) throws Exception {
      if (object == null) {
         return null;
      }
      Set<String> keyset = object.keySet();
      String[] keys = keyset.toArray(new String[0]);
      JSONObject newObj = new JSONObject();
      for (int i = 0; i < keys.length; i++) {
         Object value = object.get(keys[i]);
         if (value instanceof String) {
            value = unmask((String) value);
         } else if (value instanceof JSONObject) {
            value = unmaskObject((JSONObject) value);
         } else if (value instanceof JSONArray) {
            value = unmaskArray((JSONArray) value);
         }
         // else fall through to save value as is
         newObj.put(keys[i], value);
         if (s_debug) {
            System.out.println("processed " + keys[i] + "=" + value);
         }
      }
      return newObj;
   }

}
