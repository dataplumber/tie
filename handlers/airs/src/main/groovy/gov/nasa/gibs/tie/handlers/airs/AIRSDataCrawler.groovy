package gov.nasa.gibs.tie.handlers.airs

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.httpfetch.api.FetchException
import gov.nasa.horizon.common.httpfetch.api.HttpFetcher
import gov.nasa.horizon.common.httpfetch.api.HttpFileProduct
import gov.nasa.horizon.handlers.framework.FileHandler
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.SimpleDateFormat
import java.util.concurrent.ExecutionException
import java.util.regex.Pattern

/**
 * Implementation of the AIRS data crawler
 *
 * @author T. Huang
 * @version $Id: $
 */
class AIRSDataCrawler extends TimerTask {

   private static Log log = LogFactory.getLog(AIRSDataCrawler.class)

   private final HttpFetcher fetcher
   private FileHandler handler
   private List<String> fileExpressions
   private Iterator<String> urls
   private AIRSProductType productType

   // timeout value in seconds
   private int timeout = 20

   /**
    * Constructor to create a single service thread in the background
    * for processing HTML results.
    */
   public AIRSDataCrawler() {
      this.fetcher = new HttpFetcher()
      this.fetcher.timeout = this.timeout
   }

   public AIRSDataCrawler(AIRSProductType productType,
                          Iterator<String> urls,
                          List<String> fileExpressions,
                          FileHandler handler) {
      this.fetcher = new HttpFetcher()
      this.fetcher.timeout = this.timeout
      this.urls = urls
      this.fileExpressions = fileExpressions
      if (log.debugEnabled) {
         this.fileExpressions.each {
            log.debug("Expression: ${it}")
         }
      }
      this.handler = handler
      this.productType = productType
   }

   @Override
   public void run() {
      log.debug("inside run method")
      while (this.urls.hasNext()) {
         String url = this.urls.next()
         this.walk(url, this.fileExpressions, this.handler)
      }
      log.debug("exit run method")
   }

   public void setTimeout(int timeout) {
      this.fetcher.timeout = timeout
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

      List<Pattern> patterns = []

      log.debug("inside walk method: ${rootURL}")

      expressions.each {
         log.debug("Compile reg: ${it}")
         patterns.add(Pattern.compile(it))
         log.debug("Done Compiling ${it}")
      }

      log.debug("Create callable object")

      // create the callable object to be executed within the Thread.
      //Callable<Integer> callable = new Callable<Integer>() {
      Runnable runnable = new Runnable() {

         private int _count = 0

         /**
          * This is a recursive method to process through the modap site until
          * URLs that fit the specified regular expression is found.
          *
          * @param rootURL the top-level URL to start retrieve HTML from
          * @return the number of URLs found
          */
         public synchronized int traverse(String url) {
            try {
               Set<FileProduct> files = new HashSet<>()

               log.debug ("Pulling from URL ${url}")
               HttpFileProduct hfp = fetcher.createFileProduct(url)
               String datestr = (url =~ /\d{4}-\d{2}-\d{2}/)[0]
               hfp.name = "${productType.name}_${datestr}.png"
               SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
			   
			   Calendar preShiftedDate = Calendar.getInstance()
			   preShiftedDate.setTime(sdf.parse(datestr))
			   Calendar gmtDate = new GregorianCalendar(TimeZone.getTimeZone("GMT"))
			   gmtDate.set(preShiftedDate.get(Calendar.YEAR), preShiftedDate.get(Calendar.MONTH), preShiftedDate.get(Calendar.DAY_OF_MONTH), 0, 0, 0) 
			   
               hfp.lastModifiedTime = gmtDate.getTimeInMillis()

               files.add(hfp)
               ++_count
               log.debug("Found: " + hfp.name)
               if (handler != null) {
                  handler.process(files.toList())
                  files.clear()
                  handler.postprocess()
               }
            } catch (FetchException e) {
               log.debug(e.getMessage(), e)
            }
            return this._count
         }

         /**
          * The is the hook method call by the Executor to start the thread.
          *
          * @return return the number of URLs found.
          * @throws Exception
          */
         //public Integer call() throws Exception {
         public void run() {
            this.traverse(rootURL)
         }

      }

      runnable.run()
   }

   /**
    * Method to wait for the working thread to finish and return the number of
    * URLs found.
    *
    * @return the number of URLs found or -1 if an error is encountered.
    */
   public int checkResult() {
      int result = -1
      try {
         this.thread.get()
      } catch (InterruptedException | ExecutionException e) {
         if (this.thread == null)
            log.debug("this.thread is null")
         log.debug(e.message, e)
      }
      return result
   }

   /**
    * Method to clear all memory resources allocated by this walker.
    */
   public void shutdown() {
      if (this.fetcher != null) {
         this.fetcher.shutdown()
      }
   }
}
