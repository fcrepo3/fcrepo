<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
  xmlns:access="http://www.fedora.info/definitions/1/0/access/"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="access">
<xsl:param name="fedora"/>
<xsl:output method="html" indent="yes"/> 
<xsl:template match="access:objectMethods">
  <html>
    <head>
      <title>Object Methods HTML Presentation</title>
    </head>
    <body>
      <center>
      <table border="0" cellpadding="0" cellspacing="0">
        <tr>
          <td width="141" height="134" valign="top">
            <img src="/{$fedora}/images/newlogo2.jpg" width="141" height="134"/>
          </td>
          <td width="643" valign="top">
            <center>
              <h2>Fedora Digital Object</h2>
              <h3>List Methods</h3>
            </center>
          </td>
        </tr>
      </table>      
      <hr/>
      <font size="+1">
        <strong>Object Identifier (PID): </strong>
        <xsl:value-of select="@pid"/>
      </font>
      <p/>
      <xsl:if test="@sDef">
        <font size="+1">
          <strong>Service Definition: </strong>
          <xsl:value-of select="@sDef"/>
        </font>
        <p/>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="@asOfDateTime">
          <font size="+1">
            <strong>Version Date: </strong>
            <xsl:value-of select="@asOfDateTime"/>
          </font>
        </xsl:when>
        <xsl:otherwise>
          <font size="+1">
            <strong>Version Date: </strong>
            current
          </font>  
        </xsl:otherwise>
      </xsl:choose>
      <hr/>
      <table border="1" cellpadding="5" bgcolor="silver">
        <tr>
          <td><b><font size='+2'>Service Definition</font></b></td>
          <td><b><font size='+2'>Method Name</font></b></td>
          <td>&#x00A0;</td>
          <td><b><font size='+2'>Parm Name</font></b></td>
          <td colspan="100%"><b><font size='+1'>Parm Values<br>(Enter A value for each parm)</br></font></b></td>
        </tr>
        <xsl:apply-templates/>
      </table>    
      </center>
    </body>
  </html>
</xsl:template>

<xsl:template match="access:sdef">
<xsl:apply-templates/>
</xsl:template>

<xsl:template match="access:method">
<form>
  <xsl:attribute name="method">get</xsl:attribute>
  <xsl:attribute name="action">
    <xsl:text>/</xsl:text>
    <xsl:value-of select="$fedora"/>
    <xsl:text>/objects/</xsl:text>
    <xsl:value-of select="replace(../../@pid, '%', '%25')"/>
    <xsl:text>/methods/</xsl:text>
    <xsl:value-of select="replace(../@pid, '%', '%25')"/>
    <xsl:text>/</xsl:text>
    <xsl:value-of select="@name"/>
  </xsl:attribute>
  <tr>
    <td><strong><xsl:value-of select="../@pid"/></strong></td>
    <td><font size="+1"><xsl:value-of select="@name"/></font></td>
    <td>
      <xsl:if test="../../@asOfDateTime">
        <input>
          <xsl:attribute name="type">hidden</xsl:attribute>
          <xsl:attribute name="name">asOfDateTime</xsl:attribute>
          <xsl:attribute name="value"><xsl:value-of select="../../@asOfDateTime"/></xsl:attribute>      
        </input>
      </xsl:if>
      <input type="submit" value="Run"/>
    </td>
    <xsl:choose>
      <xsl:when test="./access:methodParm/@parmName">
        <xsl:call-template name="parmTemplate"/>
      </xsl:when>
      <xsl:otherwise>
        <td colspan="100%">
          <font color="purple">No Parameters</font>
        </td>  
      </xsl:otherwise>
    </xsl:choose>
  </tr>
  </form>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template   name="parmTemplate" >
<xsl:for-each select="access:methodParm">
  <xsl:choose>
    <xsl:when test="position()=1">
      <xsl:choose>
        <xsl:when test="access:methodParmDomain">
          <td><b><font color="purple">
            <xsl:value-of select="@parmName"/>
            </font></b>
          </td>
          <xsl:call-template name="valueTemplate"/>
        </xsl:when>
        <xsl:otherwise>
          <td><b><font color="purple">
            <xsl:value-of select="@parmName"/>
            </font></b>
          </td>
          <xsl:call-template name="noValuesTemplate"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="access:methodParmDomain">
          <tr>
            <td colspan="3" rowspan="1"></td>
            <td>
              <b><font color="purple">
              <xsl:value-of select="@parmName"/>
              </font></b>
            </td>
            <xsl:call-template name="valueTemplate"/>
          </tr>
        </xsl:when>
        <xsl:otherwise>
          <tr>
            <td colspan="3" rowspan="1"></td>
            <td>
              <b><font color="purple">
              <xsl:value-of select="@parmName"/>
              </font></b>
            </td>
            <xsl:call-template name="noValuesTemplate"/>
          </tr>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:for-each>
<xsl:apply-templates/>
</xsl:template>

<xsl:template match="access:methodParmValue">
</xsl:template>

<xsl:template name="valueTemplate">
<xsl:for-each select="access:methodParmDomain/access:methodParmValue">
  <td><xsl:value-of select="."/></td>
  <td>
    <input>
      <xsl:attribute name="type">radio</xsl:attribute>
      <xsl:attribute name="name"><xsl:value-of select="../../@parmName"/></xsl:attribute>
      <xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
    </input>
  </td>
</xsl:for-each>
</xsl:template>

<xsl:template name="noValuesTemplate">
  <td>
    <input>
      <xsl:attribute name="type">text</xsl:attribute>
      <xsl:attribute name="size">60</xsl:attribute>
      <xsl:attribute name="maxlength">60</xsl:attribute>
      <xsl:attribute name="name"><xsl:value-of select="@parmName"/></xsl:attribute>
      <xsl:attribute name="value"></xsl:attribute>
    </input>
  </td>
</xsl:template>

</xsl:stylesheet>
