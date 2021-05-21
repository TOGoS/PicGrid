package togos.picgrid

object GlobalContext {
	var debuggingEnabled : Boolean = false
	var tolerateComposeErrors : Boolean = false

	def debug( message : String ) {
		if( this.debuggingEnabled ) {
			System.err.println("# Debug: "+message);
		}
	}
	def warn( message : String ) {
		System.err.println("# Warning: "+message);
	}
	def logExternalCommand( args : Array[String] ) {
		if( this.debuggingEnabled ) {
			System.err.println("# Exec: "+CommandLine.argumentsToString(args))
		}
	}
}
