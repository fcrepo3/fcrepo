<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
  xmlns:access="http://www.fedora.info/definitions/1/0/access/"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="access">
  <xsl:param name="fedora"/>    
  <xsl:output method="html" indent="yes"/>
  <xsl:template match="access:fedoraObjectHistory">
    <html>
      <head>
        <title>Object History HTML Presentation</title>
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
                  <h3>Object History View</h3>
                </center>
              </td>
            </tr>
          </table>
          <hr/>
          <font size="+1">
            <strong>Object PID: </strong>
            <xsl:value-of select="@pid"/>
          </font>
          <hr/>
          <p/>
          <table width="784" border="1" cellpadding="5" cellspacing="5" bgcolor="silver">
            <tr>
              <td>
                <strong>Object Component Creation/Modification Dates</strong>
              </td>
            </tr>
            <xsl:for-each select="access:objectChangeDate">
            <tr>
              <td align="left">
                <xsl:value-of select="."/>
              </td>
            </tr>
            </xsl:for-each>  
          </table>
        </center>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
