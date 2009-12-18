<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
<xsl:output method="html" indent="yes"/> 
<xsl:param name="title_" >Fedora Digital Object</xsl:param>
<xsl:param name="subtitle_">Dissemination Index View</xsl:param>
<xsl:param name="soapClientServletPath_">/soapclient/apia</xsl:param>
<xsl:param name="soapClientMethodParmResolverServletPath_">/soapclient/getAccessParmResolver</xsl:param>
<xsl:template match="objectMethods">
  <html>
    <head>
      <title><xsl:value-of select="$title_"/>&#160;-&#160;<xsl:value-of select="$subtitle_"/></title>
    </head>
    <body>
      <center>
      <table border="0" cellpadding="0" cellspacing="0">
        <tr>
          <td width="141" height="134" valign="top">
            <img src="images/newlogo2.jpg" width="141" height="134"/>
          </td>
          <td width="643" valign="top">
            <center>
              <xsl:element name="h2"><xsl:value-of select="$title_" /></xsl:element>
              <xsl:element name="h3"><xsl:value-of select="$subtitle_" /></xsl:element>
            </center>
          </td>
        </tr>
      </table>      
      <hr/>
      <font size="+1" color="blue">Object Identifier (PID):   </font>
      <font size="+1"><xsl:value-of select="@pid"/></font>
      <p/>
      <xsl:choose>
        <xsl:when test="@dateTime">
          <font size="+1" color="blue">Version Date:   </font>
          <font size="+1"><xsl:value-of select="@dateTime"/></font>
        </xsl:when>
        <xsl:otherwise>
          <font size="+1" color="blue">Version Date:   </font>
          <font size="+1">current</font>  
        </xsl:otherwise>
      </xsl:choose>
      <hr/>
      <table border="1" cellpadding="5" bgcolor="#F7DBB3">
        <tr>
          <td><b><font size='+2'>Service Definition</font></b></td>
          <td><b><font size='+2'>Method Name</font></b></td>
          <td>&#x00A0;</td>
          <td><b><font size='+2'>Parm Name</font></b></td>
          <td colspan="100%"><b><font size='+1'>Parm Values<br>(Enter A value for each parm)</br>            </font></b></td>
        </tr>
        <xsl:apply-templates/>
      </table>  
      </center>
    </body>
  </html>
</xsl:template>

<xsl:template match="sdef">
<xsl:apply-templates/>
</xsl:template>

<xsl:template match="method">
  <!--<form name="parmResolverForm" method="post" action="/getAccessParmResolver?">-->
  <xsl:element name="form">
  <xsl:attribute name="name">parmResolverForm</xsl:attribute>
  <xsl:attribute name="method">post</xsl:attribute>
  <xsl:attribute name="action"><xsl:value-of select="$soapClientMethodParmResolverServletPath_"/>?</xsl:attribute>
  <tr>
    <td><font color="blue"><xsl:value-of select="../@pid"/></font></td>
    <td><font size="+1"><xsl:value-of select="@name"/></font></td>
    <td>
      <input>
        <xsl:attribute name="type">hidden</xsl:attribute>
        <xsl:attribute name="name">PID</xsl:attribute>
        <xsl:attribute name="value"><xsl:value-of select="../../@pid"/></xsl:attribute>        
      </input>
      <input>
        <xsl:attribute name="type">hidden</xsl:attribute>
        <xsl:attribute name="name">sDefPID</xsl:attribute>
        <xsl:attribute name="value"><xsl:value-of select="../@pid"/></xsl:attribute>      
      </input>
      <input>
        <xsl:attribute name="type">hidden</xsl:attribute>
        <xsl:attribute name="name">methodName</xsl:attribute>
        <xsl:attribute name="value"><xsl:value-of select="@name"/></xsl:attribute>      
      </input>
      <input>
        <xsl:attribute name="type">hidden</xsl:attribute>
        <xsl:attribute name="name">asOfDateTime</xsl:attribute>
        <xsl:attribute name="value"><xsl:value-of select="../../@dateTime"/></xsl:attribute>      
      </input>      
      <input type="submit" name="Submit" value="Run"></input>
    </td>
    <xsl:choose>
      <xsl:when test="./parm/@parmName">
        <xsl:call-template name="parmTemplate"/>
      </xsl:when>
      <xsl:otherwise>
        <td colspan="100%">
          <font color="purple">No Parameters</font>
        </td>  
      </xsl:otherwise>
    </xsl:choose>
  </tr>
  </xsl:element>
<xsl:apply-templates/>
</xsl:template>

<xsl:template   name="parmTemplate" >
<xsl:for-each select="parm">
  <xsl:choose>
    <xsl:when test="position()=1">
      <xsl:choose>
        <xsl:when test="parmDomainValues">
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
        <xsl:when test="parmDomainValues">
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

<xsl:template match="value">
</xsl:template>

<xsl:template name="valueTemplate">
<xsl:for-each select="parmDomainValues/value">
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