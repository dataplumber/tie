package gov.nasa.horizon.inventory

class ProductReference {

   String name
   String path
   String type
   String status
   String description
   
   static belongsTo = [product:Product]

   static constraints = {
      name(nullable:true)
      path(nullable:false)
      type(nullable:true)
      status(nullable:false)
      description(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_reference_id_seq']
   }
}
