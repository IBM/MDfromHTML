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

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.rmi.dgc.VMID;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Random;

/**
 *
 */
public class MDfromHTMLConstants implements Serializable {

   /**
    * The default encryption key used for properties files. This can be
    * overridden by setting the ENV_MDfromHTML_KEY in the environment.
    */
   static public final char[] MDfromHTML_DEFAULT_PROPERTY_FILE_KEY = "MDfromHTML!"
      .toCharArray();

   static public final String ERRANT_JSON_STRING = "JSON Generation Error: []";

   static final public String IPC_DEFAULT_TOPIC = "MDfromHTML";

   static final public String MDfromHTML_HOME_DIRECTORY_NAME = "MDfromHTML";
   
   static public final String MDfromHTML_DIR_PROPERTIES = "properties"
      + File.separator;
   
   static public final String MDfromHTML_SVCS_PropertiesFileName = "MDfromHTML.properties";
   
   static public final String MDfromHTML_SVCS_HOST_NAME_PROP = "hostname";

   static public final String MDfromHTML_SVCS_PORT_PROP = "port";

   static public final String MDfromHTML_SVCS_PROTOCOL_PROP = "protocol";

   static public final String MDfromHTML_SVCS_SERVLET_NAME_PROP = "servletname";

   static public final String MDfromHTML_SVCS_VERSION_PROP = "version";

   static final public String MDfromHTML_SVCS_LOGGER_DIRECTORY = "logger.directory";

   static final public String MDfromHTML_SVCS_LOGGER_STEM_NAME = "logger.stem.name";

   static final public String MDfromHTML_SVCS_LOGGER_MAX_SIZE = "logger.max.size";

   static final public String MDfromHTML_SVCS_LOGGER_MAX_NUMBER = "logger.max.number";

   static final public String MDfromHTML_SVCS_LOGGER_FILE_LEVEL = "logger.file.level";

   static final public String MDfromHTML_SVCS_LOGGER_CONSOLE_LEVEL = "logger.console.level";
   
   static public final String APPLICATION_NAME = "MDfromHTML";

   static public final String ENV_MDfromHTML_HOME = "MDfromHTML_HOME";

   static public final String MDfromHTML_IPC_PropertiesFileName = "MDfromHTMLIPC.properties";

   static public final String MDfromHTML_DELIMITER = "~";

   static public final int DAY_MS = 86400000;
   
   static public final String MDfromHTML_TRACE_FILE_NAME = "MDfromHTMLTrace.log";

   static public final String MDfromHTML_TRACE_SPEC = "*=all=disabled";

   static public final String MDfromHTML_DOMAIN = "MDfromHTML.com";
   
   static public final String MDfromHTML_EMAIL_DOMAIN = "@" + MDfromHTML_DOMAIN;
   
   static public final String INTERNAL_ActorEmail = "MDfromHTML_internal@" + MDfromHTML_DOMAIN;

   /**
    * Equal to new Double(0), this is used to initialize values, typically used
    * for optionally present values.
    */
   static public final Double EMPTY_Double = new Double(0);

   /**
    * Equal to new Float(0f), this is used to initialize values, typically used
    * for optionally present values.
    */
   static public final Float EMPTY_Float = new Float(0f);

   /**
    * Equal to new Integer(0), this is used to initialize values, typically used
    * for optionally present values.
    */
   static public final Integer EMPTY_Integer = new Integer(0);

   /**
    * Equal to new Long(0L), this is used to initialize values, typically used
    * for optionally present values.
    */
   static public final Long EMPTY_Long = new Long(0L);

   /**
    * Equal to new Short((short)0), this is used to initialize values, typically
    * used for optionally present values.
    */
   static public final Short EMPTY_Short = new Short((short) 0);

   /**
    * Equal to "", this is used to initialize values, typically used for
    * optionally present values (e.g., descriptions, notes, long names).
    */
   static public final String EMPTY_String = "";

   static public final DecimalFormat FMT = new DecimalFormat("#0.000");

   static public final byte[] HEX_BYTES = new byte[] {
      48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 65, 66, 67, 68, 69, 70
   };

   static public final String HEX_CHARS = "0123456789ABCDEF";

   static public final int HOUR_MS = 3600000;

   static public final String IPC_MAX_MESSAGE_SIZE = "IPC_MAX_MESSAGE_SIZE";

   static public final int MIN_MS = 60000;

   /**
    * If this class is initialized in a different JVM is started at the same
    * millisecond, its s_r will generate the same sequence of random numbers.
    */
   static public Random SEED_RANDOM = new Random();

   static public SecureRandom SEED_SECURE_RANDOM = null;

   private static final long serialVersionUID = -439955604090390701L;

   /**
    * Equal to 9223372036854775806L (Long.MAX_VALUE - 1), this is used to
    * identify and/or initialize a BigInteger value that has yet to receive a
    * valid value. Typically used for comparisons to glean if the value has been
    * set properly.
    */
   static public final BigInteger UNDEFINED_BigInteger = new BigInteger(
      new Long(Long.MAX_VALUE - 1).toString());

