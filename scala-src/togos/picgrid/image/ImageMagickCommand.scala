package togos.picgrid.image

import togos.picgrid.CommandLineProgram
import togos.picgrid.CommandLineExecutor

object ImageMagickCommands
{
	var convertPath:String = "/usr/bin/convert"
	
	var convert = new CommandLineProgram( Array(convertPath), CommandLineExecutor.instance )
}
