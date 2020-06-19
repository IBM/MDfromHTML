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

package com.overzealous.remark.convert;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import com.api.json.JSONArray;
import com.api.json.JSONArtifact;
import com.api.json.JSONObject;
import com.overzealous.remark.IgnoredHtmlElement;
import com.overzealous.remark.Options;
import com.overzealous.remark.util.BlockWriter;

/**
 * The class that does the heavy lifting for converting a JSoup Document into
 * valid Markdown
 *
 * @author Phil DeJarnett
 * @author Nathaniel Mills modifications for provenance and level tracking
 */
public class DocumentConverter {

   // These properties do not change for the life of this converter
   static public String DEFAULT_DOMAIN = "*";
   static public String ALL_TAGS = ":all";
   static public String TAG_NAMES = ":tagnames";
   static public String SEEK_HEADERS = ":seek_headers";
   static public String OVERRIDE_RULES = ":overrides";
   final Options options;
   final protected TextCleaner cleaner;
   final protected Set<String> ignoredHtmlTags;
   final protected Map<String, NodeHandler> blockNodes;
   final protected Map<String, NodeHandler> inlineNodes;
   protected JSONObject HTMLFilters = new JSONObject();

   // These properties change for each conversion
   protected Map<String, String> linkUrls; // for looking up links via URL
   protected int genericLinkUrlCounter;
   protected int genericImageUrlCounter;
   protected Map<String, String> linkIds; // an inverse of linkUrls, for looking
                                          // up links via ID
   protected Map<String, String> abbreviations; // a cache of abbreviations
                                                // mapped
                                                // by abbreviated form
   protected BlockWriter output = null; // the output writer, which may change
                                        // during
   // recursion

   protected Map<String, NodeHandler> lastNodeset;

   private static final Pattern COMMA = Pattern.compile(",");
   private static final Pattern LINK_MULTIPLE_SPACES = Pattern.compile(" {2,}",
      Pattern.DOTALL);
   private static final Pattern LINK_SAFE_CHARS = Pattern
      .compile("[^-\\w \\.]+", Pattern.DOTALL);
   private static final String LINK_REPLACEMENT = "_";
   private static final Pattern LINK_EDGE_REPLACE = Pattern
      .compile(String.format("(^%1$s++)|(%1$s++$)", LINK_REPLACEMENT));
   private static final Pattern LINK_MULTIPLE_REPLACE = Pattern
      .compile(String.format("%1$s{2,}", LINK_REPLACEMENT));
   private static final Pattern LINK_FILENAME = Pattern.compile("/([^/]++)$");

   public DocumentConverter(Options options) {
      this(options, null);
   }

   /**
    * Creates a DocumentConverted with the given options.
    * 
    * @param options
    *           Options for this converter.
    * @param HTMLFilters
    *           Filter directives for HTML content to be ignored. Filters may be
    *           global or by domain and provide attributes and a list of filters
    *           which are tested against lowercase versions of the attribute
    *           values to see if they contain them. If null, no filters are
    *           active.
    */
   public DocumentConverter(Options options, JSONObject HTMLFilters) {
      // configure final properties
      this.options = options;
      cleaner = new TextCleaner(options);
      ignoredHtmlTags = new HashSet<String>();
      blockNodes = new HashMap<String, NodeHandler>();
      inlineNodes = new HashMap<String, NodeHandler>();
      if (HTMLFilters != null) {
         this.HTMLFilters = HTMLFilters;
      }

      // configure ignored tags
      for (final IgnoredHtmlElement ihe : options.getIgnoredHtmlElements()) {
         ignoredHtmlTags.add(ihe.getTagName());
      }

      configureNodes();
   }

