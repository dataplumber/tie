package gov.nasa.gibs.tie.handlers.sftp

import gov.nasa.gibs.tie.handlers.sftp.SFTPConfigurator;
import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Created with IntelliJ IDEA.
 * User: thuang
 * Date: 8/30/13
 * Time: 1:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class SFTPProductTypeTest extends GroovyTestCase {
   private static Log logger = LogFactory.getLog(gov.nasa.gibs.tie.handlers.sftp.SFTPProductTypeTest.class)

   public void testSimple() {
      String[] args = ['-p', 'MORCR721LLDY', '-s', '2013-08-28']
      ApplicationConfigurator configurator = new gov.nasa.gibs.tie.handlers.sftp.SFTPConfigurator(args)
      assertFalse(configurator.hasError())

      Map<String, ProductType> pts = configurator.productTypes
      assertEquals(4, pts.size())
      assertTrue(pts['MORCR721LLDY'].ready)
   }

}
