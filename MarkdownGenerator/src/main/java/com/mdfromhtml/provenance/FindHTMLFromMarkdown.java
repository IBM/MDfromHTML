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

package com.mdfromhtml.provenance;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import com.api.json.JSON;
import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLUtils;
import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;
import com.overzealous.remark.convert.DefaultNodeHandler;
import com.overzealous.remark.convert.DocumentConverter;
import com.overzealous.remark.util.BlockWriter;

/**
 * Utility to search through a formatted HTML file for the content that
 * generated a particular markdown component.
 *
 * @author Nathaniel Mills
 */
public class FindHTMLFromMarkdown extends DocumentConverter {

   /**
    * @param args
    */
   public static void main(String[] args) {
      int exitVal = 0;
      JSONObject htmlFilters = null;
      String inputHTMLFileName = "";
      try {
         htmlFilters = MDfromHTMLUtils.loadJSONFile("HTML_Filters.json");
      } catch (Exception e1) {
         System.out.println(
            "Warning: Using no HTML Filters -- can not find \"HTML_Filters.json\": "
               + e1.getLocalizedMessage());
      }
      Options options = Options.multiMarkdown();
      options.hardwraps = true;
      FindHTMLFromMarkdown pgm = new FindHTMLFromMarkdown(
         options);
      if (pgm.getParams(args)) {
         if (pgm._thumbsucker) {
            System.out
               .println("\nLoading the input file " + pgm._inputFileName); //
         }
         if (pgm._interactive) {
            if (MDfromHTMLUtils
               .prompt("Press q to quit or press Enter to continue...")
               .length() == 0) {
               pgm._interactive = false;
            }
         }
         if (!pgm._interactive) {
            try {
               JSONObject provenance = (JSONObject) JSON
                  .parse(new FileInputStream(new File(pgm._inputFileName)));
               if (provenance == null) {
                  throw new Exception("Unable to load the input file named \""
                     + pgm._inputFileName + "\".");
               }
               htmlFilters = (JSONObject) provenance.get("htmlFilters");
               if (htmlFilters == null) {
                  htmlFilters = new JSONObject();
               }
               inputHTMLFileName = (String) provenance.get("inputFilename");
               if (inputHTMLFileName == null) {
                  throw new Exception(
                     "The input provenance file doesn't have an \"inputFilename\" entry.");
               }
               String baseUri = (String) provenance.get("baseURI");
               if (baseUri == null) {
                  throw new Exception(
                     "The input provenance file doesn't have a \"baseURI\" entry.");
               }
               String domain = Remark.getDomain(baseUri);
               Document doc = Jsoup.parse(new File(inputHTMLFileName), "UTF-8",
                  baseUri);
               JSONArray provenanceArray = (JSONArray) provenance
                  .get("provenance");
               if (provenanceArray == null) {
                  throw new Exception(
                     "The input provenance file doesn't have a \"provenance\" array entry.");
               }
               Cleaner _cleaner = Remark.updateCleaner(domain,
                  Options.multiMarkdown(), htmlFilters);
               doc = _cleaner.clean(doc);

               while (true) {
                  String provenanceLevel = pgm
                     .getProvenanceChoice(provenanceArray);
                  if (provenanceLevel == null) {
                     break;
                  }
                  Node htmlNode = pgm.findProvenanceReference(provenanceLevel,
                     htmlFilters, baseUri, domain, doc);
                  if (htmlNode != null) {
                     System.out.println("Provenance level " + provenanceLevel
                        + " found this html node:\n" + htmlNode.toString()
                        + "\n");
                  } else {
                     System.out.println("Provenance level " + provenanceLevel
                        + " could not find a corresponding node.");
                  }
               }
            } catch (Exception e) {
               System.out.println("Error: " + e.getLocalizedMessage());
               e.printStackTrace();
               exitVal = -1;
            }
         }
         if (pgm._thumbsucker) {
            System.out.println();
         }
      } else {
         exitVal = -1;
      }
      if (pgm._thumbsucker) {
         System.out.println("Goodbye");
      }
      System.exit(exitVal);
   }

   Cleaner _cleaner = null;

   String _ext = "json";

   String _inputFileName = "unknown";

   Path _inputPath = null;

   boolean _interactive = false;

   boolean _thumbsucker = false;

   Whitelist _whitelist = null;

   public FindHTMLFromMarkdown(Options options) {
      super(options);
   }

   public FindHTMLFromMarkdown(Options options, JSONObject HTMLFilters) {
      super(options, HTMLFilters);
   }

   Node findProvenanceReference(String searchLevel, JSONObject htmlFilters,
      String baseUri, String domain, Document doc) {
      Node result = null;
      BlockWriter bw = BlockWriter
         .create(DocumentConverter.calculateLength(doc, 0));
      output = bw;
      lastNodeset = blockNodes;
      linkIds = new LinkedHashMap<String, String>();
      // To keep track of already added URLs
      linkUrls = new HashMap<String, String>();
      abbreviations = new LinkedHashMap<String, String>();
      lastNodeset = blockNodes;

      String level = "1"; // top level node for doc.body
      Element body = doc.body();
      if (searchLevel != null && searchLevel.equals(level)) {
         return body;
      }
      result = walkNodes(DefaultNodeHandler.getInstance(), body, blockNodes,
         null, baseUri, domain, level, searchLevel);
      return result;
   }

   public boolean getParams(String[] args) {
      String inputPath = "./src/test/resources";
      String inputFileName = "Archive0001_001_html2md.json";
      String tmp = "";

      try {
         if (args.length >= 1) {
            inputPath = args[0];
         } else {
            _interactive = true;
            _thumbsucker = true;
            tmp = MDfromHTMLUtils
               .prompt("Enter the fully qualified path to directory containing "
                  + _ext + " html2md provenance files, or q to exit ("
                  + inputPath + "):");
            if (tmp == null || tmp.length() == 0) {
               tmp = inputPath;
            }
            if (tmp.toLowerCase().equals("q")) {
               return false;
            }
            inputPath = tmp;
         }
         if (inputPath.endsWith(File.separator) == false) {
            inputPath += File.separator;
         }
         _inputPath = FileSystems.getDefault().getPath(inputPath);
      } catch (InvalidPathException ipe) {
         System.out.println(
            "Error: " + args[0] + " is not a valid directory to form a path.");
         return false;
      }
      if (args.length >= 2) {
         inputFileName = args[1];
      } else {
         _interactive = true;
         _thumbsucker = true;
         tmp = MDfromHTMLUtils.prompt(
            "Enter the file name of the html2md provenance file, or q to exit ("
               + inputFileName + "):");
         if (tmp == null || tmp.length() == 0) {
            tmp = inputFileName;
         }
         if (tmp.toLowerCase().equals("q")) {
            return false;
         }
         inputFileName = tmp;
      }
      inputFileName = inputPath + inputFileName;
      File testOutput = new File(inputFileName);
      if (testOutput.exists() == false) {
         System.out.println(
            "Error: The input file \"" + inputFileName + "\" must exist.");
         return false;
      }
      if (testOutput.isDirectory() == true) {
         System.out.println("Error: The input file \"" + inputFileName
            + "\" must not be a directory.");
         return false;
      }
      _inputFileName = inputFileName;
      if (args.length >= 3) {
         _thumbsucker = new Boolean(args[2]);
      }

      return true;
   }

   String getProvenanceChoice(JSONArray provenanceArray) {
      String result = null;
      /**
       * Page through the provenance to display the markdown at each step and
       * allow the user to select a row, quit, or page up (>) or down (<)
       */
      int maxLines = provenanceArray.size();
      int displayMax = 10;
      String test = "";
      int currentStart = 0;
      int currentEnd = currentStart + displayMax;
      if (currentEnd > maxLines) {
         currentEnd = maxLines;
      }
      JSONObject lineObj = null;
      while (true) {
         result = null;
         for (int i = currentStart; i < currentEnd; i++) {
            lineObj = (JSONObject) provenanceArray.get(i);
            System.out.println(MDfromHTMLUtils.padLeft("" + i, 2, (char) 0x0020)
               + ": " + lineObj.get("md"));
         }
         test = MDfromHTMLUtils.prompt(
            "Enter line number, > (or n) for next page, < (or p) for prior page, s for search, q to quit:");
         if ("q".equalsIgnoreCase(test)) {
            // quit
            break;
         }
         if ("<".equals(test) || "p".equalsIgnoreCase(test)) {
            currentStart -= displayMax;
            if (currentStart < 0) {
               currentStart = 0;
            }
            currentEnd = currentStart + displayMax;
            if (currentEnd > maxLines) {
               currentEnd = maxLines;
            }
            continue;
            // prior page
         } else if (">".equals(test) || "n".equalsIgnoreCase(test)) {
            // next page
            currentStart += displayMax;
            if (currentStart >= maxLines) {
               currentStart = maxLines - displayMax;
            }
            if (currentStart < 0) {
               currentStart = 0;
            }
            currentEnd = currentStart + displayMax;
            if (currentEnd > maxLines) {
               currentEnd = maxLines;
            }
            continue;

         } else if ("e".equalsIgnoreCase(test)) {
            currentEnd = maxLines;
            currentStart = currentEnd - displayMax;
            if (currentStart < 0) {
               currentStart = 0;
            }
         } else if ("s".equalsIgnoreCase(test)) {
            String searchString = MDfromHTMLUtils
               .prompt("Enter the search string: ");
            if (searchString.length() == 0) {
               continue;
            }
            // search array for md that exactly matches the search string
            JSONObject htmlObj = null;
            boolean foundIt = false;
            for (Object obj : provenanceArray) {
               htmlObj = (JSONObject) obj;
               if (searchString.equals((String) htmlObj.get("md"))) {
                  foundIt = true;
                  break;
               }
            }
            if (foundIt == true) {
               result = (String) htmlObj.get("level");
               break;
            }
            // else
            System.out.println("Could not find a match for search \""
               + searchString + "\", try again.");
         } else {
            // item selected
            int choice = -1;
            try {
               choice = new Integer(test);
               if (choice < 0 || choice >= maxLines) { // currentStart || choice
                                                       // >= currentEnd) {
                  System.out.println("Choice must be >= " + 0 // currentStart
                     + " and < " + maxLines /* currentEnd */ + ", try again.");
                  continue;
               }
            } catch (NumberFormatException nfe) {
               System.out
                  .println(test + " is not a valid number choice, try again.");
               continue;
            }
            result = (String) ((JSONObject) provenanceArray.get(choice))
               .get("level");
            break;
         }
      }
      return result;
   }

}
