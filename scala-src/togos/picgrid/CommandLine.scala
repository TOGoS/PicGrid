package togos.picgrid

trait CommandLine 
{
	def start( args:Array[String] ):Process
	
	def run( args:Array[String] ):Int = {
		start( args ).waitFor()
	}
}
