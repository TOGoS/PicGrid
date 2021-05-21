package togos.picgrid

class CommandLineExecutor extends CommandLine
{
	def start( args:Array[String] ):Process = {
		GlobalContext.logExternalCommand(args);
		Runtime.getRuntime().exec(args);
	}
}
object CommandLineExecutor
{
	var instance = new CommandLineExecutor 
}
