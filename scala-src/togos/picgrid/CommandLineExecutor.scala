package togos.picgrid

class CommandLineExecutor extends CommandLine
{
	def start( args:Array[String] ):Process = {
		/*
		System.err.print("$")
		for( arg <- args ) {
			System.err.print(" "+arg)
		}
		System.err.println()
		*/
		
		Runtime.getRuntime().exec(args);
	}
}
object CommandLineExecutor
{
	var instance = new CommandLineExecutor 
}
