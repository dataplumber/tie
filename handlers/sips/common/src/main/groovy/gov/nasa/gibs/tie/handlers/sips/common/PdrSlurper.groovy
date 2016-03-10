/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.common

import groovy.util.slurpersupport.GPathResult

/**
 * @author T. Huang
 * @version $Id:$
 */
class PdrSlurper {

   public GPathResult parseText (String text) {
      def sw = new StringWriter()

      sw.write '<PDR>\n'
      text.eachLine { line ->
         def m = line =~ /\s*([\w|\W]+)\s*=\s*([\w|\W]+)\s*;/
         def key = m[0][1].trim()
         def value = m[0][2].trim()
         if (key == 'OBJECT') {
            sw.write ("<OBJECT>\n")
            sw.write ("<OBJECT_NAME>${value}</OBJECT_NAME>\n")
         } else if (key == 'END_OBJECT') {
            sw.write "</OBJECT>\n"
         } else {
            sw.write "<${key}>${value}</${key}>\n"
         }
      }
      sw.write '</PDR>\n'

      return new groovy.util.XmlSlurper().parseText(sw.toString())
   }
}
