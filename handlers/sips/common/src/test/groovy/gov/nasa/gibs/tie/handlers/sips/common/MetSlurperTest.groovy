/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.common

/**
 * @author T. Huang
 * @version $Id:$
 */
class MetSlurperTest extends GroovyTestCase{

   void testSimpleRead1() {
      String testMET = System.getProperty('common.test.path')  + '/MYG04_LTD_ODLO.A2013342.global.006.2013353122813.met'
      File f = new File(testMET)
      def met = new MetSlurper().parseText(f.text)

      assertEquals (met.GROUP[0].GROUP_NAME, 'INVENTORYMETADATA')
      assertEquals (met.GROUP[0].GROUP.size(), 6)

      def inputGranule = met.GROUP[0].GROUP.find {
         it.GROUP_NAME == 'INPUTGRANULE'
      }

      assertNotNull(inputGranule)
      assertEquals(inputGranule.OBJECT[0].VALUE.size(), 129)
   }

   void testSimpleRead2() {
      String testMET = System.getProperty('common.test.path')  + '/MYG09_LHD_143.A2003003.r00c00.006.2014184214640.met'
      File f = new File(testMET)
      def met = new MetSlurper().parseText(f.text)

      assertEquals (met.GROUP[0].GROUP_NAME, 'INVENTORYMETADATA')
      assertEquals (met.GROUP[0].GROUP.size(), 6)

      def inputGranule = met.GROUP[0].GROUP.find {
         it.GROUP_NAME == 'INPUTGRANULE'
      }

      assertNotNull(inputGranule)
      assertEquals(inputGranule.OBJECT[0].VALUE.size(), 1)
   }

   void testSimpleRead3() {
      String testMET = System.getProperty('common.test.path')  + '/MYG09_LQD_121.A2003002.r00c00.006.2014184214230.met'
      File f = new File(testMET)
      def met = new MetSlurper().parseText(f.text)

      assertEquals (met.GROUP[0].GROUP_NAME, 'INVENTORYMETADATA')
      assertEquals (met.GROUP[0].GROUP.size(), 6)

      def inputGranule = met.GROUP[0].GROUP.find {
         it.GROUP_NAME == 'INPUTGRANULE'
      }

      assertNotNull(inputGranule)
      assertEquals(inputGranule.OBJECT[0].VALUE.size(), 9)
   }

   void testSimpleRead4() {
      String testMET = System.getProperty('common.test.path')  + '/MYG09_LHD_143.A2003003.r00c14.006.2014184214640.met'
      File f = new File(testMET)
      def met = new MetSlurper().parseText(f.text)
   }

   void testSimpleRead5() {
      String testMET = System.getProperty('common.test.path')  + '/test_SMAP_L3_SM_AP_20140629_D05030_000.h5.met'
      File f = new File(testMET)
      def met = new MetSlurper().parseText(f.text)

      assertEquals (met.GROUP[0].GROUP_NAME, 'INVENTORYMETADATA')
      assertEquals (met.GROUP[0].GROUP.size(), 4)

      def inputGranule = met.GROUP[0].GROUP.find {
         it.GROUP_NAME == 'INPUTGRANULE'
      }

      assertNotNull(inputGranule)
      assertEquals(inputGranule.OBJECT[0].VALUE.size(), 1)
   }

   void testSimpleRead6() {
      String testMET = System.getProperty('common.test.path')  + '/SMAP_L4_SM_gph_20140115T013000_V05006_001_sm_rootzone_wetness.met'
      File f = new File(testMET)
      def met = new MetSlurper().parseText(f.text)

      assertEquals (met.GROUP[0].GROUP_NAME, 'INVENTORYMETADATA')
      assertEquals (met.GROUP[0].GROUP.size(), 5)

      def inputGranule = met.GROUP[0].GROUP.find {
         it.GROUP_NAME == 'INPUTGRANULE'
      }

      assertNotNull(inputGranule)
      assertEquals(inputGranule.OBJECT[0].VALUE.size(), 1)
   }

   void testSimpleRead7() {
      String testMET = System.getProperty('common.test.path')  + '/MISR_AM1_GRP_ELLIPSOID_GM_T20150706160512_P017_O082705_AN_F03_0024_RGB.tiff.met'
      File f = new File(testMET)
      def met = new MetSlurper().parseText(f.text)

      assertEquals (met.GROUP[0].GROUP_NAME, 'INVENTORYMETADATA')
      assertEquals (met.GROUP[0].GROUP.size(), 4)

      def inputGranule = met.GROUP[0].GROUP.find {
         it.GROUP_NAME == 'INPUTGRANULE'
      }

      assertNotNull(inputGranule)
      assertEquals(inputGranule.OBJECT[0].VALUE.size(), 1)
      println met
   }

}
