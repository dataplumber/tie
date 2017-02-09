package gov.nasa.gibs.tie.handlers.sftp

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.file.FileProductHandler
import gov.nasa.horizon.common.crawler.spider.Crawler
import gov.nasa.horizon.common.crawler.spider.CrawlerManager
import gov.nasa.horizon.common.crawler.spider.provider.VFSCrawler
import gov.nasa.horizon.common.crawler.spider.provider.VFSCrawlerManager
import gov.nasa.horizon.handlers.framework.FileHandler
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of the gov.nasa.gibs.tie.handlers.sftp.SFTP data crawler
 *
 * @author T. Huang
 * @version $Id: $
 */
class SFTPDataCrawler extends TimerTask {

   private static Log log = LogFactory.getLog(gov.nasa.gibs.tie.handlers.sftp.SFTPDataCrawler.class)

   private CrawlerManager manager = new VFSCrawlerManager()
   private FileHandler handler
   private List<String> fileExpressions
   private Iterator<String> urls
   private Boolean stop = false
   private gov.nasa.gibs.tie.handlers.sftp.SFTPProductType productType

   // timeout value in seconds
   private int timeout = 20

   /**
    * Constructor to create a single service thread in the background
    * for processing HTML results.
    */
   public SFTPDataCrawler() {
      this.manager.start()
   }

   public SFTPDataCrawler(gov.nasa.gibs.tie.handlers.sftp.SFTPProductType productType,
                         Iterator<String> urls,
                         List<String> fileExpressions,
                         FileHandler handler) {
      this.urls = urls
      this.fileExpressions = fileExpressions
      if (log.debugEnabled) {
         this.fileExpressions.each {
            log.debug("Expression: ${it}")
         }
      }
      this.handler = handler
      this.productType = productType
      this.manager.start()
   }

   public synchronized void setStop(Boolean stop) {
      this.stop = stop
   }

   @Override
   public void run() {
      log.debug("inside run method")
      while (this.urls.hasNext()) {
         String url = this.urls.next()
         this.walk(url, this.fileExpressions, this.handler)
         log.debug("Done walking: ${url}")
      }
   }

   public void setTimeout(int timeout) {
      this.timeout = timeout
   }

   /**
    * The method to perform HTTP Walk.  This is a non-blocking method,
    * that is, the input walk request is immediately queue to the background
    * service thread.  This allows the calling program to perform other
    * operations while results are being processed and files are being
    * downloads.
    *
    * @param rootURL the starting top-level URL
    * @param expressions the regular expression map <regex,
    *                    file type> to filter
    *                    embedded URLs.  If this parameter is NULL,
    *                    then all URLs will be returned.
    * @param handler the HTTP file handler.  This handler will be called when
    *                    an URL that fits the specified regular expression is found.
    */
   public void walk(final String rootURL, final List<String> expressions,
                    final FileHandler handler) {

	try{
      log.debug("inside walk method")
      Crawler crawler = new VFSCrawler(rootURL, this.productType.interval);
      crawler.setAuthentication(productType.user, productType.pass)
      crawler.registerProductHandler(handler as FileProductHandler)
      crawler.registerProductSelector(new gov.nasa.horizon.common.api.file.FileFilter(expressions as String[]))
      crawler.init()
      if (this.productType.batch) {
         Set<FileProduct> fps = crawler.crawl()
      } else {
         manager.replaceCrawler(crawler)
      }
	} catch(Exception e) {
		log.info("EXCEPTION caught in walk method: ${e}")
	}

   }

   /**
    * Method to clear all memory resources allocated by this walker.
    */
   public void shutdown() {
      this.stop = true
      this.manager.stop()
   }
}
