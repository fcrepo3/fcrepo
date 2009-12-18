<?xml version="1.0" ?>

<xsl:stylesheet
	version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:wsdldoc="http://www.cs.cornell.edu/~cwilper/wsdldoc/"
	xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
	xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
	xmlns:http="http://schemas.xmlsoap.org/wsdl/http/"
	xmlns:mime="http://schemas.xmlsoap.org/wsdl/mime/">

<xsl:output method="html" indent="yes"/>

<xsl:template match="/">
<html>
	<head>
		<title>WSDL Documentation</title>
	</head>

	<body>
      <xsl:for-each select="//wsdl:portType">
	    <h2><xsl:value-of select="@name"/></h2>
		<dir><i><xsl:value-of select="wsdl:documentation"/></i></dir>
		<hr size="1"></hr>
        <xsl:for-each select="wsdl:operation">
        <xsl:sort case-order="upper-first" select="@name"/>

        <!-- method name -->
        <h3><xsl:value-of select="@name"/></h3><dir>

        <code>
        <!-- return type -->
        <xsl:if test="not(wsdl:output)">void</xsl:if>
        <xsl:variable name="outmsgname" select="substring-after(wsdl:output/@message,':')"/>
        <xsl:for-each select="//wsdl:message[@name=$outmsgname]">
          <xsl:for-each select="wsdl:part">
		    <xsl:variable name="rtypename" select="@type"/>
              <xsl:for-each select="//wsdldoc:type[@name=$rtypename]">
			    <a>
				  <xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
			      <xsl:value-of select="$rtypename"/>
				</a>
              </xsl:for-each>

            <xsl:if test="not (position()=last())"> 
              <xsl:text>, </xsl:text> 
            </xsl:if> 
          </xsl:for-each>
        </xsl:for-each>
        <xsl:text>&#160;</xsl:text>
	
        <!-- method name -->
        <xsl:value-of select="@name"/>(

        <!-- params -->
        <xsl:variable name="inmsgname" select="substring-after(wsdl:input/@message,':')"/>
        <xsl:for-each select="//wsdl:message[@name=$inmsgname]">
          <xsl:for-each select="wsdl:part">
		    <nobr>
		    <xsl:variable name="ptypename" select="@type"/>
		    <xsl:for-each select="//wsdldoc:type[@name=$ptypename]">
			  <a>
		        <xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
			    <xsl:value-of select="$ptypename"/>
			  </a>
            </xsl:for-each><xsl:text> </xsl:text>
            <xsl:value-of select="@name"/>
            <xsl:if test="not (position()=last())"><xsl:text>, </xsl:text></xsl:if> 
			</nobr>
          </xsl:for-each>
        </xsl:for-each>
        )
	
        <!-- exceptions -->
        <xsl:if test="wsdl:fault">
          throws
          <xsl:for-each select="wsdl:fault">
            <xsl:value-of select="@message" />
          </xsl:for-each>
        </xsl:if>

        </code>

        <!-- documentation -->
        <dir><p><xsl:value-of select="wsdl:documentation"/></p></dir>

        <!-- params -->
		<xsl:if test="wsdl:input">
  	      <p>
	      <b>Parameters:</b>
          <dir>
		  <xsl:variable name="inmsgname2" select="substring-after(wsdl:input/@message,':')"/>
		  <xsl:for-each select="//wsdl:message[@name=$inmsgname2]">
            <xsl:for-each select="wsdl:part">
              <code><xsl:value-of select="@name"/> - </code>
  	          <xsl:value-of select="wsdl:documentation"/><br></br>
            </xsl:for-each>
          </xsl:for-each>
          </dir>
          </p>
        </xsl:if>

        <!-- returns -->
		<xsl:if test="wsdl:output">
		  <p>
		  <b>Returns:</b>
		  <dir>
		  <xsl:variable name="outmsgname2" select="substring-after(wsdl:output/@message,':')"/>
		  <xsl:for-each select="//wsdl:message[@name=$outmsgname2]">
            <xsl:for-each select="wsdl:part">
  	          <xsl:value-of select="wsdl:documentation"/><br></br>
            </xsl:for-each>
          </xsl:for-each>
          </dir>
		  </p>
		</xsl:if>
        </dir>

        <hr size="1"></hr>

      </xsl:for-each>
    </xsl:for-each>
  </body>
</html>
</xsl:template>

</xsl:stylesheet>
