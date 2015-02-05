package togos.picgrid

object CommandLine
{
	/**
	 * For debugging/human-readable argument strings.
	 */
	def argumentsToString( args:Seq[String] ):String = {
		var s = ""
		for( a <- args ) s += "\"" + a + "\" "
		return s
	}
}
trait CommandLine 
{
	def start( args:Array[String] ):Process
	
	def run( args:Array[String] ):Int = {
		start( args ).waitFor()
	}
}
