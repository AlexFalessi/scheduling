<?xml version='1.0'?>
<!--
		DO NOT EDIT THIS FILE UNLESS YOU ARE IN THE DOCUMENTATION PROJECT

		This file is shared by all ProActive projects. If you have to modify it,
		please refer to the INSTALL file in the root of the Documentation project
		to know how to do it properly.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="1.0">



 <!--Import xsl file for highlightting-->
 <xsl:import href="../highlighting/xsl/common.xsl"/>
 

 	<xsl:param name="use.extensions" select="'1'" />
	<xsl:param name="linenumbering.extension" select="'1'" />
	<xsl:param name="tablecolumns.extension" select="'1'" />
	 <!-- Active highlighting-->
	<xsl:param name="highlight.source" select="1"/>
	<xsl:param name="linenumbering.everyNth" select="'1'"/>
	<xsl:param name="linenumbering.separator" select="':'"/>
	<xsl:param name="textinsert.extension" select="'1'" />


 <!-- Where should the titles of formal objects be placed? -->
 <xsl:param name="formal.title.placement">
  figure after 
  example after 
  equation after 
  table after 
  procedure after
 </xsl:param>

 <xsl:param name="TODAY" select="'$TODAY NOT SET'"/>   <!--This variable should be passed as an ant argument -->
 <xsl:param name="RELEASEDATE" />   <!--This variable should be passed as an ant argument -->

 <!-- if an empty toc element is found in a source document, an automated TOC is generated. -->
 <xsl:param name="process.empty.source.toc">1</xsl:param>
 <!-- the contents of a non-empty "hard-coded" toc element in a source document are processed to generate a TOC in output. -->
 <xsl:param name="process.source.toc">0</xsl:param>
 <!-- Turn on admonition graphics. -->
 <xsl:param name="admon.graphics" select="'1'" />
 <!--  TODO : are we going to use these callout graphics ? We're not using images here!-->
 <xsl:param name="callout.graphics">1</xsl:param>
 <xsl:param name="callout.graphics.path"></xsl:param>
 <xsl:param name="callout.list.table">1</xsl:param>
 <!-- force all sections to have a number assigned, like "1. First section"-->
 <xsl:param name="section.autolabel">1</xsl:param>
 <!-- stop labelling at the fourth nesting level -->
 <xsl:param name="section.autolabel.max.depth">4</xsl:param>
 <!-- sections bear the names of their inherited sections, like in "4.2.1.3. A subsubsubsection" -->
 <xsl:param name="section.label.includes.component.label">1</xsl:param>
 <!--  Force chapter 2 of part 3 to be labelled as Chap III.2 -->
 <!-- <xsl:param name="component.label.includes.part.label">1</xsl:param> -->

<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  -->
<!--  Which levels should be having a toc? I say : book, parts, appendixes and q&a only -->
<!--                    DANGER: don't put spaces after commas!                          -->
<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  -->
 <xsl:param name="generate.toc">
  appendix nop
  book figure,table,example,equation,toc
  article nop
  part toc
  chapter nop
  appendix toc
  qandaset toc
 </xsl:param>

 <!-- The header image -->
 <xsl:param name="header.image.filename">images/ProActiveLogoSmall.png</xsl:param>
 <!-- the 3 institutes images -->
 <xsl:param name="threeinstitutes.image.filename">images/logo-cnrs-inria-unsa.png</xsl:param>
 <!--  The objectweb logo -->
 <xsl:param name="objectweb.image.filename">images/logoOW2.png</xsl:param>


<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
<!-- Strange: affiliation and phone are docbook elements, and have no corresponding template!  -->
<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
<xsl:template match="affiliation">
       <xsl:apply-templates/>
</xsl:template>

<xsl:template match="phone|fax">
  <xsl:value-of select="name(.)"/>
  <xsl:text>: </xsl:text>
  <xsl:call-template name="inline.charseq"/>
</xsl:template>


<!-- This is an ugly hack: since no entity (&TODAY;) can be set on the saxon command-line, ie in the ant file, 
      entities only known at run-time are specified in the docbook xml by a <systemitem role="foo"/>.
     This template performs the replacement : (xml) <systemitem role="foo"/> <==> (ant file) <param name="foo" expression="foo_value"/> -->
<xsl:template match="systemitem">
   <xsl:choose>
     <xsl:when test="@role='VERSION'">v<xsl:copy-of select="$VERSION"/></xsl:when>
     <xsl:when test="@role='TODAY'"><xsl:copy-of select="$TODAY"/></xsl:when>
     <xsl:when test="@role='RELEASEDATE'"><xsl:copy-of select="$RELEASEDATE"/></xsl:when>
   </xsl:choose>
</xsl:template>

</xsl:stylesheet>
