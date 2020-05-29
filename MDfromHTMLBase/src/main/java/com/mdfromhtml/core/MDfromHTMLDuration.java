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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Utility class for durations in time.
 * 
 * @author wnm3
 */
public class MDfromHTMLDuration implements Serializable, Comparable<MDfromHTMLDuration> {

   // --------
   // statics
   // --------
   static final public String m_strClassName = MDfromHTMLUtils
      .getNameFromClass(MDfromHTMLDuration.class);

   private static final long serialVersionUID = 2151322379607630650L;

   /**
    * An undefined version of this object. An undefinedMDfromHTMLDuration has an
    * undefined name.
    * 
    * @see MDfromHTMLUtils#isUndefined(String)
    */
   static public MDfromHTMLDuration UNDEFINED_MDfromHTMLDuration = new MDfromHTMLDuration();

   /**
    * Calculate the number of elapsed days between the start and end of this
    * duration. If no end date has been set, then the duration is 0L. If no
    * start date has been set, then the duration is the number of days since the
    * epoch (midnight 1/1/70 GMT) until the end date. If the start date is
    * greater than the end date, then 0L is returned.
    * 
    * @param dateStart
    *           the start date of the duration
    * @param dateEnd
    *           the end date of the duration
    * @return the number of elapsed days between the start and end of this
    *         duration.
    */
   static public long elapsedDays(Date dateStart, Date dateEnd) {
      long lElapsedTime = elapsedHours(dateStart, dateEnd);
      lElapsedTime /= 24L;
      return lElapsedTime;
   }

   /**
    * Calculate the number of elapsed hours between the start and end of this
    * duration. If no end date has been set, then the duration is 0L. If no
    * start date has been set, then the duration is the number of hours since
    * the epoch (midnight 1/1/70 GMT) until the end date. If the start date is
    * greater than the end date, then 0L is returned.
    * 
    * @param dateStart
    *           the start date of the duration
    * @param dateEnd
    *           the end date of the duration
    * @return the number of elapsed hours between the start and end of this
    *         duration.
    */
   static public long elapsedHours(Date dateStart, Date dateEnd) {
      long lElapsedTime = elapsedMinutes(dateStart, dateEnd);
      lElapsedTime /= 60L;
      return lElapsedTime;
   }

   /**
    * Calculate the number of elapsed minutes between the start and end of this
    * duration. If no end date has been set, then the duration is 0L. If no
    * start date has been set, then the duration is the number of minutes since
    * the epoch (midnight 1/1/70 GMT) until the end date. If the start date is
    * greater than the end date, then 0L is returned.
    * 
    * @param dateStart
    *           the start date of the duration
    * @param dateEnd
    *           the end date of the duration
    * @return the number of elapsed minutes between the start and end of this
    *         duration.
    */
   static public long elapsedMinutes(Date dateStart, Date dateEnd) {
      long lElapsedTime = elapsedSeconds(dateStart, dateEnd);
      lElapsedTime /= 60L;
      return lElapsedTime;
   }

   /**
    * Calculate the number of elapsed seconds between the start and end of this
    * duration. If no end date has been set, then the duration is 0L. If no
    * start date has been set, then the duration is the number of seconds since
    * the epoch (midnight 1/1/70 GMT) until the end date. If the start date is
    * greater than the end date, then 0L is returned.
    * 
    * @param dateStart
    *           the start date of the duration
    * @param dateEnd
    *           the end date of the duration
    * @return the number of elapsed seconds between the start and end of this
    *         duration.
    */
   static public long elapsedSeconds(Date dateStart, Date dateEnd) {
      long lElapsedTime = elapsedTime(dateStart, dateEnd);
      lElapsedTime /= 1000L;
      return lElapsedTime;
   }

