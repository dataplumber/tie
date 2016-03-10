package gov.nasa.horizon.inventory

import gov.nasa.gibs.inventory.Projection;

class ProductTypeMetadata {

   String project
   String instrument
   String platform
   String processingLevel
   String dataVersion
   String regionCoverage
   String dayNight
   String ascDesc
   String scienceParameter
   Integer nativeResolution
   Integer displayResolution
   
   Projection sourceProjection
   Projection targetProjection
   
   static belongsTo = [pt:ProductType]
   
   static constraints = {
      project(nullable:true)
      instrument(nullable:true)
      platform(nullable:true)
      processingLevel(nullable:true)
      dataVersion(nullable:true)
      regionCoverage(nullable:true)
      dayNight(nullable:true)
      ascDesc(nullable:true)
      scienceParameter(nullable:true)
      nativeResolution(nullable:true)
      displayResolution(nullable:true)
      sourceProjection(nullable:true)
      targetProjection(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_metadata_id_seq']
   }
}
