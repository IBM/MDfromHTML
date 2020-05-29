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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import javax.management.modelmbean.InvalidTargetObjectTypeException;

import com.api.json.JSON;
import com.api.json.JSONArray;
import com.api.json.JSONArtifact;
import com.api.json.JSONObject;

/**
 * Utility methods commonly used throughout the MDfromHTML Project
 */
public class MDfromHTMLUtils implements Serializable {

   static final public Charset UTF8_CHARSET = Charset.forName("UTF-8");

   static public final int iDayMilliseconds = 86400000;

   static public final int iHourMilliseconds = 3600000;

   static public final int iMinuteMilliseconds = 60000;

   static private final long serialVersionUID = 8109772978213632637L;

   /**
    * Utility method to create a list of the content of an ArrayList by
    * converting each object to its toString() representation and appending it
    * within a list using the supplied delimiter. If a String is encountered in
    * the ArrayList, it will be quoted (e.g., surrounded by double quote marks).
    * The list itself is enclosed by an opening ('{') and closing ('}') brace.
    * 
    * Note: there is no escaping of the characters in a String object, so if it
    * contains a delimiter or a closing brace, you may get unexpected results.
    * 
    * @param list
    *           the array list of objects to be converted to a list string.
    * @return the list comprising an opening brace, then each object in the
    *         arraylist converted to a string, followed by the closing brace,
    *         with the caveat that string objects encountered in the arraylist
    *         are enclosed in a pair of double quotes.
    * @see #listStringToArrayList
    */
   static public String arrayListToListString(ArrayList<?> list) {
      // WNM3: add support for Int[], Double[], Float[], Long[], String[]
      // WNM3: escape the Strings?
      if (list == null || list.size() == 0) {
         return "{}"; // empty string
      }
      String strDelimiter = ",";
      StringBuffer sb = new StringBuffer();
      sb.append("{");
      Object item = null;
      for (int i = 0; i < list.size(); i++) {
         if (i != 0) {
            sb.append(strDelimiter);
         }
         item = list.get(i);
         if (item instanceof Integer) {
            sb.append(item.toString());
         } else if (item instanceof Long) {
            sb.append(item.toString());
         } else if (item instanceof Double) {
            sb.append(item.toString());
         } else if (item instanceof Float) {
            sb.append(item.toString());
         } else if (item instanceof String) {
            sb.append("\"");
            sb.append(item.toString());
            sb.append("\"");
         } else {
            // WNM3: not sure what to do here... hex of serialized?
            sb.append("\"");
            sb.append(item.toString());
            sb.append("\"");
         }
      }
      sb.append("}");
      return sb.toString();
   }

   /**
    * Converts a byte into an array of char's containing the hexadecimal digits.
    * For example, 0x2f would return char[] {'2','F'}
    * 
    * @param bIn
    *           byte to be converted to hexadecimal digits
    * @return array of chars containing the hexadecimal digits for the value of
    *         the input byte.
    */
   static public char[] byteToHexChars(byte bIn) {
      char[] cOut = new char[2];
      int iMasker = bIn & 0x000000ff;
      int iMaskerHigh = iMasker & 0x000000f0;
      iMaskerHigh = iMaskerHigh >> 4;
      int iMaskerLow = iMasker & 0x0000000f;
      if (iMaskerHigh > 9) {
         iMaskerHigh = iMaskerHigh + 'A' - 10;
      } else {
         iMaskerHigh = iMaskerHigh + '0';
      }
      if (iMaskerLow > 9) {
         iMaskerLow = iMaskerLow + 'A' - 10;
      } else {
         iMaskerLow = iMaskerLow + '0';
      }
      cOut[0] = (char) iMaskerHigh;
      cOut[1] = (char) iMaskerLow;
      return cOut;
   }

   /**
    * Set up filter if the supplied word contains an @ like in an email address,
    * or starts or ends with a number, or contains http
    * 
    * @param word
    *           value to be tested
    * @return true if the word should be filtered
    */
   static public boolean checkWordFilter(String word) {
      if (word == null) {
         return true;
      }
      word = word.trim();
      if (word.length() == 0) {
         return true;
      }
      // check for email pattern
      if (word.contains("@")) {
         return true;
      }
      // check for http pattern
      if (word.toLowerCase().contains("http")) {
         return true;
      }
      // check for non-words that begin with a number
      char wordChar = word.charAt(0);
      if (0x0030 <= wordChar && wordChar <= 0x0039) {
         return true;
      }
      
      // check for non-words that end with a number
      wordChar = word.charAt(word.length()-1);
      if (0x0030 <= wordChar && wordChar <= 0x0039) {
         return true;
      }
      return false;
   }

   /**
    * Removes HTML Tags from the supplied text, replacing them with a space
    * (0x20)
    * 
    * @param text
    *           the string to be cleansed
    * @return the cleansed string
    */
   static public String cleanseHTMLTagsFromText(String text) {
      if (text == null || text.trim().length() == 0) {
         return "";
      }
      text = text.toLowerCase();
      // remove newlines
      text = text.replaceAll("\n", " ");
      text = text.replaceAll("\r", " ");
      // remove html tags
      text = text.replaceAll("\\<ul\\>", " ");
      text = text.replaceAll("\\</ul\\>", " ");
      text = text.replaceAll("\\<br\\>", " ");
      text = text.replaceAll("\\</br\\>", " ");
      text = text.replaceAll("\\<br/\\>", " ");
      text = text.replaceAll("\\<li\\>", " ");
      text = text.replaceAll("\\</li\\>", " ");
      text = text.replaceAll("\\<acc\\-body\\>", " ");
      text = text.replaceAll("\\</acc\\-body\\>", " ");
      text = text.replaceAll("\\<acc\\-header\\>", " ");
      text = text.replaceAll("\\</acc\\-header\\>", " ");
      text = text.replaceAll("\\</a\\>", " ");
      text = text.replaceAll("\\<b\\>", " ");
      text = text.replaceAll("\\</b\\>", " ");
      text = text.replaceAll("\\<i\\>", " ");
      text = text.replaceAll("\\</i\\>", " ");
      text = text.replaceAll("\\<p\\>", " ");
      text = text.replaceAll("\\</p\\>", " ");
      text = text.replaceAll("\\<ol\\>", " ");
      text = text.replaceAll("\\</ol\\>", " ");
      text = text.replaceAll("\\<strong\\>", " ");
      text = text.replaceAll("\\</strong\\>", " ");
      text = text.replaceAll("\\<big\\>", " ");
      text = text.replaceAll("\\</big\\>", " ");
      text = text.replaceAll("\\<font\\>", " ");
      text = text.replaceAll("\\</font\\>", " ");
      text = text.replaceAll("\\<u\\>", " ");
      text = text.replaceAll("\\</u\\>", " ");
      text = text.replaceAll("\\<span\\>", " ");
      text = text.replaceAll("\\</span\\>", " ");
      text = text.replaceAll("\\<table\\>", " ");
      text = text.replaceAll("\\</table\\>", " ");
      text = text.replaceAll("\\<custom-table\\>", " ");
      text = text.replaceAll("\\</custom-table\\>", " ");
      text = text.replaceAll("\\<tbody\\>", " ");
      text = text.replaceAll("\\</tbody\\>", " ");
      text = text.replaceAll("\\<td\\>", " ");
      text = text.replaceAll("\\</td\\>", " ");
      text = text.replaceAll("\\<tr\\>", " ");
      text = text.replaceAll("\\</tr\\>", " ");
      text = text.replaceAll("\\<click\\-question\\>", " ");
      text = text.replaceAll("\\</click\\-question\\>", " ");
      text = text.replaceAll("\\</iframe\\>", " ");
      text = removeTag(text, "<a ");
      text = removeTag(text, "<iframe ");
      text = removeTag(text, "<td ");
      text = removeTag(text, "<tr ");
      text = removeTag(text, "<span ");
      text = removeTag(text, "<table ");
      text = removeTag(text, "<tbody ");
      text = removeTag(text, "<p ");
      text = removeTag(text, "<font ");
      text = removeTag(text, "<gettimeoff ");
      text = removeTag(text, "<deletecontextprivatedata ");
      text = removeTag(text, "<custom-table ");
      text = removeTag(text, "<button ");
      text = removeTag(text, "<setactionstage ");
      text = removeTag(text, "<valuereplace ");
      text = removeTag(text, "<answer ");
      return text;
   }

   /**
    * Cleans trailing characters from the supplied URL based on how URL's might
    * be referenced within dialogs (e.g., removes trailing comma, double quote,
    * question mark, or period, as well as newline, and carriage return.
    * 
    * @param url
    *           the URL to be cleansed
    * @return the cleansed URL
    */
   static public String cleanURL(String url) {
      // Truncate at newlines ("\r", "\n")
      int index = url.indexOf("\r");
      while (index >= 0) {
        url = url.substring(0, index);
        index = url.indexOf("\r");
      }
      index = url.indexOf("\n");
      while (index >= 0) {
        url = url.substring(0, index);
        index = url.indexOf("\n");
      }
      index = url.indexOf("\u00A0");
      if (index >= 0) {
        url = url.substring(0, index);
      }
      // strip any trailing period, comma, double quote
      while (url.endsWith(".") || url.endsWith(",") || url.endsWith("\"") || url.endsWith("!") || url.endsWith("'")
         || url.endsWith("?") || url.endsWith(":") || url.endsWith("]") || url.endsWith(")") || url.endsWith("`")
         || url.endsWith("\\") || url.endsWith("/") || url.endsWith("\r") || url.endsWith("\n") 
         || url.endsWith("\t") || url.endsWith("\u00A0") || url.endsWith("\u2028") 
         || url.endsWith("\u2029") || url.endsWith("\u2019") || url.endsWith("\u201A")) {
         url = url.substring(0, url.length() - 1);
      }
      return url;
   }