   /**
    * Calculate the number of elapsed milliseconds between the start and end of
    * this duration. If no end date has been set, then the duration is 0L. If no
    * start date has been set, then the duration is the number of milliseconds
    * since the epoch (midnight 1/1/70 GMT) until the end date. If the start
    * date is greater than the end date, then the negative of the elapsed time
    * from the end date to the start date is returned.
    * 
    * @param dateStart
    *           the start date of the duration
    * @param dateEnd
    *           the end date of the duration
    * @return the number of elapsed milliseconds between the start and end of
    *         this duration.
    */
   static public long elapsedTime(Date dateStart, Date dateEnd) {
      dateStart = MDfromHTMLUtils.undefinedForNull(dateStart);
      dateEnd = MDfromHTMLUtils.undefinedForNull(dateEnd);

      if (MDfromHTMLDate.isUndefined(dateEnd) == true) {
         return 0L;
      }
      if (MDfromHTMLDate.isUndefined(dateStart) == true) {
         return dateEnd.getTime();
      }
      if (dateStart.compareTo(dateEnd) > 0) {
         return 0L - elapsedTime(dateEnd, dateStart);
      }
      return dateEnd.getTime() - dateStart.getTime();
   }

   static public String formattedElapsedTime(Date dateStart, Date dateEnd) {
      long lElapsedMS = elapsedTime(dateStart, dateEnd);
      return formattedElapsedTime(lElapsedMS);
   }

   static public String formattedElapsedTime(double dElapsedSeconds) {
      long lElapsedMS = (long) (dElapsedSeconds * 1000L);
      return formattedElapsedTime(lElapsedMS);
   }

   static public String formattedElapsedTime(long lElapsedMS) {
      String strSign = MDfromHTMLConstants.EMPTY_String;
      if (lElapsedMS < 0L) {
         strSign = "-";
         lElapsedMS = -lElapsedMS;
      }
      long lElapsedMilli = lElapsedMS % 1000;
      lElapsedMS /= 1000;
      StringBuffer sb = new StringBuffer();
      long lDays = lElapsedMS / 86400;
      lElapsedMS = lElapsedMS % 86400;
      long lHours = lElapsedMS / 3600;
      lElapsedMS = lElapsedMS % 3600;
      long lMinutes = lElapsedMS / 60;
      lElapsedMS = lElapsedMS % 60;
      if (MDfromHTMLUtils.isEmpty(strSign) == false) {
         sb.append("-");
      }
      sb.append(lDays);
      sb.append("-");
      sb.append(MDfromHTMLUtils.padLeftZero((int) lHours, 2));
      sb.append(":");
      sb.append(MDfromHTMLUtils.padLeftZero((int) lMinutes, 2));
      sb.append(":");
      sb.append(MDfromHTMLUtils.padLeftZero((int) lElapsedMS, 2));
      sb.append(".");
      sb.append(MDfromHTMLUtils.padLeftZero((int) lElapsedMilli, 3));
      return sb.toString();
   }

   public static void main(String[] args) {
      try {
         String strBirthday = MDfromHTMLUtils.prompt(
            "Enter your birthday in the form: YYYY/MM/DD-hh:mm:ss.SSS(ZZZ):");
         if (strBirthday == null || strBirthday.length() == 0) {
            strBirthday = "1957/08/04-07:15:00.000(EDT)";
         }
         MDfromHTMLDuration dur = new MDfromHTMLDuration("Birthday",
            new MDfromHTMLDate(strBirthday), new MDfromHTMLDate());
         String strTimeZone = MDfromHTMLUtils.prompt(
            "Enter the output timezone (e.g., -0500 for EST or +0000 for GMT):");
         System.out.println(dur.toString(strTimeZone));
         MDfromHTMLDuration negdur = new MDfromHTMLDuration("Reverse", dur.getEndDate(),
            dur.getStartDate());
         System.out.println(negdur.toString(strTimeZone));
      } catch (Exception aer) {
         aer.printStackTrace();
      }
      System.out.println("Goodbye");
   }

