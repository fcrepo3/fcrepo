<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" indent="yes"/>
<xsl:param name="title_" >Fedora Digital Object</xsl:param>
<xsl:param name="subtitle_">List Datastreams</xsl:param>
<xsl:param name="soapClientServletPath_">/soapclient/apia</xsl:param>
<xsl:param name="soapClientMethodParmResolverServletPath_">/soapclient/getAccessParmResolver</xsl:param>
  <xsl:template match="objectDatastreams">
    <html>
      <head>
      <title><xsl:value-of select="$title_"/>&#160;-&#160;<xsl:value-of select="$subtitle_"/></title>
      </head>
      <body>
        <center>
          <table width="784" border="0" cellpadding="0" cellspacing="0">
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
          <font size="+1" color="blue">Object Identifier (PID):       </font>
          <font size="+1">
            <xsl:value-of select="@PID"/>
          </font>
          <p/>
          <xsl:choose>
            <xsl:when test="@asOfDateTime">
              <font size="+1" color="blue">Version Date:   </font>
              <font size="+1"><xsl:value-of select="@asOfDateTime"/></font>
            </xsl:when>
            <xsl:otherwise>
              <font size="+1" color="blue">Version Date:   </font>
              <font size="+1">current</font>  
            </xsl:otherwise>
          </xsl:choose>    
          <hr/>      
          <table width="784" border="1" cellpadding="5" cellspacing="5" bgcolor="#F7DBB3">
            <tr>
              <td>
                <b>
                  <font size="+2">Datastream ID</font>
                </b>
              </td>
              <td>
                <b>
                  <font size="+2">Datastream Label</font>
                </b>
              </td>
              <td>
                <b>
                  <font size="+2">MIME Type</font>
                </b>
              </td>
            </tr>
            <xsl:for-each select="//datastream">
              <tr>
                <td>
                  <xsl:value-of select="@dsid"/>
                </td>
                <td>
                  <xsl:choose>
                    <xsl:when test="../@asOfDateTime">
                      <xsl:variable name="datastream-url">
                        <xsl:value-of select="../@baseURL"/><xsl:text>get/</xsl:text><xsl:value-of select="../@pid"/><xsl:text>/</xsl:text><xsl:value-of select="@dsid"/><xsl:text>/</xsl:text><xsl:value-of select="../@asOfDateTime"/>
                      </xsl:variable>
                      <a href="{$datastream-url}">
                        <xsl:value-of select="@label"/>
                      </a>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:variable name="datastream-url">
                      <xsl:value-of select="../@baseURL"/><xsl:text>get/</xsl:text><xsl:value-of select="../@pid"/><xsl:text>/</xsl:text><xsl:value-of select="@dsid"/>
                    </xsl:variable>
                    <a href="{$datastream-url}">
                      <xsl:value-of select="@label"/>
                    </a>                  
                  </xsl:otherwise>
                  </xsl:choose>                  
                </td>
                <td>
                  <xsl:value-of select="@mimeType"/>
                </td>
              </tr>
            </xsl:for-each>
          </table>
        </center>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
