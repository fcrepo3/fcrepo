package org.fcrepo.server.security.impl;

/*
 * @(#)Attribute.java
 *
 * Copyright 2003-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.DateTimeAttribute;


/**
 * Represents the AttributeType XML type found in the context schema.
 *
 * Localized for more efficient handling of values and a singleton impl
 * @since 1.0
 * @author Seth Proctor
 * @author barmintor
 */
public class SingletonAttribute extends AbstractAttribute {
   
   private final List<AttributeValue> attributeValues;

   /**
    * Creates a new <code>Attribute</code> of the type specified in the
    * given <code>AttributeValue</code>.
    *
    * @param id the id of the attribute
    * @param issuer the attribute's issuer or null if there is none
    * @param issueInstant the moment when the attribute was issued, or null
    *                     if it's unspecified
    * @param value the actual value associated with the attribute meta-data
    */
   public SingletonAttribute(URI id, String issuer, DateTimeAttribute issueInstant,
         AttributeValue value) {
      super(id, issuer, issueInstant);
      if (value != null){
          this.attributeValues = Collections.singletonList(value);
          this.type = value.getType();
      } else {
          this.attributeValues = Collections.emptyList();
      }
   }
   
   @Override
   public List<AttributeValue> getValues() {
      return this.attributeValues;
   }

   @Override
   public AttributeValue getValue() {
      if(this.attributeValues.isEmpty()) return null;
      return this.attributeValues.get(0);
   }

}