   /**
    * Create anMDfromHTMLDuration from the passed list string.
    * 
    * @param listString
    *           the list of String fields needed to create anMDfromHTMLDuration (name,
    *           start date, end date);
    * @return a newly createdMDfromHTMLDuration object filled with the content of the
    *         supplied listString. If the listString is null or empty, or does
    *         not contain at least the name field, an undefinedMDfromHTMLDuration is
    *         returned.
    * @throws Exception
    *            if listString is null or empty, or if the name in the list is
    *            null, empty or undefined.
    * @see #isUndefined()
    */
   static public MDfromHTMLDuration newInstanceFromListString(String listString)
      throws Exception {
      if (listString == null || listString.length() == 0) {
         throw new Exception(
            "String listString is null or empty.");
      }
      MDfromHTMLDuration duration = MDfromHTMLDuration.UNDEFINED_MDfromHTMLDuration;
      ArrayList<Object> list = new ArrayList<Object>();
      list = MDfromHTMLUtils.listStringToArrayList(listString);
      // process what we got from the listString
      MDfromHTMLDate dateEnd = MDfromHTMLDate.UNDEFINED_MDfromHTMLDate;
      MDfromHTMLDate dateStart = MDfromHTMLDate.UNDEFINED_MDfromHTMLDate;
      for (int i = 0; i < list.size(); i++) {
         switch (i) {
            case 0: {
               // Name
               if (list.get(i) instanceof String) {
                  duration.setName((String) list.get(i));
               }
               break;
            }
            case 1: {
               // Start Date
               if (list.get(i) instanceof String) {
                  try {
                     dateStart = new MDfromHTMLDate((String) list.get(i));
                  } catch (Exception e) {
                     // use the undefined date
                  }
                  duration.setStartDate(dateStart);
               }
               break;
            }
            case 2: {
               // End Date
               if (list.get(i) instanceof String) {
                  try {
                     dateEnd = new MDfromHTMLDate((String) list.get(i));
                  } catch (Exception e) {
                     // use the undefined date
                  }
                  duration.setEndDate(dateEnd);
               }
               break;
            }
         }
      } // end for loop
      return duration;
   }

   /**
    * Converts the inputMDfromHTMLDuration to an undefinedMDfromHTMLDuration if the input
    * MDfromHTMLDuration is null.
    * 
    * @param duration
    *           MDfromHTMLDuration to be tested against null and converted.
    * @return An undefinedMDfromHTMLDuration if the inputMDfromHTMLDuration was null,
    *         otherwise, the inputMDfromHTMLDuration is echoed back.
    */
   static public MDfromHTMLDuration undefinedForNull(MDfromHTMLDuration duration) {
      if (duration == null) {
         return UNDEFINED_MDfromHTMLDuration;
      }
      return duration;
   }

   MDfromHTMLDate m_dateEnd = MDfromHTMLDate.UNDEFINED_MDfromHTMLDate;

   MDfromHTMLDate m_dateStart = MDfromHTMLDate.UNDEFINED_MDfromHTMLDate;

   String m_strName = MDfromHTMLConstants.UNDEFINED_String;

   /**
    * Construct an undefinedMDfromHTMLDuration.
    */
   public MDfromHTMLDuration() {
      initDuration(null, null, null);
   }

   /**
    * Construct a well formedMDfromHTMLDuration
    * 
    * @param strName
    *           name of this duration
    */
   public MDfromHTMLDuration(String strName) {
      initDuration(strName, null, null);
   }

   /**
    * Construct a well formedMDfromHTMLDuration
    * 
    * @param strName
    *           name of this duration
    * @param dateStart
    *           start date of this duration
    * @param dateEnd
    *           end date of this duration
    */
   public MDfromHTMLDuration(String strName, MDfromHTMLDate dateStart, MDfromHTMLDate dateEnd) {
      initDuration(strName, dateStart, dateEnd);
   }

   /*
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   public int compareTo(MDfromHTMLDuration o1) {
      int iRC = 0;
      if (o1 == null || ((o1 instanceof MDfromHTMLDuration) == false)) {
         return -1;
      }
      iRC = getStartDate().compareTo(((MDfromHTMLDuration) o1).getStartDate());
      if (iRC != 0) {
         return iRC;
      }
      iRC = getEndDate().compareTo(((MDfromHTMLDuration) o1).getEndDate());
      return iRC;
   }

   /**
    * Calculate the number of elapsed days between the start and end of this
    * duration. If no end date has been set, then the duration is 0L. If no
    * start date has been set, then the duration is the number of days since the
    * epoch (midnight 1/1/70 GMT) until the end date. If the start date is
    * greater than the end date, then 0L is returned.
    * 
    * @return the number of elapsed days between the start and end of this
    *         duration.
    */
   public long elapsedDays() {
      long lElapsedTime = elapsedHours();
      lElapsedTime /= 24L;
      return lElapsedTime;
   }

