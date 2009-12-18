<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xd="http://www.pnp-software.com/XSLTdoc" xmlns:a="http://relaxng.org/ns/compatibility/annotations/1.0" xmlns:edate="http://exslt.org/dates-and-times" xmlns:estr="http://exslt.org/strings" xmlns:exsl="http://exslt.org/common" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:rng="http://relaxng.org/ns/structure/1.0" xmlns:tei="http://www.tei-c.org/ns/1.0" xmlns:teix="http://www.tei-c.org/ns/Examples" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" extension-element-prefixes="exsl estr edate" exclude-result-prefixes="xd exsl estr edate a fo rng tei teix" version="1.0">
  <xd:doc type="stylesheet">
    <xd:short>
    TEI stylesheet
    dealing  with elements from the
      header module, making XSL-FO output.
      </xd:short>
    <xd:detail>
    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

   
   
      </xd:detail>
    <xd:author>See AUTHORS</xd:author>
    <xd:cvsId>$Id: header.xsl 4801 2008-09-13 10:05:32Z rahtz $</xd:cvsId>
    <xd:copyright>2008, TEI Consortium</xd:copyright>
  </xd:doc>
  <xd:doc>
    <xd:short>Process elements  docAuthor</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="docAuthor">
    <fo:block font-size="{$authorSize}">
      <fo:inline font-style="italic">
        <xsl:apply-templates/>
      </fo:inline>
    </fo:block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  docDate</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="docDate">
    <fo:block font-size="{$dateSize}">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  docTitle</xd:short>
    <xd:detail>
      <p> author and title </p>
    </xd:detail>
  </xd:doc>
  <xsl:template match="docTitle">
    <fo:block text-align="left" font-size="{$titleSize}" >
      <xsl:if test="ancestor::group/text/front">
        <xsl:attribute name="id">
          <xsl:choose>
            <xsl:when test="ancestor::text/@id">
              <xsl:value-of select="translate(ancestor::text/@id,'_','-')"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="generate-id()"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="titlePart"/>
    </fo:block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  teiHeader</xd:short>
    <xd:detail>
      <p> ignore the header </p>
    </xd:detail>
  </xd:doc>
  <xsl:template match="teiHeader">
  </xsl:template>
  <xd:doc>
    <xd:short>[fo] </xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template name="textTitle">
    <xsl:apply-templates select="front"/>
  </xsl:template>
</xsl:stylesheet>
