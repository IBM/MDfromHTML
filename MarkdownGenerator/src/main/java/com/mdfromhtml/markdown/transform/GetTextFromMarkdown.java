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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLUtils;
import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;
import com.overzealous.remark.convert.ProvenanceWriter;

/**
 * Utility class to transform multimarkdown generated from HTML into text files.
 * Files are read from an input directory, and written to an output directory.
 * 
 * @author Nathaniel Mills
 */
public class GetTextFromMarkdown {

   /**
    * @param args
    */
   public static void main(String[] args) {
      int exitVal = 0;
      JSONObject HTMLFilters = null;
      try {
         HTMLFilters = MDfromHTMLUtils.loadJSONFile("."+File.separator+"properties"+File.separator+"HTML_Filters.json");
      } catch (Exception e1) {
         System.out.println(
            "Warning: Using no HTML Filters -- can not find "+"."+File.separator+"properties"+File.separator+"HTML_Filters.json\": "
               + e1.getLocalizedMessage());
      }
      GetTextFromMarkdown pgm = new GetTextFromMarkdown(Options.multiMarkdown(),
         HTMLFilters);
      if (pgm.getParams(args)) {
         if (pgm._thumbsucker) {
            System.out.println("\nFiles ending with ." + pgm._ext
               + " will be read from " + pgm._inputPath //
               + "\nand the generated text files (." + pgm._txtext
               + ") will be " + "saved in " + pgm._outputPath
               + "\nIt is "+_includeLinks+" that links will be included in the text output."); //
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
                     exitVal = pgm.doWork(file);
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
   }

   String _ext = "md";
   JSONObject _HTMLFilters = new JSONObject();
   Path _inputPath = null;
   boolean _interactive = false;
   Options _options = Options.multiMarkdown();
   String _outputPath = ".";
   boolean _thumbsucker = false;
   String _txtext = "txt";
   static boolean _includeLinks = false;

   /**
    * Constructor
    */
   public GetTextFromMarkdown() {
   }

   public GetTextFromMarkdown(Options options, JSONObject HTMLFilters) {
      _options = options;
      _HTMLFilters = HTMLFilters;
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
      ProvenanceWriter provenanceWriter = null;
      String html2mdProvenanceFileName = "unknown";
      String provenanceOutputFileName = "unknown";
      JSONObject provenance = null;
      String textOutputFileName = "unknown";
      try {
         String fqFileName = file.toString();
         if (_thumbsucker) {
            System.out.println("Processing: " + fqFileName);
         }
         List<String> markdownList = MDfromHTMLUtils.loadTextFile(fqFileName);
         String shortFileName = fqFileName
            .substring(fqFileName.lastIndexOf(File.separator) + 1);
         int index = shortFileName.lastIndexOf("." + _ext);
         if (index < 1) {
            System.out.println(
               "Error: " + shortFileName + "doesn't end with ." + _ext);
            exitVal = -1;
         } else {
            html2mdProvenanceFileName = _inputPath + File.separator
               + shortFileName.substring(0, index) + "_html2md.json";
            provenanceOutputFileName = _outputPath
               + shortFileName.substring(0, index) + "_md2txt.json";
            provenance = MDfromHTMLUtils.loadJSONFile(html2mdProvenanceFileName);
            _HTMLFilters = (JSONObject) provenance.get("htmlFilters");
            String baseURI = (String) provenance.get("baseURI");
            textOutputFileName = _outputPath + shortFileName.substring(0, index)
               + "." + _txtext;

            String domain = Remark.getDomain(baseURI);
            
            File provenanceOutputFile = new File(provenanceOutputFileName);
            if (provenanceOutputFile.exists()) {
               provenanceOutputFile.delete();
            }
            provenanceWriter = new ProvenanceWriter(fqFileName,
               textOutputFileName, _HTMLFilters, baseURI, domain,
               new FileWriter(provenanceOutputFile, true));
            try {
               StringBuffer sb = new StringBuffer();
               int lineNum = 0;
               Map<String, String> refURLs = findRefURLs(markdownList);
               for (String mdLine : markdownList) {
                  lineNum++;
                  // truncate at provenance so it isn't included 
                  if (mdLine.equals("###### Doc2Dial Provenance ######")) {
                     break;
                  }
                  String testLine = generateTextFromMarkdown(mdLine, refURLs);
                  if (testLine != null) {
                     provenanceWriter.saveMD2Text("" + lineNum, mdLine,
                        testLine);
                     sb.append(testLine);
                     sb.append("\n");
                  }
               }

               MDfromHTMLUtils.saveTextFile(textOutputFileName, sb.toString());
            } catch (Exception e) {
               e.printStackTrace();
               exitVal = -1;
            } finally {
               if (provenanceWriter != null) {
                  try {
                     provenanceWriter.close();
                  } catch (IOException e) {
                     e.printStackTrace();
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

   static public Map<String, String> findRefURLs(List<String> mdLines) {
      Map<String, String> refURLs = new HashMap<String, String>();
      int offset = 0;
      String url = "";
      String ref = "";
      for (String mdLine : mdLines) {
         mdLine = mdLine.trim();
         if (mdLine.startsWith("[")) {
            offset = mdLine.indexOf("]: ");
            if (offset > 1) {
               ref = mdLine.substring(1, offset).trim();
               url = mdLine.substring(offset + 3).trim();
               refURLs.put(ref, url);
            }
         }
      }
      return refURLs;
   }

   static public String generateTextFromMarkdown(String mdLine, Map<String, String> refURLs) {
      String test = mdLine.trim();
      if (test.length() > 0) {
         test = TextUtils.filterComments(test);
         test = processText(test, refURLs);
         if (test != null) {
            test = test.trim();
         }
      }
      return test;
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
      String inputPath = "."+File.separator+"data"+File.separator+"md";
      String outputPath = "."+File.separator+"data"+File.separator+"txt";
      String tmp = "";

      try {
         if (args.length >= 1) {
            inputPath = args[0];
         } else {
            _interactive = true;
            _thumbsucker = true;
            tmp = MDfromHTMLUtils.prompt(
               "Enter the fully qualified path to directory containing " + _ext
                  + " multimarkdown files, or q to exit (" + inputPath + "):");
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
            "Enter the fully qualified path to the text file output directory, or q to exit ("
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
      
      String includeLinks = "n";
      if (args == null || args.length >= 3) {
         includeLinks = args[2].trim().toLowerCase().substring(0,1);
      } else {
         tmp = MDfromHTMLUtils.prompt(
            "Include links in text output (y=yes, n=no), or q to exit ("
               + includeLinks + "):");
         if (tmp == null || tmp.length() == 0) {
            tmp = includeLinks;
         }
         if (tmp.toLowerCase().equals("q")) {
            return false;
         }
         includeLinks = tmp.toLowerCase().substring(0,1);
      }
      _includeLinks = ("y".equals(includeLinks));

      if (args.length >= 4) {
         _thumbsucker = new Boolean(args[3]);
      }

      return true;
   }

   static public String getRef(String line) {
      String retVal = "";
      int startOffset = line.indexOf("[");
      if (startOffset > -1) {
         int endOffset = line.substring(startOffset).indexOf("]");
         if (startOffset > -1 && endOffset > startOffset) {
            retVal = line.substring(startOffset + 1, endOffset + startOffset)
               .trim();
         }
      }
      return retVal;
   }

   static public String processText(String line, Map<String,String> refURLs) {
      StringBuffer sb = new StringBuffer();
      String testChanged = "";
      int lineLen = line.length();
      int offset = 0;
      while (offset < lineLen) {
         if (line == null) {
            break;
         }
         char startChar = line.charAt(offset); // substring(offset, offset + 1);
         switch (startChar) {
            // handle escaped characters first
            case 0x005c: { // backslash
               // skip first backslash and save next char
               offset++;
               if (offset < lineLen) {
                  sb.append(line.charAt(offset));
                  offset++;
               }
               if (offset < lineLen) {
                  line = line.substring(offset);
                  lineLen = line.length();
               } else {
                  // reached end of line
                  lineLen = 0;
               }
               offset = 0;
               break;
            }
            case 0x0021: { // exclamation or image link
               if (offset < lineLen - 1) {
                  if ("[".equals(line.substring(offset + 1, offset + 2))) {
                     // image link
                     line = removeReferencesAndLinks(line.substring(offset),
                        refURLs);
                     if (line == null) {
                     	break;
                     }
                     offset = 0;
                     lineLen = line.length();
                  } else { // just an exclamation point
                     sb.append("!");
                     line = line.substring(offset + 1);
                     offset = 0;
                     lineLen = line.length();
                  }
               } else { // just an exclamation point at end
                  sb.append("!");
                  offset = line.length();
               }
               break;
            }
            case 0x005b: { // left bracket == link
               line = removeReferencesAndLinks(line.substring(offset), refURLs);
               // check for complete line deletion
               if (line == null) {
                  return null;
               }
               offset = 0;
               lineLen = line.length();
               break;
            }
            // Table lines
            case 0x007c: { // pipe == table column separator
               line = removeTableLines(line.substring(offset));
               offset = 0;
               lineLen = line.length();
               break;
            }
            /**
             * Note: these simple cleansers should be below link cases "!" and
             * "[" and tables
             */
            case 0x002d: { // hyphen or task list "- [x]" or "- [ ]"
               testChanged = removeTaskList(line.substring(offset));
               if (testChanged.equals(line.substring(offset))) {
                  // just a hyphen, no change
                  sb.append("-");
                  line = testChanged.substring(1);
               } else {
                  line = testChanged;
               }
               offset = 0; // skip next char (space or remaining hyphen)
               lineLen = line.length();
               break;
            }
            case 0x0060: { // back tick == fenced code blocks or code
               line = removeFencing(line.substring(offset));
               offset = 0;
               lineLen = line.length();
               break;
            }
            case 0x0023: { // hash tag == headings
               line = removeHeading(line.substring(offset));
               offset = 0;
               lineLen = line.length();
               break;
            }
            case 0x005f: { // underscore
               line = removeUnderscore(line.substring(offset));
               offset = 0;
               lineLen = line.length();
               break;
            }
            case 0x007e: { // tilde == strike through or fencing
               // first remove fencing
               line = removeFencing(line.substring(offset));
               line = removeEmphasis(line);
               offset = 0;
               lineLen = line.length();
               break;
            }
            case 0x002a: { // asterisk == bold, italic
               line = removeEmphasis(line.substring(offset));
               offset = 0;
               lineLen = line.length();
               break;
            }
            default: { // just text
               sb.append(startChar);
               offset++;
               break;
            }
         }
      }
      String temp = sb.toString();
      temp = temp.replaceAll("\\\\:", ":");
      temp = temp.replaceAll("\\\\-", "-");
      return temp;
   }

   /**
    * Presented with a line beginning with an underscore. Remove all contiguous
    * underscores that are not escaped which could include patterns like:
    * "__\\_abc\\_def__ more stuff" which should become "\\_abc\\_def more
    * stuff"
    * 
    * @param line
    *           input to be cleansed of non-escaped underscores
    * @return cleansed input
    */
   static public String removeUnderscore(String line) {
      StringBuffer sb = new StringBuffer();
      int i = 0;
      int lLen = line.length();
      boolean isEscaped = false;
      while (i < lLen) {
         // check for backslash (escaped char)
         if (0x005c == line.charAt(i)) {
            isEscaped = true;
         } else if (0x005f == line.charAt(i)) {
            if (isEscaped) {
               isEscaped = false;
               // keep this escaped char
            } else { // eat contiguous unescaped underscores
               i++;
               while (i < lLen) {
                  if (line.charAt(i) == 0x005f) {
                     i++; // skip contiguous underscores
                     continue;
                  }
                  // have a non-underscore
                  break;
               }
               // i now points to valid char unless end of line
            }
         }
         if (i < lLen) {
            sb.append(line.charAt(i));
         }
         i++;
      }
      return sb.toString();
   }

   /**
    * Remove bold, italics, underscore, strikethrough
    * 
    * @param line
    *           input to be cleansed
    * @return cleansed version of input
    */
   static public String removeEmphasis(String line) {
      line = line.replaceAll("\\*", "");
      line = line.replaceAll("~", "");
      return line;
   }

   /**
    * Remove patterns like ```, ~~~, ```json, ~~~java
    * 
    * @param line
    *           input to be cleansed
    * @return cleansed version of input
    */
   static public String removeFencing(String line) {

      StringBuffer sb = new StringBuffer();
      String[] parts = line.split("```\\w+");
      for (int i = 0; i < parts.length; i++) {
         if (parts[i].length() != 0) {
            sb.append(parts[i]);
         }
      }
      line = sb.toString();
      sb = new StringBuffer();
      parts = line.split("~~~\\w+");
      for (int i = 0; i < parts.length; i++) {
         if (parts[i].length() != 0) {
            sb.append(parts[i]);
         }
      }
      line = sb.toString();
      line = line.replaceAll("~~~", "");
      // while fencing is three ticks, single ticks connote inline code
      line = line.replaceAll("`", "");
      return line;
   }

   /**
    * Processes content with surrounding #'s signifying a header and transforms
    * them to text without the #'s
    * 
    * @param line
    *           text to be cleansed of headers
    * @return text without headers
    */
   static public String removeHeading(String line) {
      StringBuffer sb = new StringBuffer();
      String[] parts = line.split("#+");
      for (int i = 0; i < parts.length; i++) {
         if (parts[i].length() != 0) {
            sb.append(parts[i]);
         }
      }
      return sb.toString();
   }

   /**
    * Search for reference links in the markdown line. A reference link contains
    * a pattern with [...] without a following ": "
    * 
    * @param line
    *           markdown line to be examined
    * @param refURLs
    *           map of a reference to its corresponding URL to enable the URL to
    *           be added where a reference is made. The URL being added will be
    *           surrounded with " {" and "} ".
    * @return the revised line stripped of links, or null if nothing from this
    *         line should be saved (e.g., for a reference with pattern [...]:...
    */
   static public String removeReferencesAndLinks(String line, Map<String, String> refURLs) {
      StringBuffer sb = new StringBuffer();
      String test = line.trim();
      char[] testChars = new char[test.length()];
      test.getChars(0, test.length(), testChars, 0);
      int offset = 0;
      boolean foundBracket = false;
      int bracketCnt = 0;
      int startOffset = -1;
      String refLink = "";
      String url = "";
      String ref = "";
      // String reference = "";
      Stack<Integer> startOffsets = new Stack<Integer>();
      boolean isImageRef = false;
      boolean needLabel = false;
      for (char testChar : testChars) {
         switch (testChar) {
            case 0x005b: { // "["
               if (!isImageRef) {
                  // flipflop need for label to skip refLink
                  needLabel = !needLabel;
               } // else in an image link so don't capture anything

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
                           /**
                            * Don't save any reference information (signal line
                            * deletion with null
                            */
                           return null;
                           // reference = test.substring(startOffset, offset +
                           // 1);
                        } else {
                           refLink = test.substring(startOffset + 1, offset).trim();
                           if (needLabel) {
                              // clean footnote starting with carat
                              if (refLink.startsWith("^")) {
                                 refLink = refLink.substring(1);
                              }
                              if (_includeLinks) {
                                 sb.append("{ ");
                                 sb.append(refLink);
                                 sb.append(" }");
                              } else {
                                 sb.append(refLink);
                              }
                              // try to insert the corresponding URL
                              ref = getRef(test.substring(offset)).trim();
                              if (ref.equals("")) {
                                 ref = refLink;
                              }
                              url = refURLs.get(ref);
                              if (url != null) {
                                 if (_includeLinks) {
                                    sb.append(":{ ");
                                    sb.append(url);
                                    sb.append(" } ");
                                 }
                              }
                           }
                           if (isImageRef) {
                              // set up so next [ makes this false
                              needLabel = true;
                           }
                        }
                     } else {
                        refLink = test.substring(startOffset + 1, offset).trim();
                        if (needLabel) {
                           // clean footnote starting with carat
                           if (refLink.startsWith("^")) {
                              refLink = refLink.substring(1);
                           }
                           if (_includeLinks) {
                              sb.append("{");
                              sb.append(refLink);
                              sb.append("}");
                           } else {
                              sb.append(refLink);
                           }
                           // try to insert the corresponding URL
                           ref = getRef(test.substring(offset)).trim();
                           if (ref.equals("")) {
                              ref = refLink;
                           }
                           url = refURLs.get(ref);
                           if (url != null) {
                              if (_includeLinks) {
                                 sb.append(":{");
                                 sb.append(url);
                                 sb.append("} ");
                              }
                           }
                        }
                        if (isImageRef) {
                           // set up so next [ makes this false
                           needLabel = true;
                        }
                     }
                  }
                  if (bracketCnt == 0) {
                     startOffset = -1;
                     foundBracket = false;
                     isImageRef = false;
                  }
               }
               break;
            }
            case 0x0021: { // ! (may be an image
               if (offset < test.length() - 1) {
                  // check the next character
                  if (0x005b == testChars[offset + 1]) {
                     isImageRef = true;
                     // set up so next [ will grab the label
                     needLabel = true;
                  } else {
                     // just an exclamation point
                     sb.append(testChar);
                  }
               } else {
                  // last char so just an exclamation point
                  sb.append(testChar);
               }
               break;
            }
            default: {
               // capture all characters not inside a link
               if (startOffsets.empty()) {
                  sb.append(testChar);
               }
               break;
            }
         }
         offset++;
      }
      return sb.toString();
   }

   static public String removeTableLines(String line) {
      line = line.replaceAll("\\|", "");
      line = line.replaceAll(":-{3,}", "");
      line = line.replaceAll("-{3,}:", "");
      line = line.replaceAll(":-*:", "");
      line = line.replaceAll("-{3,}", "");
      return line;
   }

   /**
    * Remove task list items with the pattern "- [x]" or "- [ ]" or "- [ x ]" or
    * "- [ ]"
    * 
    * @param line
    *           input to be cleansed
    * @return cleansed input
    */
   static public String removeTaskList(String line) {
      line = line.replaceAll("- [ ]", "( )");
      line = line.replaceAll("- [x]", "(x)");
      line = line.replaceAll("- [ x ]", "(x)");
      return line;
   }

}
