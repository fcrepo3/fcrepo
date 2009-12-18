<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet exclude-result-prefixes="xd tei edate"
  extension-element-prefixes="edate" version="1.0"
  xmlns:edate="http://exslt.org/dates-and-times"
  xmlns:tei="http://www.tei-c.org/ns/1.0"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xd:doc type="stylesheet">
    <xd:short> TEI stylesheet dealing with elements from the core module. </xd:short>
    <xd:detail> This library is free software; you can redistribute it and/or
      modify it under the terms of the GNU Lesser General Public License as
      published by the Free Software Foundation; either version 2.1 of the
      License, or (at your option) any later version. This library is
      distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
      without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
      PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
      details. You should have received a copy of the GNU Lesser General Public
      License along with this library; if not, write to the Free Software
      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA </xd:detail>
    <xd:author>See AUTHORS</xd:author>
    <xd:cvsId>$Id: core.xsl 6305 2009-05-10 21:39:41Z rahtz $</xd:cvsId>
    <xd:copyright>2008, TEI Consortium</xd:copyright>
  </xd:doc>
  <xsl:output indent="no"/>
    <xsl:strip-space 
	elements="author forename surname editor"/>
  <xsl:key name="MNAMES"
   match="monogr/author[surname]|monogr/editor[surname]" 
   use="ancestor::biblStruct/@id"/>
  <xsl:key name="ANAMES"
   match="analytic/author[surname]|analytic/editor[surname]" 
   use ="ancestor::biblStruct/@id"/>
  

  <xd:doc>
    <xd:short>Process all elements in depth</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="*" mode="depth">99</xsl:template>
  <xd:doc>
    <xd:short>Process all elements in plain mode</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="*" mode="plain">
    <xsl:apply-templates mode="plain"/>
  </xsl:template>
  <xsl:template match="note" mode="plain"/>
  <xsl:template match="app" mode="plain"/>
  <xsl:template match="pb" mode="plain"/>
  <xsl:template match="lb" mode="plain"/>
  <xsl:template match="ptr" mode="plain"/>
  <xd:doc>
    <xd:short>Process sic</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="sic">
    <xsl:apply-templates/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process corr</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="corr"/>
  <xd:doc>
    <xd:short>Process item in runin mode</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="item" mode="runin">
    <xsl:text> • </xsl:text>
   <xsl:apply-templates/> 
  </xsl:template>



  <xd:doc>
    <xd:short>Process elements edition</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="edition">
    <xsl:apply-templates/>
    <xsl:if test="ancestor::biblStruct">
      <xsl:text>.&#10;</xsl:text>
    </xsl:if>
  </xsl:template>


  <xd:doc>
    <xd:short>Process elements imprint</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="imprint">
    <xsl:choose>
    <xsl:when test="ancestor::biblStruct">
	<xsl:apply-templates select="date"/>
	<xsl:apply-templates select="pubPlace"/>
	<xsl:apply-templates select="publisher"/>
	<xsl:apply-templates select="biblScope"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates/>
    </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

<xsl:template name="makeQuote">
  <xsl:variable name="pre">
    <xsl:choose>
      <xsl:when test="contains(@rend,'PRE')">
	<xsl:choose>
	  <xsl:when test="contains(@rend,'POST')">
	    <xsl:call-template name="getQuote">
	      <xsl:with-param name="quote"
			      select="normalize-space(substring-before(substring-after(@rend,'PRE'),'POST'))"
			      />
	    </xsl:call-template>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:call-template name="getQuote">
	      <xsl:with-param name="quote"
			      select="normalize-space(substring-after(@rend,'PRE'))"/>
	    </xsl:call-template>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$preQuote"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="post">
    <xsl:choose>
      <xsl:when test="contains(@rend,'POST')">
	<xsl:call-template name="getQuote">
	  <xsl:with-param name="quote"
			  select="normalize-space(substring-after(@rend,'POST'))"/>
	</xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$postQuote"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:value-of select="$pre"/>
  <xsl:apply-templates/>
  <xsl:value-of select="$post"/>
