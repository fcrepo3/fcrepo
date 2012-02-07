<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/">
  <xsl:param name="fedora"/>
  <xsl:output method="html" indent="yes"/>
  <xsl:template match="oai_dc:dc">
    <html>
      <head>
        <title>Object Profile HTML Presentation</title>
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
                  <h3>Default Disseminator - Dublin Core View</h3>
                </center>
              </td>
            </tr>
          </table>
          <hr/>
          <table width="500" border="1" cellpadding="5" cellspacing="5" bgcolor="silver">
            <xsl:for-each select="//dc:title">
              <tr>
                <td align="right">
                  <strong>Title: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:creator">
              <tr>
                <td align="right">
                  <strong>Creator: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:subject">
              <tr>
                <td align="right">
                  <strong>Subject: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:description">
              <tr>
                <td align="right">
                  <strong>Description: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:publisher">
              <tr>
                <td align="right">
                  <strong>Publisher: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:contributor">
              <tr>
                <td align="right">
                  <strong>Contributor: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:date">
              <tr>
                <td align="right">
                  <strong>Date: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:type">
              <tr>
                <td align="right">
                  <strong>Type: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:format">
              <tr>
                <td align="right">
                  <strong>Format: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:identifier">
              <tr>
                <td align="right">
                  <strong>Identifier: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:source">
              <tr>
                <td align="right">
                  <strong>Source: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:language">
              <tr>
                <td align="right">
                  <strong>Language: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:relation">
              <tr>
                <td align="right">
                  <strong>Relation: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:coverage">
              <tr>
                <td align="right">
                  <strong>Coverage: </strong>
                </td>
                <td align="left">
                  <xsl:value-of select="."/>
                </td>
              </tr>
            </xsl:for-each>  
            <xsl:for-each select="//dc:rights">
              <tr>
                <td align="right">
                  <strong>Rights: </strong>
                </td>
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
