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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;

import com.api.json.JSON;
import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdfromhtml.core.MDfromHTMLUtils;
import com.mdfromhtml.remark.utils.CleanupMarkdown;
import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;
import com.overzealous.remark.convert.DocumentConverter;
import com.overzealous.remark.convert.ProvenanceWriter;

/**
 * Given an input directory containing json files with an array of objects, each
 * object containing an html entry and a url from which that html was captured,
 * parse the html and generate markdown (.md) files for each html / url pair. If
 * there is no url specified or it is empty or null then no relative url
 * mappings will be provided in the resulting markdown file. Markdown files
 * retain the name of the json file and are appended with the index within the
 * JSONArray for the object from which the html was used to generate the
 * markdown.
 * 
 * @author Nathaniel Mills
 */
public class GetMarkdownFromHTML {

   public static JSONObject getMarkdownFromHTML(JSONObject htmlObject) {
      JSONObject result = new JSONObject();
      ObjectMapper mapper = new ObjectMapper();
      JsonNode jsonnode;
      try {
         jsonnode = mapper.readTree(htmlObject.toString());
      } catch (IOException e) {
         result.put("errorMsg", String.format(
            "Error: Can not parse the object: %s" + e.getLocalizedMessage()));
         return result;
      }

      jsonnode = getMarkdownFromHTML((ObjectNode) jsonnode);
      try {
         result = (JSONObject) JSON.parse(jsonnode.toString());
      } catch (IOException e) {
         result.put("errorMsg",
            String.format("Error: Can not parse the response object: %s"
               + e.getLocalizedMessage()));
         return result;
      }
      return result;
   }

   public static ObjectNode getMarkdownFromHTML(ObjectNode htmlObject) {
      ObjectNode result = JsonNodeFactory.instance.objectNode();
      boolean returnProvenance = true;
      JsonNode testProv = htmlObject.get("returnProvenance");
      if (testProv != null && testProv.isBoolean()) {
         returnProvenance = testProv.asBoolean();
      }

      // initialize environment for this call
      ObjectNode HTMLFiltersObj = (ObjectNode) htmlObject.get("HTMLFilters");
      if (HTMLFiltersObj == null) {
         try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode filters = mapper.readTree(new File("HTML_Filters.json"));
            HTMLFiltersObj = (ObjectNode) filters;
         } catch (Exception e1) {
            result.put("errorMsg",
               "Error: Can not find \"HTML_Filters\" in the request, nor can a file named \"HTML_Filters.json\" be found: "
                  + e1.getLocalizedMessage());
            return result;
         }
      }
      JSONObject HTMLFilters;
      try {
         // fold everything to lowercase to match later during filtering
         String htmlFilters = HTMLFiltersObj.toString().toLowerCase();
         HTMLFilters = (JSONObject) JSON.parse(htmlFilters);
      } catch (IOException e) {
         result.put("errorMsg",
            "Error: Can not find parse the content of the \"HTML_Filters.json\" file: "
               + e.getLocalizedMessage());
         return result;
      }
      Options options = Options.multiMarkdown();
      options.hardwraps = true;
      GetMarkdownFromHTML pgm = new GetMarkdownFromHTML(options, HTMLFilters);
      JsonNode temp = htmlObject.get("html");
      if (temp == null) {
         // try to get this from the captureArray
         ArrayNode captureArray = (ArrayNode) htmlObject.get("captureArray");
         if (captureArray != null && captureArray.size() > 0) {
            ObjectNode obj = (ObjectNode) captureArray.get(0);
            temp = obj.get("html");
            if (temp == null) {
               result.put("errorMsg", "The request captureArray"
                  + " is missing the required \"html\" key so there is nothing to process.");
               return result;
            }
         } else {
            result.put("errorMsg", "The request"
               + " is missing the required \"html\" or \"captureArray\" key so there is nothing to process.");
            return result;
         }
      }
      String html = temp.asText();
      temp = htmlObject.get("url");
      if (temp == null) {
         result.put("errorMsg",
            "The request" + " is missing the required \"url\" key.");
         return result;
      }

