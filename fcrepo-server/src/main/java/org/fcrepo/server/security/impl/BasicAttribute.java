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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fcrepo.server.security.Attribute;
import org.jboss.security.xacml.sunxacml.ParsingException;
import org.jboss.security.xacml.sunxacml.SunxacmlUtil;
import org.jboss.security.xacml.sunxacml.UnknownIdentifierException;
import org.jboss.security.xacml.sunxacml.attr.AttributeFactory;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.DateTimeAttribute;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Represents the AttributeType XML type found in the context schema.
 *
 * Localized for more efficient handling of values and a singleton impl
 * @since 1.0
 * @author Seth Proctor
 * @author barmintor
 */
public class BasicAttribute extends AbstractAttribute {
   
   //SECURITY-157: support multiple values
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
   public BasicAttribute(URI id, String issuer, DateTimeAttribute issueInstant,
         AttributeValue value) {
      super(id, issuer, issueInstant);
      this.attributeValues = new ArrayList<AttributeValue>();
      this.attributeValues.add(value); 
      if(value != null)
        this.type = value.getType();
   }

   public BasicAttribute(URI id, URI type, String issuer, DateTimeAttribute issueInstant,
         Collection<AttributeValue> values) {
       super(id, issuer, issueInstant);
       this.attributeValues = new ArrayList<AttributeValue>(values);
       this.type = type;
   }
   
   /**
    * Creates a new <code>BasicAttribute</code>
    *
    * @deprecated As of version 1.1, replaced by
    *        {@link #Attribute(URI,String,DateTimeAttribute,AttributeValue)}.
    *             This constructor has some ambiguity in that it allows a
    *             specified datatype and a value that already has some
    *             associated datatype. The new constructor clarifies this
    *             issue by removing the datatype parameter and using the
    *             datatype specified by the given value.
    *
    * @param id the id of the attribute
    * @param type the type of the attribute
    * @param issuer the attribute's issuer or null if there is none
    * @param issueInstant the moment when the attribute was issued, or null
    *                     if it's unspecified
    * @param value the actual value associated with the attribute meta-data
    */
   public BasicAttribute(URI id, URI type, String issuer,
         DateTimeAttribute issueInstant, AttributeValue value) 
   {
      this(id,issuer,issueInstant,value); 
      this.type = type; 
   }

   /**
    * Creates an instance of an <code>Attribute</code> based on the root DOM
    * node of the XML data.
    *
    * @param root the DOM root of the AttributeType XML type
    *
    * @return the attribute
    *
    * throws ParsingException if the data is invalid
    */
   public static Attribute getInstance(Node root) throws ParsingException {
      final URI id;
      final URI type;
      String issuer = null;
      DateTimeAttribute issueInstant = null;
      AttributeValue value = null;
      
      Set<AttributeValue> valueSet = null;

      AttributeFactory attrFactory = AttributeFactory.getInstance();

      // First check that we're really parsing an Attribute
      if (! SunxacmlUtil.getNodeName(root).equals("Attribute")) {
         throw new ParsingException("Attribute object cannot be created " +
               "with root node of type: " +
               SunxacmlUtil.getNodeName(root));
      }

      NamedNodeMap attrs = root.getAttributes();

      try {
         id = new URI(attrs.getNamedItem("AttributeId").getNodeValue());
      } catch (Exception e) {
         throw new ParsingException("Error parsing required attribute " +
               "AttributeId in AttributeType", e);
      }

      try {
         type = new URI(attrs.getNamedItem("DataType").getNodeValue());
      } catch (Exception e) {
         throw new ParsingException("Error parsing required attribute " +
               "DataType in AttributeType", e);
      }            

      try {
         Node issuerNode = attrs.getNamedItem("Issuer");
         if (issuerNode != null)
            issuer = issuerNode.getNodeValue();

         Node instantNode = attrs.getNamedItem("IssueInstant");
         if (instantNode != null)
            issueInstant = DateTimeAttribute.
            getInstance(instantNode.getNodeValue());
      } catch (Exception e) {
         // shouldn't happen, but just in case...
         throw new ParsingException("Error parsing optional AttributeType"
               + " attribute", e);
      }

      // now we get the attribute value
      NodeList nodes = root.getChildNodes();
      for (int i = 0; i < nodes.getLength(); i++) {
         Node node = nodes.item(i);
         if (SunxacmlUtil.getNodeName(node).equals("AttributeValue")) {
            // only one value can be in an Attribute
            
            /* 
             * SECURITY-157: multiple values
             * 
             * if (value != null)
               throw new ParsingException("Too many values in Attribute");
             */
            // now get the value
            try {
               value = attrFactory.createValue(node, type);
            } catch (UnknownIdentifierException uie) {
               throw new ParsingException("Unknown AttributeId", uie);
            }
            if(valueSet == null)
               valueSet = new HashSet<AttributeValue>(); 
            valueSet.add(value);
         }
      }

      // make sure we got a value
      if (value == null)
         throw new ParsingException("Attribute must contain a value");

      return new BasicAttribute(id, type, issuer, issueInstant, valueSet);
   }

   @Override
   public List<AttributeValue> getValues()
   {
      return this.attributeValues;
   }

   @Override
   public AttributeValue getValue() 
   {
      if(this.attributeValues.isEmpty()) return null;
      return this.attributeValues.get(0);
   }

}