   static public String trimSpaces(String word) {
      while (word.startsWith(" ")) {
         word = word.substring(1);
      }
      while (word.endsWith(" ")) {
         word = word.substring(0,word.length()-1);
      }
      return word;
   }
   /**
    * Strip off non-word characters from the beginning and end of the supplied
    * word.
    * 
    * @param word
    *           the word to be cleansed
    * @return array of cleansed word parts: [0] prefix removed from word, [1] cleansed word, [2] suffix removed from word
    */
   static public String[] cleanWord(String word) {
      String[] result = new String[3];
      result[0] = "";
      result[1] = "";
      result[2] = "";
      if (word == null) {
         return result;
      }
      word = trimSpaces(word);
      if (word.length() == 0) {
         result[1] = word;
         return result;
      }
      // clean front
      int index = 0;
      int len = word.length();
      char wordchar = word.charAt(index);
      StringBuffer sb = new StringBuffer();
      // numbers, letters, some quotes, but not @ (to block emails)
      while (wordchar < 0x0030 || (wordchar > 0x0039 && wordchar < 0x0061 && wordchar != 0x0040)
         || (wordchar > 0x007a && wordchar <= 0x007f)
         || wordchar == '\u2003' || wordchar == '\u2013'
         || wordchar == '\u2018' || wordchar == '\u2019' || wordchar == '\u201C' || wordchar == '\u201D' 
         || wordchar == '\u2022' || wordchar == '\u2026' || wordchar == '\u2028' || wordchar == '\u202A'
         || wordchar == '\u202C' || wordchar == '\u202F') {
         sb.append(wordchar);
         index++;
         if (index == len) {
            break;
         }
         wordchar = word.charAt(index);
      }
      result[0] = sb.toString();
      if (index == len) {
         return result;
      }
      word = word.substring(index);
      len = word.length();
      index = len - 1;
      sb.setLength(0); // clear the accumulator
      wordchar = word.charAt(index);
      while (wordchar < 0x0030 || (wordchar > 0x0039 && wordchar < 0x0061 && wordchar != 0x0040)
         || (wordchar > 0x007a && wordchar <= 0x007f)
         || wordchar == '\u2003' || wordchar == '\u2013'
         || wordchar == '\u2018' || wordchar == '\u2019' || wordchar == '\u201C' || wordchar == '\u201D' 
         || wordchar == '\u2022' || wordchar == '\u2026' || wordchar == '\u2028' || wordchar == '\u202A'
         || wordchar == '\u202C' || wordchar == '\u202F') {

         sb.append(wordchar);
         index--;
         if (index == 0) {
            break;
         }
         wordchar = word.charAt(index);
      }
      result[2] = sb.reverse().toString();
      word = word.substring(0, index + 1);
      result[1] = word;
      return result;
   }

