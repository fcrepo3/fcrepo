<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<html>
  <head><title>Trippi Server</title></head>
  <body bgcolor="#eeeeff">
  <center>
    <table border="0" cellpadding="5">
      <tr><td valign="top"><font size="+2"><u><b>Hosted Here:</b></u></font></td>
      </tr>
      <xsl:for-each select="/trippi-server/profile"><tr><td valign="top">
      <table border="0" callpadding="5" cellspacing="10" bgcolor="#ddddff">
      <tr>
        <td valign="top">
          <font size="+2">
            <a>
              <xsl:attribute name="href">
                <xsl:value-of select="/trippi-server/@href"/>/<xsl:value-of select="@id"/>
              </xsl:attribute>
              <xsl:value-of select="@id"/>
            </a>
          </font><br/>
          <font size="+1"><xsl:value-of select="@label"/></font><br/>
          <i>Using <xsl:value-of select="@connector"/>.</i>
        </td>
        <td valign="top">
        <u>Configuration</u>
          <table border="0" cellpadding="0" cellspacing="0">
            <xsl:for-each select="param">
              <tr>
                <td><font size="-1"><xsl:value-of select="@name"/></font></td>
                <td>&#160;&#160;<font size="-1"><xsl:value-of select="@value"/></font></td>
              </tr>
            </xsl:for-each>
          </table>
        </td>
      </tr>
      </table>
      </td></tr></xsl:for-each>
    </table>
    </center>
  </body>
</html>
</xsl:template>

</xsl:stylesheet>