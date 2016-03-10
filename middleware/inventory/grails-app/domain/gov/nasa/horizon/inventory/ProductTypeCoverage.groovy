package gov.nasa.horizon.inventory

class ProductTypeCoverage {

   Float northLatitude
   Float southLatitude
   Float eastLongitude
   Float westLongitude
   
   Long startTime
   Long stopTime

   static belongsTo = [pt:ProductType]

   static constraints = {
      northLatitude(nullable:true)
      southLatitude(nullable:true)
      eastLongitude(nullable:true)
      westLongitude(nullable:true)
      startTime(nullable:true)
      stopTime(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_coverage_id_seq']
   }
}
