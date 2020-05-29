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

/*
 * Copyright 2011 OverZealous Creations, LLC
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
 */

package com.overzealous.remark;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import com.api.json.JSON;
import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLUtils;
import com.overzealous.remark.convert.DocumentConverter;
import com.overzealous.remark.convert.ProvenanceWriter;

/**
 * The class that manages converting HTML to Markdown.
 *
 * <p>
 * It is recommended that you save this class if it is going to be reused for
 * better performance. This class is thread-safe, but can only process a single
 * document concurrently.
 * </p>
 *
 * <p>
 * <strong>Usage:</strong>
 * </p>
 * 
 * <p>
 * Basic usage involves instantiating this class with a specific set of
 * _options, and calling one of the {@code convert*} methods on some form of
 * input.
 * </p>
 * 
 * <p>
 * Examples:
 * </p>
 * 
 * <pre>
 * // Create a generic remark that converts to pure-Markdown spec.
 * Remark remark = new Remark();
 * String cleanedUp = remark.convertFragment(inputString);
 * 
 * // Create a remark that converts to pegdown with all extensions enabled.
 * Remark pegdownAll = new Remark(Options.pegdownAllExtensions());
 * cleanedUp = pegdownAll.convert(new URL("http://www.example.com"), 15000);
 * 
 * // stream the conversion
 * pegdownAll.withStream(System.out)
 *    .convert(new URL("http://www.overzealous.com"), 15000);
 * </pre>
 * 
 *
 * @author Phil DeJarnett
 * @author Nathaniel Mills modifications for provenance and level tracking
 */
public class Remark {

   /**
    * "HTML_Filters.json" is the default name for filters.
    */
   static public String HTML_FILTER_FILENAME = "HTML_Filters.json";
   private Cleaner _cleaner;
   private final Options _options;
   private final DocumentConverter _converter;
   private final ReentrantLock _converterLock = new ReentrantLock();
   private boolean _cleanedHtmlEchoed = false;
   private JSONObject _HTMLFilters = new JSONObject();
   private Whitelist _whitelist = Whitelist.basicWithImages();

   /**
    * Creates a default, pure Markdown-compatible Remark instance.
    * 
    * @throws Exception
    */
   public Remark() {
      this(Options.markdown());
   }

   public Remark(Options options) {
      this(options, (JSONObject) null);
   }

   public Remark(JSONObject HTMLFilters) {
      this(Options.markdown(), HTMLFilters);
   }

