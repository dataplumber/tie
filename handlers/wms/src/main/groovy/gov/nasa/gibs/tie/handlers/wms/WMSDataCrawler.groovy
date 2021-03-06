package gov.nasa.gibs.tie.handlers.wms

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.httpfetch.api.FetchException
import gov.nasa.horizon.common.httpfetch.api.HttpFetcher
import gov.nasa.horizon.common.httpfetch.api.HttpFileProduct
import gov.nasa.horizon.handlers.framework.FileHandler

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.SimpleDateFormat
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException
import java.util.regex.Pattern

import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.HttpEntity
import org.apache.http.util.EntityUtils

import groovy.json.JsonSlurper
import gov.nasa.horizon.handlers.framework.*
import gov.nasa.horizon.sigevent.api.EventType
import gov.nasa.horizon.sigevent.api.SigEvent

/**
 * Implementation of the WMS data crawler
 *
 * @author T. Huang
 * @version $Id: $
 */
class WMSDataCrawler extends TimerTask {

	private static Log log = LogFactory.getLog(WMSDataCrawler.class)

	private final HttpFetcher fetcher
	private FileHandler handler
	private List<String> fileExpressions
	private String cmrUrl
	private Iterator<String> wmsUrls
	private WMSProductType productType

	private SigEvent sigEvent
	private int timeout = 40// timeout value in seconds
	private int maxWmsRetries = 3

	/**
	 * Constructor to create a single service thread in the background
	 * for processing HTML results.
	 */
	public WMSDataCrawler() {
		this.fetcher = new HttpFetcher()
		this.fetcher.timeout = this.timeout
	}

	public WMSDataCrawler(WMSProductType productType,
			String cmrUrl,
			List<String> fileExpressions,
			FileHandler handler) {
		this.fetcher = new HttpFetcher()
		this.fetcher.timeout = this.timeout
		this.cmrUrl = cmrUrl
		this.fileExpressions = fileExpressions
		if (log.debugEnabled) {
			this.fileExpressions.each {
				log.debug("Expression: ${it}")
			}
		}
		this.handler = handler
		this.productType = productType
		this.sigEvent = new SigEvent(productType.sigEventURL)
	}
	
	public WMSDataCrawler(WMSProductType productType,
			Iterator<String> urls,
			List<String> fileExpressions,
			FileHandler handler) {
		this.fetcher = new HttpFetcher()
		this.fetcher.timeout = this.timeout
		this.wmsUrls = urls
		this.fileExpressions = fileExpressions
		if (log.debugEnabled) {
			this.fileExpressions.each {
				log.debug("Expression: ${it}")
			}
		}
		this.handler = handler
		this.productType = productType
		this.sigEvent = new SigEvent(productType.sigEventURL)
	}

	@Override
	public void run() {
		if(this.productType.detectionType.equals("NRT")) {
			while (this.wmsUrls.hasNext()) {
				String url = this.wmsUrls.next()
				this.walk(url, this.fileExpressions, this.handler)
			}
		} else if(this.productType.detectionType.equals("CMR")) {
			this.generateWmsUrls(this.cmrUrl, this.handler)
		} else {
			//Sigevent
			//skip this PT?
		}
		log.debug("exit run method")
	}

	public void setTimeout(int timeout) {
		this.fetcher.timeout = timeout
		this.timeout = timeout
	}

