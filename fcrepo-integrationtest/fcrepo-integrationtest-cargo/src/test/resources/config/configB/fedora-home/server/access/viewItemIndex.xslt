<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:param name="fedora"/>
  <xsl:output method="html" indent="yes"/>
  <xsl:template match="objectItemIndex">
    <html>
      <head>
        <title>Object Items HTML Presentation</title>
      </head>
      <body>
        <center>
          <table width="784" border="0" cellpadding="0" cellspacing="0">
            <tr>
              <td width="141" height="134" valign="top">
                <img src="/{$fedora}/images/newlogo2.jpg" width="141" height="134"/>
              </td>
              <td width="643" valign="top">
                <center>
                  <h2>Fedora Digital Object</h2>
                  <h3>Default Disseminator - Item Index View</h3>
                </center>
              </td>
            </tr>
          </table>
          <hr/>
          <font size="+1">
            <strong>Object Identifier (PID): </strong>
            <xsl:value-of select="@PID"/>
          </font>
          <p/>
          <xsl:choose>
            <xsl:when test="@dateTime">
              <font size="+1">
                <strong>Version Date: </strong>
                <xsl:value-of select="@dateTime"/>
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
          <table width="784" border="1" cellpadding="5" cellspacing="5" bgcolor="silver">
            <tr>
              <td>
                <b>
                  <font size="+2">Item ID</font>
                </b>
              </td>
              <td>
                <b>
                  <font size="+2">Item Description</font>
                </b>
              </td>
              <td>
                <b>
                  <font size="+2">MIME Type</font>
                </b>
              </td>
            </tr>
            <xsl:for-each select="//item">
              <tr>
                <td>
                  <xsl:value-of select="itemId"/>
                </td>
                <td>
                  <xsl:variable name="item-url">
                    <xsl:value-of select="itemURL"/>
                  </xsl:variable>
                  <a href="{$item-url}">
                    <xsl:value-of select="itemLabel"/>
                  </a>
                </td>
                <td>
                  <xsl:value-of select="itemMIMEType"/>
                </td>
              </tr>
            </xsl:for-each>
          </table>
        </center>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
