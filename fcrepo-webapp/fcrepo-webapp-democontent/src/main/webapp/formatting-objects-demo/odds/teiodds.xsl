<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet     xmlns:sch="http://purl.oclc.org/dsdl/schematron"
xmlns:html="http://www.w3.org/1999/xhtml" xmlns:a="http://relaxng.org/ns/compatibility/annotations/1.0" xmlns:edate="http://exslt.org/dates-and-times" xmlns:estr="http://exslt.org/strings" xmlns:exsl="http://exslt.org/common" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:rng="http://relaxng.org/ns/structure/1.0" xmlns:s="http://www.ascc.net/xml/schematron" xmlns:tei="http://www.tei-c.org/ns/1.0" xmlns:teix="http://www.tei-c.org/ns/Examples" xmlns:xd="http://www.pnp-software.com/XSLTdoc" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="exsl estr rng edate teix fo a tei s xd xs html" extension-element-prefixes="edate exsl estr" version="1.0">
  <xsl:import href="../common/verbatim.xsl"/>
  <xsl:import href="../common/i18n.xsl"/>
  <xd:doc type="stylesheet">
    <xd:short> TEI stylesheet for processing TEI ODD markup </xd:short>
    <xd:detail> This library is free software; you can redistribute it and/or modify it under the
      terms of the GNU Lesser General Public License as published by the Free Software Foundation;
      either version 2.1 of the License, or (at your option) any later version. This library is
      distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
      implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
      General Public License for more details. You should have received a copy of the GNU Lesser
      General Public License along with this library; if not, write to the Free Software Foundation,
      Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA </xd:detail>
    <xd:author>See AUTHORS</xd:author>
    <xd:cvsId>$Id: teiodds.xsl 6618 2009-06-27 20:08:52Z rahtz $</xd:cvsId>
    <xd:copyright>2008, TEI Consortium</xd:copyright>
  </xd:doc>
  <xsl:include href="RngToRnc.xsl"/>
  <xsl:param name="STDOUT">true</xsl:param>
  <xsl:param name="outputSuffix">.html</xsl:param>
  <xd:doc type="string" class="output"> Public Doctype of output file(s). </xd:doc>
  <xsl:param name="selectedSchema"/>
  <xsl:param name="outputDir"/>
  <xsl:param name="localsource"/>
  <xsl:param name="lang"/>
  <xsl:param name="doclang"/>
  <xsl:param name="patternPrefix"/>
  <xsl:param name="TEIC">false</xsl:param>
  <xsl:param name="lookupDatabase">false</xsl:param>
  <xsl:param name="TEISERVER">http://tei.oucs.ox.ac.uk/Query/</xsl:param>
  <xsl:param name="verbose">false</xsl:param>
  <xsl:param name="schemaBaseURL">http://localhost/schema/relaxng/</xsl:param>
  <xsl:key match="*" name="LOCALIDENTS" use="@ident"/>
  <xsl:key match="macroSpec" name="MACROS" use="@ident"/>
  <xsl:key match="elementSpec" name="ELEMENTS" use="@ident"/>
  <xsl:key match="classSpec" name="CLASSES" use="@ident"/>
  <xsl:key match="rng:ref" name="CLASSREFS" use="@name"/>
  <xsl:key match="elementSpec/content//rng:ref" name="REFS" use="@name"/>
  <xsl:key match="elementSpec/attList/attDef/datatype/rng:ref" name="ATTREFS-ELEMENT" use="@name"/>
  <xsl:key match="classSpec/attList/attDef/datatype/rng:ref" name="ATTREFS-CLASS" use="@name"/>
  <xsl:key match="elementSpec/attList/attList/attDef/datatype/rng:ref" name="ATTREFS-ELEMENT" use="@name"/>
  <xsl:key match="classSpec/attList/attList/attDef/datatype/rng:ref" name="ATTREFS-CLASS" use="@name"/>
  <xsl:key match="macroSpec/content//rng:ref" name="MACROREFS" use="@name"/>
  <xsl:key match="elementSpec|classSpec" name="CLASSMEMBERS" use="classes/memberOf/@key"/>
  <xsl:key match="elementSpec|classSpec|macroSpec" name="IDENTS" use="@ident"/>
  <xsl:key match="*[@id]" name="IDS" use="@id"/>
  <xsl:key match="macroSpec[@type='dt']" name="DATATYPES" use="1"/>
  <xsl:key match="macroSpec" name="MACRODOCS" use="1"/>
  <xsl:key match="attDef" name="ATTDOCS" use="1"/>
  <xsl:key match="attDef" name="ATTRIBUTES" use="@ident"/>
  <xsl:key match="classSpec//attDef" name="ATTRIBUTES-CLASS" use="@ident"/>
  <xsl:key match="elementSpec//attDef" name="ATTRIBUTES-ELEMENT" use="@ident"/>
  <xsl:key match="schemaSpec" name="SCHEMASPECS" use="1"/>
  <xsl:key match="schemaSpec" name="LISTSCHEMASPECS" use="@ident"/>
  <xsl:key match="classSpec[@type='atts']" name="ATTCLASSDOCS" use="1"/>
  <xsl:key match="classSpec[@type='model']" name="MODELCLASSDOCS" use="1"/>
  <xsl:key match="elementSpec" name="ELEMENTDOCS" use="1"/>
  <xsl:key match="*" name="NameToID" use="@ident"/>
  <xsl:key match="elementSpec" name="ElementModule" use="@module"/>
  <xsl:key match="classSpec" name="ClassModule" use="@module"/>
  <xsl:key match="macroSpec" name="MacroModule" use="@module"/>
  <xsl:key match="moduleSpec" name="Modules" use="1"/>
  <xsl:key match="moduleSpec" name="MODULES" use="@ident"/>
  <xsl:key match="classSpec[@predeclare='true']" name="predeclaredClasses" use="1"/>
  <xsl:key match="macroSpec[@predeclare='true']" name="PredeclareMacros" use="@ident"/>
  <xsl:key match="macroSpec[@predeclare='true']" name="PredeclareMacrosModule" use="@module"/>
  <xsl:key match="macroSpec[@predeclare='true']" name="PredeclareAllMacros" use="1"/>
  <xsl:variable name="parameterize">
    <xsl:choose>
      <xsl:when test="$TEIC='false'">true</xsl:when>
      <xsl:when test="key('SCHEMASPECS',1)">false</xsl:when>
      <xsl:otherwise>true</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="whichSchemaSpec">
    <xsl:choose>
      <xsl:when test="$selectedSchema">
        <xsl:value-of select="$selectedSchema"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="key('SCHEMASPECS',1)[1]/@ident"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
<!-- lookup table of element contents, and templates to access the result -->
  <xsl:key match="Contains" name="ELEMENTPARENTS" use="."/>
  <xsl:variable name="patternPrefixText">
    <xsl:choose>
      <xsl:when test="string-length($patternPrefix)&gt;0">
        <xsl:value-of select="$patternPrefix"/>
      </xsl:when>
      <xsl:when test="key('LISTSCHEMASPECS',$whichSchemaSpec)[@prefix]">
        <xsl:value-of select="key('LISTSCHEMASPECS',$whichSchemaSpec)/@prefix"/>
      </xsl:when>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="targetLanguage">
    <xsl:choose>
      <xsl:when test="string-length($lang)&gt;0">
        <xsl:value-of select="$lang"/>
      </xsl:when>
      <xsl:when test="key('LISTSCHEMASPECS',$whichSchemaSpec)[@targetLang]">
        <xsl:value-of select="key('LISTSCHEMASPECS',$whichSchemaSpec)/@targetLang"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>en</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:template name="generateDoc">
    <xsl:choose>
      <xsl:when test="string-length($doclang)&gt;0">
        <xsl:value-of select="$doclang"/>
      </xsl:when>
      <xsl:when test="key('LISTSCHEMASPECS',$whichSchemaSpec)/@docLang">
        <xsl:value-of select="key('LISTSCHEMASPECS',$whichSchemaSpec)/@docLang"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>en</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="processing-instruction()">
    <xsl:if test="name(.) = 'odds'">
      <xsl:choose>
        <xsl:when test=".='date'"> This formatted version of the Guidelines was created on
            <xsl:call-template name="showDate"/>. </xsl:when>
      </xsl:choose>
    </xsl:if>
  </xsl:template>
  <xsl:template match="*" mode="literal">
    <xsl:text>&#10;</xsl:text>
    <xsl:for-each select="ancestor::rng:*">
      <xsl:text> </xsl:text>
    </xsl:for-each>
    <xsl:text>&lt;</xsl:text>
    <xsl:value-of select="local-name(.)"/>
    <xsl:for-each select="@*"><xsl:text> </xsl:text><xsl:value-of select="local-name(.)"/>="<xsl:value-of select="."/>"</xsl:for-each>
    <xsl:choose>
      <xsl:when test="child::node()">
        <xsl:text>&gt;</xsl:text>
        <xsl:apply-templates mode="literal"/>
        <xsl:if test="node()[last()]/self::rng:*">
          <xsl:text>&#10;</xsl:text>
        </xsl:if>
        <xsl:for-each select="ancestor::rng:*">
          <xsl:text> </xsl:text>
        </xsl:for-each>
        <xsl:text>&lt;/</xsl:text>
        <xsl:value-of select="local-name(.)"/>
        <xsl:text>&gt;</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>/&gt;</xsl:text>
        <xsl:if test="node()[last()]/self::rng:*">
          <xsl:text>&#10;	  </xsl:text>
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="rng:ref">
    <xsl:copy>
      <xsl:attribute name="name">
        <xsl:choose>
          <xsl:when test="key('IDENTS',@name)">
            <xsl:value-of select="$patternPrefixText"/>
          </xsl:when>
          <xsl:when test="starts-with(@name,'att.') and key('IDENTS',substring-before(@name,'.attribute.'))"> </xsl:when>
          <xsl:when test="key('IDENTS',substring-before(@name,'_'))">
            <xsl:value-of select="$patternPrefixText"/>
          </xsl:when>
        </xsl:choose>
        <xsl:value-of select="@name"/>
      </xsl:attribute>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="rng:*">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates select="rng:*|*|text()|comment()"/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="rng:zeroOrMore">
    <xsl:choose>
      <xsl:when test="rng:ref/@name='model.global'   and preceding-sibling::rng:*[1][self::rng:zeroOrMore/rng:ref/@name='model.global']"/>
      <xsl:when test="count(rng:*)=1 and rng:zeroOrMore">
        <xsl:apply-templates select="rng:*|*|text()|comment()"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:copy-of select="@*"/>
          <xsl:apply-templates select="rng:*|*|text()|comment()"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="rng:choice">
    <xsl:choose>
      <xsl:when test="count(rng:*)=1">
        <xsl:apply-templates select="rng:*|*|text()|comment()"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:copy-of select="@*"/>
          <xsl:apply-templates select="rng:*|*|text()|comment()"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="rng:group">
<!-- check if this group is identical to the last -->
    <xsl:choose>
      <xsl:when test="count(rng:*)=1 and local-name(preceding-sibling::rng:*[1])='group' and rng:zeroOrMore">
        <xsl:variable name="that">
          <xsl:for-each select="preceding-sibling::rng:*[1]">
            <xsl:apply-templates mode="decomposed"/>
          </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="this">
          <xsl:apply-templates mode="decomposed"/>
        </xsl:variable>
        <xsl:choose>
          <xsl:when test="$that=$this"/>
          <xsl:otherwise>
            <xsl:copy>
              <xsl:copy-of select="@*"/>
              <xsl:apply-templates select="rng:*|*|text()|comment()"/>
            </xsl:copy>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:copy-of select="@*"/>
          <xsl:apply-templates select="rng:*|*|text()|comment()"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="rng:*" mode="decomposed">
    <xsl:value-of select="local-name(.)"/>
    <xsl:for-each select="@*">
      <xsl:text>@</xsl:text>
      <xsl:value-of select="."/>
    </xsl:for-each>
    <xsl:apply-templates mode="decomposed"/>
  </xsl:template>
  <xsl:template match="*" mode="tangle"/>
  <xsl:template match="attRef" mode="tangle">
    <ref xmlns="http://relaxng.org/ns/structure/1.0">
      <xsl:attribute name="name">
        <xsl:choose>
          <xsl:when test="not(contains(@name,'_'))"> </xsl:when>
        </xsl:choose>
        <xsl:value-of select="@name"/>
      </xsl:attribute>
    </ref>
  </xsl:template>
  <xsl:template match="attDef" mode="tangle">
    <xsl:param name="element"/>
    <xsl:variable name="I">
      <xsl:value-of select="translate(@ident,':','')"/>
    </xsl:variable>
    <xsl:if test="not(starts-with(@ident,'xmlns'))">
      <xsl:choose>
        <xsl:when test="ancestor::elementSpec">
          <xsl:call-template name="makeAnAttribute"/>
        </xsl:when>
        <xsl:when test="ancestor::classSpec">
          <define xmlns="http://relaxng.org/ns/structure/1.0" name="{$element}.attribute.{translate(@ident,':','')}">
            <xsl:call-template name="makeAnAttribute"/>
          </define>
        </xsl:when>
      </xsl:choose>
    </xsl:if>
  </xsl:template>
  <xsl:template match="attList" mode="tangle">
    <xsl:param name="element"/>
    <xsl:choose>
      <xsl:when test="count(*)=0"/>
      <xsl:when test="@org='group' and         parent::attList[@org='choice']">
        <rng:group>
          <xsl:apply-templates mode="tangle" select="*">
            <xsl:with-param name="element" select="$element"/>
          </xsl:apply-templates>
        </rng:group>
      </xsl:when>
      <xsl:when test="@org='choice'">
        <rng:choice>
          <xsl:apply-templates mode="tangle" select="*">
            <xsl:with-param name="element" select="$element"/>
          </xsl:apply-templates>
        </rng:choice>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates mode="tangle" select="*">
          <xsl:with-param name="element" select="$element"/>
        </xsl:apply-templates>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="author">
    <xsl:apply-templates/>
    <xsl:text>, </xsl:text>
  </xsl:template>
  <xsl:template match="classSpec" mode="tangle">
    <xsl:variable name="c" select="@ident"/>
    <xsl:if test="$verbose='true'">
      <xsl:message> classSpec <xsl:value-of select="@ident"/> (type <xsl:value-of select="@type"/>)</xsl:message>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="@type='model'">
        <xsl:apply-templates mode="processModel" select=".">
          <xsl:with-param name="declare">false</xsl:with-param>
<!--	    <xsl:choose>
	      <xsl:when test="@module='tei'">true</xsl:when>
	      <xsl:otherwise>false</xsl:otherwise>
	    </xsl:choose>
	  </xsl:with-param>
-->
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="@type='atts'">
        <xsl:call-template name="bitOut">
          <xsl:with-param name="grammar">true</xsl:with-param>
          <xsl:with-param name="content">
            <Wrapper>
              <define xmlns="http://relaxng.org/ns/structure/1.0" name="{@ident}.attributes">
                <xsl:for-each select="classes/memberOf">
                  <xsl:for-each select="key('IDENTS',@key)[1]">
                    <xsl:if test="@type='atts'">
                      <ref xmlns="http://relaxng.org/ns/structure/1.0" name="{@ident}.attributes"/>
                    </xsl:if>
                  </xsl:for-each>
                </xsl:for-each>
                <xsl:choose>
                  <xsl:when test="attList//attDef">
                    <xsl:for-each select="attList//attDef">
                      <xsl:if test="not(starts-with(@ident,'xmlns'))">
                        <ref xmlns="http://relaxng.org/ns/structure/1.0" name="{$c}.attribute.{translate(@ident,':','')}"/>
                      </xsl:if>
                    </xsl:for-each>
                  </xsl:when>
                  <xsl:when test="classes/memberOf"/>
                  <xsl:otherwise>
                    <notAllowed xmlns="http://relaxng.org/ns/structure/1.0"/>
                  </xsl:otherwise>
                </xsl:choose>
                <xsl:if test="$TEIC='true'">
                  <empty xmlns="http://relaxng.org/ns/structure/1.0"/>
                </xsl:if>
              </define>
              <xsl:apply-templates mode="tangle" select="attList">
                <xsl:with-param name="element" select="@ident"/>
              </xsl:apply-templates>
            </Wrapper>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="classSpec" mode="processModel">
    <xsl:param name="declare">false</xsl:param>
    <xsl:if test="$verbose='true'">
      <xsl:message> .... model class <xsl:value-of select="@ident"/></xsl:message>
    </xsl:if>
    <xsl:call-template name="bitOut">
      <xsl:with-param name="grammar">true</xsl:with-param>
      <xsl:with-param name="content">
        <Wrapper>
          <xsl:call-template name="processClassDefinition">
            <xsl:with-param name="type">
              <xsl:choose>
                <xsl:when test="@generate">
                  <xsl:value-of select="@generate"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:text>alternation
		  sequence
		  sequenceOptional
		  sequenceOptionalRepeatable
		  sequenceRepeatable</xsl:text>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:with-param>
            <xsl:with-param name="declare" select="$declare"/>
          </xsl:call-template>
        </Wrapper>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>
  <xsl:template name="processClassDefinition">
    <xsl:param name="type"/>
    <xsl:param name="declare"/>
    <xsl:variable name="Type">
      <xsl:value-of select="normalize-space($type)"/>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="string-length($Type)=0">
        <xsl:call-template name="makeClassDefinition">
          <xsl:with-param name="type">alternation</xsl:with-param>
          <xsl:with-param name="declare" select="$declare"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="contains($Type,' ')">
        <xsl:call-template name="makeClassDefinition">
          <xsl:with-param name="type" select="substring-before($Type,' ')"/>
          <xsl:with-param name="declare" select="$declare"/>
        </xsl:call-template>
        <xsl:call-template name="processClassDefinition">
          <xsl:with-param name="type" select="substring-after($Type,' ')"/>
          <xsl:with-param name="declare" select="$declare"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="makeClassDefinition">
          <xsl:with-param name="type" select="$Type"/>
          <xsl:with-param name="declare" select="$declare"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="makeClassDefinition">
    <xsl:param name="type"/>
    <xsl:param name="declare"/>
