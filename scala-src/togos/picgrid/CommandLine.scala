package togos.picgrid

trait CommandLine 
{
	def start( args:Array[String] ):Process
	
	def run( args:Array[String] ):Integer = {
		start( args ).waitFor()
	}
}
