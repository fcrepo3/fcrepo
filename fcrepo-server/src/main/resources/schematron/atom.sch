<?xml version="1.0" encoding="UTF-8"?>
<!-- Copyright: Sylvain Hellegouarch (2007) -->
<!--
Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:
 
     * Redistributions of source code must retain the above copyright notice, 
       this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright notice, 
       this list of conditions and the following disclaimer in the documentation 
       and/or other materials provided with the distribution.
     * Neither the name of Sylvain Hellegouarch nor the names of his contributors 
       may be used to endorse or promote products derived from this software 
       without specific prior written permission.
 
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
-->
<iso:schema xmlns:iso="http://purl.oclc.org/dsdl/schematron" xml:lang="en-US">

    <!-- Namespaces declaration -->
    <iso:ns uri="http://www.w3.org/2005/Atom" prefix="atom" />
    <iso:ns uri="http://www.w3.org/2007/app" prefix="app" />
    <iso:ns uri="http://purl.org/syndication/thread/1.0" prefix="thr" />
    <iso:ns uri="http://www.w3.org/1999/xhtml" prefix="xhtml" />

    <iso:title>Atom and Atom Publishing Protocol schematron validator</iso:title>

    <iso:phase id="AtomPubService">
        <iso:active pattern="AtomPub" />
    </iso:phase>

    <iso:phase id="AtomFeed">
        <iso:active pattern="RFC4287" />
        <iso:active pattern="RFC4685" />
        <iso:active pattern="AtomPub" />
    </iso:phase>

    <iso:phase id="AtomEntry">
        <iso:active pattern="RFC4287" />
        <iso:active pattern="RFC4685" />
    </iso:phase>

    <!-- Atom format rules (RFC 4287) -->
    <iso:pattern id="RFC4287">
        <iso:rule context="atom:feed">
            <iso:assert test="count(atom:id) = 1">An atom feed must have one and only one atom:id
                element. (RFC 4287, sec.4.1.1)</iso:assert>
            <iso:assert test="count(atom:title) = 1">An atom feed must have one and only one
                atom:title element. (RFC 4287, sec.4.1.1)</iso:assert>
            <iso:assert test="count(atom:updated) = 1">An atom feed must have one and only one
                atom:updated element. (RFC 4287, sec.4.1.1)</iso:assert>
            <iso:assert test="atom:author or not(atom:entry[not(atom:author)])">An atom:feed must
                have an atom:author unless all of its atom:entry children have an atom:author. (RFC
                4287, sec.4.1.1)</iso:assert>
            <iso:assert test="count(atom:generator) &lt;= 1">An atom feed cannot have more than
                one atom:generator element. (RFC 4287, sec.4.1.1)</iso:assert>
            <iso:assert test="count(atom:icon) &lt;= 1">An atom feed cannot have more than one
                atom:icon element. (RFC 4287, sec.4.1.1)</iso:assert>
            <iso:assert test="count(atom:logo) &lt;= 1">An atom feed cannot have more than one
                atom:logo element. (RFC 4287, sec.4.1.1)</iso:assert>
            <iso:assert test="count(atom:rights) &lt;= 1">An atom feed cannot have more than one
                atom:rights element. (RFC 4287, sec.4.1.1)</iso:assert>
            <iso:assert test="count(atom:subtitle) &lt;= 1">An atom feed cannot have more than
                one atom:subtitle element. (RFC 4287, sec.4.1.1)</iso:assert>
            <iso:assert test="not(atom:published)">An atom feed cannot have an atom:published
                element. (RFC 4287, sec.4.1.1)</iso:assert>
            <iso:assert test="not(atom:source)">An atom feed cannot have an atom:source element.
                (RFC 4287, sec.4.1.1)</iso:assert>
        </iso:rule>

        <iso:rule context="atom:category">
            <iso:assert test="@term">An atom:category element must have a term attribute. (RFC 4287,
                sec.4.2.2)</iso:assert>
        </iso:rule>

        <iso:rule context="atom:entry/atom:content[@type='xhtml']">
            <iso:assert test="xhtml:div">An atom:content of type="xhtml" must have a direct
                xhtml:div child element. (RFC 4287, sec.4.1.3)</iso:assert>
        </iso:rule>

        <iso:rule context="atom:entry/atom:rights[@type='xhtml']">
            <iso:assert test="xhtml:div">An atom:rights of type="xhtml" must have a direct xhtml:div
                child element (RFC 4287, sec.4.1.10)</iso:assert>
        </iso:rule>

        <iso:rule context="atom:entry/atom:title[@type='xhtml']">
            <iso:assert test="xhtml:div">An atom:title of type="xhtml" must have a direct xhtml:div
                child element (RFC 4287, sec.4.1.14)</iso:assert>
        </iso:rule>

        <iso:rule context="atom:entry/atom:subtitle[@type='xhtml']">
            <iso:assert test="xhtml:div">An atom:subtitle of type="xhtml" must have a direct
                xhtml:div child element (RFC 4287, sec.4.1.12)</iso:assert>
        </iso:rule>

        <iso:rule context="atom:entry/atom:summary[@type='xhtml']">
            <iso:assert test="xhtml:div">An atom:summary of type="xhtml" must have a direct
                xhtml:div child element (RFC 4287, sec.4.1.13)</iso:assert>
        </iso:rule>

        <iso:rule context="atom:entry">
            <iso:assert test="count(atom:id) = 1">An atom entry must have one and only one atom:id
                element. (RFC 4287, sec.4.1.2)</iso:assert>
            <iso:assert test="count(atom:title) = 1">An atom entry must have one and only one
                atom:title element. (RFC 4287, sec.4.1.2)</iso:assert>
            <iso:assert test="count(atom:updated) = 1">An atom entry must have one and only one
                atom:updated element. (RFC 4287, sec.4.1.2)</iso:assert>
            <iso:assert test="count(atom:content) &lt;= 1">An atom entry cannot have more than
                one atom:content element. (RFC 4287, sec.4.1.2)</iso:assert>
            <iso:assert test="count(atom:published) &lt;= 1">An atom entry cannot have more than
                one atom:published element. (RFC 4287, sec.4.1.2)</iso:assert>
            <iso:assert test="count(atom:source) &lt;= 1">An atom entry cannot have more than
                one atom:source element. (RFC 4287, sec.4.1.2)</iso:assert>
            <iso:assert test="count(atom:rights) &lt;= 1">An atom entry cannot have more than
                one atom:rights element. (RFC 4287, sec.4.1.2)</iso:assert>
            <iso:assert test="count(atom:summary) &lt;= 1">An atom entry cannot have more than
                one atom:summary element.</iso:assert>
            <iso:assert test="not(atom:subtitle)">An atom entry cannot have an atom:subtitle
                element. (RFC 4287, sec.4.1.2)</iso:assert>
        </iso:rule>

        <iso:rule context="atom:author">
            <iso:assert test="count(atom:name) = 1">An atom:author element must have one and only
                one atom:name element. (RFC 4287, sec.3.2)</iso:assert>
            <iso:assert test="count(atom:uri) &lt;= 1">An atom:author element cannot have more
                than one atom:uri element. (RFC 4287, sec.3.2)</iso:assert>
            <iso:assert test="count(atom:email) &lt;= 1">An atom:author element cannot have more
                than one atom:email element. (RFC 4287, sec.3.2)</iso:assert>
        </iso:rule>

        <iso:rule context="atom:contributor">
            <iso:assert test="count(atom:name) = 1">An atom:contributor element must have one and
                only one atom:name element. (RFC 4287, sec.3.2)</iso:assert>
            <iso:assert test="count(atom:uri) &lt;= 1">An atom:contributor element cannot have
                more than one atom:uri element. (RFC 4287, sec.3.2)</iso:assert>
            <iso:assert test="count(atom:email) &lt;= 1">An atom:contributor element cannot have
                more than one atom:email element. (RFC 4287, sec.3.2)</iso:assert>
        </iso:rule>

        <iso:rule context="atom:link">
            <iso:assert test="@href">An atom:link must have one and only one href attribute. (RFC
                4287, sec.4.2.7)</iso:assert>
        </iso:rule>
    </iso:pattern>


    <!-- Atom Publishing Protocol format rules -->
    <iso:pattern id="AtomPub">
        <iso:rule context="app:service">
            <iso:assert test="count(app:workspace) > 0">An app:service document must have at least
                one app:workspace element.</iso:assert>
        </iso:rule>

        <iso:rule context="app:workspace">
            <iso:assert test="count(atom:title) = 1">An app:worspace element must have one and only
                one atom:title element.</iso:assert>
        </iso:rule>

        <iso:rule context="app:collection">
            <iso:assert test="@href">The app:collection element must have an "href" attribute.</iso:assert>
            <iso:assert test="atom:title">The app:collection element must have one and only one
                atom:title element.</iso:assert>
        </iso:rule>

        <iso:rule context="app:collection/app:categories/@href">
            <iso:assert test="count(../atom:category) = 0">An out of line app:categories should not
                have atom:category children.</iso:assert>
        </iso:rule>

        <iso:rule context="app:collection/app:categories/@fixed">
            <iso:assert test="(. = 'yes') or (. = 'no')">When present the fixed attribute must be
                'yes' or 'no'.</iso:assert>
            <iso:assert test="not(../@href)">An app:categories element cannot have a fixed and href
                attribute at the same time.</iso:assert>
        </iso:rule>
    </iso:pattern>


    <!-- Atom Thread Extension format rules (RFC 4685) -->
    <iso:pattern id="RFC4685">
        <iso:rule context="thr:in-reply-to">
            <iso:assert test="@ref">A thr:in-reply-to element must have a "ref" attribute. (RFC4685,
                sec. 3)</iso:assert>
        </iso:rule>
    </iso:pattern>

</iso:schema>
