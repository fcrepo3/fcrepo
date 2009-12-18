<?xml version="1.0" encoding="UTF-8"?>
<?xmlspysamplexml C:\mellon\src\xsl\access\getItemList.xml?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:param name="fedora"/>
  <xsl:output method="html" indent="yes"/>
  <xsl:template match="pidList">
    <html>
      <head>
        <title>Repository Information HTML Presentation</title>
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
                  <h2>Fedora</h2>
                  <h3>Get Next PIDs View</h3>
                </center>
              </td>
            </tr>
          </table>
          <hr/>
          <font size="+1">
            <strong>Generated PID List for next: </strong>
            <xsl:value-of select="count(//pid)"/>
            PID(s)
          </font>
          <hr/>
          <p/>
          <table width="784" border="1" cellpadding="5" cellspacing="5" bgcolor="silver">
            <tr>
              <td align="center">
                <strong>PID</strong>
              </td>
            </tr>
            <xsl:for-each select="//pid">
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
