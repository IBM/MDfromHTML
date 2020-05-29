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

package com.mdfromhtml.remark.utils;

/**
 * General utilities for formatting html
 * 
 * @author Nathaniel Mills
 */
public class HTMLFormatter {

   /**
    * @param args
    */
   public static void main(String[] args) {
   }

   /**
    * Provides a breakdown of the html into individual tagged content without
    * any indentation.
    * 
    * @param html
    *           the HTML content to be formatted
    * @return the original HTML formatted with tags on separate lines.
    */
   public static String formatHTML(String html) {
      return formatHTML(html, 0);
   }

   /**
    * Provides a breakdown of the html into individual tagged content with the
    * option to indent the content using the supplied indentAmt if > 0. Note:
    * indented HTML is easier to read, but if using the formatted HTML as a
    * source for annotations referencing back to its content, it is best to use
    * indentAmt = 0.
    * 
    * @param html
    *           the HTML content to be formatted
    * @param indentAmt
    *           the size of the incremental indent when navigating the dom (may
    *           be 0 for no indentation)
    * @return the original HTML formatted with tags on separate lines.
    */
   public static String formatHTML(String html, int indentAmt) {
      String blanks = "                                               ";
      int maxIndent = indentAmt <= 0 ? 0
         : (blanks.length() / indentAmt) * indentAmt;
      int currentIndent = 0;
      int openIndex = -1; // location of next <
      int closeIndex = -1; // location of next >
      StringBuffer sb = new StringBuffer();
      openIndex = html.indexOf("<");
      String indenter = "";
      String testTagType = "";
      while (openIndex >= 0) {
         if (openIndex > 0) {
            if (openIndex == 1 && "\n".equals(html.substring(0, 1))) {
               // ignore newlines embedded between tags
               html = html.substring(1);
               openIndex = 0;
               continue;
            }
            // keep same indent level
            // sb.append(indenter);
            // elected to left justify to preserve actual spacing

            // process content between closing tag and opening tag
            sb.append(html.substring(0, openIndex));
            html = html.substring(openIndex);
            sb.append("\n");
            openIndex = 0;
            continue;
         }
         // otherwise, we have several cases depending on next char
         testTagType = html.substring(1, 2);
         switch (testTagType) {
            case "!": {
               // comment
               // keep same indent level
               sb.append(indenter);
               closeIndex = html.indexOf("-->");
               sb.append(html.substring(0, closeIndex + 3));
               html = html.substring(closeIndex + 3);
               sb.append("\n");
               openIndex = html.indexOf("<");
               break;
            }
            case "/": {
               // closing tag (unindent and print the tag
               currentIndent -= indentAmt;
               if (currentIndent < 0) {
                  currentIndent = 0;
               }
               indenter = blanks.substring(0,
                  Math.min(maxIndent, currentIndent));
               sb.append(indenter);
               closeIndex = html.indexOf(">");
               sb.append(html.substring(0, closeIndex + 1));
               html = html.substring(closeIndex + 1);
               sb.append("\n");
               openIndex = html.indexOf("<");
               break;
            }
            default: {
               // opening tag (can be <...> or <.../>
               closeIndex = html.indexOf(">");
               testTagType = html.substring(closeIndex - 1, closeIndex);
               if ("/".equals(testTagType)) {
                  // <.../> so use same indent
                  sb.append(indenter);
                  sb.append(html.substring(0, closeIndex + 1));
                  html = html.substring(closeIndex + 1);
                  sb.append("\n");
                  openIndex = html.indexOf("<");
               } else {
                  // <...> so add indent after printing tag
                  sb.append(indenter);
                  sb.append(html.substring(0, closeIndex + 1));
                  html = html.substring(closeIndex + 1);
                  currentIndent += indentAmt;
                  indenter = blanks.substring(0,
                     Math.min(maxIndent, currentIndent));
                  sb.append("\n");
                  openIndex = html.indexOf("<");
               }
               break;
            }
         } // end switch on tagType
      } // end while we have tags to process
      return sb.toString();
   }

}
