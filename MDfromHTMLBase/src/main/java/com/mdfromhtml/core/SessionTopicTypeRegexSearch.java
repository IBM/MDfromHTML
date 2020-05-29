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

/**
 * Bean for filters for getMessages
 *
 */

public class SessionTopicTypeRegexSearch {
	MDfromHTMLID _sessionID = MDfromHTMLID.UNDEFINED_ID;
	String _topicRegex = MDfromHTMLConstants.UNDEFINED_String;
	String _typeRegex = MDfromHTMLConstants.UNDEFINED_String;

	/**
	 * Constructor
	 * 
	 * @param sessionID
	 *                   the identity of the session to be matched, or a null or an
	 *                   {@link MDfromHTMLID#UNDEFINED_ID} to allow any to match
	 * @param topicRegex
	 *                   the regular expression used to test the message topic to
	 *                   find a match, or a null or
	 *                   {@link MDfromHTMLConstants#UNDEFINED_String} to allow any
	 *                   match
	 * @param typeRegex
	 *                   the regular expression used to test the message type to
	 *                   find a match, or a null or
	 *                   {@link MDfromHTMLConstants#UNDEFINED_String} to allow any
	 *                   match
	 */
	public SessionTopicTypeRegexSearch(MDfromHTMLID sessionID, String topicRegex, String typeRegex) {
		if (MDfromHTMLID.isUndefined(sessionID) == false) {
			_sessionID = sessionID;
		}
		if (MDfromHTMLUtils.isUndefined(topicRegex) == false) {
			_topicRegex = topicRegex;
		}
		if (MDfromHTMLUtils.isUndefined(typeRegex) == false) {
			_typeRegex = typeRegex;
		}
	}

	/**
	 * 
	 * @return {@link MDfromHTMLID} of desired session
	 */
	public MDfromHTMLID getSessionID() {
		return _sessionID;
	}

	/**
	 * 
	 * @return desired topicRegex
	 */
	public String getTopicRegex() {
		return _topicRegex;
	}

	/**
	 * 
	 * @return desired typeRegex
	 */
	public String getTypeRegex() {
		return _typeRegex;
	}

	/**
	 * Determines whether the supplied msg matches the search criteria of this
	 * object
	 * 
	 * @param msg
	 * @return true if any of the required components are undefined or what is
	 *         supplied matches what is in the message
	 */
	public boolean isMessageWanted(IMDfromHTMLIPCMessage msg) {
		if (MDfromHTMLUtils.isUndefined(_sessionID) || msg.getSessionID().equals(_sessionID)) {
			if (MDfromHTMLUtils.isUndefined(_topicRegex) || msg.getTopic().matches(_topicRegex)) {
				if (MDfromHTMLUtils.isUndefined(_typeRegex) || msg.getType().matches(_typeRegex)) {
					return true;
				}
			}
		}
		return false;
	}
}
