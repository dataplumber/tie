package gov.nasa.gibs.tie.handlers.sips.common

/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
import groovy.util.slurpersupport.GPathResult
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @author T. Huang
 * @version $Id:$
 */
class MetSlurper {
   private static Log logger = LogFactory.getLog(MetSlurper.class)

   public GPathResult parseText(String text) {

      def sw = new StringWriter()
      sw.write '<MET>\n'
      text.eachLine { line ->
         def m = line =~ /\s*([\w|\W]+)\s*=\s*([\w|\W]+)\s*/
         if (m.find()) {
            def key = m[0][1].trim()
            def value = m[0][2].trim()
            if (key == 'GROUP') {
               sw.write "<GROUP>\n"
               sw.write "<GROUP_NAME>${value}</GROUP_NAME>\n"
            } else if (key == 'END_GROUP') {
               sw.write "</GROUP>\n"
            } else if (key == 'OBJECT') {
               sw.write '<OBJECT>\n'
               sw.write "<OBJECT_NAME>${value}</OBJECT_NAME>\n"
            } else if (key == 'END_OBJECT') {
               sw.write "</OBJECT>\n"
            } else if (value =~ /\(([\w|\W]+)\)/) {
               value = value =~ /\(([\w|\W]+)\)/
               value[0][1].split(',').each {
                  sw.write "<${key}>${it.trim()}</${key}>\n"
               }
            } else if (value =~ /\((\s*)\)/) {
               sw.write "<${key}/>\n"
            } else {
               sw.write "<${key}>${value}</${key}>\n"
            }
         }
      }
      sw.write '</MET>\n'
      logger.trace(sw.toString())
      return new groovy.util.XmlSlurper().parseText(sw.toString())

   }
}
