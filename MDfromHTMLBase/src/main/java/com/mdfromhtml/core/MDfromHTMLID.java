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

import java.io.Serializable;
import java.util.Base64;
import java.util.UUID;

/**
 * Implementation of the MDfromHTMLID class which provides a unique identifier for
 * an MDfromHTML object.
 * 
 */
public class MDfromHTMLID implements Serializable, Comparable<Object>, Cloneable {

   static public int ID_LENGTH = 22; // matches database settings
   static public final String ID_UNDEFINED = MDfromHTMLConstants.UNDEFINED_ID;
   static private final long serialVersionUID = 2726688498762622537L;
   static public final MDfromHTMLID UNDEFINED_ID = new MDfromHTMLID();
   static public final String val_safe = new String(
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_=");

   static {
      UNDEFINED_ID._id = MDfromHTMLConstants.UNDEFINED_ID;
   }

   /**
    * General purpose factory constructor
    * 
    * @return a new MDfromHTMLID
    */
   public static MDfromHTMLID generateNewID() {
      return new MDfromHTMLID();
   }

   /**
    * Constructor
    * 
    * @param uuid
    *           - UUID of this MDfromHTMLID
    */
   public static MDfromHTMLID getExistingID(String uuid) {
      if (uuid != null && uuid.length() == ID_LENGTH) {
         if (uuid.compareTo(MDfromHTMLConstants.UNDEFINED_ID) == 0) {
            return MDfromHTMLID.UNDEFINED_ID;
         }
         // check for valid characters
         byte[] uuidBytes = uuid.getBytes();
         for (byte uuidByte : uuidBytes) {
            if (val_safe.indexOf((int) uuidByte) == -1) {
               return MDfromHTMLID.UNDEFINED_ID;
            }
         }
         MDfromHTMLID id = new MDfromHTMLID(uuid);
         return id;
      }
      return MDfromHTMLID.UNDEFINED_ID;
   }

   static public boolean isUndefined(MDfromHTMLID test) {
      if (test == null) {
         return true;
      }
      return test.isUndefined();
   }

   static public void main(String[] args) {
      if (MDfromHTMLID.isUndefined(MDfromHTMLID.getExistingID("too_small")) == false) {
         System.out.println("Error for \"too_small\"");
      } else if (MDfromHTMLID.isUndefined(
         MDfromHTMLID.getExistingID("much_much_much_much_too_long")) == false) {
         System.out.println("Error for \"much_much_much_much_too_long\"");
      } else if (MDfromHTMLID.isUndefined(
         MDfromHTMLID.getExistingID("bad char______________")) == false) {
         System.out.println("Error for \"bad char______________\"");
      } else if (MDfromHTMLID.isUndefined(
         MDfromHTMLID.getExistingID("??????????????????????")) == false) {
         System.out.println("Error for \"??????????????????????\"");
      } else if (MDfromHTMLID
         .isUndefined(MDfromHTMLID.getExistingID("ABCDEFGHIJKLMNOPQRSTUV"))) {
         System.out.println("Error for \"ABCDEFGHIJKLMNOPQRSTUV\"");
      } else {
         System.out.println("Everything is okay.");
      }
      while (true) {
         String quit = MDfromHTMLUtils.prompt("Enter quit to exit:");
         if ("quit".compareToIgnoreCase(quit) == 0) {
            break;
         }
         System.out.println("MDfromHTMLID: \"" + MDfromHTMLID.generateNewID() + "\"");
      }
      System.out.println("Goodbye");
   }

   private String _id = ID_UNDEFINED;

   /**
    * Construct a compressed MDfromHTMLID based on
    * {@link MDfromHTMLBASE64Codec#encode(byte[],boolean)} of the bytes gleaned from
    * {@link UUID#randomUUID()}
    */
   public MDfromHTMLID() {
      _id = getUUID();
   }

   /**
    * Constructor
    * 
    * @param uuid
    *           - UUID of this MDfromHTMLID
    */
   private MDfromHTMLID(String uuid) {
      _id = uuid;
   }

   public MDfromHTMLID clone() {
      return getExistingID(_id);
   }

   @Override
   public int compareTo(Object o) {
      if (o == null) {
         o = MDfromHTMLID.UNDEFINED_ID;
      }
      if (o instanceof MDfromHTMLID) {
         return _id.compareTo(((MDfromHTMLID) o).toString());
      }
      return this.toString().compareTo(o.toString());
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (MDfromHTMLID.class.isInstance(obj)) {
         if (isUndefined() == false && this._id.equals(((MDfromHTMLID) obj)._id)) {
            return true;
         } else if (isUndefined() && MDfromHTMLID.isUndefined((MDfromHTMLID) obj)) {
            return true;
         }
      }
      return false;
   }

   private String getUUID() {
      UUID uuid = UUID.randomUUID();
      long msb = uuid.getMostSignificantBits();
      long lsb = uuid.getLeastSignificantBits();
      byte[] bUUID = new byte[16];
      for (int i = 0; i < 8; i++) {
         bUUID[i] = (byte) (msb >>> 8 * (7 - i));
      }
      for (int i = 8; i < 16; i++) {
         bUUID[i] = (byte) (lsb >>> 8 * (7 - i));
      }
      /**
       * strip the trailing "==" from the Base64 encoding and use URL Safe
       * content
       */
      return Base64.getUrlEncoder().encodeToString(bUUID).substring(0, 22);
   }

   @Override
   public int hashCode() {
      if (_id != null && _id.trim().length() != 0 && _id.compareTo("?") != 0) {
         return _id.hashCode();
      } else {
         return 0;
      }
   }

   /**
    * @return true if the MDfromHTMLID is uninitialized
    */
   public boolean isUndefined() {
      return ID_UNDEFINED.equals(_id);
   }

   /**
    * @return the string representation of the MDfromHTMLID
    */
   @Override
   public String toString() {
      return this._id;
   }

}
