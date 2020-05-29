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

package com.mdfromhtml.services;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLConstants;
import com.mdfromhtml.core.MDfromHTMLUtils;

/**
 * Implementation of static initializers for use by the generic ILD Services as
 * a Singleton to avoid initializing on every request
 *
 */
@Singleton
@ManagedBean
public class ServicesManager implements Serializable {

   static public boolean debug = false; // true;

   static public JSONObject deleteRequests = new JSONObject();

   static public JSONObject getRequests = new JSONObject();

   static public JSONObject postRequests = new JSONObject();

   public static JSONObject putRequests = new JSONObject();

   static protected int responseToken = 0;

   private static final long serialVersionUID = -2206805773040898651L;

   /**
    * static initializer to read the services expected to be performed by the
    * application that injects it
    */
   static {
      if (debug) {
         System.out.println("Initializing Service");
      }
      try {
         JSONObject services = MDfromHTMLUtils.loadJSONFile(
            MDfromHTMLConstants.MDfromHTML_DIR_PROPERTIES+
            MDfromHTMLConstants.MDfromHTML_SVCS_SCHEMA_FILENAME);
         // ensure this is a valid ILD schema
         JSONObject schema = (JSONObject) services.get("schema");
         if (schema == null) {
            throw new Exception("Request is missing schema object.");
         }
         JSONObject MDfromHTMLSchema = (JSONObject) schema.get("MDfromHTML");
         if (MDfromHTMLSchema == null) {
            throw new Exception("Request is missing MDfromHTML object.");
         }
         JSONObject requests = (JSONObject) MDfromHTMLSchema.get("requests");
         if (requests == null || requests.size() == 0) {
            throw new Exception(
               "Can not find requests for the MDfromHTML schema in "
                  + MDfromHTMLConstants.MDfromHTML_SVCS_SCHEMA_FILENAME);
         }
         getRequests = (JSONObject) requests.get("get");
         if (getRequests == null) {
            throw new Exception(
               "Can not find get requests for the MDfromHTML schema in "
                  + MDfromHTMLConstants.MDfromHTML_SVCS_SCHEMA_FILENAME);
         }
         putRequests = (JSONObject) requests.get("put");
         if (putRequests == null) {
            throw new Exception(
               "Can not find put requests for the MDfromHTML schema in "
                  + MDfromHTMLConstants.MDfromHTML_SVCS_SCHEMA_FILENAME);
         }
         postRequests = (JSONObject) requests.get("post");
         if (postRequests == null) {
            throw new Exception(
               "Can not find post requests for the MDfromHTML schema in "
                  + MDfromHTMLConstants.MDfromHTML_SVCS_SCHEMA_FILENAME);
         }
         deleteRequests = (JSONObject) requests.get("delete");
         if (deleteRequests == null) {
            throw new Exception(
               "Can not find delete requests for the MDfromHTML schema in "
                  + MDfromHTMLConstants.MDfromHTML_SVCS_SCHEMA_FILENAME);
         }
      } catch (Exception e) {
         throw new Error(
            "Can not initialize ServicesManager due to errors:  "
               + e.getMessage(),
            e);
      }
   }

   static protected Set<String> getPublishTopics(JSONObject reqType)
      throws Exception {
      Set<String> retSet = new HashSet<String>();
      if (reqType == null) {
         throw new Exception("null request type");
      }
      Set<?> keys = reqType.keySet();
      for (Object key : keys) {
         JSONObject request = (JSONObject) reqType.get(key);
         if (request == null) {
            throw new Exception(
               "Can not get reqeust for key=" + key);
         }
         if (debug) {
            System.out.println("Processing request \"" + key + "\"");
         }
         JSONArray actions = (JSONArray) request.get("actions");
         if (actions == null) {
            throw new Exception(
               "Can not find actions in request for key=" + key);
         }
         for (Iterator<?> it = actions.iterator(); it.hasNext();) {
            JSONObject action = (JSONObject) it.next();
            JSONArray pubArray = (JSONArray) action.get("publish");
            // okay for publish to be optionally present
            if (pubArray != null) {
               for (Iterator<?> itPub = pubArray.iterator(); itPub.hasNext();) {
                  JSONObject pubObj = (JSONObject) itPub.next();
                  if (pubObj != null) {
                     Object topicName = pubObj.get("topic");
                     if (topicName != null) {
                        retSet.add(topicName.toString());
                     }
                  }
               }
            }
         }
      }

      return retSet;
   }

   static synchronized public int getResponseToken() {
      return ++responseToken;
   }

   static public void main(String... args) {
      System.out.println("ServicesManager");
   }

