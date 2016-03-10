package gov.nasa.horizon.inventory

class ProductTypeResource {

   String name
   String path
   String type
   String description

   static belongsTo = [pt:ProductType]

   static constraints = {
      name(nullable:false)
      path(nullable:false)
      type(nullable:false)
      description(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_resource_id_seq']
   }
}