   /**
    * Calculate the number of elapsed hours between the start and end of this
    * duration. If no end date has been set, then the duration is 0L. If no
    * start date has been set, then the duration is the number of hours since
    * the epoch (midnight 1/1/70 GMT) until the end date. If the start date is
    * greater than the end date, then 0L is returned.
    * 
    * @return the number of elapsed hours between the start and end of this
    *         duration.
    */
   public long elapsedHours() {
      long lElapsedTime = elapsedMinutes();
      lElapsedTime /= 60L;
      return lElapsedTime;
   }

   /**
    * Calculate the number of elapsed minutes between the start and end of this
    * duration. If no end date has been set, then the duration is 0L. If no
    * start date has been set, then the duration is the number of minutes since
    * the epoch (midnight 1/1/70 GMT) until the end date. If the start date is
    * greater than the end date, then 0L is returned.
    * 
    * @return the number of elapsed minutes between the start and end of this
    *         duration.
    */
   public long elapsedMinutes() {
      long lElapsedTime = elapsedSeconds();
      lElapsedTime /= 60L;
      return lElapsedTime;
   }

   /**
    * Calculate the number of elapsed seconds between the start and end of this
    * duration. If no end date has been set, then the duration is 0L. If no
    * start date has been set, then the duration is the number of seconds since
    * the epoch (midnight 1/1/70 GMT) until the end date. If the start date is
    * greater than the end date, then 0L is returned.
    * 
    * @return the number of elapsed seconds between the start and end of this
    *         duration.
    */
   public long elapsedSeconds() {
      long lElapsedTime = elapsedTime();
      lElapsedTime /= 1000L;
      return lElapsedTime;
   }

   /**
    * Calculate the number of elapsed milliseconds between the start and end of
    * this duration. If no end date has been set, then the duration is 0L. If no
    * start date has been set, then the duration is the number of milliseconds
    * since the epoch (midnight 1/1/70 GMT) until the end date. If the start
    * date is greater than the end date, then 0L is returned.
    * 
    * @return the number of elapsed milliseconds between the start and end of
    *         this duration.
    */
   public long elapsedTime() {
      if (MDfromHTMLDate.isUndefined(m_dateEnd) == true) {
         return 0L;
      }
      if (MDfromHTMLDate.isUndefined(m_dateStart) == true) {
         return m_dateEnd.getTime();
      }
      if (m_dateStart.compareTo(m_dateEnd) > 0) {
         return 0L;
      }
      return m_dateEnd.getTime() - m_dateStart.getTime();
   }

   /*
    * @see java.lang.Object#equals(java.lang.Object)
    */
   public boolean equals(MDfromHTMLDuration o1) {
      return (compareTo(o1) == 0);
   }

   /**
    * @return the elapsed time of this duration formatted as a String containing
    *         an optional sign (if the duration is negative) followed by the
    *         days-hours:minutes:seconds.milliseconds
    */
   public String formattedElapsedTime() {
      return formattedElapsedTime(m_dateStart, m_dateEnd);
   }

   /**
    * @return the end date of this duration. Note: this may be set to an
    *         undefinedMDfromHTMLDate. Test for this condition using isUndefined().
    * @see MDfromHTMLDate#UNDEFINED_MDfromHTMLDate
    * @see MDfromHTMLDate#isUndefined()
    */
   public MDfromHTMLDate getEndDate() {
      return m_dateEnd;
   }

   /**
    * @return the name of this duration. Note: this may be set to
    *         UNDEFINED_String. Test for this condition using isUndefined().
    * @see MDfromHTMLConstants#UNDEFINED_String
    * @see MDfromHTMLUtils#isUndefined(String)
    */
   public String getName() {
      return m_strName;
   }

   /**
    * @return the start date of this duration. Note: this may be set to an
    *         undefinedMDfromHTMLDate. Test for this condition using isUndefined().
    * @see MDfromHTMLDate#UNDEFINED_MDfromHTMLDate
    * @see MDfromHTMLDate#isUndefined()
    */
   public MDfromHTMLDate getStartDate() {
      return m_dateStart;
   }

   /**
    * Initialize the content of this duration
    * 
    * @param strName
    *           name for this duration
    * @param dateStart
    *           start date for this duration
    * @param dateEnd
    *           end date for this duration
    */
   private void initDuration(String strName, MDfromHTMLDate dateStart,
      MDfromHTMLDate dateEnd) {
      m_strName = MDfromHTMLUtils.undefinedForNull(strName);
      m_dateStart = MDfromHTMLDate.undefinedForNull(dateStart);
      m_dateEnd = MDfromHTMLDate.undefinedForNull(dateEnd);
   }

