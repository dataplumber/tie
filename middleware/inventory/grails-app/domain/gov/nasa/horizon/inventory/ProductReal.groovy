package gov.nasa.horizon.inventory

class ProductReal {

   BigDecimal value
   String units

   static belongsTo = [pe:ProductElement, product:Product]

   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_real_id_seq']
   }
}
