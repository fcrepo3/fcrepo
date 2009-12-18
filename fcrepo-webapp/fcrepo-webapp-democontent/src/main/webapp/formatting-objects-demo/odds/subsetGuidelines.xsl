<?xml version="1.0" encoding="utf-8"?>
<!-- $Date: 
Text Encoding Initiative Consortium XSLT stylesheet family
2001/10/01 $, $Revision: 5774 $, $Author: rahtz $

XSL stylesheet to format TEI XML documents using ODD markup

 
##LICENSE
-->
<xsl:stylesheet xmlns:rng="http://relaxng.org/ns/structure/1.0" xmlns:eg="http://www.tei-c.org/ns/Examples" xmlns:tei="http://www.tei-c.org/ns/1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:exsl="http://exslt.org/common" xmlns:estr="http://exslt.org/strings" extension-element-prefixes="exsl estr" exclude-result-prefixes="tei exsl estr" version="1.0">
  <xsl:import href="../common/tei.xsl"/>
  <xsl:import href="teiodds.xsl"/>
  <xsl:param name="TEISERVER">http://localhost/Query/</xsl:param>
  <xsl:param name="localsource"/>
  <xsl:key name="MODS" match="moduleSpec" use="@ident"/>
  <xsl:key name="SPECS" match="specGrp" use="@id"/>
  <xsl:key name="LOCAL" match="classSpec|elementSpec|macroSpec" use="@ident"/>
  <xsl:key name="LOCALATT" match="attDef" use="concat(../../@ident,'::',@ident)"/>
  <xsl:output method="xml" indent="yes"/>
  <xsl:param name="verbose"/>
  <xsl:variable name="MAIN" select="/"/>
  <xsl:template match="text">
    <text>
      <xsl:apply-templates/>
      <xsl:if test="not(back)">
        <back>
          <xsl:call-template name="CAT"/>
        </back>
      </xsl:if>
    </text>
  </xsl:template>
  <xsl:template match="body">
    <body>
      <div0>
        <head>
          <xsl:call-template name="generateTitle"/>
        </head>
        <xsl:apply-templates/>
      </div0>
    </body>
  </xsl:template>
  <xsl:template match="back">
    <xsl:apply-templates/>
    <xsl:call-template name="CAT"/>
  </xsl:template>
  <xsl:template name="CAT">
    <div1 xml:id="REFCLA">
      <head>Model Class catalogue</head>
      <divGen type="modelclasscat"/>
    </div1>
    <div1 xml:id="ATTREFCLA">
      <head>Attribute Class catalogue</head>
      <divGen type="attclasscat"/>
    </div1>
    <div1 xml:id="REFENT">
      <head>Macro catalogue</head>
      <divGen type="macrocat"/>
    </div1>
    <div1 xml:id="REFTAG">
      <head>Element catalogue</head>
      <divGen type="elementcat"/>
    </div1>
  </xsl:template>
  <xsl:template match="schemaSpec">
    <div>
      <head>Schema [<xsl:value-of select="@ident"/>]</head>
      <eg>
        <xsl:apply-templates mode="verbatim"/>
      </eg>
      <xsl:apply-templates select="specGrp"/>
      <xsl:apply-templates select="moduleRef"/>
      <xsl:apply-templates select="*[@mode='add']"/>
    </div>
  </xsl:template>
  <xsl:template match="moduleRef[@key]">
    <xsl:variable name="test" select="@key"/>
    <xsl:call-template name="findNames">
      <xsl:with-param name="modname">
        <xsl:value-of select="$test"/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>
  <xsl:template name="findNames">
    <xsl:param name="modname"/>
    <xsl:variable name="KD" select="concat($modname,'-decl')"/>
    <xsl:choose>
      <xsl:when test="not($localsource='')">
        <xsl:variable name="Local">
          <List>
            <xsl:for-each select="document($localsource)/TEI.2">
              <xsl:copy-of select="*[@module=$modname]"/>
              <xsl:copy-of select="*[@module=$KD]"/>
            </xsl:for-each>
          </List>
        </xsl:variable>
        <xsl:for-each select="exsl:node-set($Local)/List">
          <xsl:call-template name="processThing"/>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="Remote">
          <xsl:value-of select="$TEISERVER"/>
          <xsl:text>allbymod.xql?module=</xsl:text>
          <xsl:value-of select="$modname"/>
        </xsl:variable>
        <xsl:for-each select="document($Remote)/List">
          <xsl:call-template name="processThing"/>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="processThing">
    <xsl:variable name="me" select="@ident"/>
    <xsl:variable name="here" select="."/>
    <xsl:for-each select="$MAIN">
      <xsl:choose>
        <xsl:when test="key('LOCAL',$me)">
          <xsl:for-each select="key('LOCAL',$me)">
            <xsl:choose>
              <xsl:when test="@mode='delete'"/>
              <xsl:when test="@mode='change'">
                <xsl:for-each select="$here">
                  <xsl:apply-templates select="." mode="change"/>
                </xsl:for-each>
              </xsl:when>
              <xsl:otherwise>
                <xsl:for-each select="$here">
                  <xsl:apply-templates select="." mode="copy"/>
                </xsl:for-each>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:for-each select="$here">
            <xsl:apply-templates select="." mode="copy"/>
          </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>
  <xsl:template match="@mode"/>
  <xsl:template match="elementSpec|classSpec|macroSpec" mode="add">
    <xsl:copy-of select="."/>
  </xsl:template>
  <xsl:template match="elementSpec|classSpec|macroSpec" mode="change">
    <xsl:variable name="me" select="@ident"/>
    <xsl:copy>
      <xsl:apply-templates select="@*" mode="change"/>
      <xsl:for-each select="$MAIN">
        <xsl:for-each select="key('LOCAL',$me)">
          <xsl:choose>
            <xsl:when test="@mode='delete'"/>
            <xsl:when test="@mode='replace'">
              <xsl:copy-of select="."/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:copy-of select="altIdent"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()|comment()" mode="change"/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="attDef" mode="change">
    <xsl:variable name="me" select="concat(../../@ident,'::',@ident)"/>
    <xsl:copy>
      <xsl:apply-templates select="@*" mode="change"/>
      <xsl:for-each select="$MAIN">
        <xsl:for-each select="key('LOCALATT',$me)">
          <xsl:choose>
            <xsl:when test="@mode='delete'"/>
            <xsl:when test="@mode='replace'">
              <xsl:copy-of select="."/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:copy-of select="altIdent"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
      </xsl:for-each>
      <xsl:apply-templates select="*|eg:*|text()|comment()" mode="change"/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="attList" mode="change">
    <xsl:variable name="me" select="../@ident"/>
    <xsl:copy>
      <xsl:apply-templates select="@*" mode="change"/>
      <xsl:for-each select="$MAIN">
        <xsl:for-each select="key('LOCAL',$me)/attList">
          <xsl:copy-of select="attDef[@mode='add']"/>
        </xsl:for-each>
      </xsl:for-each>
      <xsl:apply-templates select="*|text()|comment()" mode="change"/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="*">
    <xsl:copy>
      <xsl:apply-templates select="@*|*|rng:*|eg:*|text()|comment()"/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="@*|comment()|text()" mode="change">
    <xsl:copy/>
  </xsl:template>
  <xsl:template match="@*|comment()">
    <xsl:copy/>
  </xsl:template>
  <xsl:template match="text()">
    <xsl:copy-of select="."/>
  </xsl:template>
  <xsl:template match="eg:*" mode="change">
    <xsl:copy-of select="."/>
  </xsl:template>
  <xsl:template match="eg:*">
    <xsl:copy-of select="."/>
  </xsl:template>
  <xsl:template match="rng:*">
    <xsl:copy-of select="."/>
  </xsl:template>
  <xsl:template match="rng:*" mode="copy">
    <xsl:copy-of select="."/>
  </xsl:template>
  <xsl:template match="*|rng:*" mode="change">
    <xsl:if test="not(@mode='delete')">
      <xsl:copy>
        <xsl:apply-templates select="@*|*|text()|comment()" mode="change"/>
      </xsl:copy>
    </xsl:if>
  </xsl:template>
  <xsl:template match="*|@*|processing-instruction()|text()" mode="copy">
    <xsl:copy>
      <xsl:apply-templates select="*|@*|processing-instruction()|comment()|text()" mode="copy"/>
    </xsl:copy>
  </xsl:template>
  <xsl:template name="verbatim">
    <xsl:param name="text"/>
    <xsl:param name="startnewline">false</xsl:param>
    <xsl:param name="autowrap">true</xsl:param>
    <div class="pre_eg">
      <xsl:if test="$startnewline='true'">
        <xsl:call-template name="lineBreak"/>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="$autowrap='false'">
          <xsl:value-of select="."/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="lines" select="estr:tokenize($text,'&#10;')"/>
          <xsl:apply-templates select="$lines[1]" mode="normalline"/>
        </xsl:otherwise>
      </xsl:choose>
    </div>
  </xsl:template>
</xsl:stylesheet>
