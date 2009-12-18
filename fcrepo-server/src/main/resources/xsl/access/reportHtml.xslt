<?xml version="1.0" ?> 
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:fo="http://www.w3.org/1999/XSL/Format" version="1.0">
  <xsl:param name="fedora"/>
  <xsl:param name="REQUEST-TYPE" select="1"/>  
  <xsl:param name="GENERAL-TITLE"/>
  <xsl:param name="SPECIFIC-TITLE"/>
  <xsl:param name="FIELDARRAY-LENGTH"/>
  <xsl:param name="VIEWINGSTART"/>
  <xsl:param name="VIEWINGEND"/>
  <xsl:param name="COMPLETELISTSIZE"/>
  <xsl:param name="REPORTNAME"/>  
  <xsl:param name="SESSIONTOKEN"/>
  <xsl:param name="MAXRESULTS"/>
  <xsl:param name="NEWBASE"/>  
  <xsl:param name="PREFIX"/>
  <xsl:param name="DATERANGE"/>  
  <xsl:param name="DATERANGELABEL"/>    

  <xsl:template match="/result">
    <html>
      <head>
        <title>
          <xsl:value-of select="$GENERAL-TITLE"/> - <xsl:value-of select="$SPECIFIC-TITLE"/>
        </title>
      </head>
      <body>
        <center>
          <table width="784" border="0" cellpadding="0" cellspacing="0">
            <tr>
              <td width="141" height="134" valign="top" rowspan="2">
                <img src="/{$fedora}/images/newlogo2.jpg" width="141" height="134"/>
              </td>
              <td width="643" valign="top" colspan="3">
                <center>
                  <h2><xsl:value-of select="$GENERAL-TITLE"/></h2>
                  <h3><xsl:value-of select="$SPECIFIC-TITLE"/></h3>
                  <xsl:if test="$DATERANGE != '' or $PREFIX != ''">
                    <h4>
                      <xsl:if test="$DATERANGELABEL != ''">
                        <xsl:value-of select="$DATERANGELABEL"/>
                      </xsl:if>
                      <xsl:if test="$PREFIX != ''">
                        whose pids begin with 
                        &quot;<xsl:value-of select="$PREFIX"/>&quot;
                      </xsl:if>
                    </h4>                    
                  </xsl:if>
                </center>
              </td>
            </tr>
            <xsl:call-template name="commonRow"/>
          </table>
          <hr size="1"/>
          <table width="90%" border="1" cellpadding="5" cellspacing="5" bgcolor="silver">
            <xsl:apply-templates select="fieldNames"/>
            <xsl:apply-templates select="resultList/objectFields"/>
            <tr>
              <td colspan="{$FIELDARRAY-LENGTH}"></td>
            </tr>
          </table>
          <table width="90%" border="0" cellpadding="5" cellspacing="5">
            <xsl:call-template name="commonRow"/>
          </table>
        </center>        
      </body>
    </html>
  </xsl:template>
  
  <xsl:template match="*">
    node is <xsl:value-of select="name(.)"/>
  </xsl:template>

  <xsl:template name="commonRow">
    <tr>
      <td align="center" valign="top">
        Viewing results
        <xsl:if test="$VIEWINGSTART">
           <xsl:value-of select="$VIEWINGSTART"/>
         </xsl:if>
        <xsl:if test="$VIEWINGEND">
           to <xsl:value-of select="$VIEWINGEND"/>
         </xsl:if>
       </td>
      <xsl:choose>  
        <xsl:when test="$SESSIONTOKEN">
          <td align="center" valign="center">
            <form method="post" action="/{$fedora}/report">
              <xsl:for-each select="fieldNames/*">
                <input type="hidden" name="{./text()}" value="true"/>  
              </xsl:for-each>
              <input type="hidden" name="report" value="{$REPORTNAME}"/>              
              <input type="hidden" name="sessionToken" value="{$SESSIONTOKEN}"/>
              <input type="hidden" name="maxResults" value="{$MAXRESULTS}"/>
              <input type="hidden" name="newBase" value="{$NEWBASE}"/>
              <input type="hidden" name="dateRange" value="{$DATERANGE}"/>
              <input type="hidden" name="prefix" value="{$PREFIX}"/>                    
              <input type="submit" value="More Results &gt;"/>
            </form>
           </td>
        </xsl:when>
        <xsl:otherwise>
          <td align="center" valign="top">
            No more results.
          </td>
        </xsl:otherwise>
      </xsl:choose>
       <td align="center" valign="center">
        <form method="/{$fedora}/get" action="report">
          <input type="submit" value="New Report"/>
        </form>
       </td>
     </tr>  
  </xsl:template>

  <xsl:template match="fieldNames">
    <tr>
      <xsl:apply-templates select="fieldName"/>
    </tr>
  </xsl:template>

  <xsl:template match="fieldName">
    <td valign="top">
      <strong>
        <xsl:value-of select="./text()"/>
      </strong>
    </td>
  </xsl:template>
  
  <xsl:template match="objectFields">
    <tr>
      <xsl:for-each select="*">
        <td valign="top">
          <xsl:choose>
            <xsl:when test="name(.) = 'pid'">
              <a href="{concat('/',$fedora,'/get/',./text())}">
                <xsl:value-of select="./text()"/>
              </a>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="./text()"/>
            </xsl:otherwise>
          </xsl:choose>
        </td>      
      </xsl:for-each>
    </tr>
  </xsl:template>
  
</xsl:stylesheet>  





        




