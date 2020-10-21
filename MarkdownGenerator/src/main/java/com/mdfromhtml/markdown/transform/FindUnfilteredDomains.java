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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLUtils;
import com.overzealous.remark.Remark;

public class FindUnfilteredDomains {

   public FindUnfilteredDomains() {
   }

   public static void main(String[] args) {
      String defaultFileToURLFilename = "."+File.separator+"data"+File.separator+"file_to_url.txt";
      String htmlFiltersFilename = "."+File.separator+"properties"+File.separator+"HTML_Filters.json";
      String fileToURLFilename = "";
      boolean quietMode = false;
      try {
         if (args.length > 0) {
            htmlFiltersFilename = args[0];
         } else {
            String tmp = MDfromHTMLUtils.prompt("Enter the HTML_Filters.json file location or q to quit: ("+htmlFiltersFilename+"):");
            if (tmp.length() == 0) {
               tmp = htmlFiltersFilename;
            }
            if ("q".equalsIgnoreCase(tmp)) {
               System.exit(0);
            }
            htmlFiltersFilename = tmp;
         }
         if (args.length > 1) {
            fileToURLFilename = args[1];
            quietMode = true;
         } else {
            String tmp = MDfromHTMLUtils.prompt("Enter the path and filename of the tab delimited file cross referencing file names to URLs or q to quit ("+defaultFileToURLFilename+"):");
            if (tmp.length() == 0) {
               tmp = defaultFileToURLFilename;
            }
            if ("q".equalsIgnoreCase(fileToURLFilename)) {
               System.exit(0);
            }
            fileToURLFilename = tmp;
         }
         if (!quietMode) {
            System.out.println("This utility will read URLs from a tab delimited file with URLs in \n"
               + "the 2nd column named "+fileToURLFilename+" and compare these URLs domains against the domain filters in ."+htmlFiltersFilename);
            System.out.println("Loading HTML_Filters.json");
         }
         JSONObject htmlfilters = MDfromHTMLUtils.loadJSONFile(htmlFiltersFilename);
         List<String>files2urls = MDfromHTMLUtils.loadTextFile(fileToURLFilename);
         String[] parts = new String[0];
         String url="";
         String domain="";
         Set<String>unfiltered = new HashSet<String>();
         int urlCount = 1;
         if (!quietMode) {
            System.out.println("Processing URLs (. == 50 urls processed)\n");
         }
         for (String file_url : files2urls) {
            if (file_url.trim().startsWith("#")) {
               continue;
            }
            parts = file_url.split("\t");
            url = parts[1];
            domain = Remark.getDomain(url);
            if (htmlfilters.get(domain)==null) {
               unfiltered.add(domain);
            }
            if (!quietMode) {
               if (urlCount % 50 == 0) {
                  System.out.print(".");
               }
               if (urlCount % 4000 == 0) {
                  System.out.println();
               }
            }
            urlCount++;
         }
         if (!quietMode) {
            System.out.println("\n");
         }
         List<String>unfilteredDomains = new ArrayList<String>();
         unfilteredDomains.addAll(unfiltered);
         Collections.sort(unfilteredDomains);
         if (!quietMode) {
            System.out.println("Domains without filters:");
         }
         for(String unfilteredDomain:unfilteredDomains) {
            System.out.println(unfilteredDomain);
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         if (!quietMode) {
            System.out.println("Goodbye");
         }
      }
   }
}
