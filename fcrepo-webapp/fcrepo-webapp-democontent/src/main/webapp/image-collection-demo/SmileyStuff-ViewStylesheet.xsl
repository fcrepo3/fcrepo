<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:s="http://www.w3.org/2001/sw/DataAccess/rf1/result">

<xsl:template match="/">
<xsl:variable name="collTitle" select="/s:sparql/s:results/s:result/s:collTitle"/>
<xsl:variable name="collDesc" select="/s:sparql/s:results/s:result/s:collDesc"/>
<html>
  <head><title><xsl:value-of select="$collTitle"/></title></head>
  <body bgcolor="#bbddbb">
    <center>
    <font face="arial,helvetica">
      <h2><xsl:value-of select="$collTitle"/><br/>
      <i><xsl:value-of select="$collDesc"/></i></h2>
    </font>
    </center>
    <hr size="1"/>
    <center>
    <table border="0" cellpadding="5">
      <xsl:for-each select="/s:sparql/s:results/s:result">
        <xsl:variable name="pid" select="substring-after(s:member/@uri, '/')"/>
        <tr>
          <td>
          <center>
            <a>
              <xsl:attribute name="href">
                <xsl:text>/fedora/objects/</xsl:text>
                <xsl:value-of select="$pid"/>
                <xsl:text>/methods/demo:DualResolution/fullSize</xsl:text>
              </xsl:attribute>
              <img width="160" height="120">
                <xsl:attribute name="src">
                  <xsl:text>/fedora/objects/</xsl:text>
                  <xsl:value-of select="$pid"/>
                  <xsl:text>/methods/demo:DualResolution/mediumSize</xsl:text>
                </xsl:attribute>
              </img><br/>
              ( Full Size )
            </a>
            </center>
          </td>
          <td>
            <b><xsl:value-of select="s:memberTitle"/></b><br/>
            <xsl:value-of select="s:memberDesc"/>
          </td>
        </tr>
      </xsl:for-each>
    </table>
    </center>
  </body>
</html>
</xsl:template>

</xsl:stylesheet>
