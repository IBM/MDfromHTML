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

/**
 * Valid choices for the verbs describing what kind of action the message
 * reflects. These are modeled after the REST HTTP Verbs: GET, POST, PUT, PATCH,
 * DELETE
 * 
 */

public enum MDfromHTMLIPCVerbs {
   UNDEFINED(0), GET(1), POST(2), PUT(3), PATCH(4), DELETE(5);

   public static MDfromHTMLIPCVerbs fromValue(int value) {
      try {
         return MDfromHTMLIPCVerbs.values()[value];
      } catch (ArrayIndexOutOfBoundsException e) {
         return MDfromHTMLIPCVerbs.UNDEFINED;
      }
   }

   public static MDfromHTMLIPCVerbs fromName(String name) {
      for (MDfromHTMLIPCVerbs v : values()) {
         if (v.toString().compareToIgnoreCase(name) == 0) {
            return v;
         }
      }
      return MDfromHTMLIPCVerbs.UNDEFINED;
   }

   private int _value;

   private MDfromHTMLIPCVerbs(int value) {
      _value = value;
   }

   public int getValue() {
      return _value;
   }

   @Override
   public String toString() {
      switch (this) {
         case UNDEFINED: {
            return "UNDEFINED: " + _value;
         }
         case GET: {
            return "GET: " + _value;
         }
         case POST: {
            return "POST: " + _value;
         }
         case PUT: {
            return "PUT: " + _value;
         }
         case PATCH: {
            return "PATCH: " + _value;
         }
         case DELETE: {
            return "DELETE: " + _value;
         }
         default: {
            return null;
         }
      }
   }
}
