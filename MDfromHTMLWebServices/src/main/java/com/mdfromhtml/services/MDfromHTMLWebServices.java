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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Iterator;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLUtils;
import com.mdfromhtml.services.Patch.PATCH;

@SuppressWarnings("serial")
@Path("/v1/")

/**
 * Description Here
 *
 */

public class MDfromHTMLWebServices implements Serializable {

   static public final String ACTION = "action";
   static public final String ACTION_COMMAND = "actionCommand";

   static public final String ACTION_TYPE = "actionType";
   static public final String ACTIONS = "actions";
   static public final String ACTIVITIES = "activities";
   static public final String ACTIVITY = "activity";
   static public final String ACTIVITY_GRAPH = "activityGraph";
   static public final String ACTIVITY_ID = "activityId";
   static public final String ARRAY = "array";
   static public final String BODY = "body";
   static public final String CHILDREN = "children";
   static public final String COLS = "Cols";
   static public final String COMPLETE = "complete";
   static public final String CONSTANT = "constant";
   static final boolean debug = true; // false for no _debug messages
   static public final String DELETE = "delete";
   static public final String DETAILS = "details";
   static public final String ERRANT_JSON_STRING = "JSON Generation Error: []";
   static public final String GET = "get";
   static public final String GOAL = "goal";
   static public final String GOALS = "goals";
   static public final String INPUTS = "inputs";
   static public final String JSON_REQUEST = "jsonRequest";
   static public final String NAME = "name";
   static public final String OBJID = "id";
   static public final String OPTOINS = "options";
   static public final String OUTPUTS = "outputs";
   static public final String PARAMETERS = "parameters";
   static public final String PARENT_ID = "parentId";
   static public final String PATCH = "patch";
   static public final String PATH = "path";
   static public final String PLAN = "plan";
   static public final String PLANNING_STAGE = "planningStage";
   static public final String POST = "post";
   static public final String PRECONDITIONS = "preconditions";
   static public final String PUB_VERB = "pubVerb";
   static public final String PUBLISH = "publish";
   static public final String PUT = "put";
   static public final String QUERIES = "queries";
   static public final String QUERY = "query";
   static public final String QUERY_NAME = "queryName";
   static public final String QUERY_RESULT = "queryResult";
   static public final String REL_INCOMING = "rel_incoming";
   static public final String REL_OUTGOING = "rel_outgoing";
   static public final String RESPONSE = "response";
   static public final String RESPONSE_TOKEN = "responseToken";
   static public final String RESULTS = "results";
   static public final String RETURN = "return";
   static public final String RETURN_VALUES = "returnValues";
   static public final String ROWS = "Rows";
   static public final String SCHEMA = "schema";
   static public final String SERVICE_URI_STEM = "serviceURIStem";
   static public final String STATUS = "status";
   static public final String TOPIC = "topic";
   static public final String TYPE = "type";
   static public final String USER_ID = "userId";
   static public final String VALUE = "value";
   static public final String WAITING = "waiting";

   @Inject
   public ServicesManager _servicesManager;

   @DELETE
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("{topic}/{type}")
   public Response doDeleteV1(@Context HttpHeaders headers,
      @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
      @PathParam(TYPE) String type, InputStream jsonRequest) {
      JSONObject request = null;
      try {
         request = JSONObject.parse(jsonRequest);
      } catch (IOException e) {
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_INVALID_JSON_GET_REQUEST);
      }

