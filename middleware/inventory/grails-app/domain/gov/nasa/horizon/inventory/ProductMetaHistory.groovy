package gov.nasa.horizon.inventory

class ProductMetaHistory {

   Integer versionId
   Long creationDate
   Long lastRevisionDate
   String revisionHistory
   //Product product
   
   static belongsTo = [product:Product]
   
   static constraints = {
      versionId(nullable:false)
      creationDate(nullable:false)
      lastRevisionDate(nullable:false)
      revisionHistory(nullable:false)
      product(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_meta_history_id_seq']
   }
}