   public static void performAction(String verb, JSONObject request, JSONObject action,
      JSONObject actionResponse) throws Exception {
      // check inputs
      if (request == null) {
         throw new Exception("null request received.");
      }
      if (debug) {
         System.out.println("Request: " + request.toString());
      }
      if (action == null) {
         throw new Exception("null action received.");
      }
      if (debug) {
         System.out.println("Action: " + action.toString());
      }
      if (actionResponse == null) {
         throw new Exception(
            "null actionResponse received.");
      }
      if (debug) {
         System.out.println("Action Response: " + actionResponse.toString());
      }
      // parse the action into its components
      String className = (String) action.get("class"); // must exist
      if (className == null) {
         throw new Exception("Action is missing a class name.");
      }
      if (debug) {
         System.out.println("Loading class " + className);
      }
      Class<?> cls = null;
      try {
         cls = Class.forName(className);
      } catch (ClassNotFoundException e) {
         if (debug) {
            System.err.println("Can not load class \"" + className + "\"");
         }
         throw new Exception(
            "Can not load class \"" + className + "\"", e);
      }
      String methodName = (String) action.get("method"); // may be null
      // get parameter class names
      JSONArray paramArray = (JSONArray) action.get("params"); // may be null
      Method method = null;
      StringBuffer sb = new StringBuffer();
      String retKey = (String) action.get("return"); // may be null..
      if (paramArray != null) {
         List<String> paramDefs = new ArrayList<String>();
         try {
            List<Class<?>> paramClasses = new ArrayList<Class<?>>();
            // now get the other parameters for the method
            for (Iterator<?> it = paramArray.iterator(); it.hasNext();) {
               String param = it.next().toString();
               paramDefs.add(param);
               String[] paramParts = param.split(":");
               if (paramParts.length == 1) {
                  paramClasses.add(String.class);
                  sb.append(String.class.getName());
                  continue;
               }
               if (paramParts.length != 2) {
                  throw new Exception(
                     className+":"+methodName+": Malformed parameter definition. Should either be a single String or a String:ClassName. \""
                        + param + "\"");
               }
               String paramClassName = param.split(":")[1].toString();
               sb.append(paramClassName);
               sb.append(",");
               Class<?> paramCls = null;

               if (debug) {
                  System.out
                     .println(className+":"+methodName+": Loading Parameter class " + paramClassName);
               }
               try {
                  paramCls = Class.forName(paramClassName);
               } catch (ClassNotFoundException e) {
                  throw new Exception(
                     className+":"+methodName+": Can not load class named \"" + paramClassName + "\"", e);
               }
               paramClasses.add(paramCls);
            }
            // collect the data values
            List<Object> paramData = ParameterUtils.getParameterData(
               request, className, methodName, paramDefs.toArray(new String[0]));
            // see if the class has this method
            if (methodName != null) {
               StringBuffer sbCls = new StringBuffer();
               try {
                  /**
                   * Put in these classes in reverse to have method like
                   * (x, y, z) parameters
                   */
                  for (Class<?> paramCls : paramClasses) {
                     sbCls.append(",");
                     sbCls.append(paramCls.getName());
                  }
                  if (debug) {
                     System.out.print(
                        "Loading method " + className + "." + methodName + "(");
                     System.out.print(sbCls);
                     System.out.println(")");
                  }
                  method = cls.getMethod(methodName,
                     paramClasses.toArray(new Class<?>[0]));
               } catch (Exception e) {
                  if (debug) {
                     System.err.println(className+":"+methodName+": Can not find method named \""
                        + methodName + "\" with parameters "
                        + sbCls.substring(0, sbCls.length() - 1) + " in class "
                        + cls.getName());
                  }
                  throw new Exception(className+":"+methodName+": Can not find method named \""
                     + methodName + "\" with parameters "
                     + sb.substring(0, sb.length() - 1) + " in class "
                     + cls.getName());
               }
               // try to execute the method
               method.setAccessible(true);
               Object retVal = null;
               try {
                  retVal = method.invoke(null,
                     paramData.toArray(new Object[0]));
               } catch (Exception e) {
                  StringBuffer sbData = new StringBuffer();
                  for (Object obj : paramData) {
                     if (obj != null) {
                        sbData.append(obj.toString());
                     } else {
                        sbData.append("null");
                     }
                     sbData.append(",");
                  }
                  throw new Exception(
                     className+":"+methodName+": Error invoking method \"" + methodName
                        + "\" with parameters "
                        + sbData.substring(0, sbData.length() - 1)+ "   Cause: " + e.getCause(),
                     e);
               }
               if (retKey != null) {
                  // check to see if the retKey is already in the retVal and return
                  // it
                  if (retVal instanceof JSONObject) {
                     if (((JSONObject) retVal).get(retKey) != null) {
                        actionResponse.put(retKey, ((JSONObject) retVal).get(retKey));
                     } else {
                        actionResponse.put(retKey, retVal);
                     }
                  } else {
                     // store whatever was returned
                     actionResponse.put(retKey, retVal);
                  }
               }
            }

         } catch (Exception e) {
            throw new Exception(e.getLocalizedMessage(), e);
         }
      } else {
         try {
            // default is to always have the sessionID and userID passed
            method = cls.getMethod(methodName);
            // try to execute the method
            method.setAccessible(true);
            Object retVal = method.invoke(null);
            if (retKey != null) {
               // check to see if the retKey is already in the retVal and return
               // it
               if (retVal instanceof JSONObject) {
                  if (((JSONObject) retVal).get(retKey) != null) {
                     actionResponse.put(retKey, ((JSONObject) retVal).get(retKey));
                  } else {
                     actionResponse.put(retKey, retVal);
                  }
               } else {
                  // store whatever was returned
                  actionResponse.put(retKey, retVal);
               }
            }
         } catch (Exception e) {
            throw new Exception("Can not find method named \""
               + methodName + "\" in class " + cls.getName());
         }
      }
   }

   public ServicesManager() {
   }

   @PreDestroy
   public void cleanup() {
      // cleaning up resources
   }

   @PostConstruct
   public void startup() {
      // initialize the connection pools, etc
      if (debug) {
         System.out.println("Creating Connection Pool");
      }

      if (debug) {
         System.out.println("Opening IPC");
      }
   }
}