   /**
    * Equal to null, this is used to identify and/or initialize a Boolean value
    * that has yet to receive a valid value. Typically used for comparisons to
    * glean if the value has been set properly. There is no equivalent value for
    * boolean as it can only have two states (true or false).
    */
   static public final Boolean UNDEFINED_Boolean = null;

   /**
    * Equal to 126 (Byte.MAX_VALUE - 1), this is used to identify and/or
    * initialize a byte value that has yet to receive a valid value. Typically
    * used for comparisons to glean if the value has been set properly.
    */
   static public final byte UNDEFINED_byte = (byte) (Byte.MAX_VALUE - 1);

   /**
    * Equal to 126, this is used to identify and/or initialize a Byte value that
    * has yet to receive a valid value. Typically used for comparisons to glean
    * if the value has been set properly.
    */
   static public final Byte UNDEFINED_Byte = new Byte(UNDEFINED_byte);

   /**
    * Equal to 65534 (Character.MAX_VALUE-1), this is used to identify and/or
    * initialize a char value that has yet to receive a valid value. Typically
    * used for comparisons to glean if the value has been set properly.
    */
   static public final char UNDEFINED_char = (char) (Character.MAX_VALUE - 1);

   /**
    * Equal to Character.MAX_VALUE-1 (65534), this is used to identify and/or
    * initialize a Character value that has yet to receive a valid value.
    * Typically used for comparisons to glean if the value has been set
    * properly.
    */
   static public final Character UNDEFINED_Character = new Character(
      UNDEFINED_char);

   static public Class<?> UNDEFINED_Class = null;

   /**
    * Equal to one millisecond after the epoch date (midnight 1/1/1970
    * 00:00:00.001), this is used to identify and/or initialize a Date value
    * that has yet to receive a valid value. Typically used for comparisons to
    * glean if the value has been set properly.
    */
   static public final Date UNDEFINED_Date = new Date(1);

   /**
    * Equal to Double.longBitsToDouble(0x7feffffffffffffeL), this is used to
    * identify and/or initialize a double value that has yet to receive a valid
    * value. Typically used for comparisons to glean if the value has been set
    * properly.
    */
   static public final double UNDEFINED_double = 1.7976931348623155E308;

   /**
    * Equal to Double.longBitsToDouble(0x7feffffffffffffeL), this is used to
    * identify and/or initialize a Double value that has yet to receive a valid
    * value. Typically used for comparisons to glean if the value has been set
    * properly.
    */
   static public final Double UNDEFINED_Double = new Double(UNDEFINED_double);

   /**
    * Equal to Float.intBitsToFloat(0x7f7ffffe), this is used to identify and/or
    * initialize a float value that has yet to receive a valid value. Typically
    * used for comparisons to glean if the value has been set properly.
    */
   static public final float UNDEFINED_float = Float.intBitsToFloat(0x7f7ffffe); // (Float.NaN-1);

   /**
    * Equal to Float.intBitsToFloat(0x7f7ffffe), this is used to identify and/or
    * initialize a Float value that has yet to receive a valid value. Typically
    * used for comparisons to glean if the value has been set properly.
    */
   static public final Float UNDEFINED_Float = new Float(UNDEFINED_float);

   static public final String UNDEFINED_ID = "??????????????????????";

   /**
    * Equal to 2147483646 (Integer.MAX_VALUE - 1), this is used to identify
    * and/or initialize an int value that has yet to receive a valid value.
    * Typically used for comparisons to glean if the value has been set
    * properly.
    */
   static public final int UNDEFINED_int = Integer.MAX_VALUE - 1;
   /**
    * Equal to 2147483646, this is used to identify and/or initialize a Integer
    * value that has yet to receive a valid value. Typically used for
    * comparisons to glean if the value has been set properly.
    */
   static public final Integer UNDEFINED_Integer = new Integer(UNDEFINED_int);
   /**
    * Equal to 9223372036854775806L (Long.MAX_VALUE - 1), this is used to
    * identify and/or initialize a long value that has yet to receive a valid
    * value. Typically used for comparisons to glean if the value has been set
    * properly.
    * 
    * Note: altered for Javascript compatibility Number.MAX_SAFE_INTEGER-1
    */
   static public final long UNDEFINED_long = 9007199254740990L;
   /**
    * Equal to 9223372036854775806, this is used to identify and/or initialize a
    * Long value that has yet to receive a valid value. Typically used for
    * comparisons to glean if the value has been set properly.
    */
   static public final Long UNDEFINED_Long = new Long(UNDEFINED_long);
   /**
    * Equal to -1, this is used to identify and/or initialize an int value that
    * is intended to be nonnegative but that has yet to receive a valid (i.e.,
    * >= 0) value. Typically used for comparisons to glean if the value has been
    * set properly. Also useful for initializing loop-control variables.
    */
   static public final int UNDEFINED_nonnegative_int = -1;
   /**
    * Equal to -1, this is used to identify and/or initialize an Integer value
    * that is intended to be nonnegative but that has yet to receive a valid
    * (i.e., >= 0) value. Typically used for comparisons to glean if the value
    * has been set properly. Also useful for initializing loop-control
    * variables.
    */
   static public final Integer UNDEFINED_Nonnegative_Integer = new Integer(-1);
   /**
    * Equal to -1L, this is used to identify and/or initialize a long value that
    * is intended to be nonnegative but that has yet to receive a valid (i.e.,
    * >= 0) value. Typically used for comparisons to glean if the value has been
    * set properly. Also useful for initializing loop-control variables.
    */
   static public final long UNDEFINED_nonnegative_long = -1L;
   /**
    * Equal to -1, this is used to identify and/or initialize a Long value that
    * is intended to be nonnegative but that has yet to receive a valid (i.e.,
    * >= 0) value. Typically used for comparisons to glean if the value has been
    * set properly. Also useful for initializing loop-control variables.
    */
   static public final Long UNDEFINED_Nonnegative_Long = new Long(-1);
   /**
    * Equal to -1L, this is used to identify and/or initialize a short value
    * that is intended to be nonnegative but that has yet to receive a valid
    * (i.e., >= 0) value. Typically used for comparisons to glean if the value
    * has been set properly. Also useful for initializing loop-control
    * variables.
    */
   static public final short UNDEFINED_nonnegative_short = -1;
   /**
    * Equal to -1, this is used to identify and/or initialize a Short value that
    * is intended to be nonnegative but that has yet to receive a valid (i.e.,
    * >= 0) value. Typically used for comparisons to glean if the value has been
    * set properly. Also useful for initializing loop-control variables.
    */
   static public final Short UNDEFINED_Nonnegative_Short = new Short(
      (short) -1);
   /**
    * Equal to 0, this is used to identify and/or initialize an int value that
    * is intended to be positive but that has yet to receive a valid (i.e., > 0)
    * value. Typically used for comparisons to glean if the value has been set
    * properly. Also useful for initializing loop-control variables.
    */
   static public final int UNDEFINED_positive_int = -1;

