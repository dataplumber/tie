package gov.nasa.gibs.tie.handlers.cmr

import java.io.IOException
import java.util.List
import org.xml.sax.SAXException
import groovy.xml.MarkupBuilder
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import gov.nasa.gibs.tie.handlers.common.CacheFileInfo


class CMRCacheFile {
	private static Log _logger = LogFactory.getLog(CMRCacheFile.class)

	private List<CacheFileInfo> cacheEntries
	private long lastRetrieved //Timestamp of the last time the handler retrieved data
	Set<String> retrievedDataIds //List of IDs associated with the last retrieved data

	public CMRCacheFile(Set<String> retrievedDataIds) {
		this.cacheEntries = new ArrayList<CacheFileInfo>()
		this.retrievedDataIds = retrievedDataIds
		//lastRetrieved = System.currentTimeMillis()
	}
	
	public void load(String cachefile) throws IOException {
		_logger.debug("Inside CacheFileInfo.load method, loading ${cachefile}")
		this.retrievedDataIds = new HashSet<String>()
		
		def crawlercache
		//TODO do something if file doesn't exist?
		if (new File(cachefile).exists()) {
			try {
				crawlercache = new XmlSlurper().parseText(new File(cachefile).text)
				
				this.lastRetrieved = Long.parseLong(crawlercache.lastRetrieved.toString())
				
				crawlercache.retrieveddataids.id.each { currId ->
					this.retrievedDataIds.add(currId)		
				}
				
				crawlercache.fileinfo.each { fileinfo ->
					CacheFileInfo info = new CacheFileInfo()
					info.product = fileinfo.product as String
					info.name = fileinfo.name as String
					info.modified = Long.parseLong(fileinfo.modified.toString())
					info.size = Long.parseLong(fileinfo.size.toString())
					if (fileinfo.checksum) {
						info.checksumAlgorithm = fileinfo.checksum.algorithm as String
						info.checksumValue = fileinfo.checksum.value as String
					}
					this.cacheEntries.add(info)
				}
			} catch (Exception e) {
				_logger.error("Unable to process cache file ${cachefile}", e)
				throw new IOException(e)
			}
		}
	}

	public void save(ArrayList<CacheFileInfo> fileInfos, def cachefile)
	throws IOException {
		if(fileInfos == null || fileInfos.size == 0) {
			fileInfos = this.cacheEntries
		}
		this.lastRetrieved = System.currentTimeMillis()
		
		def writer = new FileWriter(new File(cachefile))
		def xml = new MarkupBuilder(writer)
		xml.crawlercache() {
			
			lastRetrieved(this.lastRetrieved)
			retrieveddataids() {
				retrievedDataIds.each { currId ->
					_logger.debug("writing granuleID to cache file: ${currId}")
					id(currId)	
				}
			}
			
			fileInfos.each { fi ->
				fileinfo() {
					product(fi.product)
					name(fi.name)
					modified(fi.modified)
					size(fi.size)
					if (fi.checksumAlgorithm) {
						checksum() {
							algorithm(fi.checksumAlgorithm)
							value(fi.checksumValue)
						}
					}
				}
			}
		}
	}
	
	public void addDataId(String id) {
		if(this.retrievedDataIds == null || this.retrievedDataIds.size() < 1) {
			this.retrievedDataIds = new HashSet<String>()
		}
		
		this.retrievedDataIds.add(id)
	}
}