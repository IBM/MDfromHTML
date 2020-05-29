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

package com.overzealous.remark.convert;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;
import com.api.json.JSONObject;

/**
 * Class used to capture annotations during markdown generation linking the
 * markdown "line" to the html document element(s)
 * 
 * @author Nathaniel Mills
 */
public class ProvenanceWriter extends BufferedWriter {

   String _HTMLFileName = "unknown";
   String _MarkdownFileName = "unknown";
   boolean needsComma = false;

   /**
    * 
    * @param inputFileName
    * @param outputFileName
    * @param HTMLFilters
    * @param baseURI
    * @param domain
    * @param out
    * @throws IOException
    */
   public ProvenanceWriter(String inputFileName, String outputFileName,
      JSONObject HTMLFilters, String baseURI, String domain, Writer out) throws IOException {
      this(inputFileName, outputFileName, HTMLFilters, baseURI, domain, out, 4096);
   }

   /**
    * 
    * @param inputFileName
    * @param outputFileName
    * @param HTMLFilters
    * @param baseURI
    * @param domain
    * @param out
    * @param sz
    * @throws IOException
    */
   public ProvenanceWriter(String inputFileName, String outputFileName,
      JSONObject HTMLFilters, String baseURI, String domain, Writer out, int sz)
      throws IOException {
      super(out, sz);
      if (baseURI == null) {
         baseURI = "";
      }
      if (domain == null) {
         domain = "";
      }
      Boolean seekHeaders = true;
      _HTMLFileName = inputFileName;
      _MarkdownFileName = outputFileName;
      JSONObject fileData = new JSONObject();
      fileData.put("inputFilename", inputFileName);
      fileData.put("outputFilename", outputFileName);
      if (baseURI != null) {
         fileData.put("baseURI", baseURI);
      }
      JSONObject specificHtmlFilters = new JSONObject();
      if (HTMLFilters != null && HTMLFilters.size() > 0) {
         specificHtmlFilters.put(DocumentConverter.DEFAULT_DOMAIN,(JSONObject)HTMLFilters.get(DocumentConverter.DEFAULT_DOMAIN));
         JSONObject domainFilters = (JSONObject)HTMLFilters.get(domain);
         if (domainFilters == null) {
            domainFilters = new JSONObject();
            domainFilters.put(DocumentConverter.SEEK_HEADERS,seekHeaders);
         }
         seekHeaders = (Boolean)domainFilters.get(DocumentConverter.SEEK_HEADERS); 
         if (seekHeaders == null) {
            seekHeaders = Boolean.TRUE;
            domainFilters.put(DocumentConverter.SEEK_HEADERS,seekHeaders);
         }
         specificHtmlFilters.put(domain, domainFilters);
         fileData.put("HTMLFilters", specificHtmlFilters);
      }
      String serialStart = fileData.serialize(true);
      // remove }\n
      serialStart = serialStart.substring(0, serialStart.length() - 2);
      write(serialStart);
      if (Boolean.TRUE.equals(seekHeaders)) {
         write(",\n   \"provenanceReminder\": \"seekHeaders is TRUE so markdown is filtered until the first header is encountered.\"");
      } else {
         write(",\n   \"provenanceReminder\": \"seekHeaders is FALSE so all generated markdown is available.\"");
      }
      write(",\n   \"provenance\": [\n");
      flush();
   }

   /**
    * Must be called to finish a valid JSONObject
    * 
    * @throws IOException
    */
   @Override
   public void close() throws IOException {
      write("\n   ]\n}\n");
      flush();
      super.close();
   }

   /**
    * Creates a provenance annotation with the provenance level for the html
    * used to create the markdown snippet
    * 
    * @param level
    *           dom level using dotted construct with .# for peer children, ~#
    *           for inlined content, and ^# for text
    * @param node
    *           the node triggering markdown generation
    * @param markdown
    *           the markdown snippet created
    * @throws IOException
    *            if the annotation can not be saved
    */
   public void saveHTML2MD(String level, Node node, String markdown)
      throws IOException {
      // do nothing if markdown is empty
      if (markdown == null || markdown.trim().length() == 0) {
         return;
      }
      /**
       * Node can be a Comment, DataNode, DocumentType, Element, TextNode,
       * XmlDeclaration
       */
      String nodeHTML = "";
      if (node instanceof Element) {
         nodeHTML = ((Element) node).shallowClone().toString();
      } else if (node instanceof TextNode) {
         // saving the parent inclosing the text
         nodeHTML = ((TextNode) node).parentNode().toString();
      } else if (node instanceof DocumentType) {
         nodeHTML = ((DocumentType) node).shallowClone().toString();
      } else if (node instanceof Comment) {
         nodeHTML = ((Comment) node).shallowClone().toString();
      } else if (node instanceof DataNode) {
         nodeHTML = ((DataNode) node).shallowClone().toString();
      } else if (node instanceof XmlDeclaration) {
         nodeHTML = ((XmlDeclaration) node).shallowClone().toString();
      }
      JSONObject provenance = new JSONObject();
      provenance.put("level", level);
      provenance.put("html", nodeHTML);
      provenance.put("md", markdown);
      if (needsComma) {
         write(",\n");
      } else {
         write("\n");
         needsComma = true;
      }
      write(provenance.serialize(true));
      flush();
   }

   /**
    * Creates a provenance annotation with the provenance level for the html
    * filtered rather than being used to create a markdown snippet
    * 
    * @param level
    *           dom level using dotted construct with .# for peer children, ~#
    *           for inlined content, and ^# for text
    * @param node
    *           the node triggering markdown generation
    * @param filterReason
    *           the reason the HTML was filtered
    * @throws IOException
    *            if the annotation can not be saved
    */
   public void saveFilteredHTML(String level, Node node, String filterReason)
      throws IOException {
      // do nothing if no reason is provided
      if (filterReason == null || filterReason.trim().length() == 0) {
         return;
      }
      /**
       * Node can be a Comment, DataNode, DocumentType, Element, TextNode,
       * XmlDeclaration
       */
      String nodeHTML = "";
      if (node instanceof Element) {
         nodeHTML = ((Element) node).shallowClone().toString();
      } else if (node instanceof TextNode) {
         // saving the parent inclosing the text
         nodeHTML = ((TextNode) node).parentNode().toString();
      } else if (node instanceof DocumentType) {
         nodeHTML = ((DocumentType) node).shallowClone().toString();
      } else if (node instanceof Comment) {
         nodeHTML = ((Comment) node).shallowClone().toString();
      } else if (node instanceof DataNode) {
         nodeHTML = ((DataNode) node).shallowClone().toString();
      } else if (node instanceof XmlDeclaration) {
         nodeHTML = ((XmlDeclaration) node).shallowClone().toString();
      }
      JSONObject provenance = new JSONObject();
      provenance.put("level", level);
      provenance.put("html", nodeHTML);
      provenance.put("md",""); // not sure this is necessary
      provenance.put("filterReason", filterReason);
      if (needsComma) {
         write(",\n");
      } else {
         write("\n");
         needsComma = true;
      }
      write(provenance.serialize(true));
      flush();
   }

   /**
    * Creates a provenance annotation with the provenance level for the markdown
    * used to create the text snippet
    * 
    * @param level
    *           markdown line number
    * @param markdown
    *           the markdown triggering text generation
    * @param text
    *           the text snippet created
    * @throws IOException
    *            if the annotation can not be saved
    */
   public void saveMD2Text(String level, String markdown, String text)
      throws IOException {
      // do nothing if markdown is empty
      if (text == null || text.trim().length() == 0) {
         return;
      }
      JSONObject provenance = new JSONObject();
      provenance.put("level", level);
      provenance.put("md", markdown);
      provenance.put("text", text);
      if (needsComma) {
         write(",\n");
      } else {
         write("\n");
         needsComma = true;
      }
      write(provenance.serialize(true));
      flush();
   }

   /**
    * Creates a provenance annotation with the provenance level for the text
    * used to create the conditional
    * 
    * @param level
    *           text line number "." sentence line number
    * @param text
    *           the text containing the conditional
    * @param conditional
    *           the conditional text created
    * @throws IOException
    *            if the annotation can not be saved
    */
   public void saveText2Cond(String level, String text, String conditional)
      throws IOException {
      // do nothing if conditional is empty
      if (conditional == null || conditional.trim().length() == 0) {
         return;
      }
      JSONObject provenance = new JSONObject();
      provenance.put("level", level);
      provenance.put("text", text);
      provenance.put("conditional", conditional);
      if (needsComma) {
         write(",\n");
      } else {
         write("\n");
         needsComma = true;
      }
      write(provenance.serialize(true));
      flush();
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
   }

}
