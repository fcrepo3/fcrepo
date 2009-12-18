<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

<xsl:output
	method="text"
	indent="no"
	omit-xml-declaration="yes"
	media-type="text/plain"/>

<xsl:strip-space elements="*"/>

<xsl:template match="/"><xsl:apply-templates/></xsl:template>

<xsl:template match="rdf:Description">
	<xsl:for-each select="*">
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="../@rdf:about"/>
		<xsl:text>&gt;</xsl:text>

		<xsl:text> </xsl:text>

		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="namespace-uri()"/>
		<xsl:value-of select="name()"/>
		<xsl:text>&gt;</xsl:text>
		<xsl:text> </xsl:text>
		<xsl:if test="./text()">
			<xsl:text>"</xsl:text>
			<xsl:value-of select="./text()"/>
			<xsl:text>"</xsl:text>
			<xsl:if test="@rdf:datatype">
				<xsl:text>^^&lt;</xsl:text>
				<xsl:value-of select="@rdf:datatype"/>
				<xsl:text>&gt;</xsl:text>
			</xsl:if>
		</xsl:if>
		<xsl:if test="not (./text())">
			<xsl:text>&lt;</xsl:text>
			<xsl:value-of select="@rdf:resource"/>
			<xsl:text>&gt;</xsl:text>
		</xsl:if>

		<xsl:text> .
</xsl:text>
	</xsl:for-each>

</xsl:template>

</xsl:stylesheet>
