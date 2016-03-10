package gov.nasa.horizon.inventory

class ProductInteger {

   Integer value
   String units

   static belongsTo = [pe:ProductElement, product:Product]
   
   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_integer_id_seq']
   }
}
