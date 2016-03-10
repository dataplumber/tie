package gov.nasa.gibs.inventory
import gov.nasa.horizon.inventory.ProductType

class DatasetImagery {

   Dataset dataset
   ProductType pt

   static constraints = {
      dataset(nullable:false)
      pt(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'dataset_imagery_id_seq']
   }
}
