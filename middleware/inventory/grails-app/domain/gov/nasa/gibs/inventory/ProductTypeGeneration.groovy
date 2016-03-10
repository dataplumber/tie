package gov.nasa.gibs.inventory
import gov.nasa.horizon.inventory.ProductType

class ProductTypeGeneration {
   
   Integer outputSizeX
   Integer outputSizeY
   Integer overviewScale
   Integer overviewLevels
   String overviewResample
   String resizeResample
   String reprojectionResample
   String vrtNodata
   Integer mrfBlockSize
   
   static belongsTo = [pt:ProductType]
   
   static constraints = {
      outputSizeX(nullable:true)
      outputSizeY(nullable:true)
      overviewScale(nullable:true)
      overviewLevels(nullable:true)
      overviewResample(nullable:true, inList:["nearest","average","gauss","cubic","average_mp","average_magphase","mode","avg"])
      resizeResample(nullable:true, inList:["near","bilinear","cubic","cubicspline","lanczos","average","mode","none"])
      reprojectionResample(nullable:true, inList:["near","bilinear","cubic","cubicspline","lanczos","average","mode"])
      vrtNodata(nullable:true)
      mrfBlockSize(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_generation_id_seq']
   }
}