</xsl:template>

<!-- biblStruct -->
<xsl:template match="biblStruct" mode="xref">
    <xsl:choose>
      <xsl:when test="count(key('ANAMES',@id))=1">
	<xsl:value-of select="key('ANAMES',@id)/surname"/>
      </xsl:when>
      <xsl:when test="count(key('ANAMES',@id))=2">
	<xsl:value-of
	    select="key('ANAMES',@id)[1]/surname"/>
	<xsl:text> and </xsl:text>
	<xsl:value-of select="key('ANAMES',@id)[2]/surname"/>
      </xsl:when>
      <xsl:when test="count(key('ANAMES',@id))&gt;2">
	<xsl:value-of
	    select="key('ANAMES',@id)[1]/surname"/>
	<xsl:text> et al.</xsl:text>
      </xsl:when>
      <xsl:when test="count(key('MNAMES',@id))=1">
	<xsl:value-of select="key('MNAMES',@id)/surname"/>
      </xsl:when>
      <xsl:when test="count(key('MNAMES',@id))=2">
	<xsl:value-of
	 select="key('MNAMES',@id)[1]/surname"/>
	<xsl:text> and </xsl:text>
	<xsl:value-of select="key('MNAMES',@id)[2]/surname"/>
      </xsl:when>
      <xsl:when test="count(key('MNAMES',@id))&gt;2">
	<xsl:value-of
	    select="key('MNAMES',@id)[1]/surname"/>
	<xsl:text> et al.</xsl:text>
      </xsl:when>
      <xsl:when test=".//author[surname]">
	<xsl:value-of select=".//author/surname[1]"/>
      </xsl:when>
      <xsl:when test=".//author[orgName]">
	<xsl:value-of select=".//author/orgName[1]"/>
      </xsl:when>
      <xsl:when test=".//author">
	<xsl:value-of select=".//author[1]"/>
      </xsl:when>
      <xsl:when test=".//editor[surname]">
	<xsl:value-of select=".//editor/surname[1]"/>
      </xsl:when>
      <xsl:when test=".//editor">
	<xsl:value-of select=".//editor[1]"/>
      </xsl:when>
      <xsl:otherwise>
	  <xsl:value-of select=".//title[1]"/>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="count(*[1]/editor)=1">
	<xsl:text> (ed.)</xsl:text>
      </xsl:when>
      <xsl:when test="count(*[1]/editor)&gt;1">
	<xsl:text> (eds.)</xsl:text>
      </xsl:when>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="monogr/imprint/date/@when">
	<xsl:text> (</xsl:text>
	<xsl:value-of select="substring-before(monogr/imprint/date/@when,'-')"/>
	<xsl:text>)</xsl:text>
      </xsl:when>
      <xsl:when test="monogr/imprint/date">
	<xsl:text> (</xsl:text>
	<xsl:value-of select="monogr/imprint/date"/>
	<xsl:text>)</xsl:text>
      </xsl:when>
    </xsl:choose>
</xsl:template>

<!-- authors and editors -->
<xsl:template match="author|editor">
  <xsl:choose>
    <!-- last name in a list -->
    <xsl:when test="ancestor::bibl">
      <xsl:apply-templates/>
    </xsl:when>
    <xsl:when test="self::author and not(following-sibling::author)">
      <xsl:apply-templates/>
      <xsl:text>. </xsl:text>
    </xsl:when>
    <xsl:when test="self::editor and not(following-sibling::editor)">
      <xsl:apply-templates/>
      <xsl:text> (</xsl:text>
      <xsl:text>ed</xsl:text>
      <xsl:if test="preceding-sibling::editor">s</xsl:if>
      <xsl:text>.</xsl:text>
      <xsl:text>) </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <!-- first or middle name in a list -->
      <xsl:apply-templates/>
      <xsl:text>, </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template  match="surname">
  <xsl:if test="../forename">
    <xsl:apply-templates select="../forename" mode="use"/>
    <xsl:text> </xsl:text>
  </xsl:if>
  <xsl:if test="../nameLink">
    <xsl:apply-templates select="../nameLink" mode="use"/>
    <xsl:text> </xsl:text>
  </xsl:if>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="forename">
