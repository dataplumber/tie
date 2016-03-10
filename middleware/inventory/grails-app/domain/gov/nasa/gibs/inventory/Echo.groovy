package gov.nasa.gibs.inventory

class Echo {

	String env
	String baseUrl
	
    static constraints = {
       env(nullable:false)
       baseUrl(nullable:false)
    }
    
    static mapping = {
       id generator: 'sequence', params: [sequence: 'echo_id_seq']
    }
}
