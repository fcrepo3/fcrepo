<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:access="http://www.fedora.info/definitions/1/0/access/"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="access">
  <xsl:param name="fedora" />
  <xsl:output method="html" indent="yes" />
  <xsl:template match="access:objectDatastreams">
    <html>
      <head>
        <title>Object Datastreams HTML Presentation</title>
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
                  <h3>List Datastreams</h3>
                </center>
              </td>
            </tr>
          </table>
          <hr />
          <font size="+1">
            <strong>Object Identifier (PID): </strong>
            <xsl:value-of select="@pid" />
          </font>
          <p />
          <xsl:choose>
            <xsl:when test="@asOfDateTime">
              <font size="+1">
                <strong>Version Date: </strong>
                <xsl:value-of select="@asOfDateTime" />
              </font>
            </xsl:when>
            <xsl:otherwise>
              <font size="+1">
                <strong>Version Date: </strong>
                current
              </font>
            </xsl:otherwise>
          </xsl:choose>
          <hr />
          <table width="784" border="1" cellpadding="5"
            cellspacing="5" bgcolor="silver">
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
            <xsl:for-each select="//access:datastream">
              <tr>
                <td>
                  <table width="100%">
                    <tr>
                      <td width="65%">
                        <a>
                          <xsl:attribute name="href">
                            <xsl:text>/</xsl:text>
                            <xsl:value-of select="$fedora"/>
                            <xsl:text>/objects/</xsl:text>
                            <xsl:value-of select="replace(../@pid, '%', '%25')"/>
                            <xsl:text>/datastreams/</xsl:text>
                            <xsl:value-of select="@dsid"/>
                            <xsl:if test="../@asOfDateTime">
                              <xsl:text>/?asOfDateTime=</xsl:text>
                              <xsl:value-of select="../@asOfDateTime" />
                            </xsl:if>
                          </xsl:attribute>
                          <xsl:value-of select="@dsid"/>
                        </a>
                      </td>
                      <td width="35%">
                        <font size="-1">
                          <a>
                            <xsl:attribute name="href">
                              <xsl:text>/</xsl:text>
                              <xsl:value-of select="$fedora"/>
                              <xsl:text>/objects/</xsl:text>
                              <xsl:value-of select="replace(../@pid, '%', '%25')"/>
                              <xsl:text>/datastreams/</xsl:text>
                              <xsl:value-of select="@dsid"/>
                              <xsl:text>/content</xsl:text>
                              <xsl:if test="../@asOfDateTime">
                                <xsl:text>?asOfDateTime=</xsl:text>
                                <xsl:value-of select="../@asOfDateTime" />
                              </xsl:if>
                            </xsl:attribute>
                            <xsl:text>view content</xsl:text>
                          </a>
                        </font>
                      </td>
                    </tr>
                  </table>
                </td>
                <td>
                  <xsl:value-of select="@label" />
                </td>
                <td>
                  <xsl:value-of select="@mimeType" />
                </td>
              </tr>
            </xsl:for-each>
          </table>
        </center>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
