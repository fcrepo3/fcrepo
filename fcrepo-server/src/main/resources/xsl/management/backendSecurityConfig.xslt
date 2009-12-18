<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY titleText              "Fedora Backend Security Configuration">
  <!ENTITY saveButtonText         "Save Changes">
  <!ENTITY desc                   "ns:serviceSecurityDescription">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:ns="info:fedora/fedora-system:def/beSecurity#" 
                version="1.1">
   <xsl:template match="/">
      <html>
         <head>
            <title>&titleText;</title>

            <meta http-equiv="Pragma" content="no-cache"/>
            <meta http-equiv="Cache-Control" content="no-cache, must-revalidate"/>

            <script>
<![CDATA[

var activeLayer = null;
var activeLayerName = null;
var activeBottomLayer = new Array();

function setActiveLayer(layerName) {
  if (activeLayer != null) {
    if (activeBottomLayer[activeLayerName] != null) {
      activeBottomLayer[activeLayerName].style.visibility = "hidden";
    }
    activeLayer.style.visibility = "hidden";
  }
  activeLayerName = layerName;
  activeLayer = document.getElementById(layerName);
  activeLayer.style.visibility = "visible";
  if (activeBottomLayer[activeLayerName] != null) {
    activeBottomLayer[activeLayerName].style.visibility = "visible";
  }
}


function setActiveBottomLayer(layerName, methodName) {
  var bottomLayerName = layerName + "/" + methodName;
  if (activeBottomLayer[layerName] != null) {
    activeBottomLayer[layerName].style.visibility = "hidden";
  }
  activeBottomLayer[layerName] = document.getElementById(bottomLayerName);
  activeBottomLayer[layerName].style.visibility = "visible";
}

function init() {
  setActiveLayer('default');
  document.getElementById("activeLayerChooser").selectedIndex = 0;
  theForm.reset();
}

function doAdd(name) {
  var ip = prompt("Enter a new IP address.", "");
  if (ip == null || ip.length == 0) return;
  var err = verifyIP(ip);
  if (err == null) {
    var box = theForm.elements[name + "/box"];
    box.options[box.options.length] = new Option(ip, ip);
    if (box.options.length > 1) {
      box.size = box.options.length;
    }
    setHiddenIPList(name);
  } else {
    alert(err);
  }
}

function verifyIP(IPvalue) {
  var ipPattern = /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/;
  var ipArray = IPvalue.match(ipPattern); 
  if (IPvalue == "0.0.0.0" || IPvalue == "255.255.255.255") {
    return IPvalue + ' is a special IP address and cannot be used here.';
  }
  if (ipArray == null) {
    return IPvalue + ' is not a valid IP address.';
  } else {
    for (i = 0; i < 4; i++) {
      thisSegment = ipArray[i];
      if (thisSegment > 255) {
        return IPvalue + ' is not a valid IP address.';
      }
    }
  }
  return null;
}

function doDelete(name) {
  var box = theForm.elements[name + "/box"];
  var oldSelectedIndex = box.selectedIndex;
  box.options[oldSelectedIndex] = null;
  if (box.options.length > 0) {
    if (box.options.length == oldSelectedIndex) {
      box.selectedIndex = oldSelectedIndex - 1;
    } else {
      box.selectedIndex = oldSelectedIndex;
    }
  } else {
    theForm.elements[name + "/delete"].disabled = true;
  }
  if (box.options.length > 1) {
    box.size = box.options.length;
  }
  setHiddenIPList(name);
}

function setHiddenIPList(name) {
  var box = theForm.elements[name + "/box"];
  var val = "";
  for (i = 0; i < box.options.length; i++) {
    if (i > 0) val = val + " ";
    val = val + box.options[i].value;
  }
  theForm.elements[name].value = val;
}

function doSelect(name) {
  theForm.elements[name + "/delete"].disabled = false;
}

function ipInheritChanged(name) {
  var inherit = theForm.elements[name + "/inherit"];
  if (inherit.selectedIndex == 0) {
    theForm.elements[name + "/box"].selectedIndex = -1;
    theForm.elements[name + "/box"].disabled = true;
    theForm.elements[name + "/add"].disabled = true;
    theForm.elements[name + "/delete"].disabled = true;
  } else {
    theForm.elements[name + "/box"].disabled = false;
    theForm.elements[name + "/add"].disabled = false;
  }
}

function defaultBasicAuthChanged() {
  var basicAuth = theForm.elements["default/callBasicAuth"];
  if (basicAuth.selectedIndex == 0) {
      theForm.elements["default/callUsername"].disabled = false;
      theForm.elements["default/callPassword"].disabled = false;
  } else {
      theForm.elements["default/callUsername"].disabled = true;
      theForm.elements["default/callPassword"].disabled = true;
  }
}

function internalBasicAuthChanged() {
  var basicAuth = theForm.elements["internal/basicAuth"];
  if (basicAuth.selectedIndex == 1) {
      theForm.elements["internal/username"].disabled = false;
      theForm.elements["internal/password"].disabled = false;
  } else {
      theForm.elements["internal/username"].disabled = true;
      theForm.elements["internal/password"].disabled = true;
  }
}

function callBasicAuthChanged(name) {
  var basicAuth = theForm.elements[name + "/callBasicAuth"];
  if (basicAuth.selectedIndex == 1) {
      theForm.elements[name + "/callUsername"].disabled = false;
      theForm.elements[name + "/callPassword"].disabled = false;
  } else {
      theForm.elements[name + "/callUsername"].disabled = true;
      theForm.elements[name + "/callPassword"].disabled = true;
  }
}


]]>
            </script>

            <style type="text/css">