      String baseURI = temp.asText();

      // testing for hidden tags
      // html = html.replaceAll("&lt;", "<");
      // html = html.replaceAll("&gt;", ">");
      Document doc = Jsoup.parse(html, baseURI);
      doc.outputSettings().escapeMode(EscapeMode.extended);

      // determine if we should skip markdown until first header is encountered
      boolean seekHeaders = true; // default is true so only special sites need
                                  // override this
      String domain = Remark.getDomain(baseURI);
      int testindex = baseURI.indexOf(domain);
      // need to find actual domain for proper filters
      String workingURI = baseURI.substring(testindex + domain.length());
      testindex = workingURI.toLowerCase().indexOf("http");
      if (testindex >= 0) {
         workingURI = workingURI.substring(testindex);
         domain = Remark.getDomain(workingURI);
         baseURI = workingURI;
      }
      JSONObject domainFilters = (JSONObject) HTMLFilters.get(domain);
      if (domainFilters != null) {
         Boolean test = (Boolean) domainFilters
            .get(DocumentConverter.SEEK_HEADERS);
         if (test != null) {
            seekHeaders = test;
         }
      }

      // create a provenance writer using a string writer so we can return the
      // provenance in the response
      ProvenanceWriter provWriter = null;
      StringWriter sw = null;
      BufferedWriter bw = null;
      if (returnProvenance != false) {
         sw = new StringWriter();
         bw = new BufferedWriter(sw);
         try {
            provWriter = new ProvenanceWriter("", "", HTMLFilters, baseURI,
               domain, bw);
         } catch (IOException e) {
            e.printStackTrace();
            provWriter = null;
         }
      }
      String markdown = pgm.generateMarkdownFromHTML(doc, provWriter, baseURI,
         seekHeaders);

      result.set("HTMLFilters", HTMLFiltersObj);
      if (returnProvenance != false && provWriter != null) {
         // note: close finished the JSON object in sw
         try {
            provWriter.close();
         } catch (IOException e1) {
            e1.printStackTrace();
         }
         String provenance = sw.getBuffer().toString();
         ObjectMapper mapper = new ObjectMapper();
         JsonNode provObj = null;
         try {
            provObj = mapper.readTree(provenance);
         } catch (IOException e) {
            e.printStackTrace();
         }

         result.set("provenance", provObj);
         // override the HTMLFilters with the one reported by provenance.
         ObjectNode testHTMLFilters = (ObjectNode) provObj.get("HTMLFilters");
         if (testHTMLFilters != null) {
            result.set("HTMLFilters", testHTMLFilters);
         }
      }