</xsl:template>

<xsl:template match="nameLink">
</xsl:template>

<xsl:template  match="forename" mode="use">
  <xsl:if test="preceding-sibling::forename">
    <xsl:text> </xsl:text>
  </xsl:if>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template  match="nameLink" mode="use">
  <xsl:apply-templates/>
</xsl:template>

<!-- title  -->
<xsl:template match="titlePart" mode="simple">
   <xsl:if test="preceding-sibling::titlePart">
      <xsl:text> &#8212; </xsl:text>
    </xsl:if>
    <xsl:value-of select="."/>
</xsl:template>
<xsl:template match="title" mode="simple">
   <xsl:if test="preceding-sibling::title">
      <xsl:text> &#8212; </xsl:text>
    </xsl:if>
    <xsl:value-of select="."/>
</xsl:template>

<xsl:template match="titlePart">
   <xsl:if test="preceding-sibling::titlePart">
      <xsl:text> &#8212; </xsl:text>
    </xsl:if>
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="title">
  <xsl:choose>
    <xsl:when test="parent::titleStmt">
      <xsl:if test="preceding-sibling::title">
	<xsl:text> &#8212; </xsl:text>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:when>
    <xsl:when test="@level='m' or not(@level)">
      <xsl:call-template name="emphasize">
	<xsl:with-param name="class">
	  <xsl:text>titlem</xsl:text>
	</xsl:with-param>
	<xsl:with-param name="content">
	  <xsl:apply-templates/>
	</xsl:with-param>
      </xsl:call-template>
      <xsl:if test="ancestor::biblStruct">
	<xsl:text>, </xsl:text>
      </xsl:if>
    </xsl:when>
    <xsl:when test="@level='s'">
      <xsl:call-template name="emphasize">
	<xsl:with-param name="class">
	  <xsl:text>titles</xsl:text>
	</xsl:with-param>
	<xsl:with-param name="content">
	  <xsl:apply-templates/>
	</xsl:with-param>
      </xsl:call-template>
      <xsl:if test="following-sibling::* and ancestor::biblStruct">
	<xsl:text> </xsl:text>
      </xsl:if>
    </xsl:when>
    <xsl:when test="@level='j'">
      <xsl:call-template name="emphasize">
	<xsl:with-param name="class">
	  <xsl:text>titlej</xsl:text>
	</xsl:with-param>
	<xsl:with-param name="content">
	  <xsl:apply-templates/>
	</xsl:with-param>
      </xsl:call-template>
      <xsl:text> </xsl:text>
    </xsl:when>
    <xsl:when test="@level='a'">
      <xsl:call-template name="emphasize">
	<xsl:with-param name="class">
	  <xsl:text>titlea</xsl:text>
	</xsl:with-param>
	<xsl:with-param name="content">
	  <xsl:apply-templates/>
	</xsl:with-param>
      </xsl:call-template>
      <xsl:if test="ancestor::biblStruct">
	<xsl:text>. </xsl:text>
      </xsl:if>
    </xsl:when>
    <xsl:when test="@level='u'">
      <xsl:call-template name="emphasize">
	<xsl:with-param name="class">
	  <xsl:text>titleu</xsl:text>
	</xsl:with-param>
	<xsl:with-param name="content">
	  <xsl:apply-templates/>
	</xsl:with-param>
      </xsl:call-template>
      <xsl:if test="ancestor::biblStruct">
	<xsl:text>. </xsl:text>
      </xsl:if>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<xsl:template match="idno[@type='url']">
  <xsl:text> &lt;</xsl:text>
  <xsl:call-template name="makeExternalLink">
    <xsl:with-param name="ptr">true</xsl:with-param>
    <xsl:with-param name="dest">
      <xsl:value-of select="normalize-space(.)"/>
    </xsl:with-param>
  </xsl:call-template>
  <xsl:text>&gt;.</xsl:text>
