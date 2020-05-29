/**
 * (c) Copyright 2020 IBM Corporation
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

package com.mdfromhtml.core;

import java.util.Properties;

/**
 * Loads the MDfromHTML properties and provides accessors to their values
 *
 */

public class MDfromHTMLPropertyManager {

   static public Properties MDfromHTMLProps;
   static {
      try {
         reloadProperties();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   static public String getApplicationName() {
      return MDfromHTMLConstants.APPLICATION_NAME;
   }
   
   static public Properties getServiceProperties() {
      return (Properties)MDfromHTMLProps.clone();
   }
   
   static public String getHostName() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_HOST_NAME_PROP,"localhost");
   }

   static public String getLoggerConsoleLevel() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_LOGGER_CONSOLE_LEVEL,"SEVERE");
   }

   static public String getLoggerDirectory() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_LOGGER_DIRECTORY,"logs");
   }

   static public String getLoggerFileLevel() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_LOGGER_FILE_LEVEL,"FINE");
   }

   static public String getLoggerMaxNumber() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_LOGGER_MAX_NUMBER,"3");
   }

   static public String getLoggerMaxSize() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_LOGGER_MAX_SIZE,"5000000");
   }

   static public String getLoggerStemName() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_LOGGER_STEM_NAME,"MDfromHTML");
   }

   static public String getPortNumber() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_PORT_PROP,"9080");
   }

   static public String getProtocol() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_PROTOCOL_PROP,"http");
   }

   static public String getServletName() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_SERVLET_NAME_PROP,"MDfromHTMLWebServices");
   }

   static public String getVersion() {
      return MDfromHTMLProps.getProperty(MDfromHTMLConstants.MDfromHTML_SVCS_VERSION_PROP,"v1");
   }

   static public void reloadProperties() throws Exception {
      MDfromHTMLProps = MDfromHTMLUtils.getMDfromHTMLServicesProps();
   }

   public MDfromHTMLPropertyManager() {
      
   }
}