      // add in the html used for generation in a captureArray
      ArrayNode captureArray = JsonNodeFactory.instance.arrayNode();
      ObjectNode htmlInfo = JsonNodeFactory.instance.objectNode();
      htmlInfo.put("content", doc.text());
      htmlInfo.put("html", html);
      htmlInfo.put("url", baseURI);
      captureArray.add(htmlInfo);
      result.set("captureArray", captureArray);
      result.set("markdown", JsonNodeFactory.instance.textNode(markdown));
      result.put("returnProvenance", returnProvenance);
      result.put("url", baseURI);
      return result;
   }

   /**
    * Main entry point to read a specified input directory to find json files
    * containing an array of objects with the html and url from the HTML capture
    * utility () and transform the markdown and structured html files saved in
    * *.md and *_formatted.html files in the specified output directory.
    * 
    * @param args
    *           inputPath, outputPath, showAnnotationsFlag (if not supplied, the
    *           program prompts for their values)
    */
   public static void main(String[] args) {
      int exitVal = 0;
      JSONObject HTMLFilters = null;
      try {
         HTMLFilters = MDfromHTMLUtils.loadJSONFile("." + File.separator
            + "properties" + File.separator + "HTML_Filters.json");
         // fold to lowercase
         try {
            HTMLFilters = (JSONObject) JSON
               .parse(HTMLFilters.toString().toLowerCase());
         } catch (Exception e) {
            System.out.println("Error: \"." + File.separator + "properties"
               + File.separator + "HTML_Filters.json\" has a parsing error: "
               + e.getLocalizedMessage());
            return;
         }
      } catch (Exception e1) {
         System.out.println("Error: No HTML Filters -- can not find \"."
            + File.separator + "properties" + File.separator
            + "HTML_Filters.json\": " + e1.getLocalizedMessage());
         return;
      }
      Options options = Options.multiMarkdown();
      options.hardwraps = true;
      GetMarkdownFromHTML pgm = new GetMarkdownFromHTML(options, HTMLFilters);
      if (pgm.getParams(args)) {
         if (pgm._thumbsucker) {
            System.out.println("\nFiles ending with ." + pgm._ext
               + " will be read from " + pgm._inputPath //
               + "\nand the generated markdown (.md), and html (.html and _foramtted.html) "
               + "saved in " + pgm._outputPath); //
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
                  try {
                     exitVal = pgm.doWork(file, HTMLFilters);
                     if (exitVal != 0) {
                        break;
                     }
                  } catch (Exception e) {
                     e.printStackTrace();
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

   String _ext = "json";
   Path _inputPath = null;
   boolean _interactive = false;
   String _outputPath = ".";
   Remark _remark = new Remark(Options.multiMarkdown());
   boolean _thumbsucker = false;
   boolean _keepProvenanceLinks = true;

   // public GetMarkdownFromHTML() {
   // this(Options.multiMarkdown());
   // }
   //
   // public GetMarkdownFromHTML(Options options) {
   // this(options, (JSONObject) null);
   // }

   public GetMarkdownFromHTML(Options options, JSONObject HTMLFilters) {
      _remark = new Remark(options, HTMLFilters);
   }

   /**
    * Process the specified file to transform its content into formatted text
    * and save it to a txt file in the specified output directory.
    * 
    * @param file
    *           the file containing the annotation json from ICCC
    * @param HTMLFilters
    *           object containing global and domain specific filter rules to
    *           control markdown generation
    * @return exit value (0 indicates success, otherwise -1 for failure)
    */
   int doWork(Path file, JSONObject HTMLFilters) {
      int exitVal = 0;
      ProvenanceWriter provenanceWriter = null;
      String provenanceOutputFileName = "unknown";
      try {
         String fqFileName = file.toString();
         if (_thumbsucker) {
            System.out.println("Processing: " + fqFileName);
         }
         ObjectMapper mapper = new ObjectMapper();
         File jsonTestFile = new File(file.toString());
         JsonNode tempJSON = mapper.readTree(jsonTestFile);
         String shortFileName = fqFileName
            .substring(fqFileName.lastIndexOf(File.separator) + 1);
         int index = shortFileName.lastIndexOf("." + _ext);
         if (index < 1) {
            System.out.println(shortFileName + "doesn't end with ." + _ext);
            exitVal = -1;
         } else {
            ObjectNode inputJson = (ObjectNode) tempJSON;
            int htmlCounter = 0;
            ArrayNode htmlList = (ArrayNode) inputJson.get("captureArray");
            if (htmlList == null) {
               JsonNode htmlObj = (JsonNode) inputJson.get("captureDict");
               if (htmlObj == null) {
                  System.err.println(fqFileName
                     + " is missing the \"captureArray\" and the \"captureDict\" tag. Please fix and retry.");
                  System.exit(-1);
               }
               htmlList = JsonNodeFactory.instance.arrayNode();
               htmlList.add(htmlObj);
            }
            String baseURI = null;
            String html = null;
            JsonNode temp = null;
            for (Object obj : htmlList) {
               boolean seekHeaders = true; // default is true so only special
                                           // sites need override this
               JSONObject globalFilters = (JSONObject) HTMLFilters.get("*");
               if (globalFilters != null) {
                  Boolean test = (Boolean) globalFilters
                     .get(DocumentConverter.SEEK_HEADERS);
                  if (test != null) {
                     seekHeaders = test;
                  }
               }

               try {
                  htmlCounter++;
                  ObjectNode htmlObj = (ObjectNode) obj;
                  temp = htmlObj.get("html");
                  if (temp == null) {
                     System.err.println(fqFileName
                        + " is missing the \"html\" key in the ["
                        + (htmlCounter - 1) + "] element of the captureArray.");
                     System.exit(-1);
                  }
                  html = temp.asText();
                  temp = htmlObj.get("url");
                  if (temp == null) {
                     System.err.println(fqFileName
                        + " is missing the \"utl\" key in the ["
                        + (htmlCounter - 1) + "] element of the captureArray.");
                     System.exit(-1);
                  }
                  baseURI = temp.asText();
                  String htmlOutputFileName = _outputPath
                     + shortFileName.substring(0, index) + "_"
                     + MDfromHTMLUtils.padLeftZero(htmlCounter, 3) + ".html";
                  MDfromHTMLUtils.saveTextFile(htmlOutputFileName, html);
                  // testing for hidden tags
                  // html = html.replaceAll("&lt;", "<");
                  // html = html.replaceAll("&gt;", ">");
                  Document doc = Jsoup.parse(html, baseURI);
                  doc.outputSettings().escapeMode(EscapeMode.extended);

                  // TODO: process iframe elements in a loop making below a
                  // routine passing an Element
                  // Elements elements = document.select("iframe");
                  // Document iframeDoc = Jsoup.parse(elements.get(0).data());
                  // String iframeSrc = iframeDoc.attr("src");
                  /**
                   * <iframe scrolling="no" allowtransparency="true" border="0"
                   * frameborder="0" style=
                   * "z-index:99999!important;display:block!important;background-color:transparent!important;border:none!important;overflow:hidden!important;visibility:visible!important;margin:0!important;padding:0!important;-webkit-tap-highlight-color:transparent!important;width:100%!important;height:932px!important;min-height:932px!important;"
                   * src=
                   * "https://upland.zendesk.com/auth/v2/login/signin?return_to=https%3A%2F%2Fcommunity.uplandsoftware.com%2Fhc%2Fen-us&amp;theme=hc&amp;locale=en-us&amp;brand_id=256119&amp;auth_origin=256119%2Ctrue%2Ctrue">
                   * </iframe>
                   */

                  String formattedHTML = doc.toString();
                  formattedHTML = formattedHTML.replaceAll("&amp;", "&");
                  // formattedHTML = formattedHTML.replaceAll("&lt;", "<");
                  // formattedHTML = formattedHTML.replaceAll("&gt;", ">");
                  formattedHTML = formattedHTML.replaceAll("&quot;", "\"");

                  String formattedHTMLOutputFileName = _outputPath
                     + shortFileName.substring(0, index) + "_"
                     + MDfromHTMLUtils.padLeftZero(htmlCounter, 3)
                     + "_formatted.html";
                  MDfromHTMLUtils.saveTextFile(formattedHTMLOutputFileName,
                     formattedHTML);

                  String domain = Remark.getDomain(baseURI);
                  int testindex = baseURI.indexOf(domain);
                  // need to find actual domain for proper filters
                  String workingURI = baseURI
                     .substring(testindex + domain.length());
                  testindex = workingURI.toLowerCase().indexOf("http");
                  if (testindex >= 0) {
                     workingURI = workingURI.substring(testindex);
                     domain = Remark.getDomain(workingURI);
                     baseURI = workingURI;
                  }

                  String markdownOutputFileName = _outputPath
                     + shortFileName.substring(0, index) + "_"
                     + MDfromHTMLUtils.padLeftZero(htmlCounter, 3) + ".md";

                  provenanceOutputFileName = _outputPath
                     + shortFileName.substring(0, index) + "_"
                     + MDfromHTMLUtils.padLeftZero(htmlCounter, 3)
                     + "_html2md.json";

                  File provenanceOutputFile = new File(
                     provenanceOutputFileName);
                  if (provenanceOutputFile.exists()) {
                     provenanceOutputFile.delete();
                  }
                  provenanceWriter = new ProvenanceWriter(
                     formattedHTMLOutputFileName, markdownOutputFileName,
                     _remark.getHTMLFilters(), baseURI, domain,
                     new FileWriter(provenanceOutputFile, true));

                  // determine if we should skip markdown until first header is
                  // encountered
                  JSONObject domainFilters = (JSONObject) HTMLFilters
                     .get(domain);
                  if (domainFilters != null) {
                     Boolean test = (Boolean) domainFilters
                        .get(DocumentConverter.SEEK_HEADERS);
                     if (test != null) {
                        seekHeaders = test;
                     }
                  }

                  String markdown = generateMarkdownFromHTML(doc,
                     provenanceWriter, baseURI, seekHeaders);

                  if (_keepProvenanceLinks) {
                     markdown += "\n###### Doc2Dial Provenance ######\n\n"
                        + " * [Doc2Dial Original URL][]\n"
                        + " * [Doc2Dial File Processed][]\n\n[Doc2Dial Original URL]: "
                        + baseURI.replaceAll(" ", "%20")
                        // + .replaceAll("#", "%23").replaceAll("&", "%26")
                        + "\n[Doc2Dial File Processed]: file://"
                        + file.toAbsolutePath().toString();
                  }

                  MDfromHTMLUtils.saveTextFile(markdownOutputFileName,
                     markdown);
               } catch (Exception e) {
                  e.printStackTrace();
                  exitVal = -1;
               } finally {
                  if (provenanceWriter != null) {
                     try {
                        provenanceWriter.close();
                        // Note: leave all provenance with seekHeaders explicit
                        // in the HTMLFilters
                        // if (seekHeaders) {
                        // cleanUpAnnotations(provenanceOutputFileName);
                        // }
                     } catch (IOException e) {
                        e.printStackTrace();
                     }
                     provenanceWriter = null;
                  }
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
         exitVal = -1;
      }
      return exitVal;
   }

   String generateMarkdownFromHTML(Document doc,
      ProvenanceWriter provenanceWriter, String baseUri, boolean seekHeaders) {
      String markdown = _remark.convert(doc, provenanceWriter, baseUri);
      markdown = CleanupMarkdown.cleanAll(markdown, seekHeaders);
      return removeUnusedReferences(markdown);
   }

   void cleanUpAnnotations(String provenanceFileName) throws Exception {
      JSONObject provenanceObj = MDfromHTMLUtils
         .loadJSONFile(provenanceFileName);
      JSONArray provenanceArray = (JSONArray) provenanceObj.get("provenance");
      for (Iterator<Object> it = provenanceArray.iterator(); it.hasNext();) {
         JSONObject annotation = (JSONObject) it.next();
         String md = (String) annotation.get("md");
         if (md != null && md.startsWith("#") == false) {
            it.remove();
         } else {
            // found first header so break out
            break;
         }
      }
      provenanceObj.put("provenance", provenanceArray);
      MDfromHTMLUtils.saveJSONFile(provenanceFileName, provenanceObj);
   }

   /**
    * Get the parameters necessary for program execution: input directory,
    * output directory, and whether to append annotation details to sentences
    * 
    * @param args
    *           inputPath, outputPath, showAnnotationsFlag
    * @return true if we have sufficient parameters to execute the program
    */
   boolean getParams(String[] args) {
      String inputPath = "." + File.separator + "data" + File.separator
         + "htmljson";
      String outputPath = "." + File.separator + "data" + File.separator + "md";
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
            "Enter the fully qualified path to the markdown output directory, or q to exit ("
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
         _thumbsucker = new Boolean(args[2]);
      }

      if (args.length >= 4) {
         _keepProvenanceLinks = new Boolean(args[3]);
      }

      return true;
   }

   /**
    * Search for references in the markdown line. A reference contains a pattern
    * with [...]: ...
    * 
    * @param line
    *           markdown line to be examined
    * @return set of references found in the line.
    */
   Set<String> getReferences(String line) {
      Set<String> results = new HashSet<String>();
      String test = line.trim();
      int index = test.indexOf("[");
      int index2 = -1;
      String reference = "";
      while (index != -1) {
         // Check that we have found a reference containing [...]: ...
         index2 = test.indexOf("]: ");
         if (index2 == -1) {
            break;
         }
         if ((index2 - index) > 2) {
            reference = test.substring(index, index2 + 1);
            results.add(reference);
         }
         test = test.substring(index2 + 3);
         index = test.indexOf("[");
      }
      return results;
   }

   /**
    * Search for reference links in the markdown line. A referrrence link
    * contains a pattern with [...] without a following ": "
    * 
    * @param line
    *           markdown line to be examined
    * @param refLinks
    *           set of reference links in the markdown
    * @param references
    *           set of references in the markdown
    */
   static public void getReferencesAndLinks(String line, Set<String> refLinks,
      Set<String> references) {
      String test = line.trim();
      char[] testChars = new char[test.length()];
      test.getChars(0, test.length(), testChars, 0);
      int offset = 0;
      boolean foundBracket = false;
      int bracketCnt = 0;
      int startOffset = -1;
      String refLink = "";
      String reference = "";
      Stack<Integer> startOffsets = new Stack<Integer>();
      for (char testChar : testChars) {
         switch (testChar) {
            case 0x005b: { // "["
               if (!foundBracket) {
                  foundBracket = true;
               }
               startOffsets.push(offset);
               bracketCnt++;
               break;
            }
            case 0x005d: { // "]"
               if (foundBracket) {
                  bracketCnt--;
                  startOffset = startOffsets.pop();
                  if ((offset - startOffset) > 1) {
                     if ((offset + 1) < test.length()) {
                        if (0x003a == testChars[offset + 1]) { // ":"
                           reference = test.substring(startOffset, offset + 1);
                           references.add(reference);
                        } else {
                           refLink = test.substring(startOffset, offset + 1);
                           refLinks.add(refLink);
                        }
                     } else {
                        refLink = test.substring(startOffset, offset + 1);
                        refLinks.add(refLink);
                     }
                  }
                  if (bracketCnt == 0) {
                     startOffset = -1;
                     foundBracket = false;
                  }
               }
               break;
            }
            default: {
               break;
            }
         }
         offset++;
      }
      return;
   }

   static public String removeUnusedReferences(String markdown) {
      /**
       * Read through the markdown twice, once to find referenced links and
       * references and then to remove unreferenced references
       */
      StringBuffer sb = new StringBuffer();
      BufferedReader br = new BufferedReader(new StringReader(markdown));
      String line = "";
      String test = "";
      Set<String> refLinks = new HashSet<String>();
      Set<String> references = new HashSet<String>();
      int index = -1;
      String refStr = "";
      try {
         while ((line = br.readLine()) != null) {
            // TODO: combine getRefLinks and getReferences into one method
            getReferencesAndLinks(line, refLinks, references);
         }
         // remove referenced links from references leaving only unreferenced
         // references
         for (String link : refLinks) {
            references.remove(link);
         }
         // if all are accounted for, we are done
         if (references.size() == 0) {
            // return original markdown as-is
            sb = new StringBuffer(markdown);
         } else { // run through markdown to remove unreferenced references
            br = new BufferedReader(new StringReader(markdown));
            while ((line = br.readLine()) != null) {
               test = line.trim();
               if (test.startsWith("[")) {
                  index = test.indexOf("]: ");
                  if (index != -1) {
                     refStr = test.substring(0, index + 1);
                     if (references.contains(refStr)) {
                        references.remove(refStr);
                        // filter this line from markdown as it contains an
                        // unreferenced reference
                        continue;
                     }
                  } else {
                     if (test.endsWith("]:")) {
                        // this is an invalid reference "[blah]: "
                        continue;
                     }
                  }
               }
               sb.append(line);
               sb.append("\n");
            }
         }
      } catch (IOException ioe) {

      }
      return sb.toString();
   }

}
