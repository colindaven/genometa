<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://biodas.org/documents/das2"  
    xmlns:das2="http://biodas.org/documents/das2"  
>
<xsl:output method="xml" encoding="UTF-8"/> 

<xsl:template match="/das2:TYPES/das2:TYPE[ @title='cytoBand' or 
                                           @title='cytoband' or 
                                           @title='__cytobands' or 
                                           @title='refseq'
                                           ]"  >

   <xsl:copy>
       <xsl:apply-templates select="@*|node()"/>
       <PROP key="load_hint" value="Whole Sequence" />
   </xsl:copy>
</xsl:template>

<xsl:template match="node()|@*">
  <xsl:copy>
  <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<!-- 
<xsl:namespace-alias
  stylesheet-prefix="das2"
  result-prefix=""
/>
-->

</xsl:stylesheet> 

