package gov.nasa.gibs.inventory

class OgcTileMatrixSet {

   String identifier

   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'ogc_tile_matrix_set_id_seq']
   }
}