<!--
      alternation
      sequence
      sequenceOptional
      sequenceOptionalRepeatable
      sequenceRepeatable
  -->
    <xsl:variable name="thisClass">
      <xsl:value-of select="@ident"/>
    </xsl:variable>
    <xsl:variable name="suffix">
      <xsl:choose>
        <xsl:when test="$type='alternation'"/>
        <xsl:otherwise>
          <xsl:text>_</xsl:text>
          <xsl:value-of select="$type"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$declare='true'">
        <xsl:apply-templates mode="tangleModel" select="classes/memberOf"/>
        <define xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{$thisClass}{$suffix}">
          <xsl:if test="@predeclare='true'">
            <xsl:attribute name="combine">choice</xsl:attribute>
          </xsl:if>
          <notAllowed xmlns="http://relaxng.org/ns/structure/1.0"/>
        </define>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="makeDecls">
          <xsl:call-template name="findUses">
            <xsl:with-param name="pattern" select="$suffix"/>
            <xsl:with-param name="class" select="$thisClass"/>
          </xsl:call-template>
        </xsl:variable>
<!--
<xsl:message><xsl:value-of select="$thisClass"/><xsl:value-of
select="$suffix"/> generated <xsl:value-of
select="$makeDecls"/></xsl:message>
-->
        <xsl:choose>
          <xsl:when test="$makeDecls=''">
            <xsl:if test="$verbose='true'">
              <xsl:message>ZAP <xsl:value-of select="$thisClass"/><xsl:value-of select="$suffix"/>
              </xsl:message>
            </xsl:if>
          </xsl:when>
          <xsl:when test="count(key('CLASSMEMBERS',$thisClass))&gt;0">
            <xsl:if test="$verbose='true'">
              <xsl:message> .... ... generate model <xsl:value-of select="$thisClass"/><xsl:value-of select="$suffix"/>
              </xsl:message>
            </xsl:if>
            <define xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{$thisClass}{$suffix}">
              <xsl:choose>
                <xsl:when test="$type='sequence'">
                  <xsl:for-each select="key('CLASSMEMBERS',$thisClass)">
                    <ref xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}"/>
                  </xsl:for-each>
                </xsl:when>
                <xsl:when test="$type='sequenceOptional'">
                  <xsl:for-each select="key('CLASSMEMBERS',$thisClass)">
                    <optional xmlns="http://relaxng.org/ns/structure/1.0">
                      <ref xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}"/>
                    </optional>
                  </xsl:for-each>
                </xsl:when>
                <xsl:when test="$type='sequenceRepeatable'">
                  <xsl:for-each select="key('CLASSMEMBERS',$thisClass)">
                    <oneOrMore xmlns="http://relaxng.org/ns/structure/1.0">
                      <ref xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}"/>
                    </oneOrMore>
                  </xsl:for-each>
                </xsl:when>
                <xsl:when test="$type='sequenceOptionalRepeatable'">
                  <xsl:for-each select="key('CLASSMEMBERS',$thisClass)">
                    <zeroOrMore xmlns="http://relaxng.org/ns/structure/1.0">
                      <ref xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}"/>
                    </zeroOrMore>
                  </xsl:for-each>
                </xsl:when>
                <xsl:otherwise>
                  <rng:choice>
                    <xsl:for-each select="key('CLASSMEMBERS',$thisClass)">
                      <ref xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}"/>
                    </xsl:for-each>
                  </rng:choice>
                </xsl:otherwise>
              </xsl:choose>
            </define>
          </xsl:when>
          <xsl:otherwise>
            <define xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{$thisClass}{$suffix}">
              <xsl:choose>
                <xsl:when test="$type='sequence' or     $type='sequenceOptional' or      $type='sequenceOptionalRepeatable'">
                  <empty xmlns="http://relaxng.org/ns/structure/1.0"/>
                </xsl:when>
                <xsl:otherwise>
                  <notAllowed xmlns="http://relaxng.org/ns/structure/1.0"/>
                </xsl:otherwise>
              </xsl:choose>
            </define>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="findUses">
    <xsl:param name="pattern"/>
    <xsl:param name="class"/>
    <xsl:choose>
      <xsl:when test="not(ancestor::schemaSpec)">x</xsl:when>
      <xsl:when test="key('CLASSREFS',concat($class,$pattern))">x</xsl:when>
      <xsl:when test="not($pattern='')"/>
      <xsl:when test="classes/memberOf">
        <xsl:for-each select="classes/memberOf">
          <xsl:for-each select="key('CLASSES',@key)">
            <xsl:call-template name="findUses">
              <xsl:with-param name="pattern"/>
              <xsl:with-param name="class" select="@ident"/>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise/>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="classSpec" mode="tangleadd">
    <xsl:apply-templates mode="tangleadd"/>
  </xsl:template>
  <xsl:template match="classSpec/@ident"/>
  <xsl:template match="code">
    <xsl:call-template name="typewriter">
      <xsl:with-param name="text">
        <xsl:apply-templates/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>
  <xsl:template match="text()" mode="doc">
    <xsl:value-of select="."/>
  </xsl:template>
  <xsl:template match="desc" mode="tangle"/>
  <xsl:template match="elementSpec" mode="tangle">
    <xsl:if test="$verbose='true'">
      <xsl:message> elementSpec <xsl:value-of select="@ident"/>
        <xsl:if test="@id">: <xsl:value-of select="@id"/></xsl:if>
      </xsl:message>
    </xsl:if>
    <xsl:call-template name="bitOut">
      <xsl:with-param name="grammar"/>
      <xsl:with-param name="content">
        <Wrapper>
          <xsl:variable name="name">
            <xsl:choose>
              <xsl:when test="altIdent=@ident">
                <xsl:value-of select="@ident"/>
              </xsl:when>
              <xsl:when test="altIdent">
                <xsl:value-of select="normalize-space(altIdent)"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="@ident"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:choose>
            <xsl:when test="content/rng:notAllowed">
              <define xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}">
                <notAllowed xmlns="http://relaxng.org/ns/structure/1.0"/>
              </define>
            </xsl:when>
            <xsl:otherwise>
              <xsl:variable name="Attributes">
                <xsl:call-template name="summarizeAttributes"/>
              </xsl:variable>
              <define xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}">
                <element xmlns="http://relaxng.org/ns/structure/1.0" name="{$name}">
                  <xsl:if test="@ns">
                    <xsl:attribute name="ns">
                      <xsl:value-of select="@ns"/>
                    </xsl:attribute>
                  </xsl:if>
                  <xsl:if test="not($oddmode='tei')">
                    <a:documentation>
                      <xsl:call-template name="makeDescription">
                        <xsl:with-param name="includeValList">true</xsl:with-param>
                        <xsl:with-param name="coded">false</xsl:with-param>
                      </xsl:call-template>
                    </a:documentation>
                  </xsl:if>
                  <xsl:choose>
                    <xsl:when test="$parameterize='false'">
                      <xsl:call-template name="defineContent"/>
                      <xsl:if test="not($Attributes='') or $TEIC='true'">
                        <xsl:call-template name="defineAttributes"/>
                      </xsl:if>
                    </xsl:when>
                    <xsl:otherwise>
                      <ref xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}.content"/>
                      <xsl:if test="not($Attributes='') or $TEIC='true'">
                        <ref xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}.localattributes"/>
                      </xsl:if>
                    </xsl:otherwise>
                  </xsl:choose>
<!--
		  <xsl:if test="@ident='TEI' or @ident='teiCorpus'">
		    <rng:optional>
		      <rng:attribute name="schemaLocation"
				     ns="http://www.w3.org/2001/XMLSchema-instance">
			<rng:text/>
		      </rng:attribute>
		    </rng:optional>
		  </xsl:if>
