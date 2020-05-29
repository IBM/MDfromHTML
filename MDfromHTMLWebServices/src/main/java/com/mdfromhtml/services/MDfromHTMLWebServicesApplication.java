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


import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class MDfromHTMLWebServicesApplication extends Application {
   private Set<Object> singletons = new HashSet<Object>();
   private Set<Class<?>> classes = new HashSet<Class<?>>();
 
   public MDfromHTMLWebServicesApplication() {
      singletons.add(new MDfromHTMLWebServices());
   }
 
   @Override
   public Set<Object> getSingletons() {
      return singletons;
   }
   public Set<Class<?>> getClasses() {
      classes.add(MDfromHTMLWebServices.class);
      return classes;
   }

}
