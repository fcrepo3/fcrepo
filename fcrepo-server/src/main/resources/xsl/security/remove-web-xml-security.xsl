<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">     
<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

<!-- copy web.xml without security -->

<!-- remove security nodes, including possibly misleading comments -->
<xsl:template match="login-config"/>
<xsl:template match="security-constraint"/>
<xsl:template match="comment()" priority="1"/>

<!-- copy other elements -->
<xsl:template match="node()">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates select="node()"/>
  </xsl:copy>
</xsl:template>

<!-- copy attribute -->
<xsl:template match="@*">
  <xsl:copy/>
</xsl:template>
   
</xsl:stylesheet>