-->
                </element>
              </define>
              <xsl:if test="$parameterize='true'">
                <define xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}.content">
                  <xsl:call-template name="defineContent"/>
                </define>
                <xsl:if test="not($Attributes='') or $TEIC='true'">
                  <define xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}.localattributes">
                    <xsl:call-template name="defineAttributes"/>
                  </define>
                </xsl:if>
                <xsl:apply-templates mode="tangleModel" select="classes/memberOf"/>
              </xsl:if>
            </xsl:otherwise>
          </xsl:choose>
        </Wrapper>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>
  <xsl:template name="summarizeAttributes">
    <xsl:for-each select=".//attDef">x</xsl:for-each>
    <xsl:for-each select="classes/memberOf">
      <xsl:for-each select="key('CLASSES',@key)">
        <xsl:if test="@type='atts'">x</xsl:if>
      </xsl:for-each>
    </xsl:for-each>
  </xsl:template>
  <xsl:template name="defineAttributes">
    <xsl:variable name="name" select="@ident"/>
    <xsl:if test="$parameterize='true'">
      <xsl:if test="$TEIC='true'">
        <rng:ref name="att.global.attributes"/>
      </xsl:if>
      <xsl:for-each select="classes/memberOf">
        <xsl:for-each select="key('CLASSES',@key)">
          <xsl:if test="@type='atts'">
            <ref xmlns="http://relaxng.org/ns/structure/1.0" name="{@ident}.attributes"/>
          </xsl:if>
        </xsl:for-each>
      </xsl:for-each>
    </xsl:if>
    <xsl:apply-templates mode="tangle" select="attList">
      <xsl:with-param name="element">
        <xsl:value-of select="$name"/>
      </xsl:with-param>
    </xsl:apply-templates>
<!-- place holder to make sure something gets into the
	     pattern -->
    <xsl:if test="$TEIC='true'">
      <empty xmlns="http://relaxng.org/ns/structure/1.0"/>
    </xsl:if>
<!--
    <xsl:choose>
      <xsl:when test="$TEIC='true'">
	<optional xmlns="http://relaxng.org/ns/structure/1.0">
	  <attribute name="TEIform" a:defaultValue="{@ident}" xmlns="http://relaxng.org/ns/structure/1.0">
	    <text xmlns="http://relaxng.org/ns/structure/1.0"/>
	  </attribute>
	</optional>
      </xsl:when>
      <xsl:otherwise>
	<empty xmlns="http://relaxng.org/ns/structure/1.0"/>
      </xsl:otherwise>
    </xsl:choose>
-->
  </xsl:template>
  <xsl:template name="defineContent">
    <xsl:variable name="Contents">
      <BLAH>
        <xsl:choose>
          <xsl:when test="content/valList[@type='closed' and @repeatable='true']">
            <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
              <rng:oneOrMore>
                <rng:choice>
                  <xsl:for-each select="content">
                    <xsl:call-template name="valListChildren"/>
                  </xsl:for-each>
                </rng:choice>
              </rng:oneOrMore>
            </rng:list>
          </xsl:when>
          <xsl:when test="content/valList[@type='closed'] and content/datatype[@maxOccurs='unbounded']">
            <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
              <rng:oneOrMore>
                <rng:choice>
                  <xsl:for-each select="content">
                    <xsl:call-template name="valListChildren"/>
                  </xsl:for-each>
                </rng:choice>
              </rng:oneOrMore>
            </rng:list>
          </xsl:when>
          <xsl:when test="content/valList[@type='closed']">
            <xsl:for-each select="content">
              <xsl:call-template name="valListChildren"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:when test="content">
            <xsl:apply-templates select="content/*"/>
          </xsl:when>
          <xsl:otherwise>
            <rng:empty/>
          </xsl:otherwise>
        </xsl:choose>
      </BLAH>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="count(exsl:node-set($Contents)/BLAH/*)=0">
        <rng:empty/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="exsl:node-set($Contents)/BLAH">
          <xsl:copy-of select="*"/>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:apply-templates select="constraintSpec"/>


  </xsl:template>
  <xsl:template name="valListChildren">
    <rng:choice>
      <xsl:for-each select="valList/valItem">
        <rng:value>
          <xsl:choose>
            <xsl:when test="altIdent=@ident">
              <xsl:value-of select="@ident"/>
            </xsl:when>
            <xsl:when test="altIdent">
              <xsl:value-of select="normalize-space(altIdent)"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@ident"/>
            </xsl:otherwise>
          </xsl:choose>
        </rng:value>
        <xsl:if test="not($oddmode='tei')">
          <a:documentation>
            <xsl:call-template name="makeDescription">
              <xsl:with-param name="includeValList">true</xsl:with-param>
              <xsl:with-param name="coded">false</xsl:with-param>
            </xsl:call-template>
          </a:documentation>
        </xsl:if>
      </xsl:for-each>
    </rng:choice>
  </xsl:template>
  <xsl:template match="elementSpec/@ident"/>
  <xsl:template match="elementSpec/desc"/>
  <xsl:template match="classSpec/desc"/>
  <xsl:template match="macroSpec/desc"/>
  <xsl:template match="elementSpec/gloss"/>
  <xsl:template match="classSpec/gloss"/>
  <xsl:template match="macroSpec/gloss"/>
  <xsl:template match="index">
    <xsl:call-template name="makeAnchor">
      <xsl:with-param name="name">IDX-<xsl:number level="any"/></xsl:with-param>
    </xsl:call-template>
  </xsl:template>
  <xsl:template match="macroSpec" mode="tangle">
    <xsl:param name="msection"/>
    <xsl:param name="filename"/>
    <xsl:variable name="entCont">
      <BLAH>
        <xsl:choose>
          <xsl:when test="not($msection='') and content/rng:group">
            <rng:choice>
              <xsl:apply-templates select="content/rng:group/rng:*"/>
            </rng:choice>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="content/rng:*"/>
          </xsl:otherwise>
        </xsl:choose>
      </BLAH>
    </xsl:variable>
    <xsl:variable name="entCount">
      <xsl:for-each select="exsl:node-set($entCont)/BLAH">
        <xsl:value-of select="count(rng:*)"/>
      </xsl:for-each>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="@ident=&quot;TEI.singleBase&quot;"/>
      <xsl:when test="starts-with($entCont,&quot;'&quot;)">
        <xsl:if test="$verbose='true'">
          <xsl:message>Omit <xsl:value-of select="$entCont"/> for <xsl:value-of select="@ident"/></xsl:message>
        </xsl:if>
      </xsl:when>
      <xsl:when test="starts-with($entCont,&quot;-&quot;)">
        <xsl:if test="$verbose='true'">
          <xsl:message>Omit <xsl:value-of select="$entCont"/> for <xsl:value-of select="@ident"/></xsl:message>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:if test="$verbose='true'">
          <xsl:message> macroSpec <xsl:value-of select="@ident"/></xsl:message>
        </xsl:if>
        <xsl:call-template name="bitOut">
          <xsl:with-param name="grammar">true</xsl:with-param>
          <xsl:with-param name="content">
            <Wrapper>
              <define xmlns="http://relaxng.org/ns/structure/1.0" name="{$patternPrefixText}{@ident}">
                <xsl:if test="$parameterize='true'">
                  <xsl:if test="starts-with(@ident,'macro.component')     or @predeclare='true'">
                    <xsl:attribute name="combine">choice</xsl:attribute>
                  </xsl:if>
                </xsl:if>
                <xsl:choose>
                  <xsl:when test="starts-with(@ident,'type')">
                    <xsl:copy-of select="exsl:node-set($entCont)/BLAH/rng:*"/>
                  </xsl:when>
                  <xsl:when test="$entCount=0">
                    <rng:choice>
                      <empty xmlns="http://relaxng.org/ns/structure/1.0"/>
                    </rng:choice>
                  </xsl:when>
                  <xsl:when test="$entCount=1">
                    <xsl:copy-of select="exsl:node-set($entCont)/BLAH/rng:*"/>
                  </xsl:when>
                  <xsl:when test="content/rng:text|content/rng:ref">
                    <rng:choice>
                      <xsl:copy-of select="exsl:node-set($entCont)/BLAH/rng:*"/>
                    </rng:choice>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:copy-of select="exsl:node-set($entCont)/BLAH/rng:*"/>
                  </xsl:otherwise>
                </xsl:choose>
              </define>
            </Wrapper>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="macroSpec/@ident"/>
  <xsl:template match="macroSpec/content/rng:*"/>
  <xsl:template match="memberOf" mode="tangleModel">
<!--
    <xsl:variable name="owner">
      <xsl:value-of
        select="ancestor::elementSpec/@ident|ancestor::classSpec/@ident"
      />
    </xsl:variable>
    <xsl:for-each select="key('IDENTS',@key)">
      <xsl:if test="@type='model'">
        <define combine="choice" name="{@ident}"
          xmlns="http://relaxng.org/ns/structure/1.0">
          <ref name="{$patternPrefixText}{$owner}"
            xmlns="http://relaxng.org/ns/structure/1.0"/>
        </define>
      </xsl:if>
    </xsl:for-each>