   /**
    * Creates a Remark instance with the specified _options.
    *
    * @param options
    *           Specified _options to use on this instance. See the docs for the
    *           Options class for common _options sets.
    * @param HTMLFilters
    *           the JSON object containing the HTML
    *           filter directives. If null, the default
    *           {@link #HTML_FILTER_FILENAME}
    */
   public Remark(Options options, JSONObject HTMLFilters) {
      this._options = options.getCopy();
      try {
         if (HTMLFilters != null) {
            _HTMLFilters = (JSONObject) JSON.parse(HTMLFilters.toString());
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      HTMLFilters = getHTMLFilters(HTMLFilters,
         DocumentConverter.DEFAULT_DOMAIN);
      _cleaner = updateCleaner(DocumentConverter.DEFAULT_DOMAIN, this._options,
         HTMLFilters);
      _whitelist = makeBaselineWhitelist(options);
      _cleaner = new Cleaner(_whitelist);

      if (options.getTables().isLeftAsHtml()) {
         // we need to allow the table nodes to be ignored
         // since they are automatically ignored recursively, this is the only
         // node we worry about.
         options.getIgnoredHtmlElements()
            .add(IgnoredHtmlElement.create("table"));
      }

      _converter = new DocumentConverter(options, HTMLFilters);
   }

   static public Whitelist makeBaselineWhitelist(Options options) {
      Whitelist whitelist = Whitelist.basicWithImages()
         .addTags("div", "h1", "h2", "h3", "h4", "h5", "h6", "table", "tbody",
            "td", "tfoot", "th", "thead", "tr", "hr", "span", "font", "header",
            "footer","noscript","form","input","section","aside","svg", "button",
            "article","textarea")
         .addAttributes("th", "colspan", "align", "style")
         .addAttributes("td", "colspan", "align", "style")
         .addAttributes("input","value","type","placeholder")
         .addAttributes("img","data-src")
         .addAttributes("nav","role")
         .addAttributes("svg","style")
         .addAttributes("button","value")
         .addAttributes(":all", "title", "style");
      if (options.preserveRelativeLinks) {
         whitelist.preserveRelativeLinks(true);
      }
      if (options.abbreviations) {
         whitelist.addTags("abbr", "acronym");
      }
      if (options.headerIds) {
         for (int i = 1; i <= 6; i++) {
            whitelist.addAttributes("h" + i, "id");
         }
      }
      for (final IgnoredHtmlElement el : options.getIgnoredHtmlElements()) {
         whitelist.addTags(el.getTagName());
         if (!el.getAttributes().isEmpty()) {
            whitelist.addAttributes(el.getTagName(), el.getAttributes()
               .toArray(new String[el.getAttributes().size()]));
         }
      }
      return whitelist;
   }

   static public JSONObject getHTMLFilters(String filterFileName) {
      JSONObject htmlFilters = new JSONObject();
      if (filterFileName == null) {
         filterFileName = HTML_FILTER_FILENAME;
      }
      File testFile = new File(filterFileName);
      if (testFile.exists() == false) {
         System.err.println("Filter file \"" + filterFileName
            + "\" does not exist. Using default \"" + HTML_FILTER_FILENAME
            + "\"");
         filterFileName = HTML_FILTER_FILENAME;
      } else if (testFile.isDirectory()) {
         System.err.println("Filter file \"" + filterFileName
            + "\" is a directory. Using default \"" + HTML_FILTER_FILENAME
            + "\"");
         filterFileName = HTML_FILTER_FILENAME;
      }
      try {
         // this sets the basic domain
         htmlFilters = getHTMLFilters(MDfromHTMLUtils.loadJSONFile(filterFileName),
            DocumentConverter.DEFAULT_DOMAIN);
      } catch (Exception e) {
         System.err.println("Could not load the JSON content from the file \""
            + filterFileName + "\" -- error: " + e.getLocalizedMessage());
         System.err.println("No HTML filtering will be performed.");
      }
      return htmlFilters;
   }

   static public JSONObject getHTMLFilters(JSONObject HTMLFilters,
      String domain) {
      if (HTMLFilters == null) {
         HTMLFilters = new JSONObject();
         HTMLFilters.put(DocumentConverter.DEFAULT_DOMAIN, new JSONObject());
      }
      return HTMLFilters;
   }

   public void resetCleaner() {
      _cleaner = new Cleaner(_whitelist);
   }

   /**
    * Creates a new whitelist based on the baseline settings, augmented by the
    * {@link DocumentConverter#DEFAULT_DOMAIN} and the the supplied domain (if it differs from the
    * {@link DocumentConverter#DEFAULT_DOMAIN} and is not null) filters.
    * 
    * @param domain
    *           the domain from the baseUri to be used to add filters
    * @param options
    * @param HTMLFilters
    * @return updated cleaner
    */
   public static Cleaner updateCleaner(String domain, Options options,
      JSONObject HTMLFilters) {
      Whitelist whitelist = makeBaselineWhitelist(options);
      JSONObject domainFilters = (JSONObject) HTMLFilters
         .get(DocumentConverter.DEFAULT_DOMAIN);
      if (domainFilters != null) {
         // for each key, ensure we are capturing them as tags, and add their
         // attributes
         Set<String> tags = domainFilters.keySet();
         whitelist.addTags(tags.toArray(new String[0]));
         for (String tag : tags) {
            if (DocumentConverter.TAG_NAMES.equals(tag) || DocumentConverter.SEEK_HEADERS.equals(tag)) {
               continue;
            }
            JSONObject attributeObj = (JSONObject) domainFilters.get(tag);
            Set<String> attributes = attributeObj.keySet();
            whitelist = whitelist.addAttributes(tag,
               attributes.toArray(new String[0]));
         }
      }
      if ((domain != null)
         && (DocumentConverter.DEFAULT_DOMAIN.equals(domain) == false)) {
         domainFilters = (JSONObject) HTMLFilters.get(domain);
         if (domainFilters != null) {
            // for each key, ensure we are capturing them as tags, and add their
            // attributes
            Set<String> tags = domainFilters.keySet();
            whitelist.addTags(tags.toArray(new String[0]));
            for (String tag : tags) {
               if (DocumentConverter.TAG_NAMES.equals(tag) || DocumentConverter.SEEK_HEADERS.equals(tag)) {
                  continue;
               }
               JSONObject attributeObj = (JSONObject) domainFilters.get(tag);
               Set<String> attributes = attributeObj.keySet();
               whitelist = whitelist.addAttributes(tag,
                  attributes.toArray(new String[0]));
            }
         }
      }
      return new Cleaner(whitelist);
   }

   /**
    * Provides access to the DocumentConverter for customization.
    *
    * @return the configured DocumentConverter.
    */
   public DocumentConverter getConverter() {
      return _converter;
   }

   /**
    * Returns true if the cleaned HTML document is echoed to {@code System.out}.
    * 
    * @return true if the cleaned HTML document is echoed
    */
   public boolean isCleanedHtmlEchoed() {
      return _cleanedHtmlEchoed;
   }

   /**
    * To see the cleaned and processed HTML document, set this to true. It will
    * be rendered to {@code System.out} for debugging purposes.
    * 
    * @param cleanedHtmlEchoed
    *           true to echo out the cleaned HTML document
    */
   public void setCleanedHtmlEchoed(boolean cleanedHtmlEchoed) {
      this._cleanedHtmlEchoed = cleanedHtmlEchoed;
   }

   /**
    * Captures a copy of the supplied HTMLFilters object into this object's
    * instance variable
    * 
    * @param HTMLFilters
    * @throws IOException
    */
   public void setHTMLFilters(JSONObject HTMLFilters) throws IOException {
      this._HTMLFilters = (JSONObject) JSON.parse(HTMLFilters.toString());
   }

   /**
    * This class is used to handle conversions that convert directly to streams.
    */
   private final class StreamRemark extends Remark {

      private final Remark remark;
      private final Writer writer;
      private final OutputStream os;
      private final ProvenanceWriter aw;

      private StreamRemark(Remark remark, Writer writer) {
         this.remark = remark;
         this.writer = writer;
         this.os = null;
         this.aw = null;
      }

      private StreamRemark(Remark remark, OutputStream out) {
         this.remark = remark;
         this.writer = null;
         this.os = out;
         this.aw = null;
      }

      private StreamRemark(Remark remark, ProvenanceWriter aw)
         throws Exception {
         this.remark = remark;
         this.writer = null;
         this.os = null;
         this.aw = aw;
      }

      private StreamRemark(Remark remark, Writer writer,
         JSONObject HTMLFilters) {
         this.remark = remark;
         this.writer = writer;
         this.os = null;
         this.aw = null;
         try {
            this.remark.setHTMLFilters(
               getHTMLFilters(HTMLFilters, DocumentConverter.DEFAULT_DOMAIN));
         } catch (IOException e) {
            e.printStackTrace();
         }
         remark._cleaner = updateCleaner(DocumentConverter.DEFAULT_DOMAIN,
            this.remark._options, HTMLFilters);
      }

      private StreamRemark(Remark remark, OutputStream out,
         JSONObject HTMLFilters) {
         this.remark = remark;
         this.writer = null;
         this.os = out;
         this.aw = null;
         try {
            this.remark.setHTMLFilters(
               getHTMLFilters(HTMLFilters, DocumentConverter.DEFAULT_DOMAIN));
         } catch (IOException e) {
            e.printStackTrace();
         }
         remark._cleaner = updateCleaner(DocumentConverter.DEFAULT_DOMAIN,
            this.remark._options, HTMLFilters);
      }

      private StreamRemark(Remark remark, ProvenanceWriter aw,
         JSONObject HTMLFilters) {
         this.remark = remark;
         this.writer = null;
         this.os = null;
         this.aw = aw;
         try {
            this.remark.setHTMLFilters(
               getHTMLFilters(HTMLFilters, DocumentConverter.DEFAULT_DOMAIN));
         } catch (IOException e) {
            e.printStackTrace();
         }
         remark._cleaner = updateCleaner(DocumentConverter.DEFAULT_DOMAIN,
            this.remark._options, HTMLFilters);
      }

      @Override
      public Remark withWriter(Writer writer) {
         return remark.withWriter(writer);
      }

      @Override
      public Remark withOutputStream(OutputStream os) {
         return remark.withOutputStream(os);
      }

      public String convert(Document doc, String baseUri) {
         return remark.processConvert(doc, writer, os, aw, baseUri);
      }
   }

   /**
    * Use this method in a chain to handle streaming the output to an
    * ProvenanceWriter. The returned class can be saved for repeated writing to
    * the same streams.
    *
    * <p>
    * <strong>Note: The convert methods on the returned class will always return
    * {@code null}.</strong>
    * </p>
    *
    * <p>
    * <strong>Note: It is up to the calling class to handle closing the
    * writer!</strong>
    * </p>
    *
    * <p>
    * Example:
    * </p>
    *
    * <blockquote>{@code new Remark(_options).withAnnotationWriter(myAnnotationWiter).convert(htmlText);}</blockquote>
    *
    * @param aw
    *           ProvenanceWriter to receive the converted output annotations
    * @return A Remark that writes annotations to streams.
    * @throws Exception
    */
   public synchronized Remark withAnnotationWriter(ProvenanceWriter aw)
      throws Exception {
      if (aw == null) {
         throw new NullPointerException("ProvenanceWriter cannot be null.");
      }
      return new StreamRemark(this, aw);
   }

   /**
    * Use this method in a chain to handle streaming the output to a Writer. The
    * returned class can be saved for repeated writing to the same streams.
    *
    * <p>
    * <strong>Note: The convert methods on the returned class will always return
    * {@code null}.</strong>
    * </p>
    *
    * <p>
    * <strong>Note: It is up to the calling class to handle closing the
    * writer!</strong>
    * </p>
    *
    * <p>
    * Example:
    * </p>
    *
    * <blockquote>{@code new Remark(_options).withWriter(myWiter).convert(htmlText);}</blockquote>
    *
    * @param writer
    *           Writer to receive the converted output
    * @return A Remark that writes to streams.
    */
   public synchronized Remark withWriter(Writer writer) {
      if (writer == null) {
         throw new NullPointerException("Writer cannot be null.");
      }
      return new StreamRemark(this, writer);
   }

   /**
    * Use this method in a chain to handle streaming the output to an
    * OutputStream. The returned class can be saved for repeated writing to the
    * same streams.
    *
    * <p>
    * <strong>Note: The convert methods on the returned class will always return
    * {@code null}.</strong>
    * </p>
    *
    * <p>
    * <strong>Note: It is up to the calling class to handle closing the
    * stream!</strong>
    * </p>
    *
    * <p>
    * Example:
    * </p>
    *
    * <blockquote>{@code new Remark(_options).withOutputStream(myOut).convert(htmlText);}</blockquote>
    *
    * @param os
    *           OutputStream to receive the converted output
    * @return A Remark that writes to streams.
    */
   public synchronized Remark withOutputStream(OutputStream os) {
      if (os == null) {
         throw new NullPointerException("OutputStream cannot be null.");
      }
      return new StreamRemark(this, os);
   }

   /**
    * Use this method in a chain to handle streaming the output to an
    * ProvenanceWriter. The returned class can be saved for repeated writing to
    * the same streams.
    *
    * <p>
    * <strong>Note: The convert methods on the returned class will always return
    * {@code null}.</strong>
    * </p>
    *
    * <p>
    * <strong>Note: It is up to the calling class to handle closing the
    * writer!</strong>
    * </p>
    *
    * <p>
    * Example:
    * </p>
    *
    * <blockquote>{@code new Remark(_options).withAnnotationWriter(myAnnotationWiter).convert(htmlText);}</blockquote>
    *
    * @param aw
    *           ProvenanceWriter to receive the converted output annotations
    * @param HTMLFilters
    *           General or domain specific files by attribute type with list of
    *           filters
    * @return A Remark that writes annotations to streams.
    */
   public synchronized Remark withAnnotationWriter(ProvenanceWriter aw,
      JSONObject HTMLFilters) {
      if (aw == null) {
         throw new NullPointerException("ProvenanceWriter cannot be null.");
      }
      return new StreamRemark(this, aw, HTMLFilters);
   }

   /**
    * Use this method in a chain to handle streaming the output to a Writer. The
    * returned class can be saved for repeated writing to the same streams.
    *
    * <p>
    * <strong>Note: The convert methods on the returned class will always return
    * {@code null}.</strong>
    * </p>
    *
    * <p>
    * <strong>Note: It is up to the calling class to handle closing the
    * writer!</strong>
    * </p>
    *
    * <p>
    * Example:
    * </p>
    *
    * <blockquote>{@code new Remark(_options).withWriter(myWiter).convert(htmlText);}</blockquote>
    *
    * @param writer
    *           Writer to receive the converted output
    * @param HTMLFilters
    *           General or domain specific files by attribute type with list of
    *           filters
    * @return A Remark that writes to streams.
    */
   public synchronized Remark withWriter(Writer writer,
      JSONObject HTMLFilters) {
      if (writer == null) {
         throw new NullPointerException("Writer cannot be null.");
      }
      return new StreamRemark(this, writer, HTMLFilters);
   }

   /**
    * Use this method in a chain to handle streaming the output to an
    * OutputStream. The returned class can be saved for repeated writing to the
    * same streams.
    *
    * <p>
    * <strong>Note: The convert methods on the returned class will always return
    * {@code null}.</strong>
    * </p>
    *
    * <p>
    * <strong>Note: It is up to the calling class to handle closing the
    * stream!</strong>
    * </p>
    *
    * <p>
    * Example:
    * </p>
    *
    * <blockquote>{@code new Remark(_options).withOutputStream(myOut).convert(htmlText);}</blockquote>
    *
    * @param os
    *           OutputStream to receive the converted output
    * @param HTMLFilters
    *           General or domain specific files by attribute type with list of
    *           filters
    * @return A Remark that writes to streams.
    */
   public synchronized Remark withOutputStream(OutputStream os,
      JSONObject HTMLFilters) {
      if (os == null) {
         throw new NullPointerException("OutputStream cannot be null.");
      }
      return new StreamRemark(this, os, HTMLFilters);
   }

   /**
    * Converts an HTML document retrieved from a URL to Markdown.
    * 
    * @param url
    *           URL to connect to.
    * @param timeoutMillis
    *           Maximum time to wait before giving up on the connection.
    * @return Markdown text.
    * @throws IOException
    *            If an error occurs while retrieving the document.
    * @see org.jsoup.Jsoup#parse(URL, int)
    */
   public String convert(URL url, int timeoutMillis) throws IOException {
      Document doc = Jsoup.parse(url, timeoutMillis);
      return convert(doc, url.toString());
   }

   /**
    * Converts an HTML file to Markdown.
    * 
    * @param file
    *           The file to load.
    * @return Markdown text.
    * @throws IOException
    *            If an error occurs while loading the file.
    * @see org.jsoup.Jsoup#parse(File, String, String)
    */
   public String convert(File file) throws IOException {
      return convert(file, null);
   }

   /**
    * Converts an HTML file to Markdown.
    * 
    * @param file
    *           The file to load.
    * @param charset
    *           The charset of the file (if not specified and not UTF-8). Set to
    *           {@code null} to determine from {@code http-equiv} meta tag, if
    *           present, or fall back to {@code UTF-8} (which is often safe to
    *           do).
    * @return Markdown text.
    * @throws IOException
    *            If an error occurs while loading the file.
    * @see org.jsoup.Jsoup#parse(File, String, String)
    */
   public String convert(File file, String charset) throws IOException {
      return convert(file, charset, "");
   }

   /**
    * Converts an HTML file to Markdown.
    * 
    * @param file
    *           The file to load.
    * @param charset
    *           The charset of the file (if not specified and not UTF-8). Set to
    *           {@code null} to determine from {@code http-equiv} meta tag, if
    *           present, or fall back to {@code UTF-8} (which is often safe to
    *           do).
    * @param baseUri
    *           The base URI for resolving relative links.
    * @return Markdown text.
    * @throws IOException
    *            If an error occurs while loading the file.
    * @see org.jsoup.Jsoup#parse(File, String, String)
    */
   public String convert(File file, String charset, String baseUri)
      throws IOException {
      Document doc = Jsoup.parse(file, charset, baseUri);
      return convert(doc, baseUri);
   }

   /**
    * Converts HTML in memory to Markdown.
    * 
    * @param html
    *           The string to processConvert from HTML
    * @return Markdown text.
    * @see org.jsoup.Jsoup#parse(String, String)
    */
   public String convert(String html) {
      return convert(html, "");
   }

   /**
    * Converts HTML in memory to Markdown.
    * 
    * @param html
    *           The string to processConvert from HTML
    * @param baseUri
    *           The base URI for resolving relative links.
    * @return Markdown text.
    * @see org.jsoup.Jsoup#parse(String, String)
    */
   public String convert(String html, String baseUri) {
      Document doc = Jsoup.parse(html, baseUri);
      return convert(doc, baseUri);
   }

   /**
    * Converts an HTML body fragment to Markdown.
    * 
    * @param body
    *           The fragment string to processConvert from HTML
    * @return Markdown text.
    * @see org.jsoup.Jsoup#parseBodyFragment(String, String)
    */
   public String convertFragment(String body) {
      return convertFragment(body, "");
   }

   /**
    * Converts an HTML body fragment to Markdown.
    * 
    * @param body
    *           The fragment string to processConvert from HTML
    * @param baseUri
    *           The base URI for resolving relative links.
    * @return Markdown text.
    * @see org.jsoup.Jsoup#parseBodyFragment(String, String)
    */
   public String convertFragment(String body, String baseUri) {
      Document doc = Jsoup.parseBodyFragment(body, baseUri);
      return convert(doc, baseUri);
   }

   /**
    * Converts an already-loaded JSoup Document to Markdown.
    *
    * @param doc
    *           Document to be processed
    * @param aw
    *           Annotation Writer for annotations
    * @return Markdown text.
    */
   public String convert(Document doc, ProvenanceWriter aw) {
      // Note: all convert methods should end up going through this method!
      return processConvert(doc, null, null, aw, null);
   }

   /**
    * Converts an already-loaded JSoup Document to Markdown.
    *
    * @param doc
    *           Document to be processed
    * @param aw
    *           Annotation Writer for annotations
    * @param baseUri
    *           The baseUri from which the domain is aptured for additional
    *           filtering
    * @return Markdown text.
    */
   public String convert(Document doc, ProvenanceWriter aw, String baseUri) {
      // Note: all convert methods should end up going through this method!
      return processConvert(doc, null, null, aw, baseUri);
   }

   /**
    * Converts an already-loaded JSoup Document to Markdown.
    *
    * @param doc
    *           Document to be processed
    * @param baseUri
    *           The baseUri from which the domain is aptured for additional
    *           filtering
    * @return Markdown text.
    */
   public String convert(Document doc, String baseUri) {
      // Note: all convert methods should end up going through this method!
      String markdown = processConvert(doc, null, null, null, baseUri);
      return markdown;
   }

   /**
    * Retrieves the domain from the baseUri stripping off protocol if found and
    * stopping at the colon (for port) or slash (for query string). If nothing
    * terminates the URI then we assume the entire URI is the domain.
    * 
    * @param baseUri
    *           URI from which we'll grab the domain
    * @return the domain (or null if the baseUri is null)
    */
   public static String getDomain(String baseUri) {
      String result = null;
      int index = -1;
      int index1 = -1;
      int index2 = -1;

      if (baseUri != null) {
         baseUri = baseUri.trim();
         if (baseUri.length() > 0) {
            // domain should be after protocol and before port or /
            index = baseUri.indexOf("//");
            if ((index != -1) && (baseUri.length() > (index + 2))) {
               // lop off the protocol through the //
               baseUri = baseUri.substring(index + 2);
            }
            // now try to find the end of the domain
            index1 = baseUri.indexOf("/");
            index2 = baseUri.indexOf(":");
            if (index1 != -1 && index2 != -1) {
               if (index1 < index2) {
                  // domain is through /
                  result = baseUri.substring(0, index1);
               } else {
                  // domain is through :
                  result = baseUri.substring(0, index2);
               }
            } else if (index1 != -1 && index2 == -1) {
               // domain is through /
               result = baseUri.substring(0, index1);
            } else if (index1 == -1 && index2 != -1) {
               // domain is up through :
               result = baseUri.substring(0, index2);
            } else {
               // no colon nor slash so assume uri is the domain
               result = baseUri;
            }
         }
      }
      return result;
   }

   /**
    * Direct access the _HTMLFilters object (be careful if you change what you
    * got back as it could affect this objects behavior)
    * 
    * @return _HTMLFilters object
    */
   public JSONObject getHTMLFilters() {
      return (JSONObject) _HTMLFilters;
   }

   /**
    * Handles the actual conversion
    * 
    * @param doc
    *           document to convert
    * @param writer
    *           Optional Writer for output
    * @param os
    *           Optional OutputStream for output
    * @param aw
    *           Annotation Writer for annotations
    * @param baseURI
    *           URI from which we get the domain to add the HTML filters
    * @return String result if not using an output stream, else null
    */
   private String processConvert(Document doc, Writer writer, OutputStream os,
      ProvenanceWriter aw, String baseURI) {
      String domain = getDomain(baseURI);
      int testindex = baseURI.indexOf(domain);
      // need to find actual domain for proper filters
      String workingURI = baseURI.substring(testindex+domain.length());
      testindex = workingURI.toLowerCase().indexOf("http");
      if (testindex >= 0) {
         workingURI = workingURI.substring(testindex);
         domain = Remark.getDomain(workingURI);
         baseURI = workingURI;
      }
      _cleaner = updateCleaner(domain, _options, _HTMLFilters);
      doc = _cleaner.clean(doc);
      if (_cleanedHtmlEchoed) {
         System.out.println("Cleaned and processed HTML document:");
         System.out.println(doc.toString());
         System.out.println();
      }
      String result = null;
      _converterLock.lock();
      try {
         if (writer != null) {
            _converter.convert(doc, writer, aw, baseURI, domain);
         } else if (os != null) {
            _converter.convert(doc, os, aw, baseURI, domain);
         } else {
            result = _converter.convert(doc, aw, baseURI, domain);
         }
      } finally {
         _converterLock.unlock();
         resetCleaner();
      }
      return result;
   }
}
