<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<html>
  <head><title>Query Error</title></head>
  <body bgcolor="#ffeeee">
    <table border="0" cellpadding="5">
      <tr><td valign="top"><font size="+2"><u><b><xsl:value-of select="/error/message"/></b></u></font></td>
      </tr>
      <tr><td valign="top"><font size="-1"><pre><xsl:value-of select="/error/detail"/></pre></font></td>
      </tr>
    </table>
  </body>
</html>
</xsl:template>

</xsl:stylesheet>