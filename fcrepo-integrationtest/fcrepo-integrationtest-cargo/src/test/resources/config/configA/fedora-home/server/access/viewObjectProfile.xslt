<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:access="http://www.fedora.info/definitions/1/0/access/"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="access">
  <xsl:param name="fedora" />
  <xsl:output method="html" indent="yes" />
  <xsl:template match="access:objectProfile">
    <html>
      <head>
        <title>Object Profile HTML Presentation</title>
      </head>
      <body>
        <center>
          <table width="784" border="0" cellpadding="0" cellspacing="0">
            <tr>
              <td width="141" height="134" valign="top">
                <img src="/{$fedora}/images/newlogo2.jpg" width="141" height="134" />
              </td>
              <td width="643" valign="top">
                <center>
                  <h2>Fedora Digital Object</h2>
                  <h3>Object Profile View</h3>
                </center>
              </td>
            </tr>
          </table>
          <hr />
          <xsl:choose>
            <xsl:when test="@dateTime">
              <font size="+1">
                <strong>Version Date: </strong>
                <xsl:value-of select="@dateTime" />
              </font>
            </xsl:when>
            <xsl:otherwise>
              <font size="+1">
                <strong>Version Date: </strong>
                current
              </font>
            </xsl:otherwise>
          </xsl:choose>
          <p />
          <xsl:variable name="datastreams-url">
            <xsl:text>objects/</xsl:text>
            <xsl:value-of select="replace(@pid, '%', '%25')" />
            <xsl:text>/datastreams</xsl:text>
          </xsl:variable>
          <a href="/{$fedora}/{$datastreams-url}">View the Datastreams List for this Object</a>
          <p />
          <xsl:variable name="methods-url">
            <xsl:text>objects/</xsl:text>
            <xsl:value-of select="replace(@pid, '%', '%25')" />
            <xsl:text>/methods</xsl:text>
          </xsl:variable>
          <a href="/{$fedora}/{$methods-url}">View the Methods List for this Object</a>
          <p />
          <xsl:variable name="history-url">
            <xsl:text>objects/</xsl:text>
            <xsl:value-of select="replace(@pid, '%', '%25')" />
            <xsl:text>/versions</xsl:text>
          </xsl:variable>
          <a href="/{$fedora}/{$history-url}">View the Version History for this Object</a>
          <p />
          <xsl:variable name="objectxml-url">
            <xsl:text>objects/</xsl:text>
            <xsl:value-of select="replace(@pid, '%', '%25')" />
            <xsl:text>/objectXML</xsl:text>
          </xsl:variable>
          <a href="/{$fedora}/{$objectxml-url}">View the XML Representation of this Object</a>          
          <hr />
          <table width="784" border="1" cellpadding="5"
            cellspacing="5" bgcolor="silver">
            <tr>
              <td align="right">
                <strong>Object Identifier (PID): </strong>
              </td>
              <td align="left">
                <xsl:value-of select="@pid" />
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Object Label: </strong>
              </td>
              <td align="left">
                <xsl:value-of select="access:objLabel" />
              </td>
            </tr>
            <tr>
              <td align="right" valign="top">
                <strong>Object Content Model(s): </strong>
              </td>
              <td align="left">
                <table border="0">
                  <xsl:for-each select="access:objModels/access:model">
                    <tr>
                      <td>
                        <xsl:value-of select="." />
                      </td>
                    </tr>
                  </xsl:for-each>
                </table>
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Object Creation Date: </strong>
              </td>
              <td align="left">
                <xsl:value-of select="access:objCreateDate" />
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Object Last Modified: </strong>
              </td>
              <td align="left">
                <xsl:value-of select="access:objLastModDate" />
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Object Owner Identifier: </strong>
              </td>
              <td align="left">
                <xsl:value-of select="access:objOwnerId" />
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Object State: </strong>
              </td>
              <td align="left">
                <xsl:value-of select="access:objState" />
              </td>
            </tr>
          </table>
        </center>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
