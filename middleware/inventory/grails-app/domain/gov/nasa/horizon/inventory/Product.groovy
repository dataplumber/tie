package gov.nasa.horizon.inventory

import gov.nasa.gibs.inventory.GranuleImagery

class Product {

   String name
   Long startTime
   Long stopTime
   Long createTime
   Long archiveTime
   Integer versionNum
   String status
   String rootPath
   String relPath
   String partialId 
   ProductType pt

   //static belongsTo = [pt:ProductType]

   static hasMany = [
      archive:ProductArchive, 
      metaHistory:ProductMetaHistory, 
      references:ProductReference, 
      operations:ProductOperation,
      elements:ProductElement,
      productCharacter:ProductCharacter,
      productDatetime:ProductDatetime,
      productInteger:ProductInteger,
      productReal:ProductReal,
      granuleImagery:GranuleImagery,
	  dataDay:ProductDataDay
   ]
   
   static mapping = {
      version("revision")
      versionNum(column:"version")
      id generator: 'sequence', params: [sequence: 'product_id_seq']
   }
   
   static constraints = {
      name(nullable:false)
      startTime(nullable:false)
      stopTime(nullable:true)
      createTime(nullable:true)
      archiveTime(nullable:true)
      versionNum(nullable:false)
      status(nullable:false)
      rootPath(nullable:true)
      relPath(nullable:true)
	  partialId(nullable:true)
      pt(nullable:false)
   }
}