-->
  </xsl:template>
  <xsl:template match="moduleRef" mode="tangle">
    <xsl:variable name="This" select="@key"/>
    <xsl:if test="$verbose='true'">
      <xsl:message> .... import module [<xsl:value-of select="$This"/>
        <xsl:value-of select="@url"/>] </xsl:message>
    </xsl:if>
    <xsl:call-template name="bitOut">
      <xsl:with-param name="grammar">true</xsl:with-param>
      <xsl:with-param name="content">
        <Wrapper>
          <xsl:choose>
            <xsl:when test="@url and $parameterize='true'">
              <include xmlns="http://relaxng.org/ns/structure/1.0" href="{@url}">
                <xsl:copy-of select="content/*"/>
              </include>
            </xsl:when>
            <xsl:when test="@url and $parameterize='false'">
              <xsl:comment>Start of import of <xsl:value-of select="@url"/></xsl:comment>
              <rng:div>
                <xsl:for-each select="document(@url)/rng:grammar">
                  <xsl:apply-templates mode="expandRNG" select="*|@*|text()|comment()|processing-instruction()"/>
                </xsl:for-each>
                <xsl:copy-of select="content/*"/>
              </rng:div>
              <xsl:comment>End of import of <xsl:value-of select="@url"/>
              </xsl:comment>
            </xsl:when>
            <xsl:otherwise>
              <include xmlns="http://relaxng.org/ns/structure/1.0" href="{$schemaBaseURL}{$This}.rng">
                <xsl:attribute name="ns">
                  <xsl:choose>
                    <xsl:when test="ancestor::schemaSpec/@ns">
                      <xsl:value-of select="ancestor::schemaSpec/@ns"/>
                    </xsl:when>
                    <xsl:otherwise>http://www.tei-c.org/ns/1.0</xsl:otherwise>
                  </xsl:choose>
                </xsl:attribute>
                <xsl:for-each select="../*[@module=$This and not(@mode='add')]">
                  <xsl:apply-templates mode="tangle" select="."/>
                </xsl:for-each>
              </include>
            </xsl:otherwise>
          </xsl:choose>
        </Wrapper>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>
  <xsl:template match="@*|text()|comment()|processing-instruction" mode="expandRNG">
    <xsl:copy/>
  </xsl:template>
  <xsl:template match="*" mode="expandRNG">
    <xsl:choose>
      <xsl:when test="local-name(.)='start'"/>
      <xsl:when test="local-name(.)='include'">
        <xsl:if test="$verbose='true'">
          <xsl:message> .... import <xsl:value-of select="@href"/></xsl:message>
        </xsl:if>
        <xsl:comment>Start of import of <xsl:value-of select="@href"/>
        </xsl:comment>
        <rng:div>
          <xsl:for-each select="document(@href)/rng:grammar">
            <xsl:apply-templates mode="expandRNG" select="*|@*|text()|comment()|processing-instruction()"/>
          </xsl:for-each>
        </rng:div>
        <xsl:comment>End of import of <xsl:value-of select="@href"/>
        </xsl:comment>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:apply-templates mode="expandRNG" select="*|@*|text()|comment()|processing-instruction()"/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="remarks" mode="tangle"/>
  <xsl:template match="specGrp" mode="ok">
    <xsl:param name="filename"/>
    <xsl:if test="$verbose='true'">
      <xsl:message> processing specGrp <xsl:value-of select="@id"/></xsl:message>
    </xsl:if>
    <xsl:call-template name="processSchemaFragment">
      <xsl:with-param name="filename" select="$filename"/>
    </xsl:call-template>
  </xsl:template>
  <xsl:template match="tag">
    <xsl:call-template name="typewriter">
      <xsl:with-param name="text">
        <xsl:text>&lt;</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>&gt;</xsl:text>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>
  <xsl:template name="compositeNumber">
    <xsl:choose>
      <xsl:when test="ancestor::div1">
        <xsl:for-each select="ancestor::div1">
          <xsl:number/>
        </xsl:for-each>
        <xsl:text>.</xsl:text>
        <xsl:number from="div1" level="any"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="ancestor::div[1]">
          <xsl:number count="div" from="text" level="multiple"/>
        </xsl:for-each>
        <xsl:text>.</xsl:text>
        <xsl:number from="div"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="copyright">
    <xsl:apply-templates mode="copyrighttext" select="/TEI.2/teiHeader/fileDesc/publicationStmt/availability"/>
  </xsl:template>
  <xsl:template match="p" mode="copyrighttext">
    <xsl:text>&#10;</xsl:text>
    <xsl:apply-templates/>
  </xsl:template>
  <xsl:template match="list" mode="copyrighttext">
    <xsl:text>&#10;</xsl:text>
    <xsl:apply-templates/>
  </xsl:template>
  <xsl:template match="item" mode="copyrighttext">
    <xsl:text>&#10; *</xsl:text>
    <xsl:apply-templates/>
  </xsl:template>
  <xsl:template name="attributeData">
    <xsl:choose>
      <xsl:when test="valList[@type='closed']">
        <rng:choice>
          <xsl:for-each select="valList/valItem">
            <rng:value>
              <xsl:choose>
                <xsl:when test="altIdent=@ident">
                  <xsl:value-of select="@ident"/>
                </xsl:when>
                <xsl:when test="altIdent">
                  <xsl:value-of select="normalize-space(altIdent)"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="@ident"/>
                </xsl:otherwise>
              </xsl:choose>
            </rng:value>
            <xsl:if test="not($oddmode='tei')">
              <a:documentation>
                <xsl:call-template name="makeDescription">
                  <xsl:with-param name="includeValList">true</xsl:with-param>
                  <xsl:with-param name="coded">false</xsl:with-param>
                </xsl:call-template>
              </a:documentation>
            </xsl:if>
          </xsl:for-each>
        </rng:choice>
      </xsl:when>
      <xsl:when test="valList[@type='semi']">
        <rng:choice>
          <xsl:for-each select="valList/valItem">
            <rng:value>
              <xsl:choose>
                <xsl:when test="altIdent=@ident">
                  <xsl:value-of select="@ident"/>
                </xsl:when>
                <xsl:when test="altIdent">
                  <xsl:value-of select="normalize-space(altIdent)"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="@ident"/>
                </xsl:otherwise>
              </xsl:choose>
            </rng:value>
            <xsl:if test="not($oddmode='tei')">
              <a:documentation>
                <xsl:call-template name="makeDescription">
                  <xsl:with-param name="includeValList">true</xsl:with-param>
                  <xsl:with-param name="coded">false</xsl:with-param>
                </xsl:call-template>
              </a:documentation>
            </xsl:if>
          </xsl:for-each>
          <xsl:choose>
            <xsl:when test="datatype/rng:ref[@name='data.enumerated']">
              <rng:data type="Name"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates mode="forceRNG" select="datatype/rng:*"/>
            </xsl:otherwise>
          </xsl:choose>
        </rng:choice>
      </xsl:when>
      <xsl:when test="datatype/rng:*">
        <xsl:apply-templates mode="forceRNG" select="datatype/rng:*"/>
      </xsl:when>
      <xsl:otherwise>
        <text xmlns="http://relaxng.org/ns/structure/1.0"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
<!-- occursOutOfBounds is a template that is called only as -->
<!-- a subroutine of makeSimpleAttribute. It is used iff the -->
<!-- big <choose> that handles minOccurs= and maxOccurs= runs -->
<!-- into values of those attributes that it doesn't know how -->
<!-- to handle properly. -->
  <xsl:template name="occursOutOfBounds">
    <xsl:param name="min"/>
    <xsl:param name="max"/>
<!-- $myMin = min( $min, 3 ) -->
    <xsl:variable name="myMin">
      <xsl:choose>
        <xsl:when test="$min &gt; 3">3</xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$min"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="message">
      <xsl:text>Sorry, unable to create schema that uses actual counts minOccurs=</xsl:text>
      <xsl:value-of select="$min"/>
      <xsl:text> and maxOccurs=</xsl:text>
      <xsl:value-of select="$max"/>
      <xsl:text>; approximating to minOccurs=</xsl:text>
      <xsl:value-of select="$myMin"/>
      <xsl:text> and maxOccurs=unbounded.</xsl:text>
    </xsl:variable>
    <xsl:if test="$verbose='true'">
      <xsl:message>
        <xsl:value-of select="$message"/>
      </xsl:message>
    </xsl:if>
    <a:documentation>
      <xsl:value-of select="$message"/>
    </a:documentation>
    <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
      <xsl:if test="$myMin &gt; 0">
        <xsl:call-template name="attributeData"/>
      </xsl:if>
      <xsl:if test="$myMin &gt; 1">
        <xsl:call-template name="attributeData"/>
      </xsl:if>
      <xsl:if test="$myMin &gt; 2">
        <xsl:call-template name="attributeData"/>
      </xsl:if>
      <rng:zeroOrMore>
        <xsl:call-template name="attributeData"/>
      </rng:zeroOrMore>
    </rng:list>
  </xsl:template>
  <xsl:template name="makeSimpleAttribute">
    <xsl:variable name="name">
      <xsl:choose>
        <xsl:when test="altIdent=@ident">
          <xsl:value-of select="@ident"/>
        </xsl:when>
        <xsl:when test="altIdent">
          <xsl:value-of select="normalize-space(altIdent)"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:if test="@ns='http://www.w3.org/XML/1998/namespace'">xml:</xsl:if>
          <xsl:value-of select="@ident"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <rng:attribute name="{$name}">
      <xsl:if test="@ns">
        <xsl:copy-of select="@ns"/>
      </xsl:if>
      <xsl:if test="defaultVal and not(defaultVal='')">
        <xsl:attribute name="a:defaultValue">
          <xsl:value-of select="normalize-space(defaultVal)"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="not($oddmode='tei')">
        <a:documentation>
          <xsl:call-template name="makeDescription">
            <xsl:with-param name="includeValList">true</xsl:with-param>
            <xsl:with-param name="coded">false</xsl:with-param>
          </xsl:call-template>
        </a:documentation>
      </xsl:if>
      <xsl:variable name="minOccurs">
        <xsl:choose>
          <xsl:when test="datatype/@minOccurs">
            <xsl:value-of select="datatype/@minOccurs"/>
          </xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="maxOccurs">
        <xsl:choose>
          <xsl:when test="datatype/@maxOccurs">
            <xsl:value-of select="datatype/@maxOccurs"/>
          </xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:choose>
