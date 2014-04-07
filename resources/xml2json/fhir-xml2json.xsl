<?xml version="1.0"?>

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fh="http://hl7.org/fhir"
                xmlns:saxon="http://saxon.sf.net/"
                xpath-default-namespace="http://hl7.org/fhir">

  <xsl:variable name="elementsDoc" select="document('fhir-elements.xml')"/>
  <xsl:key name="element-by-path" match="*:element" use="@path" />

  <!-- this element declares that output will be plain text -->
  <xsl:output method="text" encoding="UTF-8" media-type="text/plain"/>

  <!-- normalizes path using nameRefs in fhir-elements.xml. So

       Questionnaire.group.group.group.question.group.question.text
       becomes
       Questionnaire.group.question.text -->
  <xsl:function name="fh:normalize-path">
    <xsl:param name="pathIn" />
    <xsl:param name="pathOut" />

    <xsl:choose>
      <xsl:when test="string-length($pathIn) = 0">
        <xsl:value-of select="$pathOut" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="tokenizedPathIn" select="tokenize($pathIn,
                                                     '\.')" />

        <xsl:variable name="nameRef"
                      select="key('element-by-path', $pathOut,
                              $elementsDoc)[1]/*:nameRef/@value" />

        <xsl:choose>
          <xsl:when test="$nameRef">
            <xsl:value-of select="fh:normalize-path($pathIn,
                                  $nameRef)" />
          </xsl:when>

          <xsl:otherwise>
            <xsl:variable name="newPathIn"
                          select="string-join(subsequence($tokenizedPathIn,
                                  2, count($tokenizedPathIn) - 1),
                                  '.')" />

            <xsl:variable name="newPathOut"
                          select="string-join(($pathOut,
                                  $tokenizedPathIn[1]), '.')" />

            <xsl:value-of select="fh:normalize-path($newPathIn,
                                  replace($newPathOut, '^\.', ''))" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- resolves path using complex types, so
       Questionnaire.group.question.name.coding becomes
       CodeableConcept.coding -->
  <xsl:function name="fh:resolve-path">
    <xsl:param name="path" />
    <xsl:param name="pathTail" />

    <xsl:variable name="tokenizedPath" select="tokenize($path, '\.')"
                  />

    <xsl:if test="string-length($path) = 0">
      <xsl:message terminate="yes">
        Zero-length $path passed, stopping.
      </xsl:message>
    </xsl:if>

    <!-- fhir element currently pointed by $path -->
    <xsl:variable name="el" select="key('element-by-path', $path, $elementsDoc)[1]" />

    <!-- type of current fhir element -->
    <xsl:variable name="type" select="$el/*:type/@value" />

    <xsl:choose>
      <xsl:when test="$type and string-length($pathTail) = 0">
        <xsl:value-of select="$path" />
      </xsl:when>

      <xsl:when test="$type and string-length($pathTail) > 0">
        <xsl:choose>
          <xsl:when test="starts-with($type, 'Resource(')">
            <xsl:variable name="newPath" select="concat('ResourceReference.', $pathTail)" />
            <xsl:value-of select="fh:resolve-path($newPath, '')[1]" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:variable name="newPath" select="concat($type, '.', $pathTail)" />
            <xsl:value-of select="fh:resolve-path($newPath, '')[1]" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>

      <!-- element with $path found, but no type was specified for
           example, Questionnaire.group. In such case just return
           current $path -->
      <xsl:when test="$el">
        <xsl:value-of select="$path" />
      </xsl:when>

      <xsl:otherwise>
        <xsl:variable name="newPath"
                      select="string-join(remove($tokenizedPath,
                              count($tokenizedPath)), '.')" />

        <xsl:variable name="newPathTail"
                      select="replace(concat($tokenizedPath[count($tokenizedPath)],
                              '.', $pathTail), '\.$', '')" />

        <xsl:value-of select="fh:resolve-path($newPath, $newPathTail)[1]" />
      </xsl:otherwise>
    </xsl:choose>


  </xsl:function>

  <xsl:function name="fh:get-json-type">
    <xsl:param name="path" />

    <xsl:variable name="resolvedPath" select="fh:resolve-path($path, '')" />
    <xsl:variable name="el" select="key('element-by-path', $resolvedPath,
                                    $elementsDoc)[1]" />

    <!-- <xsl:message> -->
    <!--   <xsl:value-of select="$path" /> => <xsl:value-of -->
    <!--   select="$resolvedPath" />: <xsl:value-of select="$el/*:type/@value" /> -->
    <!-- </xsl:message> -->

    <!-- type of current fhir element -->
    <xsl:value-of select="$el/*:type/@value" />
  </xsl:function>

  <xsl:template name="element">
    <xsl:param name="path" />

    <xsl:choose>
      <!-- if we have @value attr in current element, just output it
           -->
      <xsl:when test="@value">
        <xsl:variable name="type"
                      select="fh:get-json-type(fh:normalize-path($path,
                              ''))" />

        <xsl:call-template name="value">
          <xsl:with-param name="type" select="$type" />
          <xsl:with-param name="value" select="@value" />
        </xsl:call-template>
      </xsl:when>

      <xsl:otherwise>
        <!-- open curly brace if this element contains child nodes &
             doesn't have @value -->

        <xsl:variable name="isObject" select="not(./@value) and count(./*) > 0" />
        <xsl:if test="$isObject">{</xsl:if>

        <xsl:if test="$path = local-name()">
          "resourceType": "<xsl:value-of select="local-name()" />",

          <!-- emit _id attribute, if needed -->
          <xsl:if test="@id">
            "_id": "<xsl:value-of select="@id" />",
          </xsl:if>
        </xsl:if>

        <xsl:for-each-group select="*" group-by="local-name()">
          <xsl:variable name="currentName"
                        select="name(current-group()[1])" />

          <!-- normalized path of current element -->
          <xsl:variable name="currentPath"
                        select="fh:normalize-path(concat($path, '.',
                                $currentName), '')" />

          <xsl:variable name="max"
                        select="key('element-by-path',
                                fh:resolve-path($currentPath, ''),
                                $elementsDoc)[1]/*:max/@value"
                        />

          <xsl:variable name="isArray"
                        select="$max = '*' or
                                (current-group()[1]/@value and
                                count(current-group()) > 1)" />

          <!-- output JSON attribute -->
          "<xsl:value-of select="name()" />":

          <!-- open array if needed -->
          <xsl:if test="$isArray">[</xsl:if>

          <xsl:for-each select="current-group()">
            <xsl:choose>
              <!-- special case for 'text' element -->
              <xsl:when test="local-name() = 'text' and
                              not(contains($path, '.'))">
                <xsl:call-template name="text" />
              </xsl:when>

              <!-- special case for 'contained' resource -->
              <xsl:when test="local-name() = 'contained' and
                              not(contains($path, '.'))">
                <xsl:call-template name="contained" />
              </xsl:when>

              <!-- ignore extensions for now -->
              <xsl:when test="local-name() = 'extension'">
                <xsl:message>
                  <xsl:text>WARNING: ignoring 'extension' element</xsl:text>
                </xsl:message>

                <xsl:text>null</xsl:text>
              </xsl:when>

              <!-- for all elements except 'text' we just recursively
                   call 'element' template -->
              <xsl:otherwise>
                <xsl:call-template name="element">
                  <xsl:with-param name="path"
                                  select="concat($path, '.',
                                          local-name())" />
                </xsl:call-template>
              </xsl:otherwise>
            </xsl:choose>

            <xsl:if test="position() != last()">,</xsl:if>
          </xsl:for-each>

          <!-- close array, if needed -->
          <xsl:if test="$isArray">]</xsl:if>

          <!-- insert comma if this is not last element -->
          <xsl:if test="position() != last()">,</xsl:if>
        </xsl:for-each-group>

        <xsl:if test="$isObject">}</xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="contained">
    <xsl:for-each select="*">
      <xsl:call-template name="element">
        <xsl:with-param name="path"><xsl:value-of select="name()" /></xsl:with-param>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <!-- outputs 'text' element -->
  <xsl:template name="text">
    <xsl:text>{</xsl:text>

    <xsl:if test="./status">
      <xsl:text>"status": </xsl:text>
      <xsl:call-template name="value">
        <xsl:with-param name="type" select="'string'" />
        <xsl:with-param name="value" select="./status/@value" />
      </xsl:call-template>
      <xsl:text>, </xsl:text>
    </xsl:if>

    <xsl:text>"div": </xsl:text>

    <!-- serialize div element to string -->
    <xsl:variable name="serializedHtml">
      <xsl:apply-templates select="./*:div" mode="serialize" />
    </xsl:variable>

    <xsl:call-template name="value">
      <xsl:with-param name="type" select="'string'" />
      <xsl:with-param name="value" select="$serializedHtml" />
    </xsl:call-template>

    <xsl:text>}</xsl:text>
  </xsl:template>

  <!-- outputs JSON value: string, boolean or numeric -->
  <xsl:template name="value">
    <xsl:param name="type" />
    <xsl:param name="value" />

    <xsl:choose>
      <xsl:when test="$type = 'boolean' or
                      $type = 'decimal' or
                      $type = 'integer' ">
        <xsl:value-of select="$value" />
      </xsl:when>
      <xsl:otherwise>           <!-- string fallback -->
        <xsl:text>"</xsl:text>

        <!-- escape double quotes in string, escape newlines too,
             remove linefeeds, escape tabs -->
        <xsl:value-of select="replace(replace(replace($value, '&quot;',
                              '\\&quot;'), '\r?\n', '\\n'), '\t', '\\t')" />
        <xsl:text>"</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- it's our entry point, just find root element and apply
       'element' template to it -->
  <xsl:template match="/*[1]">
    <xsl:call-template name="element">
      <xsl:with-param name="path"><xsl:value-of select="name()" /></xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <!-- set of templates to serialize XHTML to string
       http://stackoverflow.com/questions/6696382/xslt-how-to-convert-xml-node-to-string-->
  <xsl:template match="*" mode="serialize">
    <xsl:text>&lt;</xsl:text>
    <xsl:value-of select="name()"/>
    <xsl:apply-templates select="@*" mode="serialize" />
    <xsl:choose>
      <xsl:when test="node()">
        <xsl:text>&gt;</xsl:text>
        <xsl:apply-templates mode="serialize" />
        <xsl:text>&lt;/</xsl:text>
        <xsl:value-of select="name()"/>
        <xsl:text>&gt;</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text> /&gt;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="@*" mode="serialize">
    <xsl:text> </xsl:text>
    <xsl:value-of select="name()"/>
    <xsl:text>="</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>"</xsl:text>
  </xsl:template>

  <xsl:template match="text()" mode="serialize">
    <xsl:value-of select="."/>
  </xsl:template>
</xsl:stylesheet>