      // based on the uri we received, parse out the reqTopic and reqType
      try {
         URI uri = uriInfo.getAbsolutePath();
         String path = uri.getPath();
         String requestType = topic;
         if (MDfromHTMLUtils.isUndefined(type) == false) {
            requestType += "/" + type;
         }
         JSONObject serviceLogic = (JSONObject) ServicesManager.deleteRequests
            .get(requestType);
         if (serviceLogic == null) {
            return MDfromHTMLServiceUtil.getErrorResponse(
               "No DELETE service registered for \"" + requestType
                  + "\" for path \"" + path + "\"",
               MDfromHTMLResponseCodes.MDfromHTML_CLASS_NOT_FOUND);
         }
         // get what is to be returned
         JSONObject actionResponses = new JSONObject();
         // execute the actions
         JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
         for (Iterator<?> it = actions.iterator(); it.hasNext();) {
            JSONObject action = (JSONObject) it.next();
            ServicesManager.performAction(DELETE, request,
               action, actionResponses);
         }
         return MDfromHTMLServiceUtil.getResponse(actionResponses);
      } catch (Exception e) {
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_UNEXPECTED_ERROR);
      }
   }

   @GET
   // @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("{topic}/{jsonRequest}")
   public Response doGetV1(@Context HttpHeaders headers,
      @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
      @PathParam(JSON_REQUEST) String jsonRequest) {
      JSONObject request = new JSONObject();
      if (jsonRequest.startsWith("{") && jsonRequest.endsWith("}")) {
         try {
            request = JSONObject.parse(jsonRequest);
         } catch (IOException e) {
            return MDfromHTMLServiceUtil.getErrorResponse(e,
               MDfromHTMLResponseCodes.MDfromHTML_INVALID_JSON_GET_REQUEST);
         }
      }

      // based on the uri we received, parse out the reqTopic and reqType
      try {
         URI uri = uriInfo.getAbsolutePath();
         String path = uri.getPath();
         JSONObject serviceLogic = (JSONObject) ServicesManager.getRequests
            .get(topic);
         if (serviceLogic == null) {
            return MDfromHTMLServiceUtil.getErrorResponse(
               "No GET service registered for \"" + topic + "\" for path \""
                  + path + "\"",
               MDfromHTMLResponseCodes.MDfromHTML_CLASS_NOT_FOUND);
         }
         // get what is to be returned
         JSONObject actionResponses = new JSONObject();
         // execute the actions
         JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
         for (Iterator<?> it = actions.iterator(); it.hasNext();) {
            JSONObject action = (JSONObject) it.next();
            ServicesManager.performAction(GET, request, action,
               actionResponses);
         }
         return MDfromHTMLServiceUtil.getResponse(actionResponses);
      } catch (Exception e) {
         // return MDfromHTMLServiceUtil.getErrorResponse(e,
         // MDfromHTMLResponseCodes.MDfromHTML_UNEXPECTED_ERROR);
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_SESSION_ID_NOT_FOUND);
      }
   }

   @GET
   // @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("{topic}/{type}/{jsonRequest}")
   public Response doGetV1(@Context HttpHeaders headers,
      @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
      @PathParam(TYPE) String type,
      @PathParam(JSON_REQUEST) String jsonRequest) {
      JSONObject request = null;
      try {
         request = JSONObject.parse(jsonRequest);
      } catch (IOException e) {
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_INVALID_JSON_GET_REQUEST);
      }

      // based on the uri we received, parse out the reqTopic and reqType
      try {
         URI uri = uriInfo.getAbsolutePath();
         String path = uri.getPath();
         JSONObject serviceLogic = (JSONObject) ServicesManager.getRequests
            .get(topic + "/" + type);
         if (serviceLogic == null) {
            return MDfromHTMLServiceUtil.getErrorResponse(
               "No GET service registered for \"" + topic + "/" + type
                  + "\" for path \"" + path + "\"",
               MDfromHTMLResponseCodes.MDfromHTML_CLASS_NOT_FOUND);
         }
         // get what is to be returned
         JSONObject actionResponses = new JSONObject();
         // execute the actions
         JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
         for (Iterator<?> it = actions.iterator(); it.hasNext();) {
            JSONObject action = (JSONObject) it.next();
            ServicesManager.performAction(GET, request, action,
               actionResponses);
         }
         return MDfromHTMLServiceUtil.getResponse(actionResponses);
      } catch (Exception e) {
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_UNEXPECTED_ERROR);
      }
   }

   @GET
   // @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("{topic}")
   public Response doGetV1NoParams(@Context HttpHeaders headers,
      @Context UriInfo uriInfo, @PathParam(TOPIC) String topic) {
      JSONObject request = new JSONObject();

      // based on the uri we received, parse out the reqTopic and reqType
      try {
         URI uri = uriInfo.getAbsolutePath();
         String path = uri.getPath();
         JSONObject serviceLogic = (JSONObject) ServicesManager.getRequests
            .get(topic);
         if (serviceLogic == null) {
            return MDfromHTMLServiceUtil.getErrorResponse(
               "No GET service registered for \"" + topic + "\" for path \""
                  + path + "\"",
               MDfromHTMLResponseCodes.MDfromHTML_CLASS_NOT_FOUND);
         }
         // get what is to be returned
         JSONObject actionResponses = new JSONObject();
         // execute the actions
         JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
         for (Iterator<?> it = actions.iterator(); it.hasNext();) {
            JSONObject action = (JSONObject) it.next();
            ServicesManager.performAction(GET, request, action,
               actionResponses);
         }
         return MDfromHTMLServiceUtil.getResponse(actionResponses);
      } catch (Exception e) {
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_UNEXPECTED_ERROR);
      }
   }

   @PATCH
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("{topic}/{type}")
   public Response doPatchV1(@Context HttpHeaders headers,
      @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
      @PathParam(TYPE) String type, InputStream jsonRequest) {
      JSONObject request = null;
      try {
         request = JSONObject.parse(jsonRequest);
      } catch (IOException e) {
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_INVALID_JSON_GET_REQUEST);
      }

      // based on the uri we received, parse out the reqTopic and reqType
      try {
         URI uri = uriInfo.getAbsolutePath();
         String path = uri.getPath();
         String requestType = topic;
         if (MDfromHTMLUtils.isUndefined(type) == false) {
            requestType += "/" + type;
         }
         JSONObject serviceLogic = (JSONObject) ServicesManager.putRequests
            .get(requestType);
         if (serviceLogic == null) {
            return MDfromHTMLServiceUtil.getErrorResponse(
               "No PATCH service registered for \"" + requestType
                  + "\" for path \"" + path + "\"",
               MDfromHTMLResponseCodes.MDfromHTML_CLASS_NOT_FOUND);
         }
         // get what is to be returned
         JSONObject actionResponses = new JSONObject();
         // execute the actions
         JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
         for (Iterator<?> it = actions.iterator(); it.hasNext();) {
            JSONObject action = (JSONObject) it.next();
            ServicesManager.performAction(PATCH, request,
               action, actionResponses);
         }
         return MDfromHTMLServiceUtil.getResponse(actionResponses);
      } catch (Exception e) {
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_UNEXPECTED_ERROR);
      }
   }

   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("{topic}/{type}")
   public Response doPostV1(@Context HttpHeaders headers,
      @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
      @PathParam(TYPE) String type, InputStream jsonRequest) {
      JSONObject request = null;
      try {
         request = JSONObject.parse(jsonRequest);
      } catch (IOException e) {
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_INVALID_JSON_GET_REQUEST);
      }

      // based on the uri we received, parse out the reqTopic and reqType
      try {
         URI uri = uriInfo.getAbsolutePath();
         String path = uri.getPath();
         String requestType = topic;
         if (MDfromHTMLUtils.isUndefined(type) == false) {
            requestType += "/" + type;
         }
         JSONObject serviceLogic = (JSONObject) ServicesManager.postRequests
            .get(requestType);
         if (serviceLogic == null) {
            return MDfromHTMLServiceUtil.getErrorResponse(
               "No POST service registered for \"" + requestType
                  + "\" for path \"" + path + "\"",
               MDfromHTMLResponseCodes.MDfromHTML_CLASS_NOT_FOUND);
         }
         // get what is to be returned
         JSONObject actionResponses = new JSONObject();
         // execute the actions
         JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
         for (Iterator<?> it = actions.iterator(); it.hasNext();) {
            JSONObject action = (JSONObject) it.next();
            ServicesManager.performAction(POST, request,
               action, actionResponses);
         }
         return MDfromHTMLServiceUtil.getResponse(actionResponses);
      } catch (Exception e) {
         if (debug) {
            e.printStackTrace(System.err);
         }
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_UNEXPECTED_ERROR);
      }
   }

   @PUT
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("{topic}/{type}")
   public Response doPutV1(@Context HttpHeaders headers,
      @Context UriInfo uriInfo, @PathParam(TOPIC) String topic,
      @PathParam(TYPE) String type, InputStream jsonRequest) {
      JSONObject request = null;
      try {
         request = JSONObject.parse(jsonRequest);
      } catch (IOException e) {
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_INVALID_JSON_GET_REQUEST);
      }

      // based on the uri we received, parse out the reqTopic and reqType
      try {
         URI uri = uriInfo.getAbsolutePath();
         String path = uri.getPath();
         String requestType = topic;
         if (MDfromHTMLUtils.isUndefined(type) == false) {
            requestType += "/" + type;
         }
         JSONObject serviceLogic = (JSONObject) ServicesManager.putRequests
            .get(requestType);
         if (serviceLogic == null) {
            return MDfromHTMLServiceUtil.getErrorResponse(
               "No PUT service registered for \"" + requestType
                  + "\" for path \"" + path + "\"",
               MDfromHTMLResponseCodes.MDfromHTML_CLASS_NOT_FOUND);
         }
         // get what is to be returned
         JSONObject actionResponses = new JSONObject();
         // execute the actions
         JSONArray actions = (JSONArray) serviceLogic.get(ACTIONS);
         for (Iterator<?> it = actions.iterator(); it.hasNext();) {
            JSONObject action = (JSONObject) it.next();
            ServicesManager.performAction(PUT, request, action,
               actionResponses);
         }
         return MDfromHTMLServiceUtil.getResponse(actionResponses);
      } catch (Exception e) {
         return MDfromHTMLServiceUtil.getErrorResponse(e,
            MDfromHTMLResponseCodes.MDfromHTML_UNEXPECTED_ERROR);
      }
   }
}

