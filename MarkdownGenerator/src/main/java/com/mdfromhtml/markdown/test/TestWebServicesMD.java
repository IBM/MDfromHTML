/**
 * IBM Confidential
 * OCO Source Materials
 * AIDEN Project
 *
 * (C) Copyright 2019 IBM Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *----------------------------------------------------------------------
*/

package com.mdfromhtml.markdown.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLComms;
import com.mdfromhtml.core.MDfromHTMLConstants;
import com.mdfromhtml.core.MDfromHTMLUtils;

/**
 * Utility to test the WebService requests
 * 
 * @author wnm3
 *
 */
public class TestWebServicesMD {

   JSONObject _htmlFilters = new JSONObject();

   /**
    * 
    */
   public TestWebServicesMD() {
      String htmlFiltersFilename = "."+File.separator+"properties"+File.separator+"HTML_Filters.json";
      while (true) {
         String test = MDfromHTMLUtils.prompt(
            "Enter the fully qualified path of the HTML filters file (\""
               + htmlFiltersFilename + "\" or q to quit:");
         if ("q".equalsIgnoreCase(test)) {
            System.out.println("Goodbye");
            System.exit(0);
         }
         if (test.length() == 0) {
            test = htmlFiltersFilename;
         }
         htmlFiltersFilename = test;
         try {
            _htmlFilters = MDfromHTMLUtils.loadJSONFile(htmlFiltersFilename);
            break;
         } catch (Exception e) {
            System.err.println(
               "Could not load the HTML filters file \"" + htmlFiltersFilename
                  + "\". " + e.getLocalizedMessage() + ". Try again.");
            continue;
         }
      }
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      TestWebServicesMD pgm = new TestWebServicesMD();
      boolean quit = false;
      while (!quit) {
         int actionVal = 1;
         try {
            switch (actionVal) {
               case 1: {
                  JSONObject response = pgm.getMarkdownFromHTML();
                  if (response.size() == 0) {
                     System.out.println("Request aborted.");
                     quit = true;
                  } else {
                     System.out.println("Full Response:\n"+response.serialize(true));
                     System.out.println("\n--------- cut markdown below ----------\n");
                     System.out.println(((JSONObject)response.get("results")).get("markdown"));
                     System.out.println("\n--------- end markdown cutting --------\n");
                  }
                  break;
               }
               default: {
                  System.err.println(
                     "The action you entered is not equal to 1. Try again.");
                  break;
               }
            }
         } catch (NumberFormatException | IOException nfe) {
            nfe.printStackTrace();
         }
      }
      System.out.println("Goodbye");
   }

   public JSONObject getMarkdownFromHTML() {
		String mdfromhtmlWebServicesFile = MDfromHTMLConstants.MDfromHTML_DIR_PROPERTIES + "MDfromHTMLWebService.properties";
      JSONObject response = new JSONObject();
      String htmlFile = "./src/test/resources/Archive0001.json";
      String servicename = "localhost";
      while (true) {
			String propertiesFilename = MDfromHTMLUtils
					.prompt("Enter the fully qualified filename of the web service properties file (" + mdfromhtmlWebServicesFile
							+ ") or q  to quit:");
			if (propertiesFilename.length() == 0) {
				propertiesFilename = mdfromhtmlWebServicesFile;
			}
			if ("q".equalsIgnoreCase(propertiesFilename)) {
				break;
			}
			Properties propFile = new Properties();
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(propertiesFilename);
				propFile.load(fis);
			} catch (IOException ioe) {
				System.out.println("Can not load " + propertiesFilename + " Error: " + ioe.getLocalizedMessage());
				continue;
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

         String filename = MDfromHTMLUtils.prompt(
            "Enter the fully qualified filename of the HTML json file whose html is to be transformed to markdown ("+htmlFile+") or q to quit:");
         if ("q".equalsIgnoreCase(filename)) {
            break;
         }
         if (filename.length() == 0) {
            filename = htmlFile;
         }
         
         File testFile = new File(filename);
         if (testFile.exists() == false) {
            System.err.println(
               "Filename \"" + filename + "\" does not exist. Try again.");
            continue;
         }
         
         if (testFile.isDirectory()) {
            System.err.println(
               "Filename \"" + filename + "\" is a directory. Try again.");
            continue;
         }
         String hostname = MDfromHTMLUtils.prompt("Enter the hostname where the service is running or q to quit ("+servicename+")");
         if ("q".equalsIgnoreCase(hostname)) {
            break;
         }
         if (hostname.length() == 0) {
            hostname = servicename; 
         }
         servicename = hostname;
         try {
            JSONObject htmlObj = MDfromHTMLUtils.loadJSONFile(filename);
            htmlFile = filename;
            JSONObject service = new JSONObject();
				service.put("protocol", propFile.getProperty("protocol", "http"));
				service.put("domain", propFile.getProperty("hostname", "localhost"));
				service.put("portnumber", propFile.getProperty("port", "9080"));
				service.put("endpoint", "/" + propFile.getProperty("servletname", "MDfromHTMLWebServices") + "/"
						+ propFile.getProperty("version", "v1") + "/mdfromhtml/getMarkdown");
				service.put("username", propFile.getProperty("username", "mask"));
				service.put("password", propFile.getProperty("password", "password"));
				service.put("apitimeout", propFile.getProperty("apitimeout", "100000")); // 100 seconds
            JSONObject request = new JSONObject();
            JSONArray captureArray = (JSONArray) htmlObj.get("captureArray");
            JSONObject firstObj = (JSONObject) captureArray.get(0);
            request.put("html", firstObj.get("html"));
            request.put("url", firstObj.get("url"));
            request.put("HTMLFilters", _htmlFilters);
            JSONObject params = new JSONObject();
            params.put("request",request);
            response = MDfromHTMLComms.sendRequest("POST", service, params);
            break;
         } catch (Exception e) {
            System.err.println("Error calling service: "
               + e.getLocalizedMessage() + ". Try again.");
            System.err.println("Note: you can check the server is running by pasting this in a browser:\n"
               + "http://"+hostname+":9080/AidenWebServices/v1/HelloAiden");
         }
      }
      return response;
   }
}
