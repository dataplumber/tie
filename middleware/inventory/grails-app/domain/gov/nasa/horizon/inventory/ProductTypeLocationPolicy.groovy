package gov.nasa.horizon.inventory

class ProductTypeLocationPolicy {

   String type
   String basePath
   
   //Custom enum
   
   static belongsTo = [pt:ProductType]

   static constraints = {
      type(nullable:false, inList:[
         "ARCHIVE-OPEN",
         "LOCAL-FTP",
         "LOCAL-HTTP",
         "LOCAL-LINK",
         "REMOTE-FTP",
         "REMOTE-HTTP"
      ])
      basePath(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_location_policy_id_seq']
   }
   
}
