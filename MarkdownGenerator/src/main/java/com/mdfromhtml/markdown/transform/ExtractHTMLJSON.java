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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.api.json.JSON;
import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLUtils;

/**
 * Read the text file produced issuing curl commands to the html_extractor to
 * create individual files for use in the ExtractHTMLJSON utility.
 */
public class ExtractHTMLJSON {

   /**
    * 
    */
   public ExtractHTMLJSON() {
      try {
         _filters = MDfromHTMLUtils.loadTextFile("RejectStrings.txt");
         // rewrite filters in lowercase
         List<String> newFilters = new ArrayList<String>();
         for (String filter : _filters) {
            if (filter.startsWith("#")) {
               // skip comments
               continue;
            }
            filter = filter.trim();
            if (filter.length() == 0) {
               // skip empty lines
               continue;
            }
            newFilters.add(filter.toLowerCase());
         }
         _filters = newFilters;
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Main entry point to read a specified input directory to find text files
    * containing sequences of JSON Objects to be exgtracted and written to
    * separate files in the output directory.
    * 
    * @param args
    *           inputPath, and outputPath (if not supplied, the program prompts
    *           for their values)
    */
   public static void main(String[] args) {
      int exitVal = 0;
      ExtractHTMLJSON pgm = new ExtractHTMLJSON();
      if (pgm.getParams(args)) {
         if (pgm._thumbsucker) {
            System.out.println("\nFiles ending with ." + pgm._ext
               + " will be read from " + pgm._inputPath //
               + "\nand the generated htmljson files (.json) " + "saved in "
               + pgm._outputPath); //
         }
         if (pgm._thumbsucker) {
            System.out
               .println("\nFilter strings used to check html for bad pages:");
            for (String filter : pgm._filters) {
               System.out.println(filter);
            }
            System.out.println();

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
               List<Path> files = MDfromHTMLUtils.listSourceFiles(
                  FileSystems.getDefault().getPath(pgm._inputPath.toString()),
                  pgm._ext);
               for (Path file : files) {
                  exitVal = pgm.doWork(file);
                  if (exitVal != 0) {
                     break;
                  }
               }
            } catch (Exception e) {
               System.out
                  .println("Error: Can not reference files with extension "
                     + pgm._ext + " in directory " + pgm._inputPath
                     + " reason: " + e.getLocalizedMessage());
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

   /**
    * Process the specified file to transform its content into formatted text
    * and save it to a txt file in the specified output directory.
    * 
    * @param file
    *           the file containing the annotation json from ICCC
    * @return exit value (0 indicates success, otherwise -1 for failure)
    */
   int doWork(Path file) {
      int exitVal = 0;
      try {
         String fqFileName = file.toString();
         if (_thumbsucker) {
            System.out.println("Processing: " + fqFileName);
         }
         BufferedReader br = MDfromHTMLUtils.openTextFile(fqFileName);
         StringBuffer sb = new StringBuffer();
         String line = br.readLine();
         int linenum = 0;
         while (line != null) {
            linenum++;
            try {
            if (line.startsWith("}")) {
               sb.append(line);
               sb.append("\n");
               saveFile(sb.toString());
               sb = new StringBuffer();
            } else {
               sb.append(line);
               sb.append("\n");
            }
            line = br.readLine();
            } catch (OutOfMemoryError oome) {
               sb = new StringBuffer();
               System.out.println("Error reading line "+linenum);
               // read to next line starting with {
               line = br.readLine();
               linenum++;
               while (line != null) {
                  if (line.startsWith("{")) {
                     sb.append(line);
                     sb.append("\n");
                     line = br.readLine();
                     linenum++;
                     break;
                  }
                  line = br.readLine();
                  if (line.contains("\"url\":")) {
                     System.out.println("Skipping: "+line);
                  }
                  linenum++;
               }
               System.out.println("Resuming at line "+linenum);
            }
         }
         if (sb.length() > 0) {
            try {
            saveFile(sb.toString());
            } catch (Exception e) {
               System.out.println("\n\nError: "+e.getLocalizedMessage()+"\n");
               System.out.println(sb.toString());
               System.out.println("\n\nEnd Error: "+e.getLocalizedMessage()+"\n");
            }
         }
         MDfromHTMLUtils.closeTextFile(br);
      } catch (Exception e) {
         e.printStackTrace();
         exitVal = -1;
      }
      return exitVal;
   }

   /**
    * Saves the JSON content to a file in the output directory.
    * 
    * @param jsonContent
    *           JSON String to be saved as a file.
    */
   void saveFile(String jsonContent) {
      String outputFileName = _outputPath + _filePrefix
         + MDfromHTMLUtils.padLeft(_fileCounter++, 4, '0') + ".json";
      JSONObject obj = null;
      try {
         Object test = JSON.parse(jsonContent);
         if (test instanceof JSONObject) {
            obj = (JSONObject)test;
         } else {
            System.out.println("Error: got a non-JSONObject from parse: "+test);
            return;
         }
      } catch (IOException e) {
         System.out.println("Error: Can not transform to JSON: "
            + e.getLocalizedMessage() + "\n" + jsonContent);
         return;
      } catch (ClassCastException cce) {
         System.out.println("Error: Can not parse to JSON: "
                  + cce.getLocalizedMessage() + "\n" + jsonContent);
               return;
      }
      try {
         if (!filterContent(obj)) {
            MDfromHTMLUtils.saveJSONFile(outputFileName, obj);
            // System.out.println("Success: wrote file "+outputFileName);
         } else {
            MDfromHTMLUtils.saveJSONFile(outputFileName + ".rejected", obj);
            // System.out.println("Failure: wrote file "+outputFileName);
         }

      } catch (Exception e) {
         System.out.println("Can not save file " + outputFileName + "  Error: "
            + e.getLocalizedMessage());
      }
   }

   /**
    * Checks the html for filter strings and returns false of none are found. If
    * no html nor captureArray then returns true. If the capture array contains
    * an object that should be filtered, it is removed from the array.
    * 
    * @param jsonObj
    *           JSON object to be checked for filters.
    * @return
    */
   boolean filterContent(JSONObject jsonObj) {
      boolean result = true;
      if (jsonObj == null) {
         return result;
      }
      JSONArray rejectedURLs = new JSONArray();
      JSONArray captureArray = (JSONArray) jsonObj.get("captureArray");
      if (captureArray != null) {
         JSONObject htmlObj = new JSONObject();
         for (Iterator<Object> it = captureArray.iterator(); it.hasNext();) {
            htmlObj = (JSONObject) it.next();
            String url = (String) htmlObj.get("url");
            if (url.endsWith("/")) {
               url = url.substring(0,url.length()-1);
            }
            if (_processedURLs.contains(url)) {
               JSONObject rejected = new JSONObject();
               rejected.put("url",url);
               rejected.put("reason","duplicate url");
               rejectedURLs.add(rejected);
               it.remove();
               continue;
            }
            String html = (String) htmlObj.get("html");
            // expect html to have a body tag otherwise, reject it
            if (html != null && html.toLowerCase().indexOf("<body") == -1) {
               JSONObject rejected = new JSONObject();
               rejected.put("url",url);
               rejected.put("reason","no <body tag in html");
               rejectedURLs.add(rejected);
               it.remove();
               continue;
            }
            String content = (String) htmlObj.get("content");
            boolean filterIt = false;
            if (url != null && html != null) {
               _processedURLs.add(url);
               html = html.toLowerCase();
               if (content == null) {
                  content = "";
               }
               content = content.toLowerCase();
               for (String filter : _filters) {
                  if (html.contains(filter)) {
                     filterIt = true;
                     String reason = "Filter: \""+ filter
                              + "\" found in HTML";
                     System.out.println(reason + " for URL " + url);
                     JSONObject rejected = new JSONObject();
                     rejected.put("url",url);
                     rejected.put("reason",reason);
                     rejectedURLs.add(rejected);
                     break;
                  }
                  if (content.contains(filter)) {
                     filterIt = true;
                     String reason = "Filter: \""+ filter
                              + "\" found in Content";
                     System.out.println(reason + " for URL " + url);
                     JSONObject rejected = new JSONObject();
                     rejected.put("url",url);
                     rejected.put("reason",reason);
                     rejectedURLs.add(rejected);
                     break;
                  }
               }
               if (filterIt) {
                  it.remove();
               }
            } else {
               String reason = "Filter: Content at " + _fileCounter
                        + " does not have a url or html elements.";
               JSONObject rejected = new JSONObject();
               rejected.put("reason",reason);
               rejectedURLs.add(rejected);

               System.out.println(reason);
            }
         }
         result = !(captureArray.size() > 0);
      } else {
         String reason = "Filter: Content at " + _fileCounter
                  + " does not have a captureArray.";
         JSONObject rejected = new JSONObject();
         rejected.put("reason",reason);
         rejectedURLs.add(rejected);

         System.out.println(reason);
      }
      if (rejectedURLs.size() > 0) {
         jsonObj.put("rejected",rejectedURLs);
      }
      return result;
   }

   /**
    * Get the parameters necessary for program execution: input directory, and
    * output directory
    * 
    * @param args
    *           inputPath, outputPath
    * @return true if we have sufficient parameters to execute the program
    */
   boolean getParams(String[] args) {
      String inputPath = "./src/test/resources/htmljson/luisurls";
      String outputPath = "./src/test/resources/htmljson/luisurls";
      String tmp = "";

      try {
         if (args.length >= 1) {
            inputPath = args[0];
         } else {
            _interactive = true;
            _thumbsucker = true;
            tmp = MDfromHTMLUtils.prompt(
               "Enter the fully qualified path to directory containing " + _ext
                  + " html capture files, or q to exit (" + inputPath + "):");
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
         outputPath = args[1];
      } else {
         _interactive = true;
         _thumbsucker = true;
         tmp = MDfromHTMLUtils.prompt(
            "Enter the fully qualified path to the htmljson output directory, or q to exit ("
               + outputPath + "):");
         if (tmp == null || tmp.length() == 0) {
            tmp = outputPath;
         }
         if (tmp.toLowerCase().equals("q")) {
            return false;
         }
         outputPath = tmp;
      }
      if (outputPath.endsWith(File.separator) == false) {
         outputPath += File.separator;
      }
      File testOutput = new File(outputPath);
      if (testOutput.exists() == false) {
         System.out.println(
            "Error: The output directory \"" + outputPath + "\" must exist.");
         return false;
      }
      if (testOutput.isDirectory() == false) {
         System.out.println("Error: The output directory \"" + outputPath
            + "\" must be a directory.");
         return false;
      }
      _outputPath = outputPath;

      if (args.length >= 3) {
         outputPath = args[2];
      } else {
         tmp = MDfromHTMLUtils.prompt("Enter the starting file suffix or q to quit ("
            + _fileCounter + "):");
         if (tmp.length() == 0) {
            tmp = "" + _fileCounter;
         }
         if ("q".equalsIgnoreCase(tmp)) {
            return false;
         }
         try {
            int test = new Integer(tmp);
            if (test < 1) {
               System.out.println("File suffix must be a positive number.");
               return false;
            }
            _fileCounter = test;
         } catch (NumberFormatException nfe) {
            System.out.println(
               "File suffix must be a positive number. Got \"" + tmp + "\"");
            return false;
         }
      }

      if (args.length >= 4) {
         _thumbsucker = new Boolean(args[3]);
      }

      return true;
   }

   String _ext = "text";
   Path _inputPath = null;
   boolean _interactive = false;
   String _outputPath = ".";
   boolean _thumbsucker = false;
   String _filePrefix = "htmljson_";
   int _fileCounter = 1;
   List<String> _filters = new ArrayList<String>();
   Set<String>_processedURLs = new HashSet<String>();

}
