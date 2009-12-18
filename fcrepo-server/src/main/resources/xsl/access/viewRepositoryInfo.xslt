<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="fedora"/>
  <xsl:output method="html" indent="yes"/>
  <xsl:template match="fedoraRepository">
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
                  <h3>Repository Information View</h3>
                </center>
              </td>
            </tr>
          </table>
          <hr/>
          <font size="+1">
            <strong>Repository Name: </strong>
            <xsl:value-of select="repositoryName"/>
          </font>
          <hr/>
          <p/>
          <table width="784" border="1" cellpadding="5" cellspacing="5" bgcolor="silver">
            <tr>
              <td align="right">
                <strong>Base URL:</strong>
              </td>
              <td align="left">
                <xsl:value-of select="repositoryBaseURL"/>
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Version:</strong>
              </td>
              <td align="left">
                <xsl:value-of select="repositoryVersion"/>
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>PID Namespace:</strong>
              </td>
              <td align="left">
                <xsl:value-of select="repositoryPID/PID-namespaceIdentifier"/>
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>PID Delimiter:</strong>
              </td>
              <td align="left">
                <xsl:value-of select="repositoryPID/PID-delimiter"/>
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Sample PID:</strong>
              </td>
              <td align="left">
                <xsl:value-of select="repositoryPID/PID-sample"/>
              </td>
            </tr>
            <xsl:for-each select="//retainPID">
            <tr>
              <td align="right">
                <strong>Retain PID Namespace: </strong>
              </td>
              <td align="left">
                <xsl:value-of select="."/>
              </td>
            </tr>
            </xsl:for-each>            
            <tr>
              <td align="right">
                <strong>OAI Namespace:</strong>
              </td>
              <td align="left">
                <xsl:value-of select="repositoryOAI-identifier/OAI-namespaceIdentifier"/>
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>OAI Delimiter:</strong>
              </td>
              <td align="left">
                <xsl:value-of select="repositoryOAI-identifier/OAI-delimiter"/>
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Sample OAI Identifier:</strong>
              </td>
              <td align="left">
                <xsl:value-of select="repositoryOAI-identifier/OAI-sample"/>
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Sample Search URL:</strong>
              </td>
              <td align="left">
                <xsl:variable name="search-url">
                  <xsl:value-of select="sampleSearch-URL"/>
                </xsl:variable>
                <a href="{$search-url}"><xsl:value-of select="sampleSearch-URL"/></a>
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Sample Access URL:</strong>
              </td>
              <td align="left">
                <xsl:variable name="access-url">
                  <xsl:value-of select="sampleAccess-URL"/>
                </xsl:variable>
                <a href="{$access-url}"><xsl:value-of select="sampleAccess-URL"/></a>
              </td>
            </tr>
            <tr>
              <td align="right">
                <strong>Sample OAI URL:</strong>
              </td>
              <td align="left">
                <xsl:variable name="oai-url">
                  <xsl:value-of select="sampleOAI-URL"/>
                </xsl:variable>
                <a href="{$oai-url}"><xsl:value-of select="sampleOAI-URL"/></a>
              </td>
            </tr>
            <xsl:for-each select="//adminEmail">
            <tr>
              <td align="right">
                <strong>Admin Email: </strong>
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
