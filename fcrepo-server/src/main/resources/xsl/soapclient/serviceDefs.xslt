<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes"/>
  <xsl:param name="title_" >Fedora Digital Object</xsl:param>
  <xsl:param name="subtitle_">Behavior Definitions View</xsl:param>
  <xsl:param name="soapClientServletPath_">/soapclient/apia</xsl:param>
  <xsl:param name="soapClientMethodParmResolverServletPath_">/soapclient/getAccessParmResolver</xsl:param>
<xsl:template match="behaviorDefs">
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
      <font size="+1"><xsl:element name="a"><xsl:attribute name="href"><xsl:value-of select="$soapClientServletPath_"/>?action_=GetObjectMethods&amp;PID_=<xsl:value-of select="@pid"/></xsl:attribute><xsl:value-of select="@pid"/></xsl:element></font>
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
          <td><b><font size='+2'>Service Definition Object PID</font></b></td>
        </tr>
        <xsl:apply-templates/>
      </table>
      </center>
    </body>
  </html>
</xsl:template>

<xsl:template match="sdef">
  <tr>
    <td><xsl:value-of select="@pid"/></td>
  </tr>
<xsl:apply-templates/>
</xsl:template>
  
</xsl:stylesheet>
