<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
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
				<title>WSDL to Operation Summary Table</title>
			</head>
			<body>
				<center>
					<h2>Fedora Access Service (Fedora API-A)</h2>
					<h3>Summary of Operations</h3>
				</center>
				<xsl:apply-templates select="//wsdl:portType"/>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="wsdl:portType">
		<xsl:value-of select="wsdl:documentation"/>
		<center>
			<h3><a href="http://www.fedora.info">More Information on Fedora Web Services</a></h3>
			<hr/>
			<table width="784" border="1" cellpadding="5" cellspacing="5" bgcolor="silver">
				<tr>
					<td align="center">
						<font color="blue">Operation Name</font>
					</td>
					<td align="center">
						<font color="blue">Description</font>
					</td>
				</tr>
				<xsl:for-each select="wsdl:operation">
					<tr>
						<td align="left">
							<xsl:value-of select="@name"/>
						</td>
						<td align="left">
							<xsl:value-of select="wsdl:documentation"/>
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</center>
	</xsl:template>
</xsl:stylesheet>
