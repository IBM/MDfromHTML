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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * General utilities to help process markdown content
 * 
 * @author Nathaniel Mills
 */
public class CleanupMarkdown {

   /**
    * Apply all cleaners to the markdown including removing empty headers, and
    * empty list items
    * 
    * @param markdown
    *           content to be cleansed
    * @param seekHeaders
    *           should we ignore content before the first header encountered
    * @return cleansed content
    */
   public static String cleanAll(String markdown, boolean seekHeaders) {
      StringBuffer sb = new StringBuffer();
      BufferedReader br = new BufferedReader(new StringReader(markdown));
      String line = "";
      String test = "";
      boolean reachedHeader = !seekHeaders;
      try {
         while ((line = br.readLine()) != null) {
            if (!reachedHeader) {
               test = line.trim();
               if (test.startsWith("#") == false) {
                  continue;
               } else {
                  reachedHeader = true;
               }
            }
            if (isEmptyHeader(line)) {
               continue;
            }
            if (isEmptyListItem(line)) {
               continue;
            }

            // Don't remove newlines in references -- address elsewhere
            // line = cleanURLReference(line, br);
            // line = cleanImageTags(line);
            sb.append(line);
            sb.append("\n");
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      if (reachedHeader == false) {
         // no header so assume we can use everything
         sb = new StringBuffer(markdown);
      }
      return sb.toString();
   }
   
   static String cleanImageTags(String line) {
      int indexTag = line.indexOf("<img ");
      int indexEndTag = line.indexOf("/>");
      if (indexTag >= 0) {
         if (indexEndTag > indexTag) {
            String tmp = line.substring(0,indexTag);
            line = tmp + line.substring(indexEndTag+2);
         }
      }
      return line;
   }

   /**
    * Strip intervening whitespace between [ and ] which may include multiple
    * lines
    * 
    * @param line
    *           initial line to be tested for first [
    * @param br
    *           the buffered reader allowing us to read forward in the file to
    *           find the ]
    * @return the original line or the newly formed URL Reference line
    */
   static String cleanURLReference(String line, BufferedReader br) {
      String test = line.trim();
      int indexOpen = test.indexOf("[");
      int indexClose = test.indexOf("]");
      if (indexOpen == -1) {
         // no changes required
         return line;
      }
      if (indexClose != -1) {
         // no changes required
         return line;
      }
      StringBuffer sb = new StringBuffer();
      sb.append(test);
      try {
         String nextLine = br.readLine();
         while (nextLine != null) {
            test = nextLine.trim();
            indexClose = test.indexOf("]");
            if (indexClose != -1) {
               sb.append(test);
               break;
            }
            sb.append(test);
            nextLine = br.readLine();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return sb.toString();
   }

   /**
    * Tests whether the line supplied contains an empty header (e.g., a header
    * without any visible content)
    * 
    * @param line
    *           content to be examined
    * @return true if the supplied line contains an empty header
    */
   static boolean isEmptyHeader(String line) {
      boolean result = false;
      String test = line.trim();
      if (test.startsWith("#") && test.endsWith("#")) {
         String[] parts = test.split("(#)\\1+");
         if (parts.length == 2) {
            if (parts[1].trim().length() == 0) {
               // skip this line
               result = true;
            }
            // TODO: could also change max size of headers here
         }
      }
      return result;
   }

   /**
    * Tests whether the line supplied contains an empty list item (e.g., a list
    * item without any visible content)
    * 
    * @param line
    *           content to be examined
    * @return true if the supplied line contains an empty list item
    */
   static boolean isEmptyListItem(String line) {
      boolean result = false;
      String test = line.trim();
      if ("*".equals(test)) {
         // skip this line
         result = true;
      }
      return result;
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      String emptyHeader = "####   ####";
      String test = removeEmptyHeaders(emptyHeader);
      System.out.println(emptyHeader + " became \"" + test + "\"");
      String goodHeader = "#### Some Header ####";
      test = removeEmptyHeaders(goodHeader);
      System.out.println(goodHeader + " became \"" + test + "\"");
   }

   /**
    * General cleanup utility to filter empty header items that result from
    * filters applied to elements while generating markdown.
    * 
    * @param markdown
    *           content to be cleaned
    * @return cleaned version of the input markdown
    */
   public static String removeEmptyHeaders(String markdown) {
      StringBuffer sb = new StringBuffer();
      BufferedReader br = new BufferedReader(new StringReader(markdown));
      String line = "";
      try {
         while ((line = br.readLine()) != null) {
            if (isEmptyHeader(line)) {
               continue;
            }
            if (isEmptyListItem(line)) {
               continue;
            }
            sb.append(line);
            sb.append("\n");
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return sb.toString();
   }

   /**
    * General cleanup utility to filter empty list items that result from
    * filters applied to elements while generating markdown.
    * 
    * @param markdown
    *           content to be cleaned
    * @return cleaned version of the input markdown
    */
   public static String removeEmptyListItems(String markdown) {
      StringBuffer sb = new StringBuffer();
      BufferedReader br = new BufferedReader(new StringReader(markdown));
      String line = "";
      try {
         while ((line = br.readLine()) != null) {
            if (isEmptyListItem(line)) {
               continue;
            }
            sb.append(line);
            sb.append("\n");
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return sb.toString();
   }
}
