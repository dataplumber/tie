package gov.nasa.horizon.inventory

class ProductDatetime {

   Long valueLong

   static belongsTo = [pe:ProductElement, product:Product]
   
   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_datetime_id_seq']
   }
}
