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

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;

import org.fcrepo.server.security.Attribute;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.jboss.security.xacml.sunxacml.Indenter;
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
public abstract class AbstractAttribute implements Attribute {

    // required meta-data attributes
    protected URI id;
    protected URI type;

    // optional meta-data attributes
    protected String issuer = null;
    protected DateTimeAttribute issueInstant = null;

   /**
    * Creates a new <code>Attribute</code> of the type specified in the
    * given <code>AttributeValue</code>.
    *
    * @param id the id of the attribute
    * @param issuer the attribute's issuer or null if there is none
    * @param issueInstant the moment when the attribute was issued, or null
    *                     if it's unspecified
    */
   public AbstractAttribute(URI id, String issuer, DateTimeAttribute issueInstant) 
   {
      this.id = id;
      this.issuer = issuer;
      this.issueInstant = issueInstant;
   }

   @Override
   public URI getId() {
      return id;
   }

   @Override
   public URI getType() {
      return type;
   }

   @Override
   public String getIssuer() {
      return issuer;
   }

   @Override
   public DateTimeAttribute getIssueInstant() {
      return issueInstant;
   }
   
   @Override
   public void encode(OutputStream output) {
      encode(output, new Indenter(0));
   }

   @Override
   public void encode(OutputStream output, Indenter indenter) {
      // setup the formatting & outstream stuff
      String indent = indenter.makeString();
      PrintStream out = new PrintStream(output);

      // write out the encoded form
      out.append(indent);
      encodeToStream(out);
   }

   /**
    * Simple encoding method that returns the text-encoded version of
    * this attribute with no formatting.
    *
    * @return the text-encoded XML
    */
   @Override
   public String encode() {
       ReadableByteArrayOutputStream out = new ReadableByteArrayOutputStream();
       encode(out);
       return out.toString();
   }
   
   private void encodeToStream(PrintStream out) {
       out.append("<Attribute AttributeId=\"");
       if (id != null) out.append(id.toString());
       out.append("\" DataType=\"");
       if (type != null) out.append(type.toString());
       out.append('"');

      if (issuer != null)
         out.append(" Issuer=\"").append(issuer).append('"');

      if (issueInstant != null)
          out.append(" IssueInstant=\"").append(issueInstant.encode()).append('"');

      out.append('>');
      
      if(getValues() != null)
      {
         for(AttributeValue value: this.getValues())
         {
            out.append(value.encodeWithTags(false));
         }
      }
      out.println("</Attribute>\n");
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((getValues() == null) ? 0 : getValues().hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((issueInstant == null) ? 0 : issueInstant.hashCode());
      result = prime * result + ((issuer == null) ? 0 : issuer.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (!(obj instanceof AbstractAttribute))
         return false;
      AbstractAttribute other = (AbstractAttribute) obj;
      if (getValues() == null)
      {
         if (other.getValues() != null)
            return false;
      }
      else if (!getValues().equals(other.getValues()))
         return false;
      if (id == null)
      {
         if (other.id != null)
            return false;
      }
      else if (!id.equals(other.id))
         return false;
      if (issueInstant == null)
      {
         if (other.issueInstant != null)
            return false;
      }
      else if (!issueInstant.equals(other.issueInstant))
         return false;
      if (issuer == null)
      {
         if (other.issuer != null)
            return false;
      }
      else if (!issuer.equals(other.issuer))
         return false;
      if (type == null)
      {
         if (other.type != null)
            return false;
      }
      else if (!type.equals(other.type))
         return false;
      return true;
   }
}