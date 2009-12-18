<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE trippi-form [
<!ENTITY pageTitle            "Fedora Resource Index Query Service">
<!ENTITY baseStylesheet       "base.css">
<!ENTITY formStylesheet       "form.css">
<!ENTITY tupleFormTitle       "Find Tuples">
<!ENTITY tripleFormTitle      "Find Triples">
<!ENTITY responseFormatPrompt "Response">
<!ENTITY queryLanguagePrompt  "Language">
<!ENTITY queryTextPrompt      "Query Text or URL">
<!ENTITY usingTemplate        "(using template)">
<!ENTITY templateTextPrompt   "Template Text or URL (if applicable)">
<!ENTITY limitPrompt          "Limit">
<!ENTITY otherPrompt          "Advanced">
<!ENTITY distinctPrompt       "Force Distinct">
<!ENTITY safeTypesPrompt      "Fake Media-Types">
<!ENTITY streamPrompt         "Stream Immediately">
]>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<html>
  <head>
    <title>&pageTitle;</title>
    <xsl:call-template name="linkStyle">
      <xsl:with-param name="filename">&baseStylesheet;</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="linkStyle">
      <xsl:with-param name="filename">&formStylesheet;</xsl:with-param>
    </xsl:call-template>
<script language="JavaScript1.3"><![CDATA[
var panes = new Array();

function setupPanes(containerId, defaultTabId) {
  // go through the DOM, find each tab-container
  // set up the panes array with named panes
  // find the max height, set tab-panes to that height
  panes[containerId] = new Array();
  var maxHeight = 0; var maxWidth = 0;
  var container = document.getElementById(containerId);
  var paneContainer = container.getElementsByTagName("div")[0];
  var paneList = paneContainer.childNodes;
  for (var i=0; i < paneList.length; i++ ) {
    var pane = paneList[i];
    if (pane.nodeType != 1) continue;
    if (pane.offsetHeight > maxHeight) maxHeight = pane.offsetHeight;
    if (pane.offsetWidth  > maxWidth ) maxWidth  = pane.offsetWidth;
    panes[containerId][pane.id] = pane;
    pane.style.display = "none";
  }
    paneContainer.style.height = maxHeight + "px";
    paneContainer.style.width  = maxWidth + "px";
    document.getElementById(defaultTabId).onclick();
}

function showPane(paneId, activeTab) {
  // make tab active class
  // hide other panes (siblings)
  // make pane visible
  
    for (var con in panes) {
    activeTab.blur();
    activeTab.className = "tab-active";
    if (panes[con][paneId] != null) { // tab and pane are members of this container
      var pane = document.getElementById(paneId);
      pane.style.display = "block";
      var container = document.getElementById(con);
      var tabs = container.getElementsByTagName("ul")[0];
      var tabList = tabs.getElementsByTagName("a")
      for (var i=0; i<tabList.length; i++ ) {
        var tab = tabList[i];
        if (tab != activeTab) tab.className = "tab-disabled";
      }
      for (var i in panes[con]) {
        var pane = panes[con][i];
        if (pane == undefined) continue;
        if (pane.id == paneId) continue;
        pane.style.display = "none"
      }
    }
  }
  return false;    
}]]>
</script>
  </head>
  <body onLoad="setupPanes('container1', 'tab1');">
    <div class="heading">
    &pageTitle;
    </div>
    <div class="tab-container" id="container1">
      <ul class="tabs">
        <li><a href="#" onClick="return showPane('tupleForm', this)" id="tab1">&tupleFormTitle;</a></li>
        <li><a href="#" onClick="return showPane('tripleForm', this)">&tripleFormTitle;</a></li>
        <li><a href="#" onClick="return showPane('aliases', this)">Show Aliases</a></li>
      </ul>
      <div class="tab-panes">  
        <xsl:call-template name="doQueryForm"/>
        <xsl:call-template name="doQueryForm">
          <xsl:with-param name="triples">true</xsl:with-param>
        </xsl:call-template>
        <div id="aliases" class="tabContent">
          <table border="0" cellpadding="3" cellspacing="3" width="100%">
            <tr><td><u>Alias</u></td><td><u>URI Prefix</u></td></tr>
            <xsl:for-each select="/query-service/alias-map/child::alias">
              <tr>
                <td><b><xsl:value-of select="@name"/></b></td>
                <td><xsl:value-of select="@uri"/></td>
              </tr>
            </xsl:for-each>
          </table>
        </div>
      </div>
    </div>
  </body>
</html>
</xsl:template>