#header {
    clear: both;
    margin-bottom: 10px;
    min-height: 70px;
    width: 100%;
}

#title {
    text-align: center; 
}

#body {
  position: absolute;
  left: 10;
  top: 90;
}

#activeLayerChooser {
  width: 215px;
}

.innerTop {
  background: #ddddff;
  border-left: solid 1px #999999;
  border-top: solid 1px #999999;
  border-right: solid 1px #999999;
  padding: 5px;
  margin: 0px;
  width: 200;
}

* html .innerTop {
  width: 200px; 
  w\idth: 210px; 
}

.innerBottom {
  position: absolute;
  visibility: hidden;
  top: 269;
  left: 10;
  background: #ddddff;
  border-bottom: solid 1px #999999;
  border-left: solid 1px #999999;
  border-right: solid 1px #999999;
  padding: 5px;
  margin: 0px;
  width: 650;
}

* html .innerBottom {
  width: 650px; 
  w\idth: 660px; 
}

#left {
  z-index: 3;
  border-left: solid 3px #000000;
  border-top: solid 3px #000000;
  border-bottom: solid 3px #000000;
  position: absolute;
  left: 0;
  top: 0;
  width: 225;
  background: #bbbbee;
  padding: 5px;
  margin: 0px;
}

* html #left {  /* This is the Tan hack */
  width: 225px; 
  w\idth: 238px; 
}

.right {
  z-index: 2;
  border-right: solid 3px #000000;
  border-top: solid 3px #000000;
  border-bottom: solid 3px #000000;
  border-left: solid 3px #000000;
  position: absolute;
  left: 235;
  top: 0;
  width: 670;
  background: #bbbbee;
  padding: 5px;
  margin: 0px;
  visibility: hidden;
}

* html .right {
  width: 670px; 
  w\idth: 686px; 
}

p {
  font-family: sans-serif;
  font-size: 16px;
  padding: 0px;
  margin: 0px;
}

select {
  font-family: sans-serif;
  font-size: 12px;
  padding: 0px;
  margin: 0px;
}

h2 {
  font-family: sans-serif;
  font-size: 20px;
  padding-top: 1px;
  padding-bottom: 1px;
  margin-top: 2px;
  margin-bottom: 2px;
  background: #000000;
  text-color: white;
  color: white;
}

h3 {
  font-family: sans-serif;
  font-size: 18px;
  padding-top: 1px;
  margin-top: 2px;
  padding-bottom: 1px;
  margin-bottom: 2px;
  border-bottom: dashed 1px #000000;
}


           </style>
         </head>
         <body onLoad="javascript:init()">
            <div id="header">
               <table border="0" width="850">
                  <tr>
                     <td>
                        <!-- <img src="/fedora/images/newlogo2.jpg" width="141" height="134"/> -->
                        <img src="images/newlogo2.jpg" width="70" height="67"/>
                     </td>
                     <td valign="top">
                        <center>
                           <span style="font-weight: bold; color: #000000; margin-top: 4px; margin-bottom: 4px; font-size: 24px; line-height: 110%; padding-top: 8px; padding-bottom: 4px;">&titleText;</span>
                           <br/>
                           <nobr>Use this form to make changes to Fedora's backend security configuration.</nobr><br/>
                           <nobr>When finished, click <i>Save Changes</i>.  See the Fedora server documentation for more detail.</nobr></center>
                     </td>
                  </tr>
               </table>
            </div>
            <div id="body">
               <form name="theForm" method="POST">
                  <div id="left">
                     <xsl:element name="select">
                        <xsl:attribute name="id">activeLayerChooser</xsl:attribute>
                        <xsl:attribute name="size">
                           <!-- the number of sDefs or 10, whichever is less -->
                           <xsl:choose>
                              <xsl:when test="count(/&desc;/&desc;[not(contains(@role,'/'))]) &lt; 10">
                                 <xsl:value-of select="count(/&desc;/&desc;[not(contains(@role,'/'))])"/>
                              </xsl:when>
                              <xsl:otherwise>10</xsl:otherwise>
                           </xsl:choose>
                        </xsl:attribute>
                        <xsl:attribute name="onChange">javascript:setActiveLayer(this.options[this.selectedIndex].value)</xsl:attribute>
                        <option value="default" selected="selected">Default Settings</option>
                        <option value="internal">Internal Settings</option>
                        <xsl:for-each select="/&desc;/&desc;[not(contains(@role, '/')) and contains(@role, ':')]">
                           <xsl:element name="option">
                              <xsl:attribute name="value">
                                 <xsl:value-of select="@role"/>
                              </xsl:attribute>
                              BMech <xsl:value-of select="@role"/>
                           </xsl:element>
                        </xsl:for-each>
                     </xsl:element>
                     <center>
                        <input type="submit" value="&saveButtonText;"/>
                     </center>
                  </div>

                  <div id="default" class="right">
                     <h2>Default Settings</h2>
                     <p>These settings will be <i>inherited</i> by all backend services
                     that are not explicitly configured.</p>
                     <table border="0" cellpadding="5" cellspacing="0" width="100%">
                           <tr>
                              <td valign="top">
                                <xsl:call-template name="outputCallOptions">
                                  <xsl:with-param name="role">default</xsl:with-param>
                                  <xsl:with-param name="callSSL"><xsl:value-of select="/&desc;/@callSSL"/></xsl:with-param>
                                  <xsl:with-param name="callBasicAuth"><xsl:value-of select="/&desc;/@callBasicAuth"/></xsl:with-param>
                                  <xsl:with-param name="callUsername"><xsl:value-of select="/&desc;/@callUsername"/></xsl:with-param>
                                  <xsl:with-param name="callPassword"><xsl:value-of select="/&desc;/@callPassword"/></xsl:with-param>
                                </xsl:call-template>
                              </td>
                              <td valign="top">
                                <xsl:call-template name="outputCallbackOptions">
                                  <xsl:with-param name="role">default</xsl:with-param>
                                  <xsl:with-param name="callbackSSL"><xsl:value-of select="/&desc;/@callbackSSL"/></xsl:with-param>
                                  <xsl:with-param name="callbackBasicAuth"><xsl:value-of select="/&desc;/@callbackBasicAuth"/></xsl:with-param>
                                  <xsl:with-param name="iplist"><xsl:value-of select="/&desc;/@iplist"/></xsl:with-param>
                                </xsl:call-template>
                              </td>
                           </tr>
                    </table>
                  </div>

                  <div id="internal" class="right">
                     <h2>Internal Settings</h2>
                     <p>These settings will be used when Fedora makes calls
                     to itself.</p>
                     <table border="0" cellpadding="5" cellspacing="0" width="100%">
                       <tr>
                          <td valign="top">
                            <xsl:for-each select="/&desc;/&desc;[@role = 'fedoraInternalCall-1']">
                              <h3>Calls to Fedora from Fedora</h3>
                              <p>
                              <table border="0" cellpadding="0" cellspacing="3">
                                <xsl:call-template name="outputBooleanInputRow">
                                  <xsl:with-param name="label">SSL:</xsl:with-param>
                                  <xsl:with-param name="name">internal/ssl</xsl:with-param>
                                  <xsl:with-param name="value"><xsl:value-of select="@callSSL"/></xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="outputBooleanInputRow">
                                  <xsl:with-param name="label">Basic Auth:</xsl:with-param>
                                  <xsl:with-param name="name">internal/basicAuth</xsl:with-param>
                                  <xsl:with-param name="value"><xsl:value-of select="@callBasicAuth"/></xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="outputTextInputRow">
                                  <xsl:with-param name="label">Username:</xsl:with-param>
                                  <xsl:with-param name="name">internal/username</xsl:with-param>
                                  <xsl:with-param name="value"><xsl:value-of select="@callUsername"/></xsl:with-param>
                                  <xsl:with-param name="disabled">
                                    <xsl:choose>
                                      <xsl:when test="@callBasicAuth != 'true'">true</xsl:when>
                                      <xsl:otherwise>false</xsl:otherwise>
                                    </xsl:choose>
                                  </xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="outputTextInputRow">
                                  <xsl:with-param name="label">Password:</xsl:with-param>
                                  <xsl:with-param name="name">internal/password</xsl:with-param>
                                  <xsl:with-param name="value"><xsl:value-of select="@callPassword"/></xsl:with-param>
                                  <xsl:with-param name="type">password</xsl:with-param>
                                  <xsl:with-param name="disabled">
                                    <xsl:choose>
                                      <xsl:when test="@callBasicAuth != 'true'">true</xsl:when>
                                      <xsl:otherwise>false</xsl:otherwise>
                                    </xsl:choose>
                                  </xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="outputIPInputRow">
                                  <xsl:with-param name="label">Allowed IPs:</xsl:with-param>
                                  <xsl:with-param name="name">internal/iplist</xsl:with-param>
                                  <xsl:with-param name="value"><xsl:value-of select="@iplist"/></xsl:with-param>
                                </xsl:call-template>
                              </table>
                              </p>
                            </xsl:for-each>
                          </td>
                       </tr>
                    </table>
                  </div>

                  <xsl:for-each select="/&desc;/&desc;[not(contains(@role, '/')) and contains(@role, ':')]">
                     <xsl:element name="div">
                        <xsl:attribute name="id">
                           <xsl:value-of select="@role"/>
                        </xsl:attribute>
                        <xsl:attribute name="class">right</xsl:attribute>
                        <xsl:attribute name="style">height: 462;</xsl:attribute>
                        <h2>
                           Behavior Mechanism - <xsl:value-of select="@role"/>
                        </h2>
                        <table border="0" cellpadding="5" cellspacing="0" width="100%">
                           <tr>
                              <td valign="top">
                                <xsl:call-template name="outputCallOptions">
                                  <xsl:with-param name="role"><xsl:value-of select="@role"/></xsl:with-param>
                                  <xsl:with-param name="callSSL"><xsl:value-of select="@callSSL"/></xsl:with-param>
                                  <xsl:with-param name="callBasicAuth"><xsl:value-of select="@callBasicAuth"/></xsl:with-param>
                                  <xsl:with-param name="callUsername"><xsl:value-of select="@callUsername"/></xsl:with-param>
                                  <xsl:with-param name="callPassword"><xsl:value-of select="@callPassword"/></xsl:with-param>
                                </xsl:call-template>
                              </td>
                              <td valign="top">
                                <xsl:call-template name="outputCallbackOptions">
                                  <xsl:with-param name="role"><xsl:value-of select="@role"/></xsl:with-param>
                                  <xsl:with-param name="callbackSSL"><xsl:value-of select="@callbackSSL"/></xsl:with-param>
                                  <xsl:with-param name="callbackBasicAuth"><xsl:value-of select="@callbackBasicAuth"/></xsl:with-param>
                                  <xsl:with-param name="iplist"><xsl:value-of select="@iplist"/></xsl:with-param>
                                </xsl:call-template>
                              </td>
                           </tr>
                           <tr>
                              <td colspan="2">
                                 <h3 style="border: none;">Method Overrides</h3>
                                 <xsl:variable name="startString"><xsl:value-of select="@role"/>/</xsl:variable>
                                 <div class="innerTop">
                                    <xsl:element name="select">
                                       <xsl:attribute name="size">3</xsl:attribute>
                                       <xsl:attribute name="style">width:200;</xsl:attribute>
                                       <xsl:attribute name="onChange">javascript:setActiveBottomLayer('<xsl:value-of select="@role"/>', this.options[this.selectedIndex].value)</xsl:attribute>
                                       <xsl:for-each select="/&desc;/&desc;[starts-with(@role, $startString)]">
                                         <xsl:element name="option">
                                           <xsl:attribute name="value"><xsl:value-of select="substring-after(@role, '/')"/></xsl:attribute>
                                           <xsl:value-of select="substring-after(@role, '/')"/>
                                         </xsl:element>
                                       </xsl:for-each>
                                    </xsl:element>
                                 </div>

                                 <xsl:for-each select="/&desc;/&desc;[starts-with(@role, $startString)]">
                                   <xsl:element name="div">
                                      <xsl:attribute name="class">innerBottom</xsl:attribute>
                                      <xsl:attribute name="id"><xsl:value-of select="@role"/></xsl:attribute>
                                      <table border="0" cellpadding="5" cellspacing="0" width="100%">
                                         <tr>
                                            <td valign="top">
                                              <xsl:call-template name="outputCallOptions">
                                                <xsl:with-param name="role"><xsl:value-of select="@role"/></xsl:with-param>
                                                <xsl:with-param name="callSSL"><xsl:value-of select="@callSSL"/></xsl:with-param>
                                                <xsl:with-param name="callBasicAuth"><xsl:value-of select="@callBasicAuth"/></xsl:with-param>
                                                <xsl:with-param name="callUsername"><xsl:value-of select="@callUsername"/></xsl:with-param>
                                                <xsl:with-param name="callPassword"><xsl:value-of select="@callPassword"/></xsl:with-param>
                                              </xsl:call-template>
                                            </td>
                                            <td valign="top">
                                              <xsl:call-template name="outputCallbackOptions">
                                                <xsl:with-param name="role"><xsl:value-of select="@role"/></xsl:with-param>
                                                <xsl:with-param name="callbackSSL"><xsl:value-of select="@callbackSSL"/></xsl:with-param>
                                                <xsl:with-param name="callbackBasicAuth"><xsl:value-of select="@callbackBasicAuth"/></xsl:with-param>
                                                <xsl:with-param name="iplist"><xsl:value-of select="@iplist"/></xsl:with-param>
                                              </xsl:call-template>
                                            </td>
                                         </tr>
                                      </table>
                                   </xsl:element>
                                 </xsl:for-each>

                              </td>
                           </tr>
                        </table>
                     </xsl:element>
                  </xsl:for-each>
               </form>
            </div>
         </body>
      </html>
   </xsl:template>

  <xsl:template name="outputCallOptions">

    <xsl:param name="role"/>
    <xsl:param name="callSSL"/>
    <xsl:param name="callBasicAuth"/>
    <xsl:param name="callUsername"/>
    <xsl:param name="callPassword"/>

    <h3>Calls to Service</h3>
    <p>
      <table border="0" cellpadding="0" cellspacing="3">
        <xsl:call-template name="outputBooleanInputRow">
          <xsl:with-param name="label">SSL:</xsl:with-param>
          <xsl:with-param name="name"><xsl:value-of select="$role"/>/callSSL</xsl:with-param>
          <xsl:with-param name="value"><xsl:value-of select="$callSSL"/></xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="outputBooleanInputRow">
          <xsl:with-param name="label">Basic Auth:</xsl:with-param>
          <xsl:with-param name="name"><xsl:value-of select="$role"/>/callBasicAuth</xsl:with-param>
          <xsl:with-param name="value"><xsl:value-of select="$callBasicAuth"/></xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="outputTextInputRow">
          <xsl:with-param name="label">Username:</xsl:with-param>
          <xsl:with-param name="name"><xsl:value-of select="$role"/>/callUsername</xsl:with-param>
          <xsl:with-param name="value"><xsl:value-of select="$callUsername"/></xsl:with-param>
          <xsl:with-param name="disabled">
            <xsl:choose>
              <xsl:when test="$callBasicAuth != 'true'">true</xsl:when>
              <xsl:otherwise>false</xsl:otherwise>
            </xsl:choose>
          </xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="outputTextInputRow">
          <xsl:with-param name="label">Password:</xsl:with-param>
          <xsl:with-param name="name"><xsl:value-of select="$role"/>/callPassword</xsl:with-param>
          <xsl:with-param name="value"><xsl:value-of select="$callPassword"/></xsl:with-param>
          <xsl:with-param name="type">password</xsl:with-param>
          <xsl:with-param name="disabled">
            <xsl:choose>
              <xsl:when test="$callBasicAuth != 'true'">true</xsl:when>
              <xsl:otherwise>false</xsl:otherwise>
            </xsl:choose>
          </xsl:with-param>
        </xsl:call-template>
      </table>
    </p>

  </xsl:template>

  <xsl:template name="outputCallbackOptions">

    <xsl:param name="role"/>
    <xsl:param name="callbackSSL"/>
    <xsl:param name="callbackBasicAuth"/>
    <xsl:param name="iplist"/>

    <h3>Callbacks to Fedora</h3>
    <p>
    <table border="0" cellpadding="0" cellspacing="3">
      <xsl:call-template name="outputBooleanInputRow">
        <xsl:with-param name="label">SSL:</xsl:with-param>
        <xsl:with-param name="name"><xsl:value-of select="$role"/>/callbackSSL</xsl:with-param>
        <xsl:with-param name="value"><xsl:value-of select="$callbackSSL"/></xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="outputBooleanInputRow">
        <xsl:with-param name="label">Basic Auth:</xsl:with-param>
        <xsl:with-param name="name"><xsl:value-of select="$role"/>/callbackBasicAuth</xsl:with-param>
        <xsl:with-param name="value"><xsl:value-of select="$callbackBasicAuth"/></xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="outputIPInputRow">
        <xsl:with-param name="label">Allowed IPs:</xsl:with-param>
        <xsl:with-param name="name"><xsl:value-of select="$role"/>/iplist</xsl:with-param>
        <xsl:with-param name="value"><xsl:value-of select="$iplist"/></xsl:with-param>
      </xsl:call-template>
    </table>
    </p>

  </xsl:template>

  <xsl:template name="outputIPInputRow">

    <xsl:param name="label"/>
    <xsl:param name="name"/>
    <xsl:param name="value"/>

    <tr>
       <td valign="top" align="right">
          <nobr><xsl:value-of select="$label"/></nobr>
       </td>
       <td valign="top">
         <xsl:element name="select">
           <xsl:attribute name="name"><xsl:value-of select="$name"/>/inherit</xsl:attribute>
           <xsl:attribute name="onChange">javascript:ipInheritChanged('<xsl:value-of select="$name"/>')</xsl:attribute>
           <xsl:element name="option">
             <xsl:attribute name="value">true</xsl:attribute>
             <xsl:if test="$value = ''">
               <xsl:attribute name="selected">selected</xsl:attribute>
             </xsl:if>
             <xsl:choose>
               <xsl:when test="starts-with($name, 'default/')">
                 <xsl:text>All</xsl:text>
               </xsl:when>
               <xsl:otherwise>
                 <xsl:text>[inherited]</xsl:text>
               </xsl:otherwise>
             </xsl:choose>
           </xsl:element>
           <xsl:element name="option">
             <xsl:attribute name="value">false</xsl:attribute>
             <xsl:if test="$value != ''">
               <xsl:attribute name="selected">selected</xsl:attribute>
             </xsl:if>
             <xsl:text>Only the following</xsl:text>
           </xsl:element>
         </xsl:element>
       </td>
       <td valign="top">
         <xsl:variable name="ips">
           <xsl:call-template name="tokenize">
             <xsl:with-param name="string" select="$value"/>
           </xsl:call-template>
         </xsl:variable>
         <xsl:element name="input">
           <xsl:attribute name="name"><xsl:value-of select="$name"/></xsl:attribute>
           <xsl:attribute name="type">hidden</xsl:attribute>
           <xsl:attribute name="value">
             <xsl:for-each select="$ips/token">
               <xsl:if test="position() &gt; 1"><xsl:text> </xsl:text></xsl:if>
               <xsl:value-of select="."/>
             </xsl:for-each>
           </xsl:attribute>
         </xsl:element>
         <xsl:element name="select">
           <xsl:attribute name="name"><xsl:value-of select="$name"/>/box</xsl:attribute>
           <xsl:attribute name="size">
             <xsl:choose>
               <xsl:when test="count($ips/token) &gt; 2"><xsl:value-of select="count($ips/token)"/></xsl:when>
               <xsl:otherwise>2</xsl:otherwise>
             </xsl:choose>
           </xsl:attribute>
           <xsl:attribute name="style">width: 130px;</xsl:attribute>
           <xsl:attribute name="onChange">javascript:doSelect('<xsl:value-of select="$name"/>')</xsl:attribute>
           <xsl:choose>
             <xsl:when test="$value = ''">
               <xsl:attribute name="disabled">disabled</xsl:attribute>
             </xsl:when>
             <xsl:otherwise>
               <xsl:for-each select="$ips/token"> 
                 <xsl:element name="option">
                   <xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
                   <xsl:value-of select="."/>
                 </xsl:element>
               </xsl:for-each>
             </xsl:otherwise>
           </xsl:choose>
         </xsl:element>
       </td>
       <td valign="top">
          <xsl:element name="input">
             <xsl:attribute name="type">button</xsl:attribute>
             <xsl:attribute name="name"><xsl:value-of select="$name"/>/add</xsl:attribute>
             <xsl:attribute name="value">+</xsl:attribute>
             <xsl:attribute name="onClick">javascript:doAdd('<xsl:value-of select="$name"/>')</xsl:attribute>
             <xsl:attribute name="style">width: 25px; height:20px;</xsl:attribute>
             <xsl:if test="$value = ''">
               <xsl:attribute name="disabled">disabled</xsl:attribute>
             </xsl:if>
          </xsl:element>
          <br/>
          <xsl:element name="input">
             <xsl:attribute name="type">button</xsl:attribute>
             <xsl:attribute name="name"><xsl:value-of select="$name"/>/delete</xsl:attribute>
             <xsl:attribute name="value">-</xsl:attribute>
             <xsl:attribute name="onClick">javascript:doDelete('<xsl:value-of select="$name"/>')</xsl:attribute>
             <xsl:attribute name="style">width: 25px; height:20px;</xsl:attribute>
             <xsl:attribute name="disabled">disabled</xsl:attribute>
          </xsl:element>
       </td>
    </tr>

  </xsl:template>


  <xsl:template name="outputTextInputRow">

    <xsl:param name="label"/>
    <xsl:param name="name"/>
    <xsl:param name="value"/>
    <xsl:param name="type">text</xsl:param>
    <xsl:param name="disabled">false</xsl:param>

    <tr>
      <td valign="top" align="right">
        <nobr><xsl:value-of select="$label"/></nobr>
      </td>
      <td valign="top">
        <xsl:element name="input">
          <xsl:attribute name="type"><xsl:value-of select="$type"/></xsl:attribute>
          <xsl:attribute name="name"><xsl:value-of select="$name"/></xsl:attribute>
          <xsl:attribute name="value"><xsl:value-of select="$value"/></xsl:attribute>
          <xsl:if test="$disabled = 'true'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
          </xsl:if>
        </xsl:element>
      </td>
    </tr>

  </xsl:template>

  <xsl:template name="outputBooleanInputRow">

    <xsl:param name="label"/>
    <xsl:param name="name"/>
    <xsl:param name="value"/>

    <tr>
      <td valign="top" align="right">
        <p>
          <nobr><xsl:value-of select="$label"/></nobr>
        </p>
      </td>
      <td valign="top">
       <xsl:call-template name="outputBooleanInputElement">
         <xsl:with-param name="name"><xsl:value-of select="$name"/></xsl:with-param>
         <xsl:with-param name="value"><xsl:value-of select="$value"/></xsl:with-param>
       </xsl:call-template>
      </td>
    </tr>

  </xsl:template>

  <xsl:template name="outputBooleanInputElement">

    <xsl:param name="name"/>
    <xsl:param name="value"/>

    <xsl:element name="select">
      <xsl:attribute name="name"><xsl:value-of select="$name"/></xsl:attribute>
      <xsl:choose>
        <xsl:when test="$name = 'internal/basicAuth'">
          <xsl:attribute name="onChange">javascript:internalBasicAuthChanged()</xsl:attribute>
        </xsl:when>
        <xsl:when test="$name = 'default/callBasicAuth'">
          <xsl:attribute name="onChange">javascript:defaultBasicAuthChanged()</xsl:attribute>
        </xsl:when>
        <xsl:when test="contains($name, '/callBasicAuth')">
          <xsl:attribute name="onChange">javascript:callBasicAuthChanged('<xsl:value-of select="substring-before($name, '/callBasicAuth')"/>')</xsl:attribute>
        </xsl:when>
      </xsl:choose>
      <xsl:if test="not(starts-with($name, 'default/'))">
        <xsl:element name="option">
          <xsl:attribute name="value"/>
          <xsl:if test="$value = ''">
            <xsl:attribute name="selected">selected</xsl:attribute>
          </xsl:if>
          <xsl:text>[inherited]</xsl:text>
        </xsl:element>
      </xsl:if>
      <xsl:element name="option">
        <xsl:attribute name="value">true</xsl:attribute>
        <xsl:if test="$value = 'true'">
          <xsl:attribute name="selected">selected</xsl:attribute>
        </xsl:if>
        <xsl:choose>
          <xsl:when test="contains($name, '/callbackSSL') or contains($name, '/callbackBasicAuth')">
            <xsl:text>must be used</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>will be used</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:element>
      <xsl:element name="option">
        <xsl:attribute name="value">false</xsl:attribute>
        <xsl:if test="$value = 'false' or starts-with($name, 'default/')">
          <xsl:attribute name="selected">selected</xsl:attribute>
        </xsl:if>
        <xsl:choose>
          <xsl:when test="contains($name, '/callbackSSL') or contains($name, '/callbackBasicAuth')">
            <xsl:text>is not required</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>will not be used</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:element>
    </xsl:element>

  </xsl:template>




<xsl:template name="tokenize">
   <xsl:param name="string"
              select="''" />
   <xsl:param name="delimiters"
              select="' &#x9;
'" />
   <xsl:choose>
      <xsl:when test="not($string)" />
      <xsl:when test="not($delimiters)">
         <xsl:call-template name="_tokenize-characters">
            <xsl:with-param name="string"
                            select="$string" />
         </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
         <xsl:call-template name="_tokenize-delimiters">
            <xsl:with-param name="string"
                            select="$string" />
            <xsl:with-param name="delimiters"
                            select="$delimiters" />
         </xsl:call-template>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>
<xsl:template name="_tokenize-characters">
   <xsl:param name="string" />
   <xsl:if test="$string">
      <token>
         <xsl:value-of select="substring($string, 1, 1)" />
      </token>
      <xsl:call-template name="_tokenize-characters">
         <xsl:with-param name="string"
                         select="substring($string, 4)" />
      </xsl:call-template>
   </xsl:if>
</xsl:template>
<xsl:template name="_tokenize-delimiters">
   <xsl:param name="string" />
   <xsl:param name="delimiters" />
   <xsl:variable name="delimiter"
                 select="substring($delimiters, 1, 1)" />
   <xsl:choose>
      <xsl:when test="not($delimiter)">
         <token>
            <xsl:value-of select="$string" />
         </token>
      </xsl:when>
      <xsl:when test="contains($string, $delimiter)">
         <xsl:if test="not(starts-with($string, $delimiter))">
            <xsl:call-template name="_tokenize-delimiters">
               <xsl:with-param name="string"
                               select="substring-before($string, $delimiter)" />
               <xsl:with-param name="delimiters"
                               select="substring($delimiters, 4)" />
            </xsl:call-template>
         </xsl:if>
         <xsl:call-template name="_tokenize-delimiters">
            <xsl:with-param name="string"
                            select="substring-after($string, $delimiter)" />
            <xsl:with-param name="delimiters"
                            select="$delimiters" />
         </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
         <xsl:call-template name="_tokenize-delimiters">
            <xsl:with-param name="string"
                            select="$string" />
            <xsl:with-param name="delimiters"
                            select="substring($delimiters, 4)" />
         </xsl:call-template>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

</xsl:stylesheet>
