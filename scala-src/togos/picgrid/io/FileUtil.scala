package togos.picgrid.io
import java.io.File

object FileUtil
{
	def makeParentDirs( f:File ) = {
		val parentFile = f.getParentFile() 
		if( parentFile != null && !parentFile.exists() ) parentFile.mkdirs()
	}
}
