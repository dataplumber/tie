package gov.nasa.horizon.inventory

class ProductTypeReal {

   BigDecimal value
   String units

   static belongsTo = [pt:ProductType, pte:ProductTypeElement]
   
   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_real_id_seq']
   }
}
