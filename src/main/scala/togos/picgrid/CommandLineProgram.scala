package togos.picgrid

class CommandLineProgram( val initArgs:Array[String], val cmdRunner:CommandLine ) extends CommandLine
{
	def start( args:Array[String] ):Process = {
		cmdRunner.start( initArgs ++ args )
	}
}
