<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:pt="https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types"
	xmlns:cerif="https://purl.org/pe-repo/cerif-profile/1.0/"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
	
	<xsl:template match="cerif:Publication">	
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<fo:simple-page-master master-name="simpleA4"
					page-height="29.7cm" page-width="24cm" margin-top="2cm"
					margin-bottom="2cm" margin-left="1cm" margin-right="1cm">
					<fo:region-body />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:page-sequence master-reference="simpleA4">
				<fo:flow flow-name="xsl-region-body">
		         	<fo:block margin-bottom="5mm" padding="2mm">
						<fo:block font-size="26pt" font-weight="bold" text-align="center" >
							<xsl:value-of select="cerif:Title" />
						</fo:block>
					</fo:block>
			    	
					<fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
						<xsl:value-of select="cerif:Abstract" />
					</fo:block>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Información básica de la publicación'" />
			    	</xsl:call-template>
			    	
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Otros títulos'" />
				    	<xsl:with-param name="values" select="cerif:Subtitle" />
			    	</xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Fecha de publicación'" />
				    	<xsl:with-param name="value" select="cerif:PublicationDate" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'DOI'" />
				    	<xsl:with-param name="value" select="cerif:DOI" />
				    </xsl:call-template>
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'PubMed ID'" />
                        <xsl:with-param name="value" select="cerif:PMCID" />
                    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'ISBN'" />
				    	<xsl:with-param name="value" select="cerif:ISBN" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Número ISI'" />
				    	<xsl:with-param name="value" select="cerif:ISI-Number" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Número SCP'" />
				    	<xsl:with-param name="value" select="cerif:SCP-Number" />
			    	</xsl:call-template>
					<fo:block font-size="10pt" margin-top="2mm">
						<fo:inline font-weight="bold" text-align="right"  >
							<xsl:text>Authors: </xsl:text>
						</fo:inline >
						<fo:inline>
						<xsl:for-each select="cerif:Authors/cerif:Author">
							<xsl:value-of select="cerif:DisplayName" />
							<xsl:if test="cerif:Affiliation/cerif:OrgUnit/cerif:Name">
								( <xsl:value-of select="cerif:Affiliation/cerif:OrgUnit/cerif:Name"/> )
							</xsl:if>
						    <xsl:if test="position() != last()"> and </xsl:if>
						</xsl:for-each>
						</fo:inline >
					</fo:block>
					<fo:block font-size="10pt" margin-top="2mm">
						<fo:inline font-weight="bold" text-align="right"  >
							<xsl:text>Editors: </xsl:text>
						</fo:inline >
						<fo:inline>
						<xsl:for-each select="cerif:Editors/cerif:Editor">
							<xsl:value-of select="cerif:DisplayName" />
							<xsl:if test="cerif:Affiliation/cerif:OrgUnit/cerif:Name">
								( <xsl:value-of select="cerif:Affiliation/cerif:OrgUnit/cerif:Name"/> )
							</xsl:if>
						    <xsl:if test="position() != last()"> and </xsl:if>
						</xsl:for-each>
						</fo:inline >
					</fo:block>
					<xsl:call-template name="print-values">
				    	<xsl:with-param name="label" select="'Palabras clave'" />
				    	<xsl:with-param name="values" select="cerif:Keyword" />
			    	</xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Tipo'" />
				    	<xsl:with-param name="value" select="pt:Type" />
				    </xsl:call-template>
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Materia(s) OCDE'" />
                        <xsl:with-param name="value" select="cerif:Subject" />
                    </xsl:call-template>
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Licencia'" />
                        <xsl:with-param name="value" select="cerif:License" />
                    </xsl:call-template>
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Versión'" />
                        <xsl:with-param name="value" select="cerif:Version" />
                    </xsl:call-template>
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Disciplina'" />
                        <xsl:with-param name="value" select="cerif:Discipline" />
                    </xsl:call-template>
                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Asesores'" />
                        <xsl:with-param name="values" select="cerif:Advisors/cerif:Advisor/cerif:Person/cerif:Name" />
                    </xsl:call-template>
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Condición de acceso'" />
                        <xsl:with-param name="value" select="cerif:Access" />
                    </xsl:call-template>
                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Fecha de Fin del Embargo'" />
                        <xsl:with-param name="value" select="cerif:Access/@endDate" />
                    </xsl:call-template>
                    
                    
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Detalles bibliográficos de la publicación'" />
			    	</xsl:call-template>
			    	
			    	
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Publicado en'" />
				    	<xsl:with-param name="value" select="cerif:PublishedIn/cerif:Publication/cerif:Title" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'ISSN'" />
				    	<xsl:with-param name="value" select="cerif:ISSN" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Volumen'" />
				    	<xsl:with-param name="value" select="cerif:Volume" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Edición'" />
				    	<xsl:with-param name="value" select="cerif:Issue" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Página de inicio'" />
				    	<xsl:with-param name="value" select="cerif:StartPage" />
				    </xsl:call-template>
					<xsl:call-template name="print-value">
				    	<xsl:with-param name="label" select="'Página de fin'" />
				    	<xsl:with-param name="value" select="cerif:EndPage" />
				    </xsl:call-template>
			    	
			    	
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Proyectos'" />
			    	</xsl:call-template>
					<xsl:for-each select="cerif:OriginatesFrom/cerif:Project">
			    		<fo:block font-size="10pt" margin-top="2mm">
							<xsl:value-of select="cerif:Title" />
							<xsl:if test="cerif:Acronym">
								( <xsl:value-of select="cerif:Acronym"/> )
							</xsl:if>
						    <xsl:text> - </xsl:text>
						    <xsl:if test="cerif:StartDate">
						    	from <xsl:value-of select="cerif:StartDate"/>
						    </xsl:if>
						    <xsl:if test="cerif:EndDate">
						    	to <xsl:value-of select="cerif:EndDate"/>
						    </xsl:if>
						</fo:block>
					</xsl:for-each>
					
					<xsl:call-template name="section-title">
				    	<xsl:with-param name="label" select="'Financiadores'" />
			    	</xsl:call-template>
			    	<xsl:for-each select="cerif:OriginatesFrom/cerif:Funding">
			    		<fo:block font-size="10pt" margin-top="2mm">
							<xsl:value-of select="cerif:Name" />
							<xsl:if test="cerif:Acronym">
								( <xsl:value-of select="cerif:Acronym"/> )
							</xsl:if>
							<xsl:if test="cerif:Type">
						    	<xsl:text> - Type: </xsl:text>
						    	<xsl:value-of select="cerif:Type"/>
					    	</xsl:if>
						    <xsl:if test="cerif:Funder/cerif:OrgUnit/cerif:Name">
						    	<xsl:text> - Funder: </xsl:text>
						    	<xsl:value-of select="cerif:Funder/cerif:OrgUnit/cerif:Name"/>
						    </xsl:if>
						</fo:block>
					</xsl:for-each>

				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>
	
	<xsl:template name = "print-value" >
	  	<xsl:param name = "label" />
	  	<xsl:param name = "value" />
	  	<xsl:if test="$value">
		  	<fo:block font-size="10pt" margin-top="2mm">
				<fo:inline font-weight="bold" text-align="right" >
					<xsl:value-of select="$label" /> 
				</fo:inline >
				<xsl:text>: </xsl:text>
				<fo:inline>
					<xsl:value-of select="$value" /> 
				</fo:inline >
			</fo:block>
	  	</xsl:if>
	</xsl:template>
	
	<xsl:template name = "section-title" >
		<xsl:param name = "label" />
		<fo:block font-size="16pt" font-weight="bold" margin-top="8mm" >
			<xsl:value-of select="$label" /> 
		</fo:block>
		<fo:block margin-bottom="2mm" margin-top="-4mm">
			<fo:leader leader-pattern="rule" leader-length="100%" rule-style="solid" />         
		</fo:block>
	</xsl:template>
	
	<xsl:template name = "print-values" >
		<xsl:param name = "label" />
	  	<xsl:param name = "values" />
	  	<xsl:if test="$values">
		  	<fo:block font-size="10pt" margin-top="2mm">
				<fo:inline font-weight="bold" text-align="right"  >
					<xsl:value-of select="$label" /> 
				</fo:inline >
				<xsl:text>: </xsl:text>
				<fo:inline>
					<xsl:for-each select="$values">
					    <xsl:value-of select="current()" />
					    <xsl:if test="position() != last()">, </xsl:if>
					</xsl:for-each>
				</fo:inline >
			</fo:block>
		</xsl:if>
	</xsl:template>
	
</xsl:stylesheet>