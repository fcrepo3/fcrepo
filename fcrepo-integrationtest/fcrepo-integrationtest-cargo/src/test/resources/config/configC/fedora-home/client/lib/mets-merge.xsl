<?xml version="1.0" encoding="utf-8"?>

<!-- merge.xsl
  substitute per-object XML data into per-batch METS XML template
-->

<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
xmlns:xsi="http://www.w3.org/2000/10/XMLSchema-instance"
xmlns:METS="http://www.loc.gov/METS/"
xmlns:xlink="http://www.w3.org/1999/xlink" 
xmlns:fbm="http://www.fedora.info/definitions/"
>
  <xsl:param name="date" select="NO-DATE-PARAM"/>
  <xsl:param name="subfilepath" select="NO-SUBFILEPATH-PARAM"/>
  <xsl:variable name="substitutions" select="document($subfilepath)"/>
  <xsl:variable name="foo"><xsl:value-of select="$substitutions/fbm:input/@LABEL"/></xsl:variable>

  <xsl:output method="xml" indent="yes" />

  <xsl:template match="@*">
    <xsl:copy/>
  </xsl:template>

  <xsl:template name="generic-node" match="node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- add per-object comment -->
  <xsl:template match="/" xmlns:METS="http://www.loc.gov/METS/" xmlns:xlink="http://www.w3.org/1999/xlink" >
    <xsl:copy>
      <xsl:if test="$substitutions/fbm:input/fbm:comment">
        <xsl:comment>
          <xsl:value-of select="$substitutions/fbm:input/fbm:comment"/>
        </xsl:comment>
      </xsl:if>    
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>
  
  <!-- substitute per-object @OBJID and @LABEL -->
  <xsl:template match="/METS:mets">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:if test="$substitutions/fbm:input/@LABEL">
        <xsl:attribute name="LABEL">
          <xsl:value-of select="$substitutions/fbm:input/@LABEL"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="$substitutions/fbm:input/@OBJID">
        <xsl:attribute name="OBJID">
          <xsl:value-of select="$substitutions/fbm:input/@OBJID"/>
        </xsl:attribute>
      </xsl:if>      
      <xsl:apply-templates select="node()"/>
        </xsl:copy>
  </xsl:template>
  
  <!-- substitute xform param date for @CREATEDATE and @LASTMODDATE -->
  <!-- /METS:mets/METS:metsHdr -->
  <xsl:template match="METS:metsHdr" xmlns:METS="http://www.loc.gov/METS/" >
    <xsl:copy>
      <xsl:apply-templates select="@*"/>  
      <xsl:apply-templates select="node()"/>
        </xsl:copy>
    <!--<xsl:apply-templates select="node()"/>-->        
  </xsl:template>
  
  <xsl:template match="METS:agent[@ROLE='IPOWNER']" xmlns:METS="http://www.loc.gov/METS/" >
    <xsl:copy>
      <xsl:apply-templates select="@*"/>  
      <xsl:apply-templates select="node()"/>          

    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="METS:name" xmlns:METS="http://www.loc.gov/METS/" >
    <xsl:copy>
      <xsl:choose>
        <xsl:when test="$substitutions/fbm:input/@OWNERID">
          <xsl:value-of select="$substitutions/fbm:input/@OWNERID"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="@*"/>  
          <xsl:apply-templates select="node()"/>          
        </xsl:otherwise>
      </xsl:choose>
    </xsl:copy>
  </xsl:template>  
  
  <!-- substitute xform param date for @CREATED -->
  <xsl:template match="METS:techMD|METS:rightsMD|METS:sourceMD|METS:digiprovMD|METS:descMD|METS:serviceBinding" xmlns:METS="http://www.loc.gov/METS/">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="node()"/>
        </xsl:copy>
  </xsl:template>
  
  <!-- substitute metadata -->
  <!-- /METS:mets/METS:dmdSecFedora/*/METS:mdWrap/METS:xmlData|/METS:mets/METS:amdSec/*/METS:mdWrap/METS:xmlData -->
  <xsl:template match="METS:mdWrap/METS:xmlData" xmlns:METS="http://www.loc.gov/METS/">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:variable name="metadataID" select="../../../@ID" /><!-- e.g., DIGIPROV1, from amdSec element -->      
      <xsl:choose>
        <xsl:when test="$substitutions/fbm:input/fbm:metadata/fbm:metadata[@ID=$metadataID]">
          <xsl:apply-templates select="$substitutions/fbm:input/fbm:metadata/fbm:metadata[@ID=$metadataID]/node()" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:choose>
            <xsl:when test="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$metadataID]/fbm:xmlContent">
              <xsl:apply-templates select="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$metadataID]/fbm:xmlContent/node()" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="node()"/>
            </xsl:otherwise>
          </xsl:choose>          
          <!--<xsl:apply-templates select="node()"/>-->
        </xsl:otherwise>
      </xsl:choose>            
        </xsl:copy>
  </xsl:template>

  <!-- substitute metadata labels -->
  <!-- /METS:mets/METS:dmdSecFedora/*/METS:mdWrap|/METS:mets/METS:amdSec/*/METS:mdWrap -->
  <xsl:template match="METS:mdWrap" 
    xmlns:METS="http://www.loc.gov/METS/">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:variable name="metadataID" select="../../@ID" /><!-- e.g., DESC1, from METS:dmdSecFedora element -->
      <xsl:if test="$substitutions/fbm:input/fbm:metadata/fbm:metadata[@ID=$metadataID]/@LABEL" >
        <xsl:attribute name="LABEL">
          <xsl:value-of select="$substitutions/fbm:input/fbm:metadata/fbm:metadata[@ID=$metadataID]/@LABEL" />
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$metadataID]/@LABEL" >
        <xsl:attribute name="LABEL">
          <xsl:value-of select="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$metadataID]/@LABEL" />
        </xsl:attribute>
      </xsl:if>        
      <xsl:if test="$substitutions/fbm:input/fbm:metadata/fbm:metadata[@ID=$metadataID]/@MIMETYPE" >
        <xsl:attribute name="MIMETYPE">
          <xsl:value-of select="$substitutions/fbm:input/fbm:metadata/fbm:metadata[@ID=$metadataID]/@MIMETYPE" />
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$metadataID]/@MIMETYPE" >
        <xsl:attribute name="MIMETYPE">
          <xsl:value-of select="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$metadataID]/@MIMETYPE" />
        </xsl:attribute>
      </xsl:if>        
      <xsl:apply-templates select="node()"/>      
        </xsl:copy>
  </xsl:template>  

  <!-- substitute MIMETYPE for non-metadata datastreams -->
  <!-- /METS:mets/METS:dmdSecFedora/*/METS:mdWrap|/METS:mets/METS:amdSec/*/METS:mdWrap -->
  <xsl:template match="METS:file" 
    xmlns:METS="http://www.loc.gov/METS/">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:variable name="datastreamID" select="../@ID" /><!-- e.g., DESC1, from METS:dmdSecFedora element -->      
      <xsl:if test="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$datastreamID]/@MIMETYPE" >
        <xsl:attribute name="MIMETYPE">
          <xsl:value-of select="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$datastreamID]/@MIMETYPE" />
        </xsl:attribute>
      </xsl:if>  
      <xsl:apply-templates select="node()"/>      
        </xsl:copy>
  </xsl:template>  

  <!-- substitute per-datastream @xlink:title and @xlink:href -->
  <!-- /METS:mets/METS:fileSec/METS:fileGrp/METS:fileGrp/METS:file/METS:FLocat -->
   <xsl:template match="METS:FLocat" xmlns:METS="http://www.loc.gov/METS/">
    <xsl:variable name="datastreamID" select="../../@ID" />
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:variable name="prefix" select="concat('$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@id=&quot;',
        $datastreamID,
        '&quot;]/')" />
      <xsl:variable name="title_ptr" select="concat($prefix,'@title')" />
      <xsl:variable name="href_ptr" select="concat($prefix,'@href')" />
      <xsl:if test="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$datastreamID]/@xlink:title" >
        <xsl:attribute name="xlink:title">
          <xsl:value-of select="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$datastreamID]/@xlink:title" />
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$datastreamID]/@xlink:href" >
        <xsl:attribute name="xlink:href">
          <xsl:value-of select="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$datastreamID]/@xlink:href" />
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$datastreamID]/@LABEL" >
        <xsl:attribute name="xlink:title">
          <xsl:value-of select="$substitutions/fbm:input/fbm:datastreams/fbm:datastream[@ID=$datastreamID]/@LABEL" />
        </xsl:attribute>
      </xsl:if>        
      <!-- processing terminals here, so no need to xsl:apply-templates select="node()" -->
        </xsl:copy>
  </xsl:template>
  
</xsl:transform>