   /**
    * Close a buffered reader opened using {@link #openTextFile(String)}
    * 
    * @param br
    */
   static public void closeTextFile(BufferedReader br) {
      if (br != null) {
         try {
            br.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Close a buffered writer flushing its content first. Nothing happens if a
    * null is passed.
    * 
    * @param bw
    *           the buffered writer to be flushed and closed.
    */
   static public void closeTextFile(BufferedWriter bw) {
      if (bw != null) {
         try {
            bw.flush();
            try {
               bw.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Convert a number of milliseconds to a formatted String containing a sign,
    * followed by the hours and minutes as in "-0500" or "+0100" which are used
    * for TimeZones.
    * 
    * @param iMillisecs
    *           the number of milliseconds from Greenwich Mean Time.
    * @return a string of the form +/-hhmm as in "-0500" or "+0100"
    */
   static public String convertMillisecondsToTimeZone(int iMillisecs) {
      StringBuffer sb = new StringBuffer();
      if (iMillisecs < 0) {
         sb.append("-");
         iMillisecs *= -1;
      } else {
         sb.append("+");
      }
      int iHours = iMillisecs / iHourMilliseconds;
      if (iHours < 10) {
         sb.append("0");
      }
      sb.append(iHours);
      iMillisecs -= iHours * iHourMilliseconds;
      int iMinutes = iMillisecs / iMinuteMilliseconds;
      if (iMinutes < 10) {
         sb.append("0");
      }
      sb.append(iMinutes);
      return sb.toString();
   }

   /**
    * Converts a timezone of the format +/-hhmm to milliseconds
    * 
    * @param strTimeZone
    *           timezone offset from Greenwich Mean Time (GMT) for example
    *           "-0500" is Eastern Standard Time, "-0400" is Eastern Daylight
    *           Time, "+0000" is Greenwich Mean Time, and "+0100" is the offset
    *           for Europe/Paris.
    * 
    * @return milliseconds from Greenwich Mean Time
    */
   static public int convertTimeZoneToMilliseconds(String strTimeZone) {
      int iMillisecs = 0;
      if (strTimeZone == null || strTimeZone.length() != 5) {
         return iMillisecs;
      }
      // convert timezone (+/-hhmm)
      String strSign = strTimeZone.substring(0, 1);
      String strHours = strTimeZone.substring(1, 3);
      String strMinutes = strTimeZone.substring(3, 5);
      try {
         int iHours = new Integer(strHours).intValue();
         int iMinutes = new Integer(strMinutes).intValue();
         iMillisecs = iMinutes * iMinuteMilliseconds;
         iMillisecs = iMillisecs + (iHours * iHourMilliseconds);
         if (strSign.startsWith("-") == true) {
            iMillisecs *= -1;
         }
      } catch (NumberFormatException nfe) {
         iMillisecs = 0;
      }
      return iMillisecs;
   }

   /**
    * Captures an exception's stack trace as a string for inclusion in JSON
    * objects
    * 
    * @param throwableException
    *           exception whose stack is to be captured
    * @return String representation containing the exception's stack trace
    */
   static public String exceptionTraceToString(Throwable throwableException) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      throwableException.printStackTrace(pw);
      String sStackTrace = sw.toString();
      return sStackTrace;
   }

   /**
    * 
    * @return the fully qualified path to the {@link MDfromHTMLConstants#ENV_MDfromHTML_HOME}
    *         directory as defined by the Environment variable first, or System
    *         property second as defined by
    *         {@link MDfromHTMLConstants#ENV_MDfromHTML_HOME}. If that is not defined,
    *         then the user.home directory will be used. There is no trailing
    *         {@link File#separator} appended to the path.
    * @throws Exception
    */
   public static String getMDfromHTMLHomeDirectory() throws Exception {
      File retFile = null;
      String strPath = System.getenv(MDfromHTMLConstants.ENV_MDfromHTML_HOME);
      if (strPath == null) {
         strPath = System.getProperty(MDfromHTMLConstants.ENV_MDfromHTML_HOME);
      }
      if (strPath == null) {
         strPath = System.getProperty("user.home");
         if (strPath == null) {
            throw new Exception(
               "Can not find Environment variable nor System property for "
                  + MDfromHTMLConstants.ENV_MDfromHTML_HOME
                  + " nor the user.home System property so can not return the MDfromHTML Home Directory.");
         }
         while (strPath.length() > 2
            && strPath.endsWith(File.separator) == true) {
            strPath = strPath.substring(0, strPath.length() - 1);
         }
         strPath += File.separator + "MDfromHTML";
      } else {
         while (strPath.length() > 2
            && strPath.endsWith(File.separator) == true) {
            strPath = strPath.substring(0, strPath.length() - 1);
         }
      }

      try {
         retFile = new File(strPath);
         if (retFile.exists() == false || retFile.isFile() == true) {
            // try looking in the current directory
            strPath = System.getProperty("user.dir");
            if (strPath == null) {
               throw new Exception(
                  "Can not find Environment variable nor System property for "
                     + MDfromHTMLConstants.ENV_MDfromHTML_HOME
                     + " nor the user.home System property so can not return the MDfromHTML Home Directory.");
            }
            while (strPath.length() > 2
               && strPath.endsWith(File.separator) == true) {
               strPath = strPath.substring(0, strPath.length() - 1);
            }
            strPath += File.separator
               + MDfromHTMLConstants.MDfromHTML_HOME_DIRECTORY_NAME;
            retFile = new File(strPath);
            if (retFile.exists() == false || retFile.isFile() == true) {
               throw new Exception("Can not find directory " + strPath);
            } // else fall through with this file
         } // else fall through with this file
         return retFile.getCanonicalPath();
      } catch (Exception fnfe) {
         throw new Exception("Can not find directory " + strPath);
      }
   }

   /**
    * @return the key needed for encrypting content
    */
   static public char[] getKey() {
      String pwKey = System.getenv(MDfromHTMLMasker.ENV_MASKER_KEY);
      if (pwKey == null || pwKey.trim().length() == 0) {
         pwKey = System.getProperty(MDfromHTMLMasker.ENV_MASKER_KEY);
         if (pwKey == null || pwKey.trim().length() == 0) {
            return MDfromHTMLConstants.MDfromHTML_DEFAULT_PROPERTY_FILE_KEY;
         }
      }
      return pwKey.toCharArray();
   }

   /**
    * Transform a fully qualified Class' name into just the name of the class
    * without the leading package. For example, "com.mdfromhtml.core.MDfromHTMLDate"
    * would return just "MDfromHTMLDate"
    * 
    * @param inClass
    * @return name of the class without leading qualification
    */
   static public String getNameFromClass(Class<?> inClass) {
      return inClass.getName().lastIndexOf(".") == -1 ? inClass.getName()
         : inClass.getName().substring(inClass.getName().lastIndexOf(".") + 1);
   }

   /**
    * @return the properties file used to define MDfromHTML interprocess
    *         communications
    * @see #loadMDfromHTMLProperties(String)
    * @see MDfromHTMLConstants#MDfromHTML_IPC_PropertiesFileName
    */
   static public Properties getMDfromHTMLIPCProps() throws Exception {
      Properties ipcProps = MDfromHTMLUtils
         .loadMDfromHTMLProperties(MDfromHTMLConstants.MDfromHTML_IPC_PropertiesFileName);
      return ipcProps;
   }

   /**
    * @return a 40 byte String random number based on invoking the
    *         com.ibm.crypto.fips.provider.SecureRandom class.
    */
   static public synchronized String getUniqueID() {
      byte[] byteID = new byte[20];
      if (MDfromHTMLConstants.SEED_SECURE_RANDOM != null) {
         MDfromHTMLConstants.SEED_SECURE_RANDOM.nextBytes(byteID);
         return hexEncode(byteID);
      }
      // otherwise, use a less sophisticated generator.
      Date date = new Date();
      StringBuffer sb = new StringBuffer();
      sb.append("X"); // distinguish from s_sr generated.
      sb.append(Long.toHexString(date.getTime()));
      sb.append(Long.toHexString(MDfromHTMLConstants.SEED_RANDOM.nextLong()));
      return sb.toString();
   }

   /**
    * Transform the String to a UTF-8 encoded byte array
    * 
    * @param string
    * @return byte array of the supplied string
    */
   static public byte[] toUTF8Bytes(String string) {
      return string.getBytes(UTF8_CHARSET);
   }

   /**
    * Transform the string of hexadecimal digits into a byte array.
    * 
    * @param strHex
    *           a String containing pairs of hexadecimal digits
    * @return a byte array created by transforming pairs of hexadecimal digits
    *         into a byte. For example "7F41" would become byte [] { 0x7f, 0x41}
    * @throws InvalidParameterException
    *            thrown if the input string is null or empty, or if it does not
    *            contain an even number of hexadecimal digits, or if it contains
    *            something other than a hexadecimal digit.
    */
   static public byte[] hexDecode(String strHex)
      throws InvalidParameterException {
      if (strHex == null || strHex.length() == 0) {
         throw new InvalidParameterException(
            "Null or empty string passed.  Must pass string containing pairs of hexadecimal digits.");
      }
      int iLength = strHex.length();
      if (iLength % 2 > 0) {
         throw new InvalidParameterException(
            "An odd number of bytes was passed in the input string.  Must be an even number.");
      }
      byte[] inBytes = strHex.toUpperCase()
         .getBytes(MDfromHTMLConstants.UTF8_CHARSET);

      byte[] baRC = new byte[iLength / 2];
      int iHighOffset = -1;
      int iLowOffset = -1;
      for (int i = 0; i < iLength; i += 2) {
         iHighOffset = MDfromHTMLConstants.HEX_CHARS.indexOf((int) inBytes[i]);
         if (iHighOffset < 0) {
            throw new InvalidParameterException(
               "Input string contains non-hexadecimal digit at index " + i
                  + ".  Must be 0-9 or A-F");
         }
         iLowOffset = MDfromHTMLConstants.HEX_CHARS.indexOf((int) inBytes[i + 1]);
         if (iLowOffset < 0) {
            throw new InvalidParameterException(
               "Input string contains non-hexadecimal digit at index " + i
                  + ".  Must be 0-9 or A-F");
         }
         baRC[i / 2] = (byte) ((iHighOffset * 16) + iLowOffset);
      }
      return baRC;
   }

   /**
    * Convert the byte array into a String of hexadecimal digits. For example,
    * the bytes[] {0x31,0x0a} would become "310A".
    * 
    * @param bArray
    *           the array of bytes to be converted.
    * @return a String of hexadecimal digits formed by the hexadecimal digit for
    *         each nibble of the byte.
    */
   static public String hexEncode(byte[] bArray) {
      StringBuffer sb = new StringBuffer();
      // check bad input
      if (bArray == null || bArray.length == 0) {
         return sb.toString();
      }
      // else do real work
      char[] cHexPair = new char[2];
      int iByteCount = 0;
      int iArrayLength = bArray.length;
      while (iByteCount < iArrayLength) {
         cHexPair = byteToHexChars(bArray[iByteCount]);
         sb.append(new String(cHexPair));
         iByteCount++;
      } // end while
      return sb.toString();
   }

   /**
    * Determine if the String is empty (equals "").
    * 
    * @param strInput
    *           the string to be evaluated.
    * @return true if the strInput compares to
    *         {@link MDfromHTMLConstants#EMPTY_String} (""). Returns false if
    *         strInput is null or not empty.
    */
   static public boolean isEmpty(String strInput) {
      if (strInput == null) {
         return false;
      }
      return strInput.compareTo(MDfromHTMLConstants.EMPTY_String) == 0;
   }

   /**
    * Determine if the passed BigInteger matches the
    * {@link MDfromHTMLConstants#UNDEFINED_BigInteger}.
    * 
    * @param testValue
    *           value to compare against
    *           {@link MDfromHTMLConstants#UNDEFINED_BigInteger}.
    * @return true if the values are equal, false if they are not. Note, if
    *         passed value is null, true is returned.
    */
   static public boolean isUndefined(BigInteger testValue) {
      if (testValue == null) {
         return true;
      }
      return testValue.equals(MDfromHTMLConstants.UNDEFINED_BigInteger);
   }

   /**
    * Determine if the passed Boolean matches the
    * {@link MDfromHTMLConstants#UNDEFINED_Boolean}.
    * 
    * @param testValue
    *           value to compare against
    *           {@link MDfromHTMLConstants#UNDEFINED_Boolean}.
    * @return true if the values are equal, false if they are not.
    */
   static public boolean isUndefined(Boolean testValue) {
      return testValue == MDfromHTMLConstants.UNDEFINED_Boolean;
   }

   /**
    * Determine if the passed byte matches the
    * {@link MDfromHTMLConstants#UNDEFINED_byte}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_byte}.
    * @return true if the values are equal, false if they are not.
    */
   static public boolean isUndefined(byte testValue) {
      return testValue == MDfromHTMLConstants.UNDEFINED_byte;
   }

   /**
    * Determine if the passed Byte matches the
    * {@link MDfromHTMLConstants#UNDEFINED_Byte}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_Byte}.
    * @return true if the values are equal, false if they are not. Note, if
    *         passed value is null, true is returned.
    */
   static public boolean isUndefined(Byte testValue) {
      if (testValue == null) {
         return true;
      }
      return testValue.equals(MDfromHTMLConstants.UNDEFINED_Byte);
   }

   /**
    * Determine if the passed char matches the
    * {@link MDfromHTMLConstants#UNDEFINED_char}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_char}.
    * @return true if the values are equal, false if they are not.
    */
   static public boolean isUndefined(char testValue) {
      return testValue == MDfromHTMLConstants.UNDEFINED_char;
   }

   /**
    * Determine if the passed Character matches the
    * {@link MDfromHTMLConstants#UNDEFINED_Character}.
    * 
    * @param testValue
    *           value to compare against
    *           {@link MDfromHTMLConstants#UNDEFINED_Character}.
    * @return true if the values are equal, false if they are not. Note, if
    *         passed value is null, true is returned.
    */
   static public boolean isUndefined(Character testValue) {
      if (testValue == null) {
         return true;
      }
      return testValue.equals(MDfromHTMLConstants.UNDEFINED_Character);
   }

   /**
    * Determine if the pass Class matches the
    * {@link MDfromHTMLConstants#UNDEFINED_Class}.
    * 
    * @param testValue
    * @return whether (true) or not the supplied class is undefined
    */
   static public boolean isUndefined(Class<?> testValue) {
      if (testValue == null) {
         return true;
      }
      return false;
   }

   /**
    * Determine if the passed double matches the
    * {@link MDfromHTMLConstants#UNDEFINED_double}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_double}
    *           .
    * @return true if the values are equal, false if they are not.
    */
   static public boolean isUndefined(double testValue) {
      return new Double(testValue).equals(MDfromHTMLConstants.UNDEFINED_Double);
   }

   /**
    * Determine if the passed Double matches the
    * {@link MDfromHTMLConstants#UNDEFINED_Double}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_Double}
    *           .
    * @return true if the values are equal, false if they are not. Note, if
    *         passed value is null, true is returned.
    */
   static public boolean isUndefined(Double testValue) {
      if (testValue == null) {
         return true;
      }
      return testValue.equals(MDfromHTMLConstants.UNDEFINED_Double);
   }

   /**
    * Determine if the passed float matches the
    * {@link MDfromHTMLConstants#UNDEFINED_float}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_float}.
    * @return true if the values are equal, false if they are not.
    */
   static public boolean isUndefined(float testValue) {
      return new Float(testValue).equals(MDfromHTMLConstants.UNDEFINED_Float);
   }

   /**
    * Determine if the passed Float matches the
    * {@link MDfromHTMLConstants#UNDEFINED_Float}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_Float}.
    * @return true if the values are equal, false if they are not. Note, if
    *         passed value is null, true is returned.
    */
   static public boolean isUndefined(Float testValue) {
      if (testValue == null) {
         return true;
      }
      return testValue.equals(MDfromHTMLConstants.UNDEFINED_Float);
   }

   /**
    * Determine if the passed int matches the
    * {@link MDfromHTMLConstants#UNDEFINED_int} .
    * 
    * @param iTestValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_int}.
    * @return true if the values are equal, false if they are not.
    */
   static public boolean isUndefined(int iTestValue) {
      return iTestValue == MDfromHTMLConstants.UNDEFINED_int;
   }

   /**
    * Determine if the passed Integer matches the
    * {@link MDfromHTMLConstants#UNDEFINED_Integer}.
    * 
    * @param testValue
    *           value to compare against
    *           {@link MDfromHTMLConstants#UNDEFINED_Integer}.
    * @return true if the values are equal, false if they are not. Note, if
    *         passed value is null, true is returned.
    */
   static public boolean isUndefined(Integer testValue) {
      if (testValue == null) {
         return true;
      }
      return testValue.equals(MDfromHTMLConstants.UNDEFINED_Integer);
   }

   /**
    * Determine if the passed long matches the
    * {@link MDfromHTMLConstants#UNDEFINED_long}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_long}.
    * @return true if the values are equal, false if they are not.
    */
   static public boolean isUndefined(long testValue) {
      return testValue == MDfromHTMLConstants.UNDEFINED_long;
   }

   /**
    * Determine if the passed Long matches the
    * {@link MDfromHTMLConstants#UNDEFINED_Long}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_Long}.
    * @return true if the values are equal, false if they are not. Note, if
    *         passed value is null, true is returned.
    */
   static public boolean isUndefined(Long testValue) {
      if (testValue == null) {
         return true;
      }
      return testValue.equals(MDfromHTMLConstants.UNDEFINED_Long);
   }

   /**
    * Determine if the passed Object matches any of the {@link MDfromHTMLConstants}
    * for UNDEFINED_* values.
    * 
    * @param testValue
    *           value to compare against the appropriate {@link MDfromHTMLConstants}
    *           for UNDEFINED_* value.
    * @return true if the values are equal (ignoring case), or false if they are
    *         not. Note, if passed value is null, true is returned.
    */
   static public boolean isUndefined(Object testValue) {
      if (testValue == null) {
         return true;
      }
      Class<? extends Object> objClass = testValue.getClass();
      if (isUndefined(objClass)) {
         return true;
      }
      if (objClass == Double.class) {
         return isUndefined((Double) testValue);
      } else if (objClass == Integer.class) {
         return isUndefined((Integer) testValue);
      } else if (objClass == String.class) {
         return isUndefined((String) testValue);
      } else if (objClass == Byte.class) {
         return isUndefined((Byte) testValue);
      } else if (objClass == Character.class) {
         return isUndefined((Character) testValue);
      } else if (objClass == Float.class) {
         return isUndefined((Float) testValue);
      } else if (objClass == Long.class) {
         return isUndefined((Long) testValue);
      } else if (objClass == Short.class) {
         return isUndefined((Short) testValue);
      } else if (objClass == BigInteger.class) {
         return isUndefined((BigInteger) testValue);
      } else if (objClass == Date.class) {
         return MDfromHTMLDate.isUndefined((Date) testValue);
      } else if (objClass == MDfromHTMLDate.class) {
         return MDfromHTMLDate.isUndefined((MDfromHTMLDate) testValue);
      } else if (objClass == JSONObject.class) {
         try {
            ((JSONObject) testValue).serialize();
         } catch (IOException e) {
            return true;
         }
      }
      return false;
   }

   /**
    * Determine if the passed short matches the
    * {@link MDfromHTMLConstants#UNDEFINED_short}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_short}.
    * @return true if the values are equal, false if they are not.
    */
   static public boolean isUndefined(short testValue) {
      return testValue == MDfromHTMLConstants.UNDEFINED_short;
   }

   /**
    * Determine if the passed Short matches the
    * {@link MDfromHTMLConstants#UNDEFINED_Short}.
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_Short}.
    * @return true if the values are equal, false if they are not. Note, if
    *         passed value is null, true is returned.
    */
   static public boolean isUndefined(Short testValue) {
      if (testValue == null) {
         return true;
      }
      return testValue.equals(MDfromHTMLConstants.UNDEFINED_Short);
   }

   /**
    * Determine if the passed String is null, or when trimmed, matches the
    * {@link MDfromHTMLConstants#UNDEFINED_String} or is empty or is equal to "null"
    * (to support ABLE rules)
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_String}
    *           .
    * @return true if the values are equal (ignoring case), or false if they are
    *         not. Note, if passed value is null or an empty string, true is
    *         returned.
    */
   static public boolean isUndefined(String testValue) {
      if (testValue == null || testValue.trim().length() == 0) {
         return true;
      }
      testValue = testValue.trim();
      // WNM3: hack for ABLE support
      if ("null".equals(testValue)) {
         return true;
      }
      return testValue.equals(MDfromHTMLConstants.UNDEFINED_String);
   }

   /**
    * Determine if the passed URI is null, or equals the
    * {@link MDfromHTMLConstants#UNDEFINED_URI}
    * 
    * @param testValue
    *           value to compare against {@link MDfromHTMLConstants#UNDEFINED_URI}.
    * @return true if the values are equal (ignoring case), or false if they are
    *         not. Note, if passed value is null, true is returned.
    */
   static public boolean isUndefined(URI testValue) {
      if (testValue == null) {
         return true;
      }
      return (MDfromHTMLConstants.UNDEFINED_URI.equals(testValue));
   }

   /**
    * Tests whether the supplied url is valid
    * 
    * @param url
    *           the URL to be tested
    * @return true if the URL references http or https protocol
    */
   static public boolean isValidURL(String url) {
      boolean result = false;
      // must be http:// at minimum, could be https://
      if (url.length() < 7) {
         return result;
      }
      String secure = url.substring(4, 5);
      if (secure.equalsIgnoreCase("s")) {
         if (url.length() < 8) {
            return result;
         }
         if (url.length() >= 12 && url.substring(5,12).equals("://http")) {
            return result;
         }
         result = url.substring(5, 8).equals("://");
      } else {
         if (secure.equals(":") == false) {
            return result;
         }
         // now check for //
         if (url.length() >= 11 && url.substring(4,11).equals("://http")) {
            return result;
         }
         result = url.substring(5, 7).equals("//");
      }
      return result;
   }

   /**
    * Construct and return a sorted list of files in a directory identified by
    * the dir that have extensions matching the ext
    * 
    * @param dir
    *           the path to the directory containing files to be returned in the
    *           list
    * @param ext
    *           the file extension (without the leading period) used to filter
    *           files in the dir
    * @return sorted list of files in a directory identified by the dir that
    *         have extensions matching the ext
    * @throws IOException
    *            if there is difficulty accessing the files in the supplied dir
    */
   static public List<Path> listSourceFiles(Path dir, String ext)
      throws IOException {
      List<Path> result = new ArrayList<Path>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir,
         "*.{" + ext + "}")) {
         for (Path entry : stream) {
            result.add(entry);
         }
      } catch (DirectoryIteratorException ex) {
         // I/O error encountered during the iteration, the cause is an
         // IOException
         throw ex.getCause();
      }
      result.sort(null);
      return result;
   }

   /**
    * Transform a list of fields contained in a String bounded with opening
    * ('{') and closing ('}') braces, and delimited with one of the
    * delimiters (comma, space, tab, pipe). Fields containing strings are
    * expected to be enclosed in double quotes ('"').
    * 
    * @param strList
    *           the list of fields enclosed in braces.
    * @return an ArrayList of the fields parsed from the supplied list string.
    *         Note: if the passed strList is null or empty, an empty ArrayList
    *         is returned (e.g., its size() is 0).
    * @see #arrayListToListString
    */
   static public ArrayList<Object> listStringToArrayList(String strList) {
      ArrayList<Object> list = new ArrayList<Object>();
      if (strList == null || strList.length() == 0) {
         return list;
      }
      // expects a string enclosed in open/close braces
      // strip off the braces and parse the fields in the list.
      String strRecord = strList;
      while (strRecord.startsWith("{") == true) {
         strRecord = strRecord.substring(1);
      }
      while (strRecord.endsWith("}") == true) {
         strRecord = strRecord.substring(0, strRecord.length() - 1);
      }
      Object[] objList = parseFields(strRecord);
      for (int i = 0; i < objList.length; i++) {
         list.add(objList[i]);
      }
      return list;
   }

   /**
    * Load the specified JSON file from the fully qualified file name or throw
    * the appropriate exception.
    * 
    * @param jsonFQFileName
    *           name of the JSON file to be loaded
    * @return the JSONObject or JSONArray contained in the file, or an empty JSONObject if no
    *         object exists
    * @throws Exception
    *            If the file can no be located, or if there is a problem reading
    *            the file
    */
   static public JSONArtifact loadJSONArtifact(String jsonFQFileName)
      throws Exception {
      JSONArtifact retObj = null;
      BufferedReader br = null;
      try {
         br = openTextFile(jsonFQFileName);
         if (br != null) {
            retObj = JSON.parse(br);
         }
      } catch (IOException ioe) {
         throw new IOException("Can not parse \"" + jsonFQFileName + "\"", ioe);
      } catch (Exception e) {
         throw new IOException("Can not load file \"" + jsonFQFileName + "\"",
            e);
      } finally {
         closeTextFile(br);
      }
      if (retObj == null) {
         retObj = new JSONObject();
      }
      return retObj;
   }

   /**
    * Load the specified JSON file from the fully qualified file name or throw
    * the appropriate exception.
    * 
    * @param jsonFQFileName
    *           name of the JSON file to be loaded
    * @return the JSONObject contained in the file, or an empty JSONObject if no
    *         object exists
    * @throws Exception
    *            If the file can no be located, or if there is a problem reading
    *            the file
    */
   static public JSONObject loadJSONFile(String jsonFQFileName)
      throws Exception {
      JSONObject retObj = new JSONObject();
      BufferedReader br = null;
      try {
         br = openTextFile(jsonFQFileName);
         if (br != null) {
            retObj = JSONObject.parse(br);
         }
      } catch (IOException ioe) {
         throw new IOException("Can not parse \"" + jsonFQFileName + "\"", ioe);
      } catch (Exception e) {
         throw new IOException("Can not load file \"" + jsonFQFileName + "\"",
            e);
      } finally {
         closeTextFile(br);
      }
      return retObj;
   }

   /**
    * Load the JSONArray from the specified JSON file from the fully qualified file name or throw
    * the appropriate exception.
    * 
    * @param jsonFQFileName
    *           name of the JSON file to be loaded
    * @return the JSONArray contained in the file, or an empty JSONArray if no
    *         object exists
    * @throws Exception
    *            If the file can no be located, or if there is a problem reading
    *            the file
    */
   static public JSONArray loadJSONArray(String jsonFQFileName)
      throws Exception {
      JSONArray retObj = new JSONArray();
      BufferedReader br = null;
      try {
         br = openTextFile(jsonFQFileName);
         if (br != null) {
            retObj = JSONArray.parse(br);
         }
      } catch (IOException ioe) {
         throw new IOException("Can not parse \"" + jsonFQFileName + "\"", ioe);
      } catch (Exception e) {
         throw new IOException("Can not load file \"" + jsonFQFileName + "\"",
            e);
      } finally {
         closeTextFile(br);
      }
      return retObj;
   }

   /**
    * Transform the UTF-8 encoded byte array into a String
    * 
    * @param bytes
    * @return string built from the supplied UTF-8 bytes
    */
   static public String fromUTF8Bytes(byte[] bytes) {
      return new String(bytes, UTF8_CHARSET);
   }

   /**
    * 
    * @return URI for MDfromHTMLwebServices
    */
   static public String getMDfromHTMLWebServicesURI() {
      StringBuffer sb = new StringBuffer();
      sb.append(MDfromHTMLPropertyManager.getProtocol());
      sb.append("://");
      sb.append(MDfromHTMLPropertyManager.getHostName());
      sb.append(":");
      sb.append(MDfromHTMLPropertyManager.getPortNumber());
      sb.append("/");
      sb.append(MDfromHTMLPropertyManager.getServletName());
      sb.append("/");
      // sb.append(MDfromHTMLPropertyManager.getURLPattern());
      // sb.append("/");
      sb.append(MDfromHTMLPropertyManager.getVersion());
      sb.append("/");
      return sb.toString();
   }

   /**
    * @return the properties file used to identify MDfromHTML general control
    *         parameters
    * @throws Exception
    */
   static public Properties getMDfromHTMLServicesProps() throws Exception {
      return MDfromHTMLUtils
         .loadMDfromHTMLProperties(MDfromHTMLConstants.MDfromHTML_SVCS_PropertiesFileName);
   }

   /**
    * Load the specified properties file and return the properties object, or
    * null if an error occurs.
    * 
    * @param strPropFileName
    *           name of the property file to be loaded
    * @return the loaded properties object, or an empty properties if an error
    *         occurs.
    * @throws Exception
    *            if the file can no be located
    */
   static public Properties loadMDfromHTMLProperties(String strPropFileName)
      throws Exception {
      Properties propFile = new Properties();
      InputStream is = null;
      String strPath = getMDfromHTMLHomeDirectory();
      String subDirectory = MDfromHTMLConstants.MDfromHTML_DIR_PROPERTIES;
      try {
         if (strPath.endsWith(File.separator) == false) {
            strPath = strPath + File.separator;
         }
         strPath = strPath + subDirectory;
         strPath = strPath + strPropFileName;
         is = new FileInputStream(new File(strPath));
         // System.out
         // .println("MDfromHTML properties file is read from " + strPath);
      } catch (Exception fnfe) {
         // SMS 28 Jan 2016 added this case for prop file in project dir
         strPath = strPropFileName;
         try {
            is = new FileInputStream(new File(strPath));
         } catch (FileNotFoundException e) {
            ; // let this fall through
         }
         // SMS 3 Feb 2016 added this case for prop file in properties folder
         // in
         // project dir
         if (is == null) {
            try {
               strPath = MDfromHTMLConstants.MDfromHTML_DIR_PROPERTIES + strPath;
               is = new FileInputStream(new File(strPath));
            } catch (FileNotFoundException e) {
               ; // let this fall through
            }
         }
      }
      try {
         if (is == null) {
            // find property file from classpath
            is = MDfromHTMLUtils.class
               .getResourceAsStream(File.separator + strPropFileName);
         }
         if (is == null) {
            // if property file does not exist in classpath, load
            // property file from this package
            // System.out.println("Default MDfromHTML properties file is read : "
            // + strPropFileName);
            is = MDfromHTMLUtils.class.getResourceAsStream(strPropFileName);
         }
         if (is != null) {
            propFile.load(is);
         } else {
            throw new Exception(
               "Can not load properties path: " + strPropFileName);
         }
         if (propFile.size() == 0) {
            throw new Exception(strPropFileName + " is empty.");
         }
      } catch (IOException e) {
         throw new IOException("Can not load " + strPath, e);
      } finally {
         if (is != null) {
            try {
               is.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
      // Finally, if no properties were found, look in the current directory
      if (propFile.size() == 0) {
         FileInputStream fis = null;
         // System.out
         // .println("MDfromHTML properties file is read from the current directory
         // :
         // /"
         // + strPropFileName);
         try {
            propFile = new Properties();
            fis = new FileInputStream(strPropFileName);
            propFile.load(fis);
         } catch (IOException ioe) {
            throw new Exception(
               "Can not open file \"" + strPropFileName + "\" nor " + strPath,
               ioe);
            // leave propFile empty
            // propFile = null;
         } catch (Exception fnfe) {
            throw new Exception(
               "Can not open file \"" + strPropFileName + "\" nor " + strPath,
               fnfe);
            // leave propFile empty
            // propFile = null;
         } finally {
            if (fis != null) {
               try {
                  fis.close();
               } catch (IOException ioe) {
                  ; // not much we can do
               }
            }
         }
      }
      if (propFile.size() == 0) {
         throw new Exception("Can not locate property file \"" + strPropFileName
            + "\" nor " + strPath + "\n Ensure " + MDfromHTMLConstants.ENV_MDfromHTML_HOME
            + " has been set properly.");
      }
      // clean up properties by trimming whitespace
      for (Object key : propFile.keySet()) {
         Object prop = propFile.getProperty(key.toString());
         if (prop != null) {
            propFile.setProperty(key.toString(), prop.toString().trim());
         }
      }
      return propFile;
   }

   /**
    * Reads the lines of a text file into a list of strings and returns that
    * list. If no lines are present (e.g., empty file) then an empty list is
    * returned.
    * 
    * @param fqFilename
    *           fully qualified filename
    * @return list of strings read from the file
    * @throws Exception
    *            if the file can not be read.
    */
   static public List<String> loadTextFile(String fqFilename) throws Exception {
      List<String> result = new ArrayList<String>();
      BufferedReader br = openTextFile(fqFilename);
      String line = br.readLine();
      while (line != null) {
         result.add(line);
         line = br.readLine();
      }
      MDfromHTMLUtils.closeTextFile(br);
      return result;
   }

   /**
    * @param fqFilename
    *           fully qualified name of the text file to be opened
    * @return open buffered reader to allow individual lines of a text file to
    *         be read
    * @throws Exception
    * @see #closeTextFile(BufferedReader) to close the reader returned by this
    *      function
    */
   static public BufferedReader openTextFile(String fqFilename)
      throws Exception {
      BufferedReader input = null;
      File inputFile = new File(fqFilename);
      if (inputFile.exists() == false) {
         throw new Exception(inputFile.getCanonicalPath() + " does not exist.");
      }
      if (inputFile.isFile() == false) {
         throw new IOException(
            "Input is not a file: " + inputFile.getCanonicalPath()
               + File.separator + inputFile.getName());
      }
      if (inputFile.canRead() == false) {
         throw new IOException(
            "Can not read file " + inputFile.getCanonicalPath() + File.separator
               + inputFile.getName());
      }
      input = new BufferedReader(new FileReader(inputFile));
      return input;
   }

   /**
    * Helper method to create strings of the form "000nn".
    * 
    * @param iIn
    *           integer value to be right justified with leading characters in
    *           the returned String.
    * @param iWidth
    *           integer value of the width of the returned String.
    * @param cPad
    *           character value to be used to pad the left portion of the
    *           returned String to make it as wide as the specified iWidth
    *           parameter. For example, calling toLeftPaddedString(iNum,4,'0')
    *           would result in "0045" if iNum == 45, or "0004" if iNum == 4.
    * 
    * @return String containing the right justified value, padded to the
    *         specified with the specified pad character.
    */
   static public String padLeft(int iIn, int iWidth, char cPad) {
      String strTemp = String.valueOf(iIn);
      return padLeft(strTemp, iWidth, cPad);
   }

   /**
    * Creates a new String padded on its left side with the supplied pad
    * character guaranteed to be the supplied length. If the supplied length is
    * less than or equal to the length of the supplied string, the supplied
    * string is returned. If the supplied string is null, a new string is
    * returned filled with the supplied pad character that is as long as the
    * supplied length.
    * 
    * @param strInput
    * @param iMax
    * @param cPadChar
    * @return formatted string with padding
    */
   static public String padLeft(String strInput, int iMax, char cPadChar) {
      if (strInput == null) {
         char[] padChars = new char[iMax];
         Arrays.fill(padChars, cPadChar);
         return new String(padChars);
      }
      int iLength = strInput.length();
      if (iLength < iMax) {
         char[] padChars = new char[iMax - iLength];
         Arrays.fill(padChars, cPadChar);
         return new String(padChars) + strInput;
      }
      // else already bigger so leave it alone
      return strInput;
   }

   /**
    * Creates a new String padded on its left side with zeros ('0') that is
    * guaranteed to be the supplied length. If the supplied length is less than
    * or equal to the length of the supplied string, the supplied string is
    * returned. If the supplied string is null, a new string is returned filled
    * with zeros as long as the supplied length.
    * 
    * @param iValue
    *           the value to be right justified and left padded with zeros in
    *           the returned string.
    * @param iMax
    *           the desired maximum length of the String to be returned.
    * @return a string with the value right justified with leading zeros to fill
    *         out to the desired maximum length specified. If iMax is less than
    *         the number of digits in the value, the returned string will be
    *         large enough to represent the entire value with no padding
    *         applied.
    */
   static public String padLeftZero(int iValue, int iMax) {
      return padLeft(String.valueOf(iValue), iMax, '0');
   }

   /**
    * Creates a new String padded on its left side with zeros ('0') that is
    * guaranteed to be the supplied length. If the supplied length is less than
    * or equal to the length of the supplied string, the supplied string is
    * returned. If the supplied string is null, a new string is returned filled
    * with zeros as long as the supplied length.
    * 
    * @param strInput
    *           the input string to be right justified and left padded with
    *           zeros in the returned string.
    * @param iMax
    *           the desired maximum length of the String to be returned.
    * @return a string with the input string right justified with leading zeros
    *         to fill out to the desired maximum length specified. If iMax is
    *         less than the length of the input string, the returned string will
    *         be input string.
    */
   static public String padLeftZero(String strInput, int iMax) {
      return padLeft(strInput, iMax, '0');
   }

   /**
    * Helper method to create strings of the form "nn000".
    * 
    * @param iIn
    *           integer value to be right justified with leading characters in
    *           the returned String.
    * @param iWidth
    *           integer value of the width of the returned String.
    * @param cPad
    *           character value to be used to pad the right portion of the
    *           returned String to make it as wide as the specified iWidth
    *           parameter. For example, calling toRightPaddedString(iNum,4,'0')
    *           would result in "4500" if iNum == 45, or "4000" if iNum == 4.
    * 
    * @return String containing the right justified value, padded to the
    *         specified with the specified pad character.
    */
   static public String padRight(int iIn, int iWidth, char cPad) {
      String strTemp = String.valueOf(iIn);
      return padRight(strTemp, iWidth, cPad);
   }

   /**
    * Creates a new String padded on its right side with the supplied pad
    * character guaranteed to be the supplied length. If the supplied length is
    * less than or equal to the length of the supplied string, the supplied
    * string is returned. If the supplied string is null, a new string is
    * returned filled with the supplied pad character that is as long as the
    * supplied length.
    * 
    * @param strInput
    * @param iMax
    * @param cPadChar
    * @return formatted string with padding
    */
   static public String padRight(String strInput, int iMax, char cPadChar) {
      if (strInput == null) {
         char[] padChars = new char[iMax];
         Arrays.fill(padChars, cPadChar);
         return new String(padChars);
      }
      int iLength = strInput.length();
      if (iLength < iMax) {
         char[] padChars = new char[iMax - iLength];
         Arrays.fill(padChars, cPadChar);
         return strInput + new String(padChars);
      }
      // else already bigger so leave it alone
      return strInput;
   }

   /**
    * Creates a new String padded on its right side with zeros ('0') that is
    * guaranteed to be the supplied length. If the supplied length is less than
    * or equal to the length of the supplied string, the supplied string is
    * returned. If the supplied string is null, a new string is returned filled
    * with zeros as long as the supplied length.
    * 
    * @param iValue
    *           the value to be right justified and right padded with zeros in
    *           the returned string.
    * @param iMax
    *           the desired maximum length of the String to be returned.
    * @return a string with the value right justified with leading zeros to fill
    *         out to the desired maximum length specified. If iMax is less than
    *         the number of digits in the value, the returned string will be
    *         large enough to represent the entire value with no padding
    *         applied.
    */
   static public String padRightZero(int iValue, int iMax) {
      return padRight(String.valueOf(iValue), iMax, '0');
   }

   /**
    * Creates a new String padded on its right side with zeros ('0') that is
    * guaranteed to be the supplied length. If the supplied length is less than
    * or equal to the length of the supplied string, the supplied string is
    * returned. If the supplied string is null, a new string is returned filled
    * with zeros as long as the supplied length.
    * 
    * @param strInput
    *           the input string to be right justified and right padded with
    *           zeros in the returned string.
    * @param iMax
    *           the desired maximum length of the String to be returned.
    * @return a string with the input string right justified with leading zeros
    *         to fill out to the desired maximum length specified. If iMax is
    *         less than the length of the input string, the returned string will
    *         be input string.
    */
   static public String padRightZero(String strInput, int iMax) {
      return padRight(strInput, iMax, '0');
   }

   /**
    * Method to parse the passed String record to extract data values defined by
    * fields delimited by comma (0x2C), or tab (0x09). The fields are maintained
    * as Strings
    * 
    * @param strRecord
    *           A String containing a record to be parsed.
    * @param removeEmptyStrings
    *           True removes empty strings
    * @return An Object[] containing each of the data fields parsed from the
    *         record. If the input string is null or empty, an empty Object[] is
    *         returned (e.g., new Object[0]).
    */
   static public String[] parseCSV(String strRecord,
      boolean removeEmptyStrings) {
      if (strRecord == null || strRecord.length() == 0) {
         return new String[0];
      }
      if (removeEmptyStrings) {
         strRecord = strRecord.replaceAll("\"\"", "");
      }
      // WNM3: out pending better solution
      // check input for errant characters
      // if (MDfromHTMLUtils.isAcceptableASCII(strRecord) == false) {
      // return new Object[0];
      // }
      ArrayList<String> retArray = new java.util.ArrayList<String>();
      // parse the input record for its fields
      byte[] recbytes = strRecord.getBytes();
      int iLength = recbytes.length;
      boolean bInQuoted = false;
      boolean bInField = false;
      byte bLastByte = 0x00;
      boolean bNeedDelim = false;
      int iFieldStart = 0;
      int iFieldCount = 0;
      for (int i = 0; i < iLength; i++) {
         switch ((int) (recbytes[i])) {
            // space, tab, and comma are delimiters if not in quoted field
            case 0x09: // tab
            {
               if (bInField == false) {
                  // skip merrily along
                  iFieldStart++;
                  bNeedDelim = false;
                  break; // skip this
               } else if ((bInQuoted == false)) {
                  // found field delimiter, process this field
                  // get our data
                  String strField = new String(recbytes, iFieldStart,
                     iFieldCount);
                  retArray.add(strField);
                  bInQuoted = false;
                  bInField = false;
                  bNeedDelim = false;
                  iFieldCount = 0;
                  break;
               }
               // we're in a quoted field, so count this byte
               iFieldCount++;
               break;
            }
            case 0x2C: // comma
            {
               if (bInField == false) {
                  if (bLastByte == 0x2C) {
                     // consecutive commas, treat as an empty (undefined)
                     // field
                     retArray.add("");
                     bInQuoted = false;
                     bInField = false;
                     bNeedDelim = false;
                     iFieldCount = 0;
                  } else {
                     // skip merrily along
                     iFieldStart++;
                     bNeedDelim = false;
                  }
                  break; // skip this
               } else if ((bInQuoted == false)) {
                  // found field delimiter, process this field
                  // get our data
                  String strField = new String(recbytes, iFieldStart,
                     iFieldCount);
                  retArray.add(strField);
                  bInQuoted = false;
                  bInField = false;
                  bNeedDelim = false;
                  iFieldCount = 0;
                  break;
               }
               // we're in a quoted field, so count this byte
               iFieldCount++;
               break;
            }
            // start or end quoted field
            case 0x22: // double quote "
            {
               if (bLastByte == 0x2F) {
                  // just process as an escaped quote
                  break;
               } else if (bInField == false) {
                  // start new field on next char
                  iFieldStart = i + 1;
                  iFieldCount = 0;
                  bInField = true;
                  bInQuoted = true;
                  break;
               } else if (bInQuoted == true) {
                  // found end of quoted string == end of field
                  // get our data
                  String strField = new String(recbytes, iFieldStart,
                     iFieldCount);
                  retArray.add(strField); // save as a String regardless
                  bInQuoted = false;
                  bInField = false;
                  bNeedDelim = true;
                  iFieldCount = 0;
                  break;
               } // else just count this as a normal field character
               iFieldCount++;
               break;
            }
            default: { // field char
               if (bInField == false) {
                  if (bNeedDelim == false) {
                     // start new field on this char
                     iFieldStart = i;
                     iFieldCount = 1;
                     bInField = true;
                     bInQuoted = false;
                     break;
                  } // otherwise, skip this char
                  break;
               }
               // already in field, count this byte
               iFieldCount++;
               break;
            }
         }
         bLastByte = recbytes[i];
      }
      // process remainder if any
      if ((bInField == true) || (bLastByte == 0x2C)) {
         String strField = new String(recbytes, iFieldStart, iFieldCount);
         retArray.add(strField);
      }
      return retArray.toArray(new String[0]);
   }

   /**
    * Method to parse the passed String record to extract data values defined by
    * fields delimited by space (0x20), comma (0x2C), pipe (0x7C) or tab (0x09).
    * The fields are examined to determine if they contain data able to be
    * transformed into int, double, or Strings, in that order. A list is
    * represented by content enclosed in open/closed braces ('{' and '}') and is
    * preserved as such in a String. Lists may include embedded quotes and
    * delimiters.
    * 
    * @param strRecord
    *           A String containing a record to be parsed.
    * 
    * @return An Object[] containing each of the data fields parsed from the
    *         record. If the input string is null or empty, an empty Object[] is
    *         returned (e.g., new Object[0]).
    */
   static public Object[] parseFields(String strRecord) {
      if (strRecord == null || strRecord.length() == 0) {
         return new Object[0];
      }
      // WNM3: out pending better solution
      // check input for errant characters
      // if (MDfromHTMLUtils.isAcceptableASCII(strRecord) == false) {
      // return new Object[0];
      // }
      ArrayList<Object> retArray = new java.util.ArrayList<Object>();
      // parse the input record for its fields
      byte[] recbytes = strRecord.getBytes();
      int iLength = recbytes.length;
      boolean bInList = false;
      boolean bInQuoted = false;
      boolean bInField = false;
      byte bLastByte = 0x00;
      boolean bNeedDelim = false;
      int iFieldStart = 0;
      int iFieldCount = 0;
      for (int i = 0; i < iLength; i++) {
         switch ((int) (recbytes[i])) {
            // space, tab, and comma are delimiters if not in quoted field
            case 0x20: // space
            case 0x7C: // pipe '|'
            case 0x09: // tab
            {
               if (bInField == false) {
                  // skip merrily along
                  iFieldStart++;
                  bNeedDelim = false;
                  break; // skip this
               } else if ((bInQuoted == false) && (bInList == false)) {
                  // found field delimiter, process this field
                  // get our data
                  String strField = new String(recbytes, iFieldStart,
                     iFieldCount);
                  retArray.add(processField(strField));
                  bInQuoted = false;
                  bInField = false;
                  bNeedDelim = false;
                  iFieldCount = 0;
                  break;
               }
               // we're in a quoted field, so count this byte
               iFieldCount++;
               break;
            }
            case 0x2C: // comma
            {
               if (bInField == false) {
                  if (bLastByte == 0x2C) {
                     // consecutive commas, treat as an empty (undefined)
                     // field
                     retArray.add(processField(null));
                     bInQuoted = false;
                     bInField = false;
                     bNeedDelim = false;
                     iFieldCount = 0;
                  } else {
                     // skip merrily along
                     iFieldStart++;
                     bNeedDelim = false;
                  }
                  break; // skip this
               } else if ((bInQuoted == false) && (bInList == false)) {
                  // found field delimiter, process this field
                  // get our data
                  String strField = new String(recbytes, iFieldStart,
                     iFieldCount);
                  retArray.add(processField(strField));
                  bInQuoted = false;
                  bInField = false;
                  bNeedDelim = false;
                  iFieldCount = 0;
                  break;
               }
               // we're in a quoted field, so count this byte
               iFieldCount++;
               break;
            }
            // start or end quoted field
            case 0x22: // double quote "
            {
               if (bInField == false) {
                  // start new field on next char
                  iFieldStart = i + 1;
                  iFieldCount = 0;
                  bInField = true;
                  bInQuoted = true;
                  break;
               } else if (bInQuoted == true) {
                  // found end of quoted string == end of field
                  // get our data
                  String strField = new String(recbytes, iFieldStart,
                     iFieldCount);
                  // retArray.add(processField(strField));
                  retArray.add(strField); // save as a String regardless
                  bInQuoted = false;
                  bInField = false;
                  bNeedDelim = true;
                  iFieldCount = 0;
                  break;
               } // else just count this as a normal field character
               iFieldCount++;
               break;
            }
            // start or end quoted field
            case 0x7B: { // open brace '{'
               if (bInField == false) {
                  // start new field on next char
                  iFieldStart = i + 1;
                  iFieldCount = 0;
                  bInField = true;
                  bInList = true;
                  break;
               } // else just count this as a normal field character }
               iFieldCount++;
               break;
            }
            case 0x7D: { // close brace '}'
               if (bInList == true) {
                  // found end of list == end of field
                  // get our data
                  String strField = new String(recbytes, iFieldStart,
                     iFieldCount);
                  // add back list delimiters (will be treated as String in
                  // DataTable)
                  strField = "{" + strField + "}";
                  retArray.add(processField(strField));
                  bInList = false;
                  bInField = false;
                  bNeedDelim = true;
                  iFieldCount = 0;
                  break;
               } // else just count this as a normal field character }
               iFieldCount++;
               break;
            }
            default: { // field char
               if (bInField == false) {
                  if (bNeedDelim == false) {
                     // start new field on this char
                     iFieldStart = i;
                     iFieldCount = 1;
                     bInField = true;
                     bInQuoted = false;
                     break;
                  } // otherwise, skip this char
                  break;
               }
               // already in field, count this byte
               iFieldCount++;
               break;
            }
         }
         bLastByte = recbytes[i];
      }
      // process remainder if any
      if ((bInField == true) || (bLastByte == 0x2C)) {
         String strField = new String(recbytes, iFieldStart, iFieldCount);
         retArray.add(processField(strField));
      }
      return retArray.toArray();
   }

   /**
    * Retrieve the object from the passed String by interpreting the content of
    * the string to guess if it contains an Integer, Double, or String. The
    * guess is based on first checking for a decimal polong ('.') and if it is
    * present, attempting to create a Double, otherwise, attempting to create an
    * Integer. If the attempted creating fails, the content is retained as a
    * String.
    * 
    * @param strField
    *           the String containing the potential Integer or Double value.
    * @return an Integer, Double or String object. If the input string is null
    *         or empty, null is returned.
    */
   static public Object processField(String strField) {
      if (strField == null || strField.length() == 0) {
         return null;
      }
      // check to see if decimal point is present
      Object objField = null;
      try {
         if (strField.indexOf('.') == -1) {
            Integer iField = new Integer(strField);
            objField = iField;
         } else {
            Double dField = new Double(strField);
            objField = dField;
         }
      } catch (Exception e) {
         // assume String
         if (strField == null || strField.length() == 0) {
            strField = MDfromHTMLConstants.UNDEFINED_String;
         }
         objField = strField;
      }
      return objField;
   }

   /**
    * Print the supplied prompt (if not null) and return the trimmed response
    * 
    * @param strPrompt
    * @return the trimmed response to the prompt (may be the empty String ("")
    *         if nothing entered)
    */
   static public String prompt(String strPrompt) {
      return prompt(strPrompt, true);
   }

   /**
    * Print the supplied prompt (if not null) and return the trimmed response
    * according to the supplied trim control
    * 
    * @param strPrompt
    * @param bTrim
    * @return the trimmed response (if so commanded) to the prompt (may be the
    *         empty String ("") if nothing entered)
    */
   static public String prompt(String strPrompt, boolean bTrim) {
      String strReply = "";
      try {
         BufferedReader in = new BufferedReader(
            new InputStreamReader(System.in));
         if ((strPrompt != null) && (strPrompt.length() != 0)) {
            System.out.println(strPrompt);
         }
         strReply = in.readLine();
         if (bTrim && strReply != null) {
            strReply = strReply.trim();
         }

      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
      return strReply;
   }

   /**
    * Reads a buffered reader line up to a newline and returns the content read
    * as a String that does not contain the linefeed.
    * 
    * @param br
    *           buffered reader
    * @return String containing the characters read through the terminator
    *         character. If the end of file has been reached with nothing
    *         available to be returned, then null is returned.
    * @throws IOException
    *            if an error occurs while reading the buffered reader.
    * @see #readLine(BufferedReader, HashSet)
    */
   static public String readLine(BufferedReader br) throws IOException {
      HashSet<Integer> terminators = new HashSet<Integer>();
      terminators.add(10); // newline
      return readLine(br, terminators);
   }

   /**
    * Reads a buffered reader line up to any of the terminator characters (e.g.,
    * 0x0a for newline) and returns the content read as a String that does not
    * contain the terminator.
    * 
    * @param br
    *           buffered reader
    * @param terminators
    *           the set of line terminators used to signal return of the next
    *           "line" from the buffered reader.
    * @return String containing the characters read through the terminator
    *         character. If the end of file has been reached with nothing
    *         available to be returned, then null is returned.
    * @throws IOException
    *            if an error occurs while reading the buffered reader.
    */
   static public String readLine(BufferedReader br,
      HashSet<Integer> terminators) throws IOException {
      StringBuffer sb = new StringBuffer();
      int c;
      c = br.read();
      while (c != -1) {
         if (terminators.contains((Integer) c)) {
            return sb.toString();
         }
         sb.append((char) c);
         c = br.read();
      }
      if (sb.length() > 0) {
         return sb.toString();
      }
      return null;
   }

   /**
    * Removes the tag identified with tagPrefix through its closing >
    * 
    * @param text
    *           string to be cleansed
    * @param tagPrefix
    *           HTML tag prefix used to search for the tag
    * @return the cleansed text
    */
   static public String removeTag(String text, String tagPrefix) {
      int index = text.indexOf(tagPrefix);
      if (index == -1) {
         return text;
      }
      int indexEnd = 0;
      String newText = "";
      String start = "";
      String end = "";
      while (index >= 0) {
         start = text.substring(0, index);
         newText += start;
         text = text.substring(index + tagPrefix.length());
         indexEnd = text.indexOf(">");
         if (indexEnd >= 0) {
            end = text.substring(indexEnd + 1);
            text = text.substring(indexEnd + 1);
         }
         index = text.indexOf(tagPrefix);
         if (index >= 0) {
            newText += " " + end.substring(0, index);
            text = end.substring(index);
            index = 0;
         } else {
            newText += " " + end;
         }
      }
      return newText;
   }

   /**
    * Save the specified JSONObject in serialized form to the specified file or
    * throw the appropriate exception.
    * 
    * @param jsonFileName
    *           fully qualified name of the JSON file to be saved
    * @param jsonData
    *           the JSONObject to be saved to a file.
    * @return the jsonData that was saved
    * @throws Exception
    *            {@link IOException}) if there is a problem writing the file
    */
   static public JSONObject saveJSONFile(String jsonFileName,
      JSONObject jsonData) throws Exception {
      if (jsonData == null) {
         throw new InvalidObjectException("jsonData is null");
      }
      if (jsonFileName == null || jsonFileName.trim().length() == 0) {
         throw new InvalidTargetObjectTypeException(
            "Output filename is null or empty.");
      }
      BufferedWriter br = null;
      try {
         File outputFile = new File(jsonFileName);
         // write the JSON file
         br = new BufferedWriter(new FileWriter(outputFile));
         br.write(jsonData.serialize(true));
      } catch (IOException e) {
         throw new IOException("Can not write file \"" + jsonFileName + "\"",
            e);
      } finally {
         try {
            if (br != null) {
               br.close();
            }
         } catch (IOException e) {
            // error trying to close writer ...
         }
      }

      return jsonData;
   }

   /**
    * Save the specified JSONObject in serialized form to the specified file or
    * throw the appropriate exception.
    * 
    * @param textFileName
    *           fully qualified name of the JSON file to be saved
    * @param content
    *           the content to be saved to a file.
    * @throws Exception
    *            {@link IOException}) if there is a problem writing the file
    */
   static public void saveTextFile(String textFileName, String content)
      throws Exception {
      if (content == null) {
         throw new InvalidObjectException("content is null");
      }
      if (textFileName == null || textFileName.trim().length() == 0) {
         throw new InvalidTargetObjectTypeException(
            "Output filename is null or empty.");
      }
      BufferedWriter br = null;
      try {
         File outputFile = new File(textFileName);
         br = new BufferedWriter(new FileWriter(outputFile));
         br.write(content);
      } catch (IOException e) {
         throw new IOException("Can not write file \"" + textFileName + "\"",
            e);
      } finally {
         try {
            br.close();
         } catch (IOException e) {
            // error trying to close writer ...
         }
      }

      return;
   }

   /**
    * Shortens a long string to show the first maxLen characters and appends
    * "..."
    * 
    * @param input
    * @param maxLen
    * @return shortened version of the supplied input truncated at supplied maxLen with ellipses appended
    */
   static public String shortenString(String input, int maxLen) {
      if (maxLen < 2) {
         return input;
      }
      if (input == null) {
         return input;
      }
      if (input.length() < maxLen) {
         return input;
      }
      return input.substring(0, maxLen) + "...";
   }

   /**
    * Converts the input Date to {@link MDfromHTMLConstants#UNDEFINED_Date} iff the
    * input Date is null.
    * 
    * @param date
    *           Date to be tested against null and converted.
    * @return {@link MDfromHTMLConstants#UNDEFINED_Date} if the input Date was null,
    *         otherwise, the input Date is echoed back.
    */
   static public Date undefinedForNull(Date date) {
      if (date == null) {
         return MDfromHTMLConstants.UNDEFINED_Date;
      }
      return date;
   }

   /**
    * Converts the input Double to {@link MDfromHTMLConstants#UNDEFINED_Double} iff
    * the input Double is null.
    * 
    * @param DValue
    *           Double to be tested against null and converted.
    * @return {@link MDfromHTMLConstants#UNDEFINED_Double} if the input Double was
    *         null, otherwise, the input Double is echoed back.
    */
   static public Double undefinedForNull(Double DValue) {
      if (DValue == null) {
         return MDfromHTMLConstants.UNDEFINED_Double;
      }
      return DValue;
   }

   /**
    * Converts the input Integer to {@link MDfromHTMLConstants#UNDEFINED_Integer} iff
    * the input Integer is null.
    * 
    * @param intInput
    *           Integer to be tested against null and converted.
    * @return {@link MDfromHTMLConstants#UNDEFINED_Integer} if the input Integer was
    *         null, otherwise, the input Integer is echoed back.
    */
   static public Integer undefinedForNull(Integer intInput) {
      if (intInput == null) {
         return MDfromHTMLConstants.UNDEFINED_Integer;
      }
      return intInput;
   }

   /**
    * Converts the input String to {@link MDfromHTMLConstants#UNDEFINED_String} iff
    * the input String is null or empty after being trimmed.
    * 
    * @param strValue
    *           String to be tested against null or an empty string after being
    *           trimmed, and converted to the
    *           {@link MDfromHTMLConstants#UNDEFINED_String}.
    * @return {@link MDfromHTMLConstants#UNDEFINED_String} if the input String was
    *         null, otherwise, the input String is echoed back.
    */
   static public String undefinedForNull(String strValue) {
      if (strValue == null) {
         return MDfromHTMLConstants.UNDEFINED_String;
      } else if (strValue.trim().length() == 0) {
         return MDfromHTMLConstants.UNDEFINED_String;
      }
      return strValue;
   }

   /**
    * Clean up markdown escape sequences for \. and \-
    * 
    * @param mdLine
    *           markdown line to be cleansed
    * @return cleansed markdown line
    */
   static public String unescapeMarkdown(String mdLine) {
      // order is important so hyphens aren't turned into periods
      String result = mdLine.replaceAll("\\\\-", "\\-");
      result = result.replaceAll("\\\\.", "\\.");
      return result;
   }

}
