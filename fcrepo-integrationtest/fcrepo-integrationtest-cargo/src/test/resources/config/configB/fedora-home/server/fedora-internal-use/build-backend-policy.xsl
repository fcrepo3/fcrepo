<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="urn:oasis:names:tc:xacml:1.0:policy" xmlns:xacml="urn:oasis:names:tc:xacml:1.0:policy">     
<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

<!-- build backend policy -->

  <!-- ?xml version="1.0" encoding="UTF-8"? -->
<xsl:template match="/">
  <xsl:apply-templates select="*"/>
</xsl:template>

  <!-- ?xml version="1.0" encoding="UTF-8"? -->
<xsl:template match="xacml:Policy">
    <Policy xmlns="urn:oasis:names:tc:xacml:1.0:policy"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      RuleCombiningAlgId="urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:first-applicable"
      >
      <xsl:attribute name="PolicyId"><xsl:value-of select="@PolicyId"/></xsl:attribute>
      <xsl:apply-templates select="node()"/>
    </Policy>
</xsl:template>

<xsl:template match="xacml:Target">
  <Target>
    <xsl:apply-templates select="xacml:Subjects"/>
    <Resources>
      <AnyResource/>
    </Resources>      
    <Actions>
      <Action>
        <ActionMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
          <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">urn:fedora:names:fedora:2.1:action:id-resolveDatastream</AttributeValue>
          <ActionAttributeDesignator DataType="http://www.w3.org/2001/XMLSchema#string"
            AttributeId="urn:fedora:names:fedora:2.1:action:id"/>
        </ActionMatch>
      </Action>
    </Actions>    
  </Target>  
  <xsl:apply-templates select="xacml:Rule"/>
</xsl:template>

<xsl:template match="xacml:SubjectMatch">
  <SubjectMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
    <xsl:apply-templates select="*"/>
    <SubjectAttributeDesignator AttributeId="fedoraRole" MustBePresent="false" 
      DataType="http://www.w3.org/2001/XMLSchema#string"/>
  </SubjectMatch>
</xsl:template>

<xsl:template match="xacml:AttributeValue">
  <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">
    <xsl:value-of select="text()"/>
  </AttributeValue>
</xsl:template>

<xsl:template match="xacml:Rule">
  <Rule>
    <xsl:attribute name="RuleId"><xsl:value-of select="@RuleId"/></xsl:attribute>
    <xsl:attribute name="Effect"><xsl:value-of select="@Effect"/></xsl:attribute>  
    <xsl:if test="child::xacml:AuthnRequired|child::xacml:SslRequired|child::xacml:IpRegexes|child::xacml:ExcludedRoles">
      <Condition FunctionId="urn:oasis:names:tc:xacml:1.0:function:and">
        <xsl:apply-templates select="xacml:AuthnRequired|xacml:SslRequired|xacml:IpRegexes|xacml:ExcludedRoles"/>    
      </Condition>
    </xsl:if>
  </Rule>
</xsl:template>

<xsl:template match="xacml:AuthnRequired">
  <xsl:comment>requiring authn</xsl:comment>
  <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:not">
    <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
      <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string"/>
      <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-one-and-only">
        <SubjectAttributeDesignator DataType="http://www.w3.org/2001/XMLSchema#string"
          AttributeId="urn:fedora:names:fedora:2.1:subject:loginId" />
      </Apply>
    </Apply>
  </Apply>
</xsl:template>

<xsl:template match="xacml:SslRequired">
  <xsl:comment>requiring ssl</xsl:comment>
  <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
    <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string"
    >urn:fedora:names:fedora:2.1:environment:httpRequest:security-secure</AttributeValue>
    <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-one-and-only">
      <EnvironmentAttributeDesignator DataType="http://www.w3.org/2001/XMLSchema#string"
        AttributeId="urn:fedora:names:fedora:2.1:environment:httpRequest:security" />
    </Apply>
  </Apply>
</xsl:template>

<xsl:template match="xacml:IpRegexes">
  <xsl:comment>requiring client ip in range given</xsl:comment>
  <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:or">
    <xsl:apply-templates select="*"/>  
  </Apply>
</xsl:template>

<xsl:template match="xacml:IpRegex">
  <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:regexp-string-match">
    <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select="text()"/></AttributeValue>
    <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-one-and-only">
      <EnvironmentAttributeDesignator AttributeId="urn:fedora:names:fedora:2.1:environment:httpRequest:clientIpAddress" DataType="http://www.w3.org/2001/XMLSchema#string"/>
    </Apply>
  </Apply>
</xsl:template>

<xsl:template match="xacml:ExcludedRoles">
  <xsl:comment>excluding roles covered in role-specific policies</xsl:comment>
  <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:not">
    <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:or">
      <xsl:apply-templates select="*"/>  
    </Apply>
  </Apply>
</xsl:template>

<xsl:template match="xacml:ExcludedRole">
  <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
    <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select="text()"/></AttributeValue>
    <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-one-and-only">
      <SubjectAttributeDesignator AttributeId="fedoraRole" DataType="http://www.w3.org/2001/XMLSchema#string"/>
    </Apply>
  </Apply>
</xsl:template>

<!-- Description Subjects Subject AnySubject -->
<xsl:template match="*">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates select="node()"/>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>

