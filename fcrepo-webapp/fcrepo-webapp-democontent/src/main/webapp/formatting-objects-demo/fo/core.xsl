<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xd="http://www.pnp-software.com/XSLTdoc"
		xmlns:fotex="http://www.tug.org/fotex"
		xmlns:a="http://relaxng.org/ns/compatibility/annotations/1.0"
		xmlns="http://www.w3.org/1999/XSL/Format"
		xmlns:fo="http://www.w3.org/1999/XSL/Format"
		xmlns:rng="http://relaxng.org/ns/structure/1.0"
		xmlns:tei="http://www.tei-c.org/ns/1.0"
		xmlns:teix="http://www.tei-c.org/ns/Examples"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		exclude-result-prefixes="xd a fotex fo rng tei teix" 
		version="1.0">
  <xd:doc type="stylesheet">
    <xd:short>
    TEI stylesheet
    dealing  with elements from the
      core module, making XSL-FO output.
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
    <xd:cvsId>$Id: core.xsl 6657 2009-07-08 13:33:54Z rahtz $</xd:cvsId>
    <xd:copyright>2008, TEI Consortium</xd:copyright>
  </xd:doc>
  <xd:doc>
    <xd:short>Process elements  processing-instruction()[name()='xmltex']</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="processing-instruction()[name()='xmltex']">
    <xsl:message>xmltex pi <xsl:value-of select="."/></xsl:message>
    <xsl:copy-of select="."/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  ab</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="ab">
    <block>
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  abbr</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="abbr">
    <xsl:apply-templates/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  add</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="add">
    <xsl:choose>
      <xsl:when test="@place='sup'">
        <inline vertical-align="super">
          <xsl:apply-templates/>
        </inline>
      </xsl:when>
      <xsl:when test="@place='sub'">
        <inline vertical-align="sub">
          <xsl:apply-templates/>
        </inline>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  byline</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="byline">
    <block text-align="center">
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  cell//lb</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="cell//lb">
    <xsl:choose>
      <xsl:when test="$foEngine='passivetex'"> </xsl:when>
      <xsl:otherwise>
        <inline linefeed-treatment="preserve">
          <xsl:text>&#10;</xsl:text>
        </inline>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  code</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="code">
    <inline font-family="{$typewriterFont}">
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  corr</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="corr">
    <xsl:text>[</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>]</xsl:text>
    <xsl:choose>
      <xsl:when test="@sic">
        <footnote>
          <footnote-citation>
            <inline font-size="8pt" vertical-align="super">
              <xsl:number format="a" level="any" count="corr"/>
            </inline>
          </footnote-citation>
          <list-block>
            <xsl:attribute name="provisional-distance-between-starts">
              <xsl:value-of select="$betweenStarts"/>
            </xsl:attribute>
            <xsl:attribute name="provisional-label-separation">
              <xsl:value-of select="$labelSeparation"/>
            </xsl:attribute>
            <list-item>
              <list-item-label end-indent="label-end()">
                <block>
                  <inline font-size="{$footnoteSize}" vertical-align="super">
                    <xsl:number format="a" level="any" count="corr"/>
                  </inline>
                </block>
              </list-item-label>
              <list-item-body start-indent="body-start()">
                <block font-size="{$footnoteSize}">
                  <xsl:value-of select="@sic"/>
                </block>
              </list-item-body>
            </list-item>
          </list-block>
        </footnote>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  del</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="del">
    <inline text-decoration="line-through">
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  eg</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="eg">
    <block font-family="{$typewriterFont}" background-color="{$exampleBackgroundColor}" color="{$exampleColor}" white-space-treatment="preserve" linefeed-treatment="preserve" white-space-collapse="false" wrap-option="no-wrap" text-indent="0em" hyphenate="false" start-indent="{$exampleMargin}" text-align="start" font-size="{$exampleSize}" space-before.optimum="4pt" space-after.optimum="4pt">
      <xsl:if test="not($flowMarginLeft='')">
        <xsl:attribute name="padding-start">
          <xsl:value-of select="$exampleMargin"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="parent::exemplum">
        <xsl:text>&#10;</xsl:text>
      </xsl:if>
      <xsl:value-of select="translate(.,' ',' ')"/>
    </block>
  </xsl:template>

  <xd:doc>
    <xd:short>Process elements  eg</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="teix:egXML">
    <block font-family="{$typewriterFont}" background-color="{$exampleBackgroundColor}" color="{$exampleColor}" white-space-treatment="preserve" linefeed-treatment="preserve" white-space-collapse="false" wrap-option="no-wrap" text-indent="0em" hyphenate="false" start-indent="{$exampleMargin}" text-align="start" font-size="{$exampleSize}" space-before.optimum="4pt" space-after.optimum="4pt">
      <xsl:if test="not($flowMarginLeft='')">
        <xsl:attribute name="padding-start">
          <xsl:value-of select="$exampleMargin"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates mode="verbatim"/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  eg[@rend='kwic']/lb</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="eg[@rend='kwic']/lb"/>
  <xd:doc>
    <xd:short>Process elements  emph</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="emph">
    <inline font-style="italic">
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  epigraph</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="epigraph">
    <block text-align="center" space-before.optimum="4pt" space-after.optimum="4pt" start-indent="{$exampleMargin}">
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  epigraph/lg</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="epigraph/lg">
    <block text-align="center" space-before.optimum="4pt" space-after.optimum="4pt">
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  epigraph/q</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="epigraph/q">
    <block space-before.optimum="4pt" space-after.optimum="4pt" start-indent="{$exampleMargin}">
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  foreign</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="foreign">
    <inline font-style="italic">
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  gap</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="gap">
    <inline border-style="solid">
      <xsl:text>[</xsl:text>
      <xsl:value-of select="@reason"/>
      <xsl:text>]</xsl:text>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  gi</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="gi">
    <inline hyphenate="false" color="{$giColor}" font-family="{$typewriterFont}">
      <xsl:text>&lt;</xsl:text>
      <xsl:apply-templates/>
      <xsl:text>&gt;</xsl:text>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  att</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="att">
    <inline hyphenate="false" color="{$giColor}" font-family="{$typewriterFont}" font-weight="bold">
      <xsl:text>@</xsl:text>
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  gloss</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="gloss">
      <xsl:apply-templates/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  hi</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="hi">
    <inline>
      <xsl:call-template name="rend">
        <xsl:with-param name="defaultvalue" select="string('bold')"/>
        <xsl:with-param name="defaultstyle" select="string('font-weight')"/>
        <xsl:with-param name="rend" select="@rend"/>
      </xsl:call-template>
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  ident</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="ident">
    <inline color="{$identColor}" font-family="{$sansFont}">
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  index</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="index">
    <xsl:apply-templates/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  interp</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="interp">
    <xsl:apply-templates/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  interpGrp</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="interpGrp">
    <xsl:apply-templates/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  item</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="item" mode="catalogue">
    <table-cell>
      <block>
        <xsl:choose>
          <xsl:when test="label">
            <inline font-weight="bold">
              <xsl:apply-templates select="label" mode="print"/>
            </inline>
          </xsl:when>
          <xsl:otherwise>
            <inline font-weight="bold">
              <xsl:apply-templates mode="print" select="preceding-sibling::*[1]"/>
            </inline>
          </xsl:otherwise>
        </xsl:choose>
      </block>
    </table-cell>
    <table-cell>
      <block>
        <xsl:apply-templates/>
      </block>
    </table-cell>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  item</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="item|biblStruct">
    <xsl:call-template name="makeItem"/>
  </xsl:template>
  <xsl:template match="item" mode="xref">
    <xsl:variable name="listdepth" select="count(ancestor::list)"/>
    <xsl:if test="parent::list[@type='bibliography']">
      <xsl:text> [</xsl:text>
    </xsl:if>
    <xsl:variable name="listNFormat">
      <xsl:choose>
        <xsl:when test="$listdepth=1">
          <xsl:text>1</xsl:text>
        </xsl:when>
        <xsl:when test="$listdepth=2">
          <xsl:text>i</xsl:text>
        </xsl:when>
        <xsl:when test="$listdepth=3">
          <xsl:text>a</xsl:text>
        </xsl:when>
        <xsl:when test="$listdepth=4">
          <xsl:text>I</xsl:text>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>
    <xsl:number format="{$listNFormat}"/>
    <xsl:if test="parent::list[@type='bibliography']">
      <xsl:text>]</xsl:text>
    </xsl:if>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  kw</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="kw">
    <inline font-style="italic">
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  l</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="l">
    <block space-before.optimum="0pt" space-after.optimum="0pt">
      <xsl:choose>
        <xsl:when test="starts-with(@rend,'indent(')">
          <xsl:attribute name="text-indent">
            <xsl:value-of select="concat(substring-before(substring-after(@rend,'('),')'),'em')"/>
          </xsl:attribute>
        </xsl:when>
        <xsl:when test="starts-with(@rend,'indent')">
          <xsl:attribute name="text-indent">1em</xsl:attribute>
        </xsl:when>
      </xsl:choose>
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  label</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="label" mode="print">
    <xsl:apply-templates/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  label</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="label"/>
  <xd:doc>
    <xd:short>Process elements  lb</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="lb">
    <xsl:choose>
      <xsl:when test="$activeLinebreaks='true'">
        <xsl:choose>