	public void generateWmsUrls(String cmrUrl, FileHandler handler) {
		CloseableHttpResponse response
		
		def retrievedDataIds = this.productType.cacheFile.retrievedDataIds
		
		def pageNum = 1
		cmrUrl += '&pageNum=' + pageNum
		def granulesHits = 0
		def totalGranulesProcessed = 0
		
		while(true){
			try{
				CloseableHttpClient httpclient = HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier()).build()
				HttpGet httpGet = new HttpGet(cmrUrl)
				httpGet.setHeader("Client-Id", "GIBS");
				response = httpclient.execute(httpGet)
                
                Integer statusCode = response.getStatusLine().getStatusCode()
                
                if(statusCode != 200) {
                    //WMS request errored for whatever reason. Log and move on to the next wms request loop
                    log.error("WMS URL returned a non-200 response: "+statusCode+" at URL: "+cmrUrl)
                    return
                }
	
				HttpEntity entity = response.getEntity()
	
				def cmrResult = new JsonSlurper().parseText(EntityUtils.toString(entity))
				if(pageNum == 1 ) {
					granulesHits = response.getFirstHeader("CMR-Hits").getValue().toInteger()
					log.debug("${granulesHits} total granules found!")
				}
				log.debug("granules retrieved from: ${cmrUrl}")

				boolean updateLastRetrieved = this.productType.startDate == null || (this.productType.startDate >= new Date().clearTime())
				log.debug("about to save cache file in DataCrawler.generateWmsUrls(). pt.startDate: ${this.productType.startDate} updateLastRetrieved: ${updateLastRetrieved}")
				this.productType.cacheFile.save(null, "${this.productType.workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${this.productType.name}.cache.xml", updateLastRetrieved)
				
				
				if(granulesHits > 0) {
					cmrResult.feed.entry.each { def result ->
						
						//Parse the granule's updated date into a long so we can compare it against the cached time
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SS'Z'")
						Date parsedDate = format.parse(result.updated.toString())
						long updatedTimestamp = parsedDate.getTime()
										
						/*Only download imagery if:
						*    Handler is running in batch mode.
						*    Handler is running in extended daemon mode & the data we're currently processing is older than the current datetime.
						*    Handler is running in daemon mode AND...
						*       cache contains no granuleIDs (clean cache) OR
						*       cache does not contain this granuleID OR
						*       cache contains an older version of this granule.
						*/
						if(this.productType.batch || (updatedTimestamp < System.currentTimeMillis()) || retrievedDataIds == null || (!retrievedDataIds.contains(result.id.toString()) || updatedTimestamp > this.productType.cacheFile.lastRetrieved) ) {
							Date parsedStartDate = format.parse(result.time_start.toString())
							String wmsUrl = this.productType.sourceURL.replace('${time}', this.productType.wmsDateTimeFormat.format(parsedStartDate).toString())
							
							if(this.productType.batch) {
								log.debug("Running in batch mode. Processing granule: ${result.id.toString()}")
							} else {
								log.debug("Found new/updated granule: ${result.id.toString()}")
							}
							
							this.handler.setGranuleId(result.id)
							this.walk(wmsUrl, this.fileExpressions, this.handler)
						} else{
							log.trace("Imagery for granuleID ${result.id.toString()} already downloaded, skip it!")
						}
						totalGranulesProcessed++
					}
				}
				pageNum++
				cmrUrl = cmrUrl.replaceAll("\\d+\$", pageNum.toString()) 
	
			} catch(Exception e) {
				log.error("EXCEPTION while processing WMS response: " + e + " | " + e.stackTrace)
			} finally {
				response.close()
			}

			log.debug("total granules processed: ${totalGranulesProcessed}")
			if(granulesHits == 0 || totalGranulesProcessed >= granulesHits)
				break
		}
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

		log.debug("Pulling from WMS URL: ${rootURL}")

		expressions.each {
			log.debug("Compile reg: ${it}")
			patterns.add(Pattern.compile(it))
			log.debug("Done Compiling ${it}")
		}

		log.debug("Create callable object")

		//

		// create the callable object to be executed within the Thread.
		//Callable<Integer> callable = new Callable<Integer>() {
		Runnable runnable = new Runnable() {

					private int _count = 0

					/**
			 * This is a recursive method to process through the WMS site until
			 * URLs that fit the specified regular expression is found.
			 *
			 * @param rootURL the top-level URL to start retrieve HTML from
			 * @return the number of URLs found
			 */
					public synchronized int traverse(String url) {
			
						try {
							Set<FileProduct> files = new HashSet<>()
							HttpFileProduct hfp
							def retries = 1
							
							while (retries <= maxWmsRetries) {
								try{
									log.debug ("Attempt ${retries}...Pulling from URL ${url}")
									hfp = fetcher.createFileProduct(url)
									retries = maxWmsRetries+1 //If we successfully created the HttpFileProduct, then stop retries.
								} catch(FetchException e) {
									if (retries == maxWmsRetries) {
										++retries
										this.sigEvent.create(EventType.Error, productType.name, 'WMSDataHandler',
											'WMSDataHandler',
											InetAddress.localHost.hostAddress,
											"WMS failure",
											null,
											"Unable to retrieve data from WMS endpoint: ${url}, failed 3 times.")
										return 0
									} else {
										log.debug(e.getMessage(), e)
										log.info("WMS call failed to URL ${url}.\nHandler will retry WMS call...")
										sleep(30000) // sleep 30 sec and retry
										++retries
									}
								}
							}
							String datestr = (url =~ /\d{4}-\d{2}-\d{2}/)[0]
							hfp.name = "${productType.name}_${datestr}${productType.fileExtension}"
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
						} catch (Exception e) {
							log.info(e.getMessage(), e)
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


	//   public void retrieveGranules(String baseUrl) {
	//	   HTTPBuilder http = new HTTPBuilder(baseUrl)
	//
	//	   try {
	//		   http.get(path: "/$relativePath/productType/$ptId", contentType: ContentType.XML) { resp, xml ->
	//			  log.debug "http - GET - /inventory/productType/"
	//			  if (resp.getStatus() != 200) {
	//				 log.debug("Status returned non-200 code: ${resp.getStatus()}; ${xml.text()}")
	//			  } else {
	//				 // DO SOMETHING WITH "xml" NOW (its a string)
	//			  }
	//		   }
	//		}
	//		catch (HttpResponseException e) {
	//			log.debug ("Caught exception!")
	//		   //log.debug ("Unable to find ProductType with ID ${ptId}.  ${e.message}")
	//		}
	//		catch (Exception e) {
	//		   throw new InventoryException(e.getMessage())
	//		}
	//   }


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
