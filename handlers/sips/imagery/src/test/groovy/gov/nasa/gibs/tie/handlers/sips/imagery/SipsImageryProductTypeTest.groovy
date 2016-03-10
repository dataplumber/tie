/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 *
 * @author T. Huang
 * @version $Id:$
 */
public class SipsImageryProductTypeTest extends GroovyTestCase {
   private static Log logger = LogFactory.getLog(SipsImageryProductTypeTest.class)

   public void testSimple() {
      String[] args = ['-p', 'MYG04_LTD_ODLO', '-s', '2013-08-28']
      ApplicationConfigurator configurator = new SipsImageryConfigurator(args)
      assertFalse(configurator.hasError())

      Map<String, ProductType> pts = configurator.productTypes
      assertEquals(1, pts.size())
      assertTrue(pts['MYG04_LTD_ODLO'].ready)
   }

}