<!-- this is a *visible* linebreak character 
	       PassiveTeX implements it as a real line break
	  -->
          <xsl:when test="$foEngine='passivetex'"> </xsl:when>
	  <xsl:when test="parent::list">
	    <list-item>
	      <list-item-label>
		<block/>
	      </list-item-label>
	      <list-item-body>
		<block/>
	      </list-item-body>
	    </list-item>
	  </xsl:when>
          <xsl:otherwise>
	    <block/>
	    <!--
	    <inline  linefeed-treatment="preserve"
			white-space-treatment="preserve" 
			white-space-collapse="false">
	      <xsl:text>&#xA;</xsl:text>
	    </inline>
	    -->
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <inline font-size="8pt">
          <xsl:text>❡</xsl:text>
        </inline>
      </xsl:otherwise>
    </xsl:choose>
<!-- JT's suggestion:
<inline
 xml:space="preserve"
 white-space-collapse="false">&#xA;</inline>
-->
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  list</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="list|listBibl">
    <xsl:if test="child::head">
      <block font-style="italic" text-align="start" space-before.optimum="4pt">
        <xsl:for-each select="head">
          <xsl:apply-templates/>
        </xsl:for-each>
      </block>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="@type='runin'">
        <block>
          <xsl:apply-templates mode="runin"/>
        </block>
      </xsl:when>
      <xsl:otherwise>
        <list-block>
          <xsl:call-template name="setListIndents"/>
          <xsl:apply-templates/>
        </list-block>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  listBibl/bibl</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="listBibl/bibl">
    <xsl:call-template name="makeItem"/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  list[@type='catalogue']</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="list[@type='catalogue']">
    <block space-before="{$spaceAroundTable}" space-after="{$spaceAroundTable}">
      <table>
        <table-column column-number="1" column-width="20%">
          <xsl:if test="$foEngine='passivetex'">
            <xsl:attribute name="column-align" namespace="http://www.tug.org/fotex">p</xsl:attribute>
          </xsl:if>
        </table-column>
        <table-column column-number="2" column-width="80%">
        <xsl:if test="$foEngine='passivetex'">
          <xsl:attribute name="column-align" namespace="http://www.tug.org/fotex">p</xsl:attribute>
        </xsl:if>
        </table-column>
        <table-body>
          <xsl:for-each select="item">
            <table-row>
              <xsl:apply-templates select="." mode="catalogue"/>
            </table-row>
          </xsl:for-each>
        </table-body>
      </table>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  lg</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="lg">
    <block start-indent="{$exampleMargin}" text-align="start" space-before.optimum="4pt" space-after.optimum="4pt">
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  mentioned</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="mentioned">
    <inline>
      <xsl:call-template name="rend">
        <xsl:with-param name="defaultvalue" select="string('italic')"/>
        <xsl:with-param name="defaultstyle" select="string('font-style')"/>
      </xsl:call-template>
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  milestone</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="milestone">
    <block>
      <xsl:text>******************</xsl:text>
      <xsl:value-of select="@unit"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="@n"/>
      <xsl:text>******************</xsl:text>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  name</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="name">
    <xsl:apply-templates/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  note (endnote mode)</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="note" mode="endnote">
    <block id="{generate-id()}">
      <xsl:call-template name="calculateEndNoteNumber"/>
      <xsl:text>. </xsl:text>
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  note</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="note">
    <xsl:choose>
      <xsl:when test="ancestor::p or ancestor::item">
        <xsl:apply-templates select="." mode="real"/>
      </xsl:when>
      <xsl:otherwise>
        <block>
          <xsl:apply-templates select="." mode="real"/>
        </block>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  note properly</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="note" mode="real">
    <xsl:choose>
      <xsl:when test="@place='end'">
        <simple-link>
          <xsl:attribute name="internal-destination">
            <xsl:value-of select="generate-id()"/>
          </xsl:attribute>
          <inline font-size="{$footnotenumSize}" vertical-align="super">
            <xsl:choose>
              <xsl:when test="@n">
                <xsl:value-of select="@n"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="calculateEndNoteNumber"/>
              </xsl:otherwise>
            </xsl:choose>
          </inline>
        </simple-link>
      </xsl:when>
      <xsl:when test="@place='inline'">
        <inline>
          <xsl:text> (</xsl:text>
          <xsl:apply-templates/>
          <xsl:text>)</xsl:text>
        </inline>
      </xsl:when>
      <xsl:when test="@place='display'">
        <block text-indent="0pt" end-indent="{$exampleMargin}" start-indent="{$exampleMargin}" font-size="{$exampleSize}" space-before.optimum="{$exampleBefore}" space-after.optimum="{$exampleAfter}">
          <xsl:apply-templates/>
        </block>
      </xsl:when>
      <xsl:when test="@place='divtop'">
        <block text-indent="0pt" end-indent="{$exampleMargin}" start-indent="{$exampleMargin}" font-style="italic" font-size="{$exampleSize}" space-before.optimum="{$exampleBefore}" space-after.optimum="{$exampleAfter}">
          <xsl:apply-templates/>
        </block>
      </xsl:when>
      <xsl:otherwise>
	<xsl:choose>
	  <xsl:when test="parent::item">
	    <block>
	      <xsl:call-template name="makeFootnote"/>
	    </block>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:call-template name="makeFootnote"/>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xd:doc>
    <xd:short>Create a footnote</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
    
  <xsl:template name="makeFootnote">
        <xsl:variable name="FootID">
          <xsl:choose>
            <xsl:when test="@n">
              <xsl:value-of select="@n"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="calculateFootnoteNumber"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <footnote>
          <inline>
            <xsl:if test="not(@target)">
              <xsl:attribute name="font-size">
                <xsl:value-of select="$footnotenumSize"/>
              </xsl:attribute>
              <xsl:attribute name="vertical-align">super</xsl:attribute>
              <xsl:value-of select="$FootID"/>
            </xsl:if>
          </inline>
          <footnote-body>
            <block end-indent="0pt" start-indent="0pt" text-align="start" font-style="normal" text-indent="{$parIndent}" font-size="{$footnoteSize}">
              <xsl:if test="@id">
                <xsl:attribute name="id">
                  <xsl:value-of select="@id"/>
                </xsl:attribute>
              </xsl:if>
              <xsl:if test="not(@target)">
                <inline font-size="{$footnotenumSize}" vertical-align="super">
                  <xsl:value-of select="$FootID"/>
                </inline>
                <xsl:text> </xsl:text>
              </xsl:if>
              <xsl:apply-templates/>
            </block>
          </footnote-body>
        </footnote>
