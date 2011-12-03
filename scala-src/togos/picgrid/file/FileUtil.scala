package togos.picgrid.file

import java.io.File

object FileUtil
{
	def makeParentDirs( f:File ) = {
		val parentFile = f.getParentFile() 
		if( parentFile != null && !parentFile.exists() ) parentFile.mkdirs()
	}
	
	def deltree( f:File ) {
		if( f.isDirectory() ) {
			for( sf <- f.listFiles() ) {
				deltree( sf )
			}
			f.delete()
		} else if( f.exists() ) {
			f.delete()
		}
	}
}