<xsl:template name="doQueryForm">
  <xsl:param name="triples"/>
  <div class="tabContent">
    <xsl:attribute name="id">
      <xsl:choose>
        <xsl:when test="$triples">tripleForm</xsl:when>
        <xsl:otherwise>tupleForm</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    <form method="POST" target="TrippiQueryResults">
      <xsl:attribute name="action">
        <xsl:value-of select="/query-service/@href"/>
      </xsl:attribute>
      <input type="hidden" name="type">
        <xsl:attribute name="value">
          <xsl:choose>
            <xsl:when test="$triples">triples</xsl:when>
            <xsl:otherwise>tuples</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </input>
      <table border="0" cellpadding="0" cellspacing="0">
      <tr>
        <td valign="top">
        <xsl:choose>
          <xsl:when test="$triples">
              <xsl:call-template name="queryLanguageForm">
              <xsl:with-param name="languages" select="/query-service/triple-languages"/>
              <xsl:with-param name="otherLanguages" select="/query-service/tuple-languages"/>
              <xsl:with-param name="appendText">&usingTemplate;</xsl:with-param>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="queryLanguageForm">
              <xsl:with-param name="languages" select="/query-service/tuple-languages"/>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td valign="top">
      <xsl:choose>
        <xsl:when test="$triples">
          <xsl:call-template name="responseFormatForm">
            <xsl:with-param name="formats" select="/query-service/triple-output-formats"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="responseFormatForm">
            <xsl:with-param name="formats" select="/query-service/tuple-output-formats"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
      </td>
      <td valign="top"><xsl:call-template name="limitForm"/></td>
      <td valign="top"><xsl:call-template name="otherForm"/></td>
      </tr>
      <tr><td colspan="4">
      <xsl:choose>
        <xsl:when test="$triples">
          <table border="0" cellpadding="0" cellspacing="0">
            <tr>
              <td><xsl:call-template name="tripleQueryTextForm"/></td>
              <td><xsl:call-template name="templateTextForm"/></td>
            </tr>
          </table>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="tupleQueryTextForm"/>
        </xsl:otherwise>
      </xsl:choose>
      </td></tr></table>
      <center>
      <div class="inputForm"><input type="submit" value=" Launch "/></div>
      </center>
    </form>
  </div>

</xsl:template>

<xsl:template name="limitForm">
  <div class="inputForm">
    <fieldset>
      <legend>&limitPrompt;</legend>
    <select name="limit" size="4">
      <option value="">Unlimited</option>
      <option value="1000" selected="selected">1000</option>
      <option value="100">100</option>
      <option value="10">10</option>
    </select>
    </fieldset>
  </div>
</xsl:template>

<xsl:template name="otherForm">
  <div class="inputForm">
    <fieldset>
      <legend>&otherPrompt;</legend>
      <input type="checkbox" name="distinct"/> <label for="distinct">&distinctPrompt;</label><br/>
      <input type="checkbox" name="dt" checked="checked"/> <label for="dt">&safeTypesPrompt;</label><br/>
      <input type="checkbox" name="stream"/> <label for="stream">&streamPrompt;</label>
    </fieldset>
  </div>
</xsl:template>

<xsl:template name="tupleQueryTextForm">
  <div class="inputForm">
    <fieldset>
      <legend>&queryTextPrompt;</legend>
    <textarea name="query" cols="80" rows="12" wrap="off"/>
    </fieldset>
  </div>
</xsl:template>

<xsl:template name="tripleQueryTextForm">
  <div class="inputForm">
    <fieldset>
      <legend>&queryTextPrompt;</legend>
    <textarea name="query" cols="36" rows="12" wrap="off"/>
    </fieldset>
  </div>
</xsl:template>

<xsl:template name="templateTextForm">
  <div class="inputForm">
    <fieldset>
      <legend>&templateTextPrompt;</legend>
    <textarea name="template" cols="36" rows="12" wrap="off"/>
    </fieldset>
  </div>
</xsl:template>

<xsl:template name="queryLanguageForm">
  <xsl:param name="languages"/>
  <xsl:param name="otherLanguages"/>
  <xsl:param name="appendText"/>
  <div class="inputForm">
    <fieldset>
      <legend>&queryLanguagePrompt;</legend>
    <select name="lang" size="4">
    <xsl:for-each select="$languages/child::language">
      <option>
        <xsl:attribute name="value">
          <xsl:value-of select="@name"/>
        </xsl:attribute>
        <xsl:if test="position() = 1">
          <xsl:attribute name="selected">selected</xsl:attribute>
        </xsl:if>
        <xsl:value-of select="@name"/>
      </option>
    </xsl:for-each>
    <xsl:if test="$appendText">
    <xsl:for-each select="$otherLanguages/child::language">
      <option>
        <xsl:attribute name="value">
          <xsl:value-of select="@name"/>
        </xsl:attribute>
        <xsl:value-of select="@name"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$appendText"/>
      </option>
    </xsl:for-each>
    </xsl:if>
    </select>
    </fieldset>
  </div>
</xsl:template>

<xsl:template name="responseFormatForm">
  <xsl:param name="formats"/>
  <div class="inputForm">
    <fieldset>
      <legend>&responseFormatPrompt;</legend>
    <select name="format" size="4">
    <xsl:for-each select="$formats/child::format">
      <option>
        <xsl:attribute name="value">
          <xsl:value-of select="@name"/>
        </xsl:attribute>
        <xsl:if test="position() = 1">
          <xsl:attribute name="selected">selected</xsl:attribute>
        </xsl:if>
        <xsl:value-of select="@name"/>
      </option>
    </xsl:for-each>
    </select>
    </fieldset>
  </div>
</xsl:template>

<xsl:template name="linkStyle">
  <xsl:param name="filename"/>
  <link>
    <xsl:attribute name="rel">stylesheet</xsl:attribute>
    <xsl:attribute name="title">Default</xsl:attribute>
    <xsl:attribute name="type">text/css</xsl:attribute>
    <xsl:attribute name="href">
      <xsl:value-of select="/query-service/@context"/>
      <xsl:text>/</xsl:text>
      <xsl:value-of select="$filename"/>
    </xsl:attribute>
  </link>
</xsl:template>

</xsl:stylesheet>