   /**
    * Determine if thisMDfromHTMLDuration is undefined (e.g., if its name equals
    * UNDEFINED_String).
    * 
    * @return true if thisMDfromHTMLDuration is determined to be undefined. Otherwise,
    *         return false.
    * @see MDfromHTMLConstants#UNDEFINED_String
    */
   public boolean isUndefined() {
      return (getName().compareTo(MDfromHTMLConstants.UNDEFINED_String) == 0);
   }

   /**
    * Set the start and end dates for this duration. If null is passed for
    * either parameter, it is set to an undefinedMDfromHTMLDate.
    * 
    * @param dateStart
    *           the start date.
    * @param dateEnd
    *           the end date.
    * @see MDfromHTMLDate#UNDEFINED_MDfromHTMLDate
    * @see MDfromHTMLDate#isUndefined()
    */
   public void setDuration(MDfromHTMLDate dateStart, MDfromHTMLDate dateEnd) {
      setStartDate(dateStart);
      setEndDate(dateEnd);
   }

   /**
    * Set the end date for this duration. If null is passed, the endDate is set
    * to an undefinedMDfromHTMLDate.
    * 
    * @param endDate
    *           the end date.
    */
   public void setEndDate(MDfromHTMLDate endDate) {
      m_dateEnd = MDfromHTMLDate.undefinedForNull(endDate);
   }

   /**
    * Set the name for this duration.
    * 
    * @param strName
    *           the name.
    * @throws Exception
    *            if strName is null, empty, or undefined.
    * @see MDfromHTMLUtils#isUndefined(String)
    */
   public void setName(String strName) throws Exception {
      if (MDfromHTMLUtils.isUndefined(strName) || strName.length() == 0) {
         throw new Exception(
            "String strName is null, empty, or undefined.");
      }
      m_strName = MDfromHTMLUtils.undefinedForNull(strName);
   }

   /**
    * Set the start date for this duration. If null is passed, the startDate is
    * set to an undefinedMDfromHTMLDate.
    * 
    * @param startDate
    *           the start date.
    */
   public void setStartDate(MDfromHTMLDate startDate) {
      m_dateStart = MDfromHTMLDate.undefinedForNull(startDate);
   }

   /**
    * Generates a list string compatible with the Utils listStringToArrayList
    * method describing this duration.
    * 
    * @return a string list comprising delimited, quoted fields containing the
    *         name, start date, and end date of this duration, as well as the
    *         elapsed time formatted as DD-HH:MM:SS.mmm where DD is the number
    *         of days, HH is the number of hours, MM is the number of minutes,
    *         SS is the number of seconds, and mmm is the number of
    *         milliseconds.
    * @see MDfromHTMLUtils#listStringToArrayList(String)
    */
   public String toString() {
      return toString("+0000");
   }

   /**
    * Generates a list string compatible with the Utils listStringToArrayList
    * method describing this duration using the specified timezone for dates.
    * 
    * @param strTimeZone
    *           the ID for a TimeZone as a String containing a sign followed by
    *           the two digit hour and two digit minute offset from Greenwich
    *           Mean Time. For example, for Eastern Standard Time, submit
    *           "-0500". For Europe/Paris submit "+0100".
    * @return a string list comprising delimited, quoted fields containing the
    *         name, start date, and end date of this duration, as well as the
    *         elapsed time formatted as DD-HH:MM:SS.mmm where DD is the number
    *         of days, HH is the number of hours, MM is the number of minutes,
    *         SS is the number of seconds, and mmm is the number of
    *         milliseconds.
    * @see MDfromHTMLUtils#listStringToArrayList(String)
    */
   public String toString(String strTimeZone) {
      if (strTimeZone == null || strTimeZone.length() == 0) {
         strTimeZone = "+0000";
      }
      ArrayList<String> list = new ArrayList<String>();
      list.add(m_strName);
      list.add(m_dateStart.toString(strTimeZone));
      list.add(m_dateEnd.toString(strTimeZone));
      list.add(formattedElapsedTime(m_dateStart, m_dateEnd));
      return MDfromHTMLUtils.arrayListToListString(list);
   }

}
