package gov.nasa.gibs.inventory

class OgcTileMatrix {

   String identifier
   BigDecimal scaleDenominator
   Integer topLeftCornerX
   Integer topLeftCornerY
   Integer tileWidth
   Integer tileHeight
   Integer matrixWidth
   Integer matrixHeight

   static belongsTo = [tms:OgcTileMatrixSet]

   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'ogc_tile_matrix_id_seq']
   }
}
