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

package com.mdfromhtml.markdown.test;

import java.io.File;
import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdfromhtml.core.MDfromHTMLUtils;
import com.mdfromhtml.markdown.transform.GetMarkdownFromHTML;

/**
 * Call the Service API in the various conversion utilities
 * 
 * @author wnm3
 *
 */
public class TestServiceAPIs {

   public ObjectNode loadHTMLFilters() throws Exception {
      ObjectNode HTMLFiltersObj = null;
      try {
         ObjectMapper mapper = new ObjectMapper();
         JsonNode filters = mapper.readTree(new File("."+File.separator+"properties"+File.separator+"HTML_Filters.json"));
         HTMLFiltersObj = (ObjectNode) filters;
      } catch (Exception e1) {
         throw new Exception(String
            .format("Error: Can not find a file named \""+"."+File.separator+"properties"+File.separator+"HTML_Filters.json\": %s"
               + e1.getLocalizedMessage()));
      }
      return HTMLFiltersObj;
   }

   /**
    * 
    */
   public TestServiceAPIs() {
   }

   public void runTests() throws Exception {
      Boolean requestProvenance = true;
      ObjectNode HTMLFilters = loadHTMLFilters();
      String html = "";
      String url = "";
      String testFileName = "."+File.separator+"data"+File.separator+"htmljson"+File.separator+"swg21122368.json";
      while (true) {
         String htmlJsonFileName = MDfromHTMLUtils.prompt(
            "Enter the fully qualified htmljson file name or q to quit ("
               + testFileName + "):");
         if ("q".equalsIgnoreCase(htmlJsonFileName)) {
            return;
         }
         if (htmlJsonFileName.length() == 0) {
            htmlJsonFileName = testFileName;
         }
         File inputFile = new File(htmlJsonFileName);
         if (inputFile.exists() == false) {
            System.out.println(
               "Can not find \"" + htmlJsonFileName + "\". Try again.");
            continue;
         }

         String provTest = MDfromHTMLUtils
            .prompt("Return provenance? (y/n/q to quit):");
         if ("q".equalsIgnoreCase(provTest)) {
            return;
         }
         requestProvenance = "y".equalsIgnoreCase(provTest);

         JSONObject htmljsonObj = MDfromHTMLUtils.loadJSONFile(htmlJsonFileName);
         JSONArray captureArray = (JSONArray) htmljsonObj.get("captureArray");
         if (captureArray == null) {
            System.out.println(
               "The file specified is missing the required \"captureArray\" key. Try again.");
         }
         JSONObject htmlObj = (JSONObject) captureArray.get(0);
         if (htmlObj == null) {
            System.out.println(
               "The file specified has an empty \"captureArray\". Try again.");
         }
         html = (String) htmlObj.get("html");
         if (html == null) {
            System.out.println(
               "The first object in the \"captureArray\" does not contain the required \"html\" key. Try again.");
         }
         url = (String) htmlObj.get("url");
         if (url == null) {
            System.out.println(
               "The first object in the \"captureArray\" does not contain the required \"url\" key. Try again.");
         }

         // we are ready to run
         break;
      }

      ObjectNode requestObj = JsonNodeFactory.instance.objectNode();
      requestObj.set("html", JsonNodeFactory.instance.textNode(html));
      requestObj.set("url", JsonNodeFactory.instance.textNode(url));
      requestObj.set("HTMLFilters", HTMLFilters);
      requestObj.put("returnProvenance", true);
      ObjectNode responseObj = GetMarkdownFromHTML
         .getMarkdownFromHTML(requestObj);
      // check for error first
      JsonNode test = responseObj.get("errorMsg");
      if (test != null) {
         System.out.println("An error occurred: " + test.asText());
      }
      test = responseObj.get("markdown");
      if (test == null) {
         System.out.println("Response is missing the \"markdown\" key.");
      }
      String markdown = test.asText();

      System.out.println("\nGenerated Markdown:\n" + markdown + "\n");

      if (requestProvenance) {
         test = responseObj.get("provenance");
         if (test == null) {
            System.out.println("Response is missing the \"provenance\" key.");
         }

         try {
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(responseObj.toString(),
               Object.class);
            System.out.println("\nResponse:\n" + mapper
               .writerWithDefaultPrettyPrinter().writeValueAsString(json));
         } catch (Exception e) {
         }
      }
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      try {
         TestServiceAPIs pgm = new TestServiceAPIs();
         pgm.runTests();
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(-1);
      }
      System.exit(0);
   }

}