   private void configureNodes() {
      addInlineNode(new InlineStyle(), "i,em,b,strong,font,span");
      addInlineNode(new InlineCode(), "code,tt");
      addInlineNode(new Image(), "img");
      addInlineNode(new Anchor(), "a");
      addInlineNode(new Break(), "br");
      addInlineNode(new Button(), "button");
      addInlineNode(new Input(), "input");
      /**
       * Had to add special span handling in InlineStyle to 
       * avoid additional inline code processing being called.
       * broken.md was showing ***** rather than ***
       */
      // addInlineNode(new InlineStyle(), "span");
      addBlockNode(new Aside(), "aside");
      addBlockNode(new Heading(), "h1,h2,h3,h4,h5,h6");
      addBlockNode(new Header(), "header");
      addBlockNode(new Paragraph(), "p");
      addBlockNode(new Codeblock(), "pre");
      addBlockNode(new BlockQuote(), "blockquote");
      addBlockNode(new Button(), "button");
      addBlockNode(new Span(), "span");
      addBlockNode(new SVG(), "svg");
      addBlockNode(new HorizontalRule(), "hr");
      addBlockNode(new List(), "ol,ul");
      addBlockNode(new Input(), "input");
      addBlockNode(new Article(), "article");
      addBlockNode(new TextArea(), "textarea");
      // below need work to behave correctly wrt newlines
      // addBlockNode(new Section(), "section");
      // addBlockNode(new Division(), "div");

      if (options.abbreviations) {
         addInlineNode(new Abbr(), "abbr,acronym");
      }

      if (options.definitionLists) {
         addBlockNode(new Definitions(), "dl");
      }

      // TABLES
      if (options.getTables().isConvertedToText()) {
         // if we are going to process it, add the handler
         addBlockNode(new Table(), "table");

      } else if (options.getTables().isRemoved()) {
         addBlockNode(NodeRemover.getInstance(), "table");

      } // else, it's being added directly
   }

   public Options getOptions() {
      return options;
   }

   public TextCleaner getCleaner() {
      return cleaner;
   }

   public Map<String, NodeHandler> getBlockNodes() {
      return Collections.unmodifiableMap(blockNodes);
   }

   public Map<String, NodeHandler> getInlineNodes() {
      return Collections.unmodifiableMap(inlineNodes);
   }

   public BlockWriter getOutput() {
      return output;
   }

   public void setOutput(BlockWriter output) {
      this.output = output;
   }

   /**
    * Customize the processing for a node. This node is added to the inline list
    * and the block list. The inline list is used for nodes that do not contain
    * linebreaks, such as {@code <em>} or {@code <strong>}.
    *
    * The tagnames is a comma-delimited list of tagnames for which this handler
    * should be applied.
    *
    * @param handler
    *           The handler for the nodes
    * @param tagnames
    *           One or more tagnames
    */
   public void addInlineNode(NodeHandler handler, String tagnames) {
      for (final String key : COMMA.split(tagnames)) {
         if (key.length() > 0) {
            inlineNodes.put(key, handler);
            blockNodes.put(key, handler);
         }
      }
   }

   /**
    * Customize the processing for a node. This node is added to the block list
    * only. The node handler should properly use the
    * {@link com.overzealous.remark.util.BlockWriter#startBlock()} and
    * {@link com.overzealous.remark.util.BlockWriter#endBlock()} methods as
    * appropriate.
    *
    * The tagnames is a comma-delimited list of tagnames for which this handler
    * should be applied.
    *
    * @param handler
    *           The handler for the nodes
    * @param tagnames
    *           One or more tagnames
    */
   public void addBlockNode(NodeHandler handler, String tagnames) {
      for (final String key : COMMA.split(tagnames)) {
         if (key.length() > 0) {
            blockNodes.put(key, handler);
         }
      }
   }

   /**
    * Convert a document to the given writer.
    *
    * <p>
    * <strong>Note: It is up to the calling class to handle closing the
    * writer!</strong>
    * </p>
    *
    * @param doc
    *           Document to convert
    * @param out
    *           Writer to receive the final output
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           The domain culled from the baseUri to help with HTML filtering
    */
   public void convert(Document doc, Writer out, ProvenanceWriter pw,
      String baseUri, String domain) {
      this.output = new BlockWriter(out, true);
      this.convertImpl(doc, pw, baseUri, domain);
   }

   /**
    * Convert a document to the given output stream.
    *
    * <p>
    * <strong>Note: It is up to the calling class to handle closing the
    * stream!</strong>
    * </p>
    *
    * @param doc
    *           Document to convert
    * @param out
    *           OutputStream to receive the final output
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           The domain culled from the baseUri to help with HTML filtering
    */
   public void convert(Document doc, OutputStream out, ProvenanceWriter pw,
      String baseUri, String domain) {
      this.output = new BlockWriter(out, true);
      this.convertImpl(doc, pw, baseUri, domain);
   }

   /**
    * Convert a document and return a string. When wanting a final string, this
    * method should always be used. It will attempt to calculate the size of the
    * buffer necessary to hold the entire output.
    *
    * @param doc
    *           Document to convert
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           The domain culled from the baseUri to help with HTML filtering
    * @return The Markdown-formatted string.
    */
   public String convert(Document doc, ProvenanceWriter pw, String baseUri, String domain) {
      // estimate the size necessary to handle the final output
      BlockWriter bw = BlockWriter
         .create(DocumentConverter.calculateLength(doc, 0));
      this.output = bw;
      this.convertImpl(doc, pw, baseUri, domain);
      return bw.toString();
   }

