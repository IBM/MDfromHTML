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
      try {
         System.out.println("This utility will read URLs from a tab delimited file with urls in \n"
            + "the 2nd column and compare their domains against the domain filters in HTML_Filters.json.");
         System.out.println("Loading HTML_Filters.json");
         JSONObject htmlfilters = MDfromHTMLUtils.loadJSONFile("HTML_Filters.json");
         String fileToURLFilename = MDfromHTMLUtils.prompt("Enter the fully qualified path to the tab delimited file url cross reference file or q to quit:");
         if (fileToURLFilename.length() == 0) {
            fileToURLFilename = "./src/test/resources/file_to_url.txt";
         }
         if ("q".equalsIgnoreCase(fileToURLFilename) == false) {
            List<String>urlfiles = MDfromHTMLUtils.loadTextFile(fileToURLFilename);
            String[] parts = new String[0];
            String url="";
            String domain="";
            Set<String>unfiltered = new HashSet<String>();
            int urlCount = 1;
            System.out.println("Processing URLs (. == 50 urls processed)\n");
            for (String urlfile : urlfiles) {
               parts = urlfile.split("\t");
               url = parts[1];
               domain = Remark.getDomain(url);
               if (htmlfilters.get(domain)==null) {
                  unfiltered.add(domain);
               }
               if (urlCount % 50 == 0) {
                  System.out.print(".");
               }
               if (urlCount % 4000 == 0) {
                  System.out.println();
               }
               urlCount++;
            }
            System.out.println("\n");
            List<String>unfilteredDomains = new ArrayList<String>();
            unfilteredDomains.addAll(unfiltered);
            Collections.sort(unfilteredDomains);
            System.out.println("Domains without filters:");
            for(String unfilteredDomain:unfilteredDomains) {
               System.out.println(unfilteredDomain);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      System.out.println("Goodbye");
   }

}
