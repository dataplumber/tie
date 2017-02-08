package gov.nasa.gibs.tie.handlers.common

import groovy.xml.MarkupBuilder
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.xml.sax.SAXException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created with IntelliJ IDEA.
 * User: thuang
 * Date: 8/29/13
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
class CacheFileInfo {
   private static Log _logger = LogFactory.getLog(CacheFileInfo.class)

   String product
   String name
   long modified
   long size
   String checksumAlgorithm
   String checksumValue

   boolean equals(o) {
      if (this.is(o)) return true
      if (getClass() != o.class) return false

      CacheFileInfo that = (CacheFileInfo) o

      if (modified != that.modified) return false
      if (size != that.size) return false
      
      if (checksumAlgorithm) {
         if (!checksumAlgorithm.equals(that.checksumAlgorithm)) return false
         if (!checksumValue.equals(that.checksumValue)) return false
      }
	  
      if (!name.equals(that.name)) return false
      if (!product.equals(that.product)) return false

      return true
   }

   int hashCode() {
      int result
      result = product.hashCode()
      result = 31 * result + name.hashCode()
      result = 31 * result + (int) (modified ^ (modified >>> 32))
      result = 31 * result + (int) (size ^ (size >>> 32))
      result = 31 * result + (checksumAlgorithm != null ? checksumAlgorithm.hashCode() : 0)
      result = 31 * result + (checksumValue != null ? checksumValue.hashCode() : 0)
      return result
   }

    
    
    //new function to retrieve the last modified date for whole cache xml file
    
   public static List<CacheFileInfo> load(String cachefile, int cacheRetention = 30) throws IOException {
	  boolean deletedCacheEntry = false
	  Set<String> pgwsToDelete = new HashSet<String>()
	  
	  def crawlercache
      List<CacheFileInfo> result = []
      if (!new File(cachefile).exists()) {
         return result
      }
      try {
         crawlercache = new XmlSlurper().parseText(new File(cachefile).text)
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
			
			Pattern p = Pattern.compile("(\\S)*(?=\\.)")
			Matcher m = p.matcher(info.name)
			String baseFileName = ""
			if(m.find()) {baseFileName = m.group(0)}
			
			Calendar cal = Calendar.getInstance()
			cal.add(Calendar.DATE, -1*cacheRetention)
			if( info.modified < cal.getTimeInMillis() || pgwsToDelete.contains(baseFileName)) {
				deletedCacheEntry = true
				pgwsToDelete.add(baseFileName)
			} else {
				result << info
			}
         }
		 if(deletedCacheEntry) {
			 save(result, cachefile)
		 }
		 
         _logger.trace(result)
      } catch (IOException | SAXException e) {
         _logger.error("Unable to process cache file ${cachefile}. Handler will now terminate.", e)
         System.exit(-1)
      }
      return result
   }

   public static void save(List<CacheFileInfo> fileInfos, String cachefile)
   throws IOException {

      def path
      def filename
      if (cachefile.indexOf(File.separator) < 0) {
         path = System.getProperty('user.dir')
         filename = cachefile
      } else {
         path = cachefile.substring(0, cachefile.lastIndexOf(File.separator))
         filename = cachefile.substring(cachefile.lastIndexOf(File.separator)+1, cachefile.length())
      }

      // since the cache could get very big, writing it to disk is a time consuming
      // operation and if the program execution halts it could result in
      // cache corruption.  By first writing the cache into a shadow file
      // and perform an atomic move in the end will prevent ths corruption.
      def shadowDir = "${path}${File.separator}.shadow${File.separator}"

      if (!(new File(shadowDir).exists())) {
         new File(shadowDir).mkdirs()
      }

      def shadowCacheFile = "${shadowDir}${filename}"

      def writer = new FileWriter(new File(shadowCacheFile))
      def xml = new MarkupBuilder(writer)
      xml.crawlercache() {		 
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

      // the writing is complete.  replace the old cache file with new
      new File(shadowCacheFile).renameTo(cachefile)
   }
   
   public boolean update(CacheFileInfo newFileInfo) {
	   if( product.equals(newFileInfo.product) && name.equals(newFileInfo.name) ) {
		   _logger.trace("Updating cache entry for: ${newFileInfo.name}")
		   
		   modified = newFileInfo.modified
		   size = newFileInfo.size
		   checksumAlgorithm = newFileInfo.checksumAlgorithm
		   checksumValue = newFileInfo.checksumValue
		   
		   return true
	   }
	   return false
   }

}