<!-- This huge <xsl:choose> handles minOccurs= and -->
<!-- maxOccurs= by brute force. It is currently -->
<!-- set to handle: -->
<!--
          ** min=0, max=1
          ** min=0, max=2
          ** min=0, max=[anything else]  # approximates & issues warning
          ** min=0, max=unbounded
          ** min=1, max=1
          ** min=1, max=2
          ** min=1, max=[anything else]  # approximates & issues warning
          ** min=1, max=unbounded
          ** min=2, max=2
          ** min=2, max=[anything else]  # approximates & issues warning
          ** min=2, max=unbounded
          ** min>2, max=[anything]  # approximates & issues warning
          ** anything else  # approximates, issues error msg & warning
        -->
<!-- We don't provide for min=0 max=0, as that's -->
<!-- the same as using <rng:empty> as content of -->
<!-- <datatype>. -->
        <xsl:when test="$minOccurs=0 and $maxOccurs=1">
          <rng:optional xmlns:rng="http://relaxng.org/ns/structure/1.0">
            <xsl:call-template name="attributeData"/>
          </rng:optional>
        </xsl:when>
        <xsl:when test="$minOccurs=0 and $maxOccurs=2">
          <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
            <rng:optional>
              <xsl:call-template name="attributeData"/>
            </rng:optional>
            <rng:optional>
              <xsl:call-template name="attributeData"/>
            </rng:optional>
          </rng:list>
        </xsl:when>
        <xsl:when test="$minOccurs=0 and $maxOccurs=3">
          <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
            <rng:optional>
              <xsl:call-template name="attributeData"/>
            </rng:optional>
            <rng:optional>
              <xsl:call-template name="attributeData"/>
            </rng:optional>
            <rng:optional>
              <xsl:call-template name="attributeData"/>
            </rng:optional>
          </rng:list>
        </xsl:when>
        <xsl:when test="$minOccurs=0 and $maxOccurs &gt; 3">
          <xsl:call-template name="occursOutOfBounds">
            <xsl:with-param name="min">
              <xsl:value-of select="$minOccurs"/>
            </xsl:with-param>
            <xsl:with-param name="max">
              <xsl:value-of select="$maxOccurs"/>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="$minOccurs=0 and $maxOccurs='unbounded'">
          <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
            <rng:zeroOrMore>
              <xsl:call-template name="attributeData"/>
            </rng:zeroOrMore>
          </rng:list>
        </xsl:when>
        <xsl:when test="$minOccurs=1 and $maxOccurs=1">
          <xsl:call-template name="attributeData"/>
        </xsl:when>
        <xsl:when test="$minOccurs=1 and $maxOccurs=2">
          <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
            <xsl:call-template name="attributeData"/>
            <rng:optional>
              <xsl:call-template name="attributeData"/>
            </rng:optional>
          </rng:list>
        </xsl:when>
        <xsl:when test="$minOccurs=1 and $maxOccurs=3">
          <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
            <xsl:call-template name="attributeData"/>
            <rng:optional>
              <xsl:call-template name="attributeData"/>
            </rng:optional>
            <rng:optional>
              <xsl:call-template name="attributeData"/>
            </rng:optional>
          </rng:list>
        </xsl:when>
        <xsl:when test="$minOccurs=1 and $maxOccurs &gt; 3">
          <xsl:call-template name="occursOutOfBounds">
            <xsl:with-param name="min">
              <xsl:value-of select="$minOccurs"/>
            </xsl:with-param>
            <xsl:with-param name="max">
              <xsl:value-of select="$maxOccurs"/>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="$minOccurs=1 and $maxOccurs='unbounded'">
          <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
            <rng:oneOrMore>
              <xsl:call-template name="attributeData"/>
            </rng:oneOrMore>
          </rng:list>
        </xsl:when>
        <xsl:when test="$minOccurs=2 and $maxOccurs=2">
          <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
            <xsl:call-template name="attributeData"/>
            <xsl:call-template name="attributeData"/>
          </rng:list>
        </xsl:when>
        <xsl:when test="$minOccurs=2 and $maxOccurs &gt; 2">
          <xsl:call-template name="occursOutOfBounds">
            <xsl:with-param name="min">
              <xsl:value-of select="$minOccurs"/>
            </xsl:with-param>
            <xsl:with-param name="max">
              <xsl:value-of select="$maxOccurs"/>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="$minOccurs=2 and $maxOccurs='unbounded'">
          <rng:list xmlns:rng="http://relaxng.org/ns/structure/1.0">
            <xsl:call-template name="attributeData"/>
            <rng:oneOrMore>
              <xsl:call-template name="attributeData"/>
            </rng:oneOrMore>
          </rng:list>
        </xsl:when>
        <xsl:when test="$minOccurs &gt; 2">
          <xsl:call-template name="occursOutOfBounds">
            <xsl:with-param name="min">
              <xsl:value-of select="$minOccurs"/>
            </xsl:with-param>
            <xsl:with-param name="max">
              <xsl:value-of select="$maxOccurs"/>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>
            <xsl:text>Internal error: minOccurs=</xsl:text>
            <xsl:value-of select="$minOccurs"/>
            <xsl:text> maxOccurs=</xsl:text>
            <xsl:value-of select="$maxOccurs"/>
            <xsl:text> is an unanticipated combination.</xsl:text>
          </xsl:message>
          <xsl:call-template name="occursOutOfBounds">
            <xsl:with-param name="min">
              <xsl:value-of select="$minOccurs"/>
            </xsl:with-param>
            <xsl:with-param name="max">
              <xsl:value-of select="$maxOccurs"/>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </rng:attribute>
  </xsl:template>
  <xsl:template name="makeAnAttribute">
    <xsl:choose>
      <xsl:when test="@usage='req'">
        <xsl:call-template name="makeSimpleAttribute"/>
      </xsl:when>
<!--
      <xsl:when test="parent::attList[@org='choice']">
        <xsl:call-template name="makeSimpleAttribute"/>
      </xsl:when>
-->
      <xsl:otherwise>
        <rng:optional>
          <xsl:call-template name="makeSimpleAttribute"/>
        </rng:optional>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="generateClassParents">
    <xsl:choose>
      <xsl:when test="not(classes)"> (none) </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="classes/memberOf">
          <xsl:if test="preceding-sibling::memberOf">
            <xsl:text>: </xsl:text>
          </xsl:if>
          <xsl:choose>
            <xsl:when test="key('CLASSES',@key)">
              <xsl:for-each select="key('CLASSES',@key)">
                <xsl:call-template name="makeLink">
                  <xsl:with-param name="class">classlink</xsl:with-param>
                  <xsl:with-param name="name">
                    <xsl:value-of select="@ident"/>
                  </xsl:with-param>
                  <xsl:with-param name="text">
                    <xsl:value-of select="@ident"/>
                  </xsl:with-param>
                </xsl:call-template>
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@key"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="linkStyle"/>
  <xsl:template name="getSpecURL">
    <xsl:param name="name"/>
    <xsl:param name="type"/>
    <xsl:choose>
      <xsl:when test="$type='macro'">
        <xsl:for-each select="key('IDS','REFENT')">
          <xsl:apply-templates mode="generateLink" select="."/>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="$type='element'">
        <xsl:for-each select="key('IDS','REFTAG')">
          <xsl:apply-templates mode="generateLink" select="."/>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="$type='class'">
        <xsl:for-each select="key('IDS','REFCLA')">
          <xsl:apply-templates mode="generateLink" select="."/>
        </xsl:for-each>
      </xsl:when>
    </xsl:choose>
    <xsl:text>#</xsl:text>
    <xsl:value-of select="$name"/>
  </xsl:template>
  <xsl:template name="linkTogether">
    <xsl:param name="name"/>
    <xsl:param name="reftext"/>
    <xsl:param name="class">link_odd</xsl:param>
    <xsl:variable name="documentationLanguage">
      <xsl:call-template name="generateDoc"/>
    </xsl:variable>
    <xsl:variable name="partialname">
      <xsl:choose>
        <xsl:when test="contains($name,'_')">
          <xsl:value-of select="substring-before($name,'_')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="link">
      <xsl:choose>
        <xsl:when test="$reftext=''">
          <xsl:value-of select="$name"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$reftext"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="not(key('IDENTS',$partialname))">
        <xsl:value-of select="$link"/>
      </xsl:when>
      <xsl:when test="$oddmode='html' and $splitLevel=-1">
        <a xmlns="http://www.w3.org/1999/xhtml" class="{$class}" href="#{$partialname}">
          <xsl:value-of select="$link"/>
        </a>
      </xsl:when>
      <xsl:when test="$oddmode='html' and $STDOUT='true'">
        <a xmlns="http://www.w3.org/1999/xhtml" class="{$class}">
          <xsl:attribute name="href">
            <xsl:for-each select="key('IDENTS',$partialname)">
              <xsl:call-template name="getSpecURL">
                <xsl:with-param name="name">
                  <xsl:value-of select="$partialname"/>
                </xsl:with-param>
                <xsl:with-param name="type">
                  <xsl:value-of select="substring-before(local-name(),'Spec')"/>
                </xsl:with-param>
              </xsl:call-template>
            </xsl:for-each>
          </xsl:attribute>
          <xsl:value-of select="$link"/>
        </a>
      </xsl:when>
      <xsl:when test="$oddmode='html'">
        <a xmlns="http://www.w3.org/1999/xhtml" class="{$class}" href="{concat('ref-',$partialname,'.html')}">
          <xsl:value-of select="$link"/>
        </a>
      </xsl:when>
      <xsl:when test="$oddmode='pdf'">
        <fo:inline>
          <xsl:value-of select="$link"/>
        </fo:inline>
      </xsl:when>
      <xsl:when test="$oddmode='tei'">
        <ref target="#{$partialname}">
          <xsl:value-of select="$link"/>
        </ref>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$link"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="processSchemaFragment">
    <xsl:param name="filename"/>
    <xsl:variable name="secnum">
      <xsl:call-template name="sectionNumber"/>
    </xsl:variable>
    <xsl:apply-templates mode="tangle"/>
  </xsl:template>
  <xsl:template name="make-ns-declaration">
    <xsl:param name="is-default"/>
    <xsl:param name="prefix"/>
    <xsl:param name="uri"/>
  </xsl:template>
  <xsl:template name="inhnamespace"/>

  <xsl:template match="constraintSpec/desc"/>
  <xsl:template match="constraintSpec/gloss"/>
  <xsl:template match="constraintSpec/equiv"/>

  <xsl:template match="constraintSpec">
  </xsl:template>
  <xsl:template match="s:*"/>
  <xsl:template match="altIdent"/>
  <xsl:template match="a:*">
    <xsl:copy-of select="."/>
  </xsl:template>
  <xsl:template match="classSpec" mode="processDefaultAtts">
    <xsl:if test="$verbose='true'">
      <xsl:message> .. default attribute settings for <xsl:value-of select="@ident"/></xsl:message>
    </xsl:if>
    <xsl:call-template name="bitOut">
      <xsl:with-param name="grammar">true</xsl:with-param>
      <xsl:with-param name="content">
        <Wrapper>
          <rng:define combine="choice" name="{@ident}.attributes">
            <rng:empty/>
          </rng:define>
        </Wrapper>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>
<!-- Force an output element in the RNG namespace. 
       I don't see why this is necessary, but xsltproc gets
       it wrong otherwise. I suspect a bug there somewhere.
  -->
  <xsl:template match="*" mode="forceRNG">
    <xsl:element xmlns="http://relaxng.org/ns/structure/1.0" name="{local-name(.)}">
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates mode="forceRNG"/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="rng:ref" mode="forceRNG">
    <xsl:element xmlns="http://relaxng.org/ns/structure/1.0" name="ref">
      <xsl:attribute name="name">
        <xsl:choose>
          <xsl:when test="key('IDENTS',@name)">
            <xsl:value-of select="$patternPrefixText"/>
          </xsl:when>
          <xsl:when test="starts-with(@name,'att.') and key('IDENTS',substring-before(@name,'.attribute.'))"> </xsl:when>
          <xsl:when test="key('IDENTS',substring-before(@name,'_'))">
            <xsl:value-of select="$patternPrefixText"/>
          </xsl:when>
        </xsl:choose>
        <xsl:value-of select="@name"/>
      </xsl:attribute>
    </xsl:element>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements schemaSpec</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="schemaSpec">
    <xsl:call-template name="processSchemaFragment"/>
  </xsl:template>
  <xsl:template name="typewriter"/>
  <xsl:template name="refdoc"/>
  <xsl:template name="generateOutput">
    <xsl:param name="body"/>
    <xsl:param name="suffix"/>
    <xsl:param name="method">xml</xsl:param>
    <xsl:variable name="processor">
      <xsl:value-of select="system-property('xsl:vendor')"/>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$outputDir='' or $outputDir='-'">
        <xsl:copy-of select="$body"/>
      </xsl:when>
      <xsl:when test="contains($processor,'SAXON')">
        <xsl:copy-of select="$body"/>
      </xsl:when>
      <xsl:when test="$method='text' and element-available('exsl:document')">
        <xsl:if test="$verbose='true'">
          <xsl:message> File [<xsl:value-of select="$outputDir"/>/<xsl:value-of select="@ident"/><xsl:value-of select="$suffix"/>] </xsl:message>
        </xsl:if>
        <xsl:if test="element-available('exsl:document')">
          <exsl:document href="{$outputDir}/{@ident}{$suffix}" method="text">
            <xsl:copy-of select="$body"/>
          </exsl:document>
        </xsl:if>
      </xsl:when>
      <xsl:when test="element-available('exsl:document')">
        <xsl:if test="$verbose='true'">
          <xsl:message> File [<xsl:value-of select="$outputDir"/>/<xsl:value-of select="@ident"/><xsl:value-of select="$suffix"/>] </xsl:message>
        </xsl:if>
        <exsl:document href="{$outputDir}/{@ident}{$suffix}" indent="yes" method="xml">
          <xsl:copy-of select="$body"/>
        </exsl:document>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$body"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="showDate">
    <xsl:variable name="processor">
      <xsl:value-of select="system-property('xsl:vendor')"/>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="function-available('edate:date-time')">
        <xsl:value-of select="edate:date-time()"/>
      </xsl:when>
      <xsl:when test="contains($processor,'SAXON')">
        <xsl:value-of xmlns:Date="/java.util.Date" select="Date:toString(Date:new())"/>
      </xsl:when>
      <xsl:otherwise> (unknown date) </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="makeDescription">
    <xsl:param name="includeValList">false</xsl:param>
    <xsl:param name="coded">true</xsl:param>
    <xsl:variable name="documentationLanguage">
      <xsl:call-template name="generateDoc"/>
    </xsl:variable>
    <xsl:variable name="langs">
      <xsl:value-of select="concat(normalize-space($documentationLanguage),' ')"/>
    </xsl:variable>
    <xsl:variable name="firstLang">
      <xsl:value-of select="substring-before($langs,' ')"/>
    </xsl:variable>
    <xsl:call-template name="makeGloss">
      <xsl:with-param name="langs" select="$langs"/>
    </xsl:call-template>
