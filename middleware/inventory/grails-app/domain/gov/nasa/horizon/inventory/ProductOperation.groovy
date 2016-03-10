package gov.nasa.horizon.inventory

class ProductOperation {

   Product product
   String agent
   String operation
   Long startTime
   Long stopTime
   String command
   String arguments

   static belongsTo = [product:Product]

   static constraints = {
      agent(nullable:false)
      operation(nullable:false)
      startTime(nullable:false)
      stopTime(nullable:false)
      command(nullable:true)
      arguments(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_operation_id_seq']
   }
}