   // Utility method to quickly walk the DOM tree and estimate the size of the
   // buffer necessary to hold the result.
   public static int calculateLength(Element el, int depth) {
      int result = 0;
      for (final Node n : el.childNodes()) {
         if (n instanceof Element) {
            result += (4 * depth) + calculateLength((Element) n, depth + 1);
         } else if (n instanceof TextNode) {
            result += ((TextNode) n).text().length();
         }
      }
      return result;
   }

   // implementation of the convert method. Basically handles setting up the
   private void convertImpl(Document doc, ProvenanceWriter pw, String baseUri, String domain) {

      /**
       * TODO: consider tracking the annotation for the linkIds and
       * abbreviations in their maps, storing a StringPair with the value and
       * its annotation
       */
      // linked, because we want the resulting list of links in order they were
      // added
      linkIds = new LinkedHashMap<String, String>();
      // To keep track of already added URLs
      linkUrls = new HashMap<String, String>();
      genericImageUrlCounter = 0;
      genericLinkUrlCounter = 0;
      // linked, to keep abbreviations in the order they were added
      abbreviations = new LinkedHashMap<String, String>();

      lastNodeset = blockNodes;

      String level = "1"; // top level node for doc.body
      // walk the DOM
      Element body = doc.body();
      try {
         if (pw != null) {
            pw.saveHTML2MD(level, body, "");
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      walkNodes(DefaultNodeHandler.getInstance(), body, blockNodes, pw, baseUri, domain,
         level, null);

      if (!linkIds.isEmpty()) {
         // Add links
         output.startBlock();
         for (final Map.Entry<String, String> link : linkIds.entrySet()) {
            output.printf("\n[%s]: %s", link.getKey(), link.getValue());
         }
         output.endBlock();
      }
      if (!abbreviations.isEmpty()) {
         // Add abbreviations
         output.startBlock();
         for (final Map.Entry<String, String> abbr : abbreviations.entrySet()) {
            output.printf("\n*[%s]: %s", abbr.getKey(),
               cleaner.clean(abbr.getValue()));
         }
         output.endBlock();
      }

      // free up unused properties
      linkIds = null;
      linkUrls = null;
      abbreviations = null;
// wnm3 exploring 200212
//      output = null;
   }

   /**
    * Loops over the children of an HTML Element, handling TextNode and child
    * Elements.
    *
    * @param currentNode
    *           The default node handler for TextNodes and IgnoredHTMLElements.
    * @param el
    *           The parent HTML Element whose children are being looked at.
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           The domain culled from the baseUri to help with HTML filtering
    *           markdown to document element(s)
    * @param level
    *           The dotted tree notation for the location within the dom of the
    *           node being processed. e.g., The top level node (e.g., <body>)
    *           would be 1. Its first child element would be 1.1 and 2nd would
    *           be 1.2. The first childs first child would be 1.1.1. For inline
    *           expressions, we can introduce a different separator like the
    *           tilde. So, if an element is like:
    *           <a class="ocShareButton" data-bi-bhvr="SOCIALSHARE" data-bi-name="facebook" data-bi-slot="1" 
    *              href="https://web.archive.org/web/20190411221943/https://www.facebook.com/sharer.php?u=https://support.office.com/en-us/article/install-skype-for-business-8a0d4da8-9d58-44f9-9759-5c8f340cb3fb"
    *              id="ocFacebookButton" ms.cmpgrp="Share" ms.ea_action="Goto"ms.ea_offer="SOC" ms.interactiontype="1" ms.pgarea="Body" target="_blank">
    *                 <img alt="Facebook" class="ocArticleFooterImage" ms.cmpgrp="content" ms.pgarea="Body" 
    *                    src="/web/20190411221943im_/https://support.office.com/Images/SOC-Facebook.png">
    *           </a>
    *           Then the <a> tag would be x.y and its href would be x.y~1 and the <img> would be x.y.1 and its src would be 
    *           x.y.1~1. For text between tags we could use a carat as a separator.
    * @param searchLevel signals to stop dom walking if we have reached the search level, and return the node we are on. If null, no interuption occurs.
    * @return the Node where level matches searchLevel, otherwise returns null
    */
   public Node walkNodes(NodeHandler currentNode, Element el,
      ProvenanceWriter pw, String baseUri, String domain, String level, String searchLevel) {
      return walkNodes(currentNode, el, lastNodeset, pw, baseUri, domain, level,
         searchLevel);
   }

   /**
    * Loops over the children of an HTML Element, handling TextNode and child
    * Elements.
    *
    * @param currentNodeHandler
    *           The default node handler for TextNodes and IgnoredHTMLElements.
    * @param el
    *           The parent HTML Element whose children are being looked at.
    * @param nodeList
    *           The list of valid nodes at this level. Should be one of
    *           <b>blockNodes</b> or <b>inlineNodes</b>
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           The domain culled from the baseUri to help with HTML filtering
    * @param level
    *           The dotted tree notation for the location within the dom of the
    *           node being processed. e.g., The top level node (e.g., <body>)
    *           would be 1. Its first child element would be 1.1 and 2nd would
    *           be 1.2. The first childs first child would be 1.1.1. For inline
    *           expressions, we can introduce a different separator like the
    *           tilde. So, if an element is like:
    *           <a class="ocShareButton" data-bi-bhvr="SOCIALSHARE" data-bi-name="facebook" data-bi-slot="1" 
    *              href="https://web.archive.org/web/20190411221943/https://www.facebook.com/sharer.php?u=https://support.office.com/en-us/article/install-skype-for-business-8a0d4da8-9d58-44f9-9759-5c8f340cb3fb"
    *              id="ocFacebookButton" ms.cmpgrp="Share" ms.ea_action="Goto"ms.ea_offer="SOC" ms.interactiontype="1" ms.pgarea="Body" target="_blank">
    *                 <img alt="Facebook" class="ocArticleFooterImage" ms.cmpgrp="content" ms.pgarea="Body" 
    *                    src="/web/20190411221943im_/https://support.office.com/Images/SOC-Facebook.png">
    *           </a>
    *           Then the <a> tag would be x.y and its href would be x.y~1 and the <img> would be x.y.1 and its src would be 
    *           x.y.1~1. For text between tags we could use a carat as a separator.
    * @param searchLevel signals to stop dom walking if we have reached the search level, and return the node we are on. If null, no interuption occurs.
    */
   public Node walkNodes(NodeHandler currentNodeHandler, Element el,
      Map<String, NodeHandler> nodeList, ProvenanceWriter pw, String baseUri, String domain,
      String level, String searchLevel) {
      Node result = null;
      Map<String, NodeHandler> backupLastNodeset = lastNodeset;
      lastNodeset = nodeList;
      int depthLevel = 0;
      int textLevel = 0;
      String nextLevel = "";
      for (final Node n : el.childNodes()) {
         // we aren't taking newlines (that become spaces) into account
         if (n instanceof TextNode && " ".equals(n.toString()) == true) {
            continue;
         }
         if (n instanceof TextNode) {
            textLevel++;
            // It's just text!
            if (searchLevel != null
               && searchLevel.equals(level + "^" + textLevel)) {
               return n;
            }
            currentNodeHandler.handleTextNode((TextNode) n, this, pw, baseUri, domain,
               level + "^" + textLevel);
            continue;
         }
         
         depthLevel++;
         nextLevel = level + "." + depthLevel;
         if (searchLevel != null && searchLevel.equals(nextLevel)) {
            return n;
         }

         if (n instanceof Element) {
            // figure out who can handle this
            Element node = (Element) n;
            String tagName = node.tagName();
            /**
             * Process rules in HTMLFilters to determine if we skip this node by
             * continuing
             */
            if (checkHTMLFilters(HTMLFilters, tagName, node, pw, baseUri, domain, nextLevel)) {
               continue;
            }
// Note: below causes the <h2 to be ignored because we are processing inlineNodes
            if (nodeList.containsKey(tagName)) {
               // OK, we know how to handle this node
               result = nodeList.get(tagName).handleNode(currentNodeHandler,
                  node, this, pw, baseUri, domain, nextLevel, searchLevel);
               if (result != null) {
                  return result;
               }
// Note: below was a attempt to handle anchor tag with header as label
// <a ... ><h2>something<h2></a> but this results in [##something##][something]
//             if (inlineNodes.containsKey(tagName)) {
//                // OK, we know how to handle this node
//                result = inlineNodes.get(tagName).handleNode(currentNodeHandler,
//                   node, this, pw, baseUri, domain, nextLevel, searchLevel);
//                if (result != null) {
//                   return result;
//                }
//             } else if (blockNodes.containsKey(tagName)) {
//                // OK, we know how to handle this node
//                result = blockNodes.get(tagName).handleNode(currentNodeHandler,
//                   node, this, pw, baseUri, domain, nextLevel, searchLevel);
//                if (result != null) {
//                   return result;
//                }
            } else if (ignoredHtmlTags.contains(tagName)) {
                  // User wants to leave this tag in the output. Naughty user.
                  currentNodeHandler.handleIgnoredHTMLElement(node, this, pw,
                     baseUri, domain, nextLevel);

            } else {
               // No-can-do, just remove the node, and keep on walkin'
               // The only thing we'll do is add block status in if the unknown
               // node
               // usually renders as a block.
               // Due to BlockWriter's intelligent tracking, we shouldn't get a
               // whole bunch
               // of empty lines for empty nodes.
               if (node.isBlock()) {
                  output.startBlock();
               }
               try {
                  if (pw != null) {
                     pw.saveFilteredHTML(nextLevel, node, "tag: \""+tagName+"\" has no specific processing so reviewing its children.");
                  }
               } catch (IOException e) {
                  e.printStackTrace();
               }
               result = walkNodes(currentNodeHandler, node, nodeList, pw,
                  baseUri, domain, nextLevel, searchLevel);
               if (result != null) {
                  return result;
               }
               if (node.isBlock()) {
                  output.endBlock();
               }
            }
         } else {
            // not a node we care about (e.g.: comment nodes)
            try {
               if (pw != null) {
                  pw.saveFilteredHTML(nextLevel, n, "type: \""+ n.getClass().getSimpleName()+"\" is not a type we care about");
               }
            } catch (IOException e) {
               e.printStackTrace();
            }

         }
      }
      lastNodeset = backupLastNodeset;
      return result;
   }

   /**
    * Given the nodeTagName check the node's attributes and their values to
    * determine if the node should be filtered. The filtering is based on rules
    * for all domains, and rules specific for a particular domain. Each rule
    * contains a type of tag, a set of attribute names, each having a list of
    * filters compared against the attribute's value. If the lowercase of the
    * value contains the filter, the node should be filtered. As soon as a
    * filter reports true, this method returns.
    * 
    * @param nodeTagName
    *           the name of the node's tag
    * @param node
    *           the node to be checked for filtering
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           The domain culled from the baseUri to help with HTML filtering
    * @param level
    *           The dotted tree notation for the location within the dom of the
    *           node being processed. e.g., The top level node (e.g., <body>)
    *           would be 1. Its first child element would be 1.1 and 2nd would
    *           be 1.2. The first childs first child would be 1.1.1. For inline
    *           expressions, we can introduce a different separator like the
    *           tilde. So, if an element is like:
    *           <a class="ocShareButton" data-bi-bhvr="SOCIALSHARE" data-bi-name="facebook" data-bi-slot="1" 
    *              href="https://web.archive.org/web/20190411221943/https://www.facebook.com/sharer.php?u=https://support.office.com/en-us/article/install-skype-for-business-8a0d4da8-9d58-44f9-9759-5c8f340cb3fb"
    *              id="ocFacebookButton" ms.cmpgrp="Share" ms.ea_action="Goto"ms.ea_offer="SOC" ms.interactiontype="1" ms.pgarea="Body" target="_blank">
    *                 <img alt="Facebook" class="ocArticleFooterImage" ms.cmpgrp="content" ms.pgarea="Body" 
    *                    src="/web/20190411221943im_/https://support.office.com/Images/SOC-Facebook.png">
    *           </a>
    *           Then the <a> tag would be x.y and its href would be x.y~1 and the <img> would be x.y.1 and its src would be 
    *           x.y.1~1. For text between tags we could use a carat as a separator.
    * @return true if the node should be filtered
    */
   public static boolean checkHTMLFilters(JSONObject HTMLFilters,
      String nodeTagName, Element node, ProvenanceWriter pw, String baseUri, String domain,
      String level) {
      JSONObject domainRules = new JSONObject();
      JSONObject overrideRules = new JSONObject();
      JSONArray overrideTagNames = new JSONArray();
      JSONObject noOverrides =  new JSONObject(); // for domain specific filters
      JSONArtifact artifact = null;
      if (domain != null) {
         domainRules = (JSONObject) HTMLFilters.get(domain);
         if (domainRules != null) {
            artifact = (JSONObject)domainRules.get(OVERRIDE_RULES);
            if (artifact != null) {
               overrideRules = (JSONObject)artifact;
            }
            artifact = (JSONArray)overrideRules.get(TAG_NAMES);
            if (artifact != null) {
               overrideTagNames = (JSONArray)artifact;
            }
         }
      }
      
      // first check against the general
      JSONObject generalRules = (JSONObject) HTMLFilters.get(DEFAULT_DOMAIN);
      if (generalRules != null) {
         // first check the tagName
         JSONArray tagNames = (JSONArray) generalRules.get(TAG_NAMES);
         if (tagNames != null) {
            if (overrideTagNames.contains(node.tagName()) == false && tagNames.contains(node.tagName())) {
               try {
                  if (pw != null) {
                     pw.saveFilteredHTML(level, node, "tag: \""+node.tagName()+"\" is contained in \""+DEFAULT_DOMAIN+"~"+TAG_NAMES+"\"");
                  }
               } catch (IOException e) {
                  e.printStackTrace();
               }
               return true;
            }
         }
         // next do all tag's attributes
         JSONObject allTagRules = (JSONObject) generalRules.get(ALL_TAGS);
         if (allTagRules != null) {
            if (checkTagAttributeFilters(allTagRules, node, (JSONObject)overrideRules.get(ALL_TAGS), DEFAULT_DOMAIN+"~"+ALL_TAGS, pw, level)) {
               return true;
            }
         }
         // next check for tag specific attribute rules
         JSONObject nodeTagRules = (JSONObject) generalRules.get(nodeTagName);
         if (nodeTagRules != null) {
            if (checkTagAttributeFilters(nodeTagRules, node, (JSONObject)overrideRules.get(nodeTagName), DEFAULT_DOMAIN+"~"+nodeTagName, pw, level)) {
               return true;
            }
         }
      }
      // then check against the domain specific (if specified)
      if (domainRules != null) {
         // first check the tagName
         JSONArray tagNames = (JSONArray) domainRules.get(TAG_NAMES);
         if (tagNames != null) {
            if (tagNames.contains(node.tagName())) {
               try {
                  if (pw != null) {
                     pw.saveFilteredHTML(level, node, "tag: \""+node.tagName()+"\" is contained in \""+domain+"~"+TAG_NAMES+"\"");
                  }
               } catch (IOException e) {
                  e.printStackTrace();
               }
               return true;
            }
         }
         // next do all tag's attributes
         JSONObject allDomainTagRules = (JSONObject) domainRules
            .get(ALL_TAGS);
         if (allDomainTagRules != null) {
            if (checkTagAttributeFilters(allDomainTagRules, node, noOverrides, domain+"~"+ALL_TAGS, pw, level)) {
               return true;
            }
         }
         // next check for tag specific attribute rules
         JSONObject domainNodeTagRules = (JSONObject) domainRules
            .get(nodeTagName);
         if (domainNodeTagRules != null) {
            if (checkTagAttributeFilters(domainNodeTagRules, node, noOverrides, domain+"~"+nodeTagName, pw, level)) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Check whether the node's attribute's value contains a reference to any of
    * the the tagAttributeFilters keys (attributes) values
    * 
    * @param tagAttributeFilters
    *           Object from HTMLFilter for the node under review
    * @param node
    *           HTML node whose attributes will be tested
    * @param overrides
    *           Rules overriding the tagAttributeFilters
    * @param filterType
    *           The kind of filter being tested (e.g., :all or domain)
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @return true if this node should be filtered
    */
   public static boolean checkTagAttributeFilters(
      JSONObject tagAttributeFilters, Element node, JSONObject overrides, String filterType, ProvenanceWriter pw, String level) {
      String testValue = "";
      String filter = "";
      Set<String> overrideFilterValues = new HashSet<String>();
      Set<String> attributes = tagAttributeFilters.keySet();
      boolean allowOverride = false;
      if (overrides == null) {
         overrides = new JSONObject();
      }
      for (String attribute : attributes) {
         if (attribute.equals(SEEK_HEADERS)) {
            continue;
         }
         JSONArray overrideFilters = (JSONArray) overrides.get(attribute);
         if (overrideFilters == null) {
            overrideFilters = new JSONArray();
         }
         overrideFilterValues = new HashSet<String>();
         for (Object ovObj:overrideFilters) {
            overrideFilterValues.add(ovObj.toString());
         }
         JSONArray filters = (JSONArray) tagAttributeFilters.get(attribute);
         if (filters != null) {
            /**
             * for each attribute, check whether attribute value contains its filter
             */
            testValue = node.attr(attribute).toLowerCase();
            if (testValue.length() == 0) {
               continue;
            }
            for (Object filterObj : filters) {
               filter = filterObj.toString();
               if (testValue.contains(filter)) {
                  // we are about to filter this -- check for overrides
                  allowOverride = false;
                  for (String ovFilter : overrideFilterValues) {
                     if (ovFilter.contains(filter)) {
                        allowOverride = true;
                        break;
                     }
                  }
                  if (!allowOverride) {
                     try {
                        if (pw != null) {
                           pw.saveFilteredHTML(level, node, "attribute: \""+attribute +"\" value: \""+ testValue+"\" contains filter: \""+filterType+"~"+attribute+"~"+filter+"\"");
                        }
                     } catch (IOException e) {
                        e.printStackTrace();
                     }
                     return true;
                  }
               }
            }
         }
      }
      return false;
   }

   /**
    * Recursively processes child nodes and returns the potential output string.
    * 
    * @param currentNode
    *           The default node handler for TextNodes and IgnoredHTMLElements.
    * @param el
    *           The parent HTML Element whose children are being looked at.
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           The domain culled from the baseUri to help with HTML filtering
    * @param level
    *           The dotted tree notation for the location within the dom of the
    *           node being processed. e.g., The top level node (e.g., <body>)
    *           would be 1. Its first child element would be 1.1 and 2nd would
    *           be 1.2. The first childs first child would be 1.1.1. For inline
    *           expressions, we can introduce a different separator like the
    *           tilde. So, if an element is like: <a class="ocShareButton"
    *           data-bi-bhvr="SOCIALSHARE" data-bi-name="facebook"
    *           data-bi-slot="1"
    *           href="https://web.archive.org/web/20190411221943/https://www.facebook.com/sharer.php?u=https://support.office.com/en-us/article/install-skype-for-business-8a0d4da8-9d58-44f9-9759-5c8f340cb3fb"
    *           id="ocFacebookButton" ms.cmpgrp="Share"
    *           ms.ea_action="Goto"ms.ea_offer="SOC" ms.interactiontype="1"
    *           ms.pgarea="Body" target="_blank">
    *           <img alt="Facebook" class="ocArticleFooterImage" ms.cmpgrp=
    *           "content" ms.pgarea="Body" src=
    *           "/web/20190411221943im_/https://support.office.com/Images/SOC-Facebook.png">
    *           </a> Then the <a> tag would be x.y and its href would be x.y~1
    *           and the <img> would be x.y.1 and its src would be x.y.1~1. For
    *           text between tags we could use a carat as a separator.
    * @param searchLevel
    *           signals to stop dom walking if we have reached the search level,
    *           and return the node we are on. If null, no interuption occurs.
    * @return The potential output string.
    */
   public String getInlineContent(NodeHandler currentNode, Element el,
      ProvenanceWriter pw, String baseUri, String domain, String level, String searchLevel,
      Set<Node> foundNodes) {
      return this.getInlineContent(currentNode, el, false, pw, baseUri, domain, level,
         searchLevel, foundNodes);
   }

   /**
    * Recursively processes child nodes and returns the potential output string.
    * 
    * @param currentNode
    *           The default node handler for TextNodes and IgnoredHTMLElements.
    * @param el
    *           The parent HTML Element whose children are being looked at.
    * @param undoLeadingEscapes
    *           If true, leading escapes are removed
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           The domain culled from the baseUri to help with HTML filtering
    * @param level
    *           The dotted tree notation for the location within the dom of the
    *           node being processed. e.g., The top level node (e.g., <body>)
    *           would be 1. Its first child element would be 1.1 and 2nd would
    *           be 1.2. The first childs first child would be 1.1.1. For inline
    *           expressions, we can introduce a different separator like the
    *           tilde. So, if an element is like: <a class="ocShareButton"
    *           data-bi-bhvr="SOCIALSHARE" data-bi-name="facebook"
    *           data-bi-slot="1"
    *           href="https://web.archive.org/web/20190411221943/https://www.facebook.com/sharer.php?u=https://support.office.com/en-us/article/install-skype-for-business-8a0d4da8-9d58-44f9-9759-5c8f340cb3fb"
    *           id="ocFacebookButton" ms.cmpgrp="Share"
    *           ms.ea_action="Goto"ms.ea_offer="SOC" ms.interactiontype="1"
    *           ms.pgarea="Body" target="_blank">
    *           <img alt="Facebook" class="ocArticleFooterImage" ms.cmpgrp=
    *           "content" ms.pgarea="Body" src=
    *           "/web/20190411221943im_/https://support.office.com/Images/SOC-Facebook.png">
    *           </a> Then the <a> tag would be x.y and its href would be x.y~1
    *           and the <img> would be x.y.1 and its src would be x.y.1~1. For
    *           text between tags we could use a carat as a separator.
    * @param searchLevel
    *           signals to stop dom walking if we have reached the search level,
    *           and return the node we are on. If null, no interuption occurs.
    * @return The potential output string.
    */
   public String getInlineContent(NodeHandler currentNode, Element el,
      boolean undoLeadingEscapes, ProvenanceWriter pw, String baseUri, String domain,
      String level, String searchLevel, Set<Node> foundNodes) {
      BlockWriter oldOutput = output;
      output = BlockWriter.create(1000);
      Node result = walkNodes(currentNode, el, inlineNodes, pw, baseUri, domain, level,
         searchLevel);
      if (result != null) {
         foundNodes.add(result);
      }
      String ret = output.toString();
      output = oldOutput;
      if (undoLeadingEscapes) {
         ret = cleaner.unescapeLeadingCharacters(ret);
      }
      return ret;
   }

   /**
    * Adds a link to the link set, and returns the actual ID for the link.
    *
    * @param url
    *           URL for link
    * @param recommendedName
    *           A recommended name for non-simple link IDs. This might be
    *           modified.
    * @param image
    *           If true, use "img-" instead of "link-" for simple link IDs.
    * @return The actual link ID for this URL.
    */
   public String addLink(String url, String recommendedName, boolean image) {
      String linkId;
      // remove embedded newline
      int nlIndex = url.indexOf("\n");
      if (nlIndex > 0 && nlIndex < url.length()-1) {
         String tmp = url.substring(0,nlIndex);
         url = tmp+url.substring(nlIndex+1);
      }
      if (linkUrls.containsKey(url)) {
         linkId = linkUrls.get(url);
      } else {
         if (options.simpleLinkIds) {
            linkId = (image ? "image-" : "")
               + String.valueOf(linkUrls.size() + 1);
         } else {
            recommendedName = cleanLinkId(url, recommendedName, image);
            if (linkIds.containsKey(recommendedName)) {
               int incr = 1;
               while (linkIds
                  .containsKey(String.format("%s %d", recommendedName, incr))) {
                  incr++;
               }
               recommendedName = String.format("%s %d", recommendedName, incr);
            }
            linkId = recommendedName;
         }
         linkUrls.put(url, linkId);
         linkIds.put(linkId, url);
      }
      return linkId;
   }

   /**
    * Adds an abbreviation to the abbreviation set.
    * 
    * @param abbr
    *           The abbreviation to be used
    * @param definition
    *           The definition for the abbreviation, should NOT be pre-escaped.
    */
   void addAbbreviation(String abbr, String definition) {
      if (!abbreviations.containsKey(abbr)) {
         abbreviations.put(abbr, definition);
      }
   }

   String cleanLinkId(String url, String linkId, boolean image) {
      // no newlines
      String ret = linkId.replace('\n', ' ');
      // multiple spaces should be a single space
      ret = LINK_MULTIPLE_SPACES.matcher(ret).replaceAll(" ");
      // remove all characters except letters, numbers, spaces, and some basic
      // punctuation
      ret = LINK_SAFE_CHARS.matcher(ret).replaceAll(LINK_REPLACEMENT);
      // replace multiple underscores with a single underscore
      ret = LINK_MULTIPLE_REPLACE.matcher(ret).replaceAll(LINK_REPLACEMENT);
      // replace underscores on the left or right with nothing
      ret = LINK_EDGE_REPLACE.matcher(ret).replaceAll("");
      // trim any leading or trailing spaces
      ret = ret.trim();
      if (ret.length() == 0 || ret.equals(LINK_REPLACEMENT)) {
         // if we have nothing usable left, use a generic ID
         if (image) {
            if (url != null) {
               Matcher m = LINK_FILENAME.matcher(url);
               if (m.find()) {
                  ret = cleanLinkId(null, m.group(1), true);
               } else {
                  genericImageUrlCounter++;
                  ret = "Image " + genericImageUrlCounter;
               }
            } else {
               genericImageUrlCounter++;
               ret = "Image " + genericImageUrlCounter;
            }
         } else {
            genericLinkUrlCounter++;
            ret = "Link " + genericLinkUrlCounter;
         }
      } // else, use the cleaned id
      return ret;
   }
}
