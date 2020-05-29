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

package com.mdfromhtml.utility;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLUtils;

/**
 * Utility to format JSON files by reading and serializing them. Keys will be
 * sorted, and control characters or unicode will be escaped.
 */
public class JSONFormat {

   /**
    * Utility to format a single file or a directory of files matching a
    * specified file extension (without the leading period).
    * 
    * @param args
    *           Either the qualified file name of the file to be reformatted, or
    *           both the qualified directory name and the extension of files
    *           therein to be formatted (without the leading period).
    */
   public static void main(String[] args) {
      JSONFormat pgm = new JSONFormat();
      if (args.length == 2) {
         try {
            List<Path> files = MDfromHTMLUtils.listSourceFiles(
               FileSystems.getDefault().getPath(args[0]), args[1]);
            for (Path file : files) {
               pgm.doWork(file.toString());
            }
         } catch (Exception e) {
            System.out.println("Can not reference files with extension "
               + args[1] + " in directory " + args[0] + " reason: "
               + e.getLocalizedMessage());
            e.printStackTrace();
         }
      } else if (args.length == 1
         && args[0].toLowerCase().equals("-h") == false) {
         pgm.doWork(args[0]);
      } else {
         System.out.println("Usage:\n"
            + "  Pass in a qualified JSON filename to format that file or\n"
            + "  Pass in a qualified directory and the file extension\n"
            + "    (without the period) of files to be formatted.\n"
            + "  Examples:\n"
            + "    java -cp \"MDfromHTMLBase-0.0.1.jar:API4JSON-1.0.1.jar:javax.ws.rs-api-2.1.1.jar\" com.mdfromhtml.utility.JSONFormat \"/somedir/somefile.json\"\n"
            + "    java -cp \"MDfromHTMLBase-0.0.1.jar:API4JSON-1.0.1.jar:javax.ws.rs-api-2.1.1.jar\" com.mdfromhtml.utility.JSONFormat \"/somedir/someotherdir json\"\n");
      }

      System.out.println("Goodbye");
   }

   /**
    * Format the given JSON file
    * 
    * @param file
    *           JSON file to be formatted
    */
   void doWork(String file) {
      JSONObject dialogsObj;
      try {
         dialogsObj = MDfromHTMLUtils.loadJSONFile(file);
         MDfromHTMLUtils.saveJSONFile(file, dialogsObj);
         System.out.println("Formatted JSON in " + file);
      } catch (Exception e) {
         e.printStackTrace();
      }

   }
}