</xsl:template>


  <xd:doc>
    <xd:short>Process element  p</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>

  <xsl:template match="p">
    <block>
      <xsl:if test="preceding-sibling::p">
        <xsl:attribute name="text-indent">
          <xsl:value-of select="$parIndent"/>
        </xsl:attribute>
        <xsl:attribute name="space-before.optimum">
          <xsl:value-of select="$parSkip"/>
        </xsl:attribute>
        <xsl:attribute name="space-before.maximum">
          <xsl:value-of select="$parSkipmax"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@xml:lang">
        <xsl:attribute name="country">
          <xsl:value-of select="substring-before(@xml:lang,'-')"/>
        </xsl:attribute>
        <xsl:attribute name="language">
          <xsl:value-of select="substring-after(@xml:lang,'-')"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  pb</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="pb">
    <xsl:variable name="e">
      <xsl:choose>
	<xsl:when test="parent::body or parent::front or
			parent::back or parent::div">
	  <xsl:text>block</xsl:text>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:text>inline</xsl:text>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="parent::list"/>
      <xsl:when test="$pagebreakStyle='active'">
	<xsl:element name="{$e}" xmlns="http://www.w3.org/1999/XSL/Format">
	  <xsl:attribute name="break-before">page</xsl:attribute>
	  <xsl:if test="@id">
	    <xsl:attribute name="id">
	      <xsl:value-of select="@id"/>
	    </xsl:attribute>
	  </xsl:if>
	</xsl:element>
      </xsl:when>
      <xsl:when test="$pagebreakStyle='visible'">
	<xsl:element name="{$e}" xmlns="http://www.w3.org/1999/XSL/Format">
	  <xsl:if test="@id">
	    <xsl:attribute name="id">
	      <xsl:value-of select="@id"/>
	    </xsl:attribute>
	  </xsl:if>
	  <xsl:text>✁[</xsl:text>
	  <xsl:value-of select="@unit"/>
	  <xsl:text> Page </xsl:text>
	  <xsl:value-of select="@n"/>
	  <xsl:text>]✁</xsl:text>
	</xsl:element>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xd:doc>
    <xd:short>Process elements  quote</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="quote">
    <block text-align="start" text-indent="0pt" end-indent="{$exampleMargin}" start-indent="{$exampleMargin}" font-size="{$exampleSize}" space-before.optimum="{$exampleBefore}" space-after.optimum="{$exampleAfter}">
      <xsl:apply-templates/>
    </block>
  </xsl:template>


  <xd:doc>
    <xd:short>Process elements  q</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="q">
    <xsl:choose>
      <xsl:when test="text">
        <xsl:apply-templates/>
      </xsl:when>
      <xsl:when test="@rend='display' or p or lg">
        <block text-align="start" text-indent="0pt" end-indent="{$exampleMargin}" start-indent="{$exampleMargin}" font-size="{$exampleSize}" space-before.optimum="{$exampleBefore}" space-after.optimum="{$exampleAfter}">
          <xsl:apply-templates/>
        </block>
      </xsl:when>
      <xsl:when test="@rend='eg'">
        <block text-align="start" font-size="{$exampleSize}" space-before.optimum="4pt" text-indent="0pt" space-after.optimum="4pt" start-indent="{$exampleMargin}" font-family="{$typewriterFont}">
          <xsl:apply-templates/>
        </block>
      </xsl:when>
      <xsl:when test="@rend = 'qwic'">
        <block space-before="{$spaceAroundTable}" space-after="{$spaceAroundTable}">
          <inline-container>
            <table font-size="{$exampleSize}" font-family="{$typewriterFont}" start-indent="{$exampleMargin}">
              <table-column column-number="1" column-width="">
                <xsl:if test="$foEngine='passivetex'">
                  <xsl:attribute name="column-align" namespace="http://www.tug.org/fotex">p</xsl:attribute>
                </xsl:if>
              </table-column>
              <table-column column-number="2" column-width="">
                <xsl:if test="$foEngine='passivetex'">
                  <xsl:attribute name="column-align" namespace="http://www.tug.org/fotex">l</xsl:attribute>
                </xsl:if>
              </table-column>
              <table-body>
                <xsl:for-each select="q">
                  <xsl:for-each select="term">
                    <table-row>
                      <table-cell>
                        <block>
                          <xsl:apply-templates select="preceding-sibling::node()"/>
                        </block>
                      </table-cell>
                      <table-cell>
                        <block>
                          <xsl:apply-templates/>
                          <xsl:apply-templates select="following-sibling::node()"/>
                        </block>
                      </table-cell>
                    </table-row>
                  </xsl:for-each>
                </xsl:for-each>
              </table-body>
            </table>
          </inline-container>
        </block>
      </xsl:when>
      <xsl:when test="starts-with(@rend,'kwic')">
        <block space-before="{$spaceAroundTable}" space-after="{$spaceAroundTable}">
          <inline-container>
            <table font-size="{$exampleSize}" start-indent="{$exampleMargin}" font-family="{$typewriterFont}">
              <table-column column-number="1" column-width="">
                <xsl:if test="$foEngine='passivetex'">
                  <xsl:attribute name="column-align" namespace="http://www.tug.org/fotex">r</xsl:attribute>
                </xsl:if>
              </table-column>
              <table-column column-number="2" column-width="">
                <xsl:if test="$foEngine='passivetex'">
                  <xsl:attribute name="column-align" namespace="http://www.tug.org/fotex">l</xsl:attribute>
                </xsl:if>
              </table-column>
              <table-body>
                <xsl:for-each select="term">
                  <table-row>
                    <table-cell>
                      <block>
                        <xsl:value-of select="preceding-sibling::node()[1]"/>
                      </block>
                    </table-cell>
                    <table-cell>
                      <block>
                        <xsl:apply-templates/>
                        <xsl:value-of select="following-sibling::node()[1]"/>
                      </block>
                    </table-cell>
                  </table-row>
                </xsl:for-each>
              </table-body>
            </table>
          </inline-container>
        </block>
      </xsl:when>
      <xsl:when test="@rend='literal'">
        <block white-space-collapse="false" wrap-option="no-wrap" font-size="{$exampleSize}" space-before.optimum="4pt" space-after.optimum="4pt" start-indent="{$exampleMargin}" font-family="{$typewriterFont}">
          <xsl:apply-templates/>
        </block>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>“</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>”</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  reg</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="reg">
    <inline font-family="{$sansFont}">
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  rs</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="rs">
    <xsl:apply-templates/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  s</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="s">
    <xsl:apply-templates/>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  salute</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="salute">
    <block text-align="left">
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  seg</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="seg">
    <block font-family="{$typewriterFont}" background-color="yellow" white-space-collapse="false" wrap-option="no-wrap" text-indent="0em" start-indent="{$exampleMargin}" text-align="start" font-size="{$exampleSize}" padding-before="8pt" padding-after="8pt" space-before.optimum="4pt" space-after.optimum="4pt">
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  sic</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="sic">
    <xsl:apply-templates/>
    <xsl:text> (sic)</xsl:text>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  signed</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="signed">
    <block text-align="left">
      <xsl:apply-templates/>
    </block>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  term</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="term">
    <inline font-style="italic">
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>Process elements  unclear</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="unclear">
    <inline text-decoration="blink">
      <xsl:apply-templates/>
    </inline>
  </xsl:template>
  <xd:doc>
    <xd:short>[fo] </xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template name="addID">
    <xsl:attribute name="id">
      <xsl:choose>
        <xsl:when test="@id">
          <xsl:value-of select="@id"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="generate-id()"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>
  <xd:doc>
    <xd:short>[fo] process "rend" attribute</xd:short>
    <xd:param name="value">value of "rend" attribute</xd:param>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template name="applyRend">
    <xsl:param name="value"/>
    <xsl:choose>
      <xsl:when test="$value='gothic'">
        <xsl:attribute name="font-family">fantasy</xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='calligraphic'">
        <xsl:attribute name="font-family">cursive</xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='ital' or $value='italic' or $value='it' or $value='i' or $value='italics'">
        <xsl:attribute name="font-style">italic</xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='sc'">
        <xsl:attribute name="font-variant">small-caps</xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='code'">
        <xsl:attribute name="font-family">
          <xsl:value-of select="$typewriterFont"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='bo' or $value='bold'">
        <xsl:attribute name="font-weight">bold</xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='BO'">
        <xsl:attribute name="font-style">italic</xsl:attribute>
        <xsl:attribute name="text-decoration">underline</xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='UL' or $value='ul'">
        <xsl:attribute name="text-decoration">underline</xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='sub'">
        <xsl:attribute name="vertical-align">sub</xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='small'">
        <xsl:attribute name="font-size">small</xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='strike'">
        <xsl:attribute name="text-decoration">line-through</xsl:attribute>
      </xsl:when>
      <xsl:when test="$value='sup'">
        <xsl:attribute name="vertical-align">super</xsl:attribute>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  <xd:doc>
    <xd:short>[fo] </xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template name="calculateEndNoteNumber">
    <xsl:number level="any" format="i" count="note[@place='end']"/>
  </xsl:template>
  <xd:doc>
    <xd:short>[fo] </xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template name="calculateFootnoteNumber">
    <xsl:number from="text" level="any" count="note"/>
  </xsl:template>
  <xd:doc>
    <xd:short>[fo] </xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template name="makeItem">
<!-- item behaviour depends on the type attribute of our parent:
simple, bullets, ordered, gloss, unordered, or bibliography
-->
    <xsl:variable name="listdepth" select="count(ancestor::list)"/>
    <list-item>
      <xsl:if test="not(parent::note[@place='foot' or @place='bottom' ])">
        <xsl:attribute name="space-before.optimum">
          <xsl:value-of select="$listItemsep"/>
        </xsl:attribute>
      </xsl:if>
      <list-item-label end-indent="label-end()">
        <xsl:if test="@id">
          <xsl:attribute name="id">
            <xsl:value-of select="@id"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:text>&#10;</xsl:text>
        <block>
          <xsl:choose>
            <xsl:when test="@n">
              <xsl:attribute name="text-align">end</xsl:attribute>
              <xsl:value-of select="@n"/>
            </xsl:when>
            <xsl:when test="../@type='bibliography'">
              <xsl:attribute name="text-align">end</xsl:attribute>
              <xsl:apply-templates mode="xref" select="."/>
            </xsl:when>
            <xsl:when test="../@type='ordered' or self::bibl">
              <xsl:attribute name="text-align">end</xsl:attribute>
              <xsl:apply-templates mode="xref" select="."/>
              <xsl:text>.</xsl:text>
            </xsl:when>
            <xsl:when test="../@type='gloss' or self::biblStruct">
              <xsl:attribute name="text-align">start</xsl:attribute>
              <xsl:attribute name="font-weight">bold</xsl:attribute>
              <xsl:choose>
		<xsl:when test="self::biblStruct">
                  <xsl:apply-templates mode="xref" select="."/>
		</xsl:when>
                <xsl:when test="label">
                  <xsl:apply-templates mode="print" select="label"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:apply-templates mode="print" select="preceding-sibling::*[1]"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:when>
            <xsl:when test="../@type='numbered'">
