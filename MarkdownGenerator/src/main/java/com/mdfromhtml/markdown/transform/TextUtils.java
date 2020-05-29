/**
 * (c) Copyright 2019-2020 IBM Corporation
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

package com.mdfromhtml.markdown.transform;

/**
 *Common text processing utilities
 *
 */
public class TextUtils {

   static String _sentBuffer = "(S ";
   static String _commaBuffer = "(, ,";


   /**
    * Constructor 
    */
   public TextUtils() {
   }

   /**
    * Filter comments from supplied text
    * @param text string whose embedded comments are to be removed
    * @return text cleansed of embedded comments
    */
   static public String filterComments(String text) {
      String openCmt = "<!-- ";
      String closeCmt = " -->";
      StringBuffer sb = new StringBuffer();
      if (text == null) { text = ""; }
      int openIndex = text.indexOf(openCmt);
      int closeIndex = -1;
      while (openIndex >= 0) {
         sb.append(text.substring(0,openIndex));
         text = text.substring(openIndex);
         closeIndex = text.indexOf(closeCmt);
         if (closeIndex == -1) {
            sb.append(text);
            break;
         }
         text = text.substring(closeIndex+closeCmt.length());
         openIndex = text.indexOf(openCmt);
      }
      sb.append(text);
      return sb.toString();
   }

   static public void main(String[] args) {
      String[] tests = new String[] {
         "<!-- open comment -->After comment",
         "Before comment<!-- close comment -->",
         "Before comment <!-- middle comment -->after comment",
         null,
         "No comment"
      };
      for (String test:tests) {
         System.out.println(filterComments(test));
      }
   }


}