   /**
    * Equal to 0, this is used to identify and/or initialize an Integer value
    * that is intended to be positive but that has yet to receive a valid (i.e.,
    * > 0) value. Typically used for comparisons to glean if the value has been
    * set properly. Also useful for initializing loop-control variables.
    */
   static public final Integer UNDEFINED_Positive_Integer = new Integer(-1);

   /**
    * Equal to 0, this is used to identify and/or initialize a long value that
    * is intended to be positive but that has yet to receive a valid (i.e., > 0)
    * value. Typically used for comparisons to glean if the value has been set
    * properly. Also useful for initializing loop-control variables.
    */
   static public final long UNDEFINED_positive_long = -1L;

   /**
    * Equal to 0, this is used to identify and/or initialize a Long value that
    * is intended to be positive but that has yet to receive a valid (i.e., > 0)
    * value. Typically used for comparisons to glean if the value has been set
    * properly. Also useful for initializing loop-control variables.
    */
   static public final Long UNDEFINED_Positive_Long = new Long(-1);

   /**
    * Equal to -1, this is used to identify and/or initialize a short value that
    * is intended to be positive but that has yet to receive a valid (i.e., > 0)
    * value. Typically used for comparisons to glean if the value has been set
    * properly. Also useful for initializing loop-control variables.
    */
   static public final short UNDEFINED_positive_short = -1;

   /**
    * Equal to 0, this is used to identify and/or initialize a Short value that
    * is intended to be positive but that has yet to receive a valid (i.e., > 0)
    * value. Typically used for comparisons to glean if the value has been set
    * properly. Also useful for initializing loop-control variables.
    */
   static public final Short UNDEFINED_Positive_Short = new Short((short) -1);

   /**
    * Equal to 32766 (Short.MAX_VALUE - 1), this is used to identify and/or
    * initialize a short value that has yet to receive a valid value. Typically
    * used for comparisons to glean if the value has been set properly.
    */
   static public final short UNDEFINED_short = (short) (Short.MAX_VALUE - 1);

   /**
    * Equal to 32766, this is used to identify and/or initialize a short value
    * that has yet to receive a valid value. Typically used for comparisons to
    * glean if the value has been set properly.
    */
   static public final Short UNDEFINED_Short = new Short(UNDEFINED_short);

   /**
    * Equal to a question mark, this is used to identify and/or initialize a
    * String that has yet to receive a valid value. Typically used for
    * comparisons to glean if the value has been set properly.
    */
   static public final String UNDEFINED_String = "?";

   /**
    * Equal to "//", this is used to identify and/or initialize a URI that has
    * yet to receive a valid value. Typically used for comparisons to glean if
    * the value has been set properly.
    */
   static public URI UNDEFINED_URI = null;

   static final public Charset UTF8_CHARSET = Charset.forName("UTF-8");

   static {
      try {
         SEED_SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");
         SEED_SECURE_RANDOM.setSeed(new VMID().toString().getBytes());
      } catch (NoSuchAlgorithmException nsae) {
         System.out.println(
            "Unable to retrieve algorithm SHA1PRNG for unique id generation.");
      }
   }

   static public final String MDfromHTML_DIR_DATA = "data" + File.separator;

   static public final String MDfromHTML_SVCS_SCHEMA_FILENAME = "MDfromHTML_RESTServicesSchema.json";

}