</xsl:template>

<xsl:template match="meeting">
    <xsl:text> (</xsl:text>
      <xsl:apply-templates/>
    <xsl:text>)</xsl:text>
    <xsl:if test="following-sibling::* and ancestor::biblStruct">
      <xsl:text> </xsl:text>
    </xsl:if>
</xsl:template>

<xsl:template match="series">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="date">
 <xsl:apply-templates/>
 <xsl:if test="ancestor::biblStruct">
   <xsl:text>. </xsl:text>
 </xsl:if>
</xsl:template>

<xsl:template match="pubPlace">
  <xsl:choose>
    <xsl:when test="@rendition">
      <span>
	<xsl:call-template name="applyRendition"/>
	<xsl:apply-templates/>
      </span>
    </xsl:when>
    <xsl:otherwise>     
      <xsl:apply-templates/>
    </xsl:otherwise>
  </xsl:choose>
 <xsl:choose>
   <xsl:when test="ancestor::bibl"/>
   <xsl:when test="following-sibling::pubPlace">
     <xsl:text>, </xsl:text>
   </xsl:when>
   <xsl:when test="../publisher">
     <xsl:text>: </xsl:text>
   </xsl:when>
   <xsl:otherwise>
     <xsl:text>. </xsl:text>
   </xsl:otherwise>
 </xsl:choose>
</xsl:template>

<xsl:template match="publisher">
  <xsl:choose>
    <xsl:when test="@rendition">
      <span>
	<xsl:call-template name="applyRendition"/>
	<xsl:apply-templates/>
      </span>
    </xsl:when>
    <xsl:otherwise>     
      <xsl:apply-templates/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:if test="ancestor::biblStruct">
     <xsl:text>. </xsl:text>
  </xsl:if>
</xsl:template>

<!-- details and notes -->
<xsl:template match="biblScope">
 <xsl:choose>
   <xsl:when test="ancestor::bibl">
     <xsl:apply-templates/>
   </xsl:when>
  <xsl:when test="@type='vol'">
    <xsl:call-template name="emphasize">
      <xsl:with-param name="class">
	<xsl:text>vol</xsl:text>
      </xsl:with-param>
      <xsl:with-param name="content">
	<xsl:apply-templates/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:when>
  <xsl:when test="@type='chap'">
   <xsl:text>chapter </xsl:text>
   <xsl:apply-templates/>
  </xsl:when>
  <xsl:when test="@type='issue'">
    <xsl:text> (</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>) </xsl:text>
  </xsl:when>
  <xsl:when test="@type='pp'">
    <xsl:choose>
      <xsl:when test="contains(.,'-')">
	<xsl:text>pp. </xsl:text>
      </xsl:when>
      <xsl:when test="contains(.,'ff')">
	<xsl:text>pp. </xsl:text>
      </xsl:when>
      <xsl:when test="contains(.,' ')">
	<xsl:text>pp. </xsl:text>
      </xsl:when>
      <xsl:otherwise>
	<xsl:text>p. </xsl:text>
      </xsl:otherwise>
    </xsl:choose>
   <xsl:apply-templates/>
  </xsl:when>
  <xsl:otherwise>
   <xsl:apply-templates/>
  </xsl:otherwise>
 </xsl:choose>
 
 <xsl:choose>
   <xsl:when test="@type='vol' and
		   following-sibling::biblScope[@type='issue']">
     <xsl:text> </xsl:text>
   </xsl:when>
   <xsl:when test="@type='vol' and following-sibling::biblScope">
     <xsl:text> </xsl:text>
   </xsl:when>
   <xsl:when test="following-sibling::biblScope">
     <xsl:text> </xsl:text>
   </xsl:when>
   <xsl:when test="ancestor::biblStruct">
     <xsl:text>. </xsl:text>
   </xsl:when>
 </xsl:choose>

</xsl:template>

<xsl:template match="idno">
  <xsl:text> </xsl:text>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="idno[@type='doi']"/>

</xsl:stylesheet>
