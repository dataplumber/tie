package gov.nasa.horizon.inventory

class ProductCharacter {

   String value

   static belongsTo = [pe:ProductElement, product:Product]

   static constraints = {
      value(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_character_id_seq']
   }
}