<!-- numbered support added rbl 26.3.2005 -->
              <xsl:attribute name="text-align">end</xsl:attribute>
              <xsl:number/>
              <xsl:text>.</xsl:text>
            </xsl:when>
            <xsl:when test="../@type='ordered'">
<!-- numbered support added rbl 26.3.2005 -->
              <xsl:attribute name="text-align">end</xsl:attribute>
              <xsl:number/>
              <xsl:text>.</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="text-align">end</xsl:attribute>
              <xsl:choose>
                <xsl:when test="$listdepth=0">
                  <xsl:value-of select="$bulletOne"/>
                </xsl:when>
                <xsl:when test="$listdepth=1">
                  <xsl:value-of select="$bulletOne"/>
                </xsl:when>
                <xsl:when test="$listdepth=2">
                  <xsl:value-of select="$bulletTwo"/>
                </xsl:when>
                <xsl:when test="$listdepth=3">
                  <xsl:value-of select="$bulletThree"/>
                </xsl:when>
                <xsl:when test="$listdepth=4">
                  <xsl:value-of select="$bulletFour"/>
                </xsl:when>
              </xsl:choose>
            </xsl:otherwise>
          </xsl:choose>
        </block>
      </list-item-label>
      <list-item-body start-indent="body-start()">
	<xsl:choose>
	  <xsl:when test="*">
	    <xsl:for-each select="*">
	      <xsl:choose>
		<xsl:when test="self::list">
		  <xsl:apply-templates select="."/>
		</xsl:when>
		<xsl:otherwise>
		  <block font-weight="normal">
		    <xsl:apply-templates/>
		  </block>
		</xsl:otherwise>
	      </xsl:choose>
	    </xsl:for-each>
	  </xsl:when>
        <xsl:otherwise>
          <block font-weight="normal">
            <xsl:apply-templates/>
          </block>
        </xsl:otherwise>
      </xsl:choose>
      </list-item-body>
    </list-item>
  </xsl:template>
  <xd:doc>
    <xd:short>[fo] </xd:short>
    <xd:param name="defaultvalue">defaultvalue</xd:param>
    <xd:param name="defaultstyle">defaultstyle</xd:param>
    <xd:param name="rend">rend</xd:param>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template name="rend">
    <xsl:param name="defaultvalue"/>
    <xsl:param name="defaultstyle"/>
    <xsl:param name="rend"/>
    <xsl:choose>
      <xsl:when test="$rend=''">
        <xsl:attribute name="{$defaultstyle}">
          <xsl:value-of select="$defaultvalue"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:when test="contains($rend,';')">
        <xsl:call-template name="applyRend">
          <xsl:with-param name="value" select="substring-before($rend,';')"/>
        </xsl:call-template>
        <xsl:call-template name="rend">
          <xsl:with-param name="rend" select="substring-after($rend,';')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="applyRend">
          <xsl:with-param name="value" select="$rend"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xd:doc>
    <xd:short>[fo] Spacing setup for list blocks</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template name="setListIndents">
    <xsl:attribute name="provisional-distance-between-starts">
      <xsl:choose>
        <xsl:when test="self::listBibl[biblStruct]">
          <xsl:value-of select="$betweenBiblStarts"/>
        </xsl:when>
        <xsl:when test="@type='gloss'">
          <xsl:value-of select="$betweenGlossStarts"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$betweenStarts"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    <xsl:attribute name="provisional-label-separation">
      <xsl:value-of select="$labelSeparation"/>
    </xsl:attribute>
    <xsl:attribute name="text-indent">0em</xsl:attribute>
    <xsl:attribute name="margin-right">
      <xsl:value-of select="$listRightMargin"/>
    </xsl:attribute>
    <xsl:variable name="listdepth" select="count(ancestor::list)"/>
    <xsl:choose>
      <xsl:when test="$listdepth=0">
        <xsl:attribute name="space-before">
          <xsl:value-of select="$listAbove-1"/>
        </xsl:attribute>
        <xsl:attribute name="space-after">
          <xsl:value-of select="$listBelow-1"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:when test="$listdepth=1">
        <xsl:attribute name="space-before">
          <xsl:value-of select="$listAbove-2"/>
        </xsl:attribute>
        <xsl:attribute name="space-after">
          <xsl:value-of select="$listBelow-2"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:when test="$listdepth=2">
        <xsl:attribute name="space-before">
          <xsl:value-of select="$listAbove-3"/>
        </xsl:attribute>
        <xsl:attribute name="space-after">
          <xsl:value-of select="$listBelow-3"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:when test="$listdepth=3">
        <xsl:attribute name="space-before">
          <xsl:value-of select="$listAbove-4"/>
        </xsl:attribute>
        <xsl:attribute name="space-after">
          <xsl:value-of select="$listBelow-4"/>
        </xsl:attribute>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xd:doc>
    <xd:short>Process elements soCalled</xd:short>
    <xd:detail> </xd:detail>
  </xd:doc>
  <xsl:template match="soCalled">
    <xsl:value-of select="$preQuote"/>
    <xsl:apply-templates/>
    <xsl:value-of select="$postQuote"/>
  </xsl:template>

<xsl:template name="emphasize">
  <xsl:param name="class"/>
  <xsl:param name="content"/>
  <xsl:choose>
    <xsl:when test="$class='titlem'">
      <inline>
	<xsl:attribute name="font-style">italic</xsl:attribute>
	<xsl:copy-of select="$content"/>
      </inline>
    </xsl:when>
    <xsl:when test="$class='titlea'">
      <xsl:text>‘</xsl:text>
	<xsl:copy-of select="$content"/>
      <xsl:text>’</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$content"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

  <xsl:template name="applyRendition"/>

</xsl:stylesheet>
