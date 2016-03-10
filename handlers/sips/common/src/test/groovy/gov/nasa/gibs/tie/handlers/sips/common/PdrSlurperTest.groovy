/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.common

/**
 * @author T. Huang
 * @version $Id:$
 */
class PdrSlurperTest extends GroovyTestCase{

   void testSimpleRead1() {
      String testPDR = System.getProperty('common.test.path')  + '/MODAPSops8.6067375.PDR'
      File f = new File(testPDR)
      def pdr = new PdrSlurper().parseText(f.text)
      assertEquals(pdr.ORIGINATING_SYSTEM, 'MODAPS_TERRA_FPROC')
      assertEquals(pdr.ORIGINATING_SYSTEM[0], 'MODAPS_TERRA_FPROC')
      assertEquals(pdr.OBJECT.size(), 56)
      assertEquals(pdr.OBJECT[0].DATA_TYPE, 'MYG04_LTD_ODLO')
      assertEquals(pdr.OBJECT[0].OBJECT.FILE_ID, 'MYG04_LTD_ODLO.A2013343.global.006.2013353122801.tar.gz')
   }

   void testSimpleRead2() {
      String testPDR = System.getProperty('common.test.path')  + '/MODAPSops6.342617.PDR'
      File f = new File(testPDR)
      def pdr = new PdrSlurper().parseText(f.text)
      assertEquals(pdr.ORIGINATING_SYSTEM, 'MODAPS_TERRA_RPROC')
      assertEquals(pdr.ORIGINATING_SYSTEM[0], 'MODAPS_TERRA_RPROC')
      assertEquals(pdr.OBJECT.size(), 18)
      assertEquals(pdr.OBJECT[0].DATA_TYPE, 'MYG09_LHD_143')
      assertEquals(pdr.OBJECT[0].OBJECT.FILE_ID, 'MYG09_LHD_143.A2003004.tiled.006.2014184212830.tar.gz')
   }


   void testSimpleRead3() {
      String testPDR = System.getProperty('common.test.path')  + '/20101231_NSIDC0051_NORTH.pdr'
      File f = new File(testPDR)
      def pdr = new PdrSlurper().parseText(f.text)
      assertEquals(pdr.ORIGINATING_SYSTEM, 'NSIDC')
      assertEquals(pdr.ORIGINATING_SYSTEM[0], 'NSIDC')
      assertEquals(pdr.OBJECT.size(), 1)
      assertEquals(pdr.OBJECT[0].DATA_TYPE, 'NSIDC0051')
      assertEquals(pdr.OBJECT[0].OBJECT.FILE_ID, '20101231_NSIDC0051_NORTH.tar.gz')
   }

}
