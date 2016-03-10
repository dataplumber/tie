/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.pdr

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @author T. Huang
 * @version $Id:$
 */
public class PDRProductTypeTest extends GroovyTestCase {
   private static Log logger = LogFactory.getLog(PDRProductTypeTest.class)

   public void testSimple() {
      String[] args = ['-u', 'gibstest', '-p', 'gibstest', '-t', 'SIPS_PDR', '-s', '2013-08-28']
      ApplicationConfigurator configurator = new PDRConfigurator(args)
      assertFalse(configurator.hasError())

      Map<String, ProductType> pts = configurator.productTypes
      println pts
      assertEquals(1, pts.size())
      assertTrue(pts['SIPS_PDR'].ready)
   }

}
