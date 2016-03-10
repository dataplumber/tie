package gov.nasa.horizon.inventory

class CollectionImagerySet {
   
   ProductType pt
   
   static belongsTo = [collection:Collection]
   
   static constraints = {
      pt(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'collection_imagery_set_id_seq']
   }
}