<!-- now the description -->
    <xsl:choose>
      <xsl:when test="not(desc)"> </xsl:when>
      <xsl:when test="count(desc)=1">
        <xsl:for-each select="desc">
          <xsl:apply-templates select="." mode="inLanguage"/>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="desc[@xml:lang=$firstLang]">
        <xsl:for-each select="desc[@xml:lang=$firstLang]">
          <xsl:apply-templates select="." mode="inLanguage"/>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="D">
          <xsl:for-each select="desc">
            <xsl:variable name="currentLang">
              <xsl:call-template name="findLanguage"/>
            </xsl:variable>
            <xsl:if test="contains($langs,concat($currentLang,' '))">
	      <xsl:apply-templates select="." mode="inLanguage"/>
            </xsl:if>
          </xsl:for-each>
        </xsl:variable>
        <xsl:choose>
          <xsl:when test="$D='' and desc[not(@xml:lang)]">
            <xsl:for-each select="desc[not(@xml:lang)]">
	      <xsl:apply-templates select="." mode="inLanguage"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:when test="$coded='false'">
            <xsl:value-of select="$D"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:copy-of select="$D"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="$includeValList='false'"/>
      <xsl:when test="valList[@type='open']">
        <xsl:text>&#10;</xsl:text>
        <xsl:call-template name="i18n">
          <xsl:with-param name="word">
            <xsl:text>Sample values include</xsl:text>
          </xsl:with-param>
        </xsl:call-template>
        <xsl:text>: </xsl:text>
        <xsl:for-each select="valList/valItem">
          <xsl:number/>
          <xsl:text>] </xsl:text>
          <xsl:choose>
            <xsl:when test="altIdent=@ident">
              <xsl:value-of select="@ident"/>
            </xsl:when>
            <xsl:when test="altIdent">
              <xsl:value-of select="normalize-space(altIdent)"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@ident"/>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:if test="gloss">
            <xsl:text> (</xsl:text>
	    <xsl:apply-templates select="gloss" mode="inLanguage"/>
            <xsl:text>)</xsl:text>
          </xsl:if>
          <xsl:if test="following-sibling::valItem">
            <xsl:text>; </xsl:text>
          </xsl:if>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="valList[@type='semi']">
        <xsl:text>&#10;</xsl:text>
        <xsl:call-template name="i18n">
          <xsl:with-param name="word">
            <xsl:text>Suggested values include</xsl:text>
          </xsl:with-param>
        </xsl:call-template>
        <xsl:text>: </xsl:text>
        <xsl:for-each select="valList/valItem">
          <xsl:number/>
          <xsl:text>] </xsl:text>
          <xsl:choose>
            <xsl:when test="altIdent=@ident">
              <xsl:value-of select="@ident"/>
            </xsl:when>
            <xsl:when test="altIdent">
              <xsl:value-of select="normalize-space(altIdent)"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@ident"/>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:if test="gloss">
            <xsl:text> (</xsl:text>
	    <xsl:apply-templates select="gloss" mode="inLanguage"/>
            <xsl:text>)</xsl:text>
          </xsl:if>
          <xsl:if test="following-sibling::valItem">
            <xsl:text>; </xsl:text>
          </xsl:if>
        </xsl:for-each>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="makeGloss">
    <xsl:param name="langs"/>
    <xsl:variable name="firstLang">
      <xsl:value-of select="substring-before($langs,' ')"/>
    </xsl:variable>

<!-- first the gloss -->
    <xsl:choose>
      <xsl:when test="not(gloss)"/>
      <xsl:when test="string-length(gloss)=0"/>
      <xsl:when test="count(gloss)=1 and not(gloss[@xml:lang])">
        <xsl:text> (</xsl:text>
	<xsl:apply-templates select="gloss" mode="inLanguage"/>
        <xsl:text>) </xsl:text>
      </xsl:when>
      <xsl:when test="gloss[@xml:lang=$firstLang]">
        <xsl:if test="not(gloss[@xml:lang=$firstLang]='')">
          <xsl:text> (</xsl:text>
	  <xsl:apply-templates select="gloss[@xml:lang=$firstLang]" mode="inLanguage"/>
          <xsl:text>) </xsl:text>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="G">
          <xsl:for-each select="gloss">
            <xsl:variable name="currentLang">
              <xsl:call-template name="findLanguage"/>
            </xsl:variable>
            <xsl:if test="contains($langs,concat($currentLang,' '))">
              <xsl:text>(</xsl:text>
          	<xsl:apply-templates select="." mode="inLanguage"/>
              <xsl:text>) </xsl:text>
            </xsl:if>
          </xsl:for-each>
        </xsl:variable>
        <xsl:choose>
          <xsl:when test="$G='' and gloss[not(@xml:lang)]">
            <xsl:text> (</xsl:text>
	    <xsl:apply-templates select="gloss[not(@xml:lang)]" mode="inLanguage"/>
            <xsl:text>) </xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:copy-of select="$G"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="findLanguage">
    <xsl:choose>
      <xsl:when test="@xml:lang">
        <xsl:value-of select="@xml:lang"/>
      </xsl:when>
      <xsl:when test="ancestor::*[@xml:lang]">
        <xsl:value-of select="(ancestor::*[@xml:lang])[1]/@xml:lang"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>en</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="sectionNumber">
    <xsl:for-each select="(ancestor::div1|ancestor::div2|ancestor::div3|ancestor::div4)[last()]">
      <xsl:for-each select="ancestor-or-self::div1">
        <xsl:number from="body" level="any"/>
        <xsl:text>.</xsl:text>
      </xsl:for-each>
      <xsl:number count="div2|div3|div4" from="div1" level="multiple"/>
    </xsl:for-each>
  </xsl:template>
  <xsl:template match="*" mode="expandSpecs">
    <xsl:copy-of select="."/>
  </xsl:template>
  <xsl:template match="specGrpRef" mode="expandSpecs">
    <xsl:choose>
      <xsl:when test="starts-with(@target,'#')">
        <xsl:for-each select="key('IDS',substring-after(@target,'#'))">
          <xsl:apply-templates mode="expandSpecs"/>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="document(@target)">
          <xsl:apply-templates mode="expandSpecs"/>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
<!-- list inside <desc> -->
  <xsl:template match="desc/list/item">
    <xsl:text> * </xsl:text>
    <xsl:apply-templates/>
  </xsl:template>
  <xsl:template name="makeTEIVersion">
    <xsl:choose>
      <xsl:when test="ancestor-or-self::TEI.2/processing-instruction()[name()='TEIVERSION']">
        <xsl:text>&#10;TEI Edition: </xsl:text>
        <xsl:value-of select="ancestor-or-self::TEI.2/processing-instruction()[name()='TEIVERSION']"/>
        <xsl:text>&#10;</xsl:text>
      </xsl:when>
      <xsl:when test="ancestor-or-self::TEI.2/teiHeader/fileDesc/editionStmt/edition">
        <xsl:text>&#10;Edition: </xsl:text>
        <xsl:value-of select="ancestor-or-self::TEI.2/teiHeader/fileDesc/editionStmt/edition"/>
        <xsl:text>&#10;</xsl:text>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="gloss" mode="inLanguage">
    <xsl:value-of select="."/>
  </xsl:template>
  <xsl:template match="desc" mode="inLanguage">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template name="processSchematron">
    <xsl:choose>
      <xsl:when test="ancestor::teix:egXML"/>
      <xsl:when test="self::s:ns">
	<s:ns prefix="{@prefix}" uri="{@uri}"/>
      </xsl:when>
      <xsl:when test="self::s:pattern">
	<xsl:copy-of select="."/>
      </xsl:when>
      <xsl:when test="self::s:rule">
	<s:pattern name="{ancestor::elementSpec/@ident}-constraint-{ancestor::constraintSpec/@ident}">
	  <xsl:copy-of select="."/>
	</s:pattern>
      </xsl:when>
      <xsl:when test="(self::s:report or self::s:assert) and ancestor::elementSpec">
	<s:pattern>
	    <xsl:attribute name="name">
	      <xsl:value-of
		  select="ancestor::elementSpec/@ident"/>
	      <xsl:text>-constraint-</xsl:text>
	      <xsl:value-of
		  select="ancestor::constraintSpec/@ident"/>
	      <xsl:if test="count(../s:report|../s:assert) &gt;1">
		<xsl:number/>
	      </xsl:if>
	    </xsl:attribute>
	  <rule xmlns="http://www.ascc.net/xml/schematron">
	    <xsl:attribute name="context">
	      <xsl:text></xsl:text>
	      <xsl:value-of select="../../@ident"/>
	    </xsl:attribute>
	    <xsl:copy-of select="."/>
	  </rule>
	</s:pattern>
      </xsl:when>
      <xsl:when test="self::sch:ns">
	<sch:ns prefix="{@prefix}" uri="{@uri}"/>
      </xsl:when>
      <xsl:when test="self::sch:pattern">
	<xsl:copy-of select="."/>
      </xsl:when>
      <xsl:when test="self::sch:rule">
	<pattern
	    xmlns="http://purl.oclc.org/dsdl/schematron"
	    id="{ancestor::elementSpec/@ident}-constraint-{../../@ident}">
	  <xsl:copy-of select="."/>
	</pattern>
      </xsl:when>
      <xsl:when test="(self::sch:report or self::sch:assert) and
		      ancestor::elementSpec">
	<pattern xmlns="http://purl.oclc.org/dsdl/schematron">
	    <xsl:attribute name="id">
	      <xsl:value-of
		  select="ancestor::elementSpec/@ident"/>
	      <xsl:text>-constraint-</xsl:text>
	      <xsl:value-of select="../../@ident"/>
	      <xsl:if test="count(../sch:report|sch:assert) &gt;1">
		<xsl:number/>
	      </xsl:if>
	    </xsl:attribute>
	    <rule>
	      <xsl:attribute name="context">
		<xsl:text></xsl:text>
		<xsl:value-of select="ancestor::elementSpec/@ident"/>
	      </xsl:attribute>
	      <xsl:copy-of select="."/>
	    </rule>
	</pattern>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
