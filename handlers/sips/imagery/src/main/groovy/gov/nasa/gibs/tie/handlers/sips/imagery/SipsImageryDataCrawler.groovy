/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

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
 * Implementation of the SipsImagery data crawler
 *
 * @author T. Huang
 * @version $Id: $
 */
class SipsImageryDataCrawler extends TimerTask {

   private static Log log = LogFactory.getLog(SipsImageryDataCrawler.class)

   private CrawlerManager manager = new VFSCrawlerManager()
   private FileHandler handler
   private List<String> fileExpressions
   private Iterator<String> urls
   private SipsImageryProductType productType
   private Boolean stop = false


   // timeout value in seconds
   private int timeout = 20

   /**
    * Constructor to create a single service thread in the background
    * for processing HTML results.
    */
   public SipsImageryDataCrawler() {
      this.manager.start()
   }

   public SipsImageryDataCrawler(SipsImageryProductType productType,
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

   @Override
   public void run() {
      log.debug("inside run method")
      while (this.urls.hasNext() && !this.stop) {
         String url = this.urls.next()
         log.trace("${url} -> ${this.fileExpressions}")
         this.walk(url, this.fileExpressions, this.handler)
      }
      log.debug("exit run method")
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
   public synchronized void walk(final String rootURL, final List<String> expressions,
                    final FileHandler handler) {

      log.debug("inside walk method")
      Crawler crawler = new VFSCrawler(rootURL) //, this.productType.interval);
      //crawler.recursive = true
      //crawler.registerProductHandler(handler as FileProductHandler)
      crawler.registerProductSelector(new gov.nasa.horizon.common.api.file.FileFilter(expressions as String[]))
      crawler.init()
      //if (this.productType.batch) {
         Set<FileProduct> fps = crawler.crawl()
      //} else {
      //  manager.replaceCrawler(crawler)
      //}
      
         if (log.traceEnabled) {
            log.trace ("Dump crawler result: ${productType}:${expressions} ->")
            fps.each {
              log.trace (it.name)
            }
         }
         this.handler.preprocess()
         this.handler.process(fps as List)
         this.handler.postprocess()
         
      //} else {
      //if (!this.productType.batch)
      //   manager.replaceCrawler(crawler)
      //}
   }

   /**
    * Method to clear all memory resources allocated by this walker.
    */
   public void shutdown() {
      this.stop = true
      this.manager.stop()
   }
}
