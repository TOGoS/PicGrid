package togos.picgrid.image

import java.io.File
import java.lang.Integer
import scala.collection.mutable.HashMap
import togos.picgrid.file.FileUtil.makeParentDirs
import togos.picgrid.file.FSDatastore
import togos.picgrid.file.FSSHA1Datastore
import togos.picgrid.file.FileBlob
import togos.picgrid.CommandLine
import togos.picgrid.FunctionCache
import togos.picgrid.MemoryFunctionCache

/**
 * Generates fake gray+white images for use when resizing fails,
 * e.g. due to corrupted input files.
 */
class ImageMagickFallbackSource( val datastore:FSDatastore, val imConvert:CommandLine )
	extends ((String,Int,Int)=>String)
{
	def imArgs( w:Int, h:Int, outFile:File ) = Array[String](
		"-size",(w+"x"+h),
		"-fill","white",
		"xc:gray",
		"-draw","polygon 0,0 "+w+",0 0,"+h,
		"-quality","85",
		outFile.getPath()
	)
	
	def apply( origUri:String, w:Int, h:Int ):String = {
		val tempFile = datastore.tempFile(".jpg")
		makeParentDirs( tempFile )
		val args = imArgs(w, h, tempFile)
		if( imConvert.start( args ).waitFor() != 0 ) {
			throw new Exception("Convert failed to create fallback image with arguments: "+CommandLine.argumentsToString(args))
		}
		return datastore.storeAndRemove( tempFile )
	}
}

/**
 * Use this one when you need images converted to an exact size and don't mind
 * the edges being snipped off.
 */
class ImageMagickCropResizer( val datastore:FSDatastore, val imConvert:CommandLine, val fallbackImageSource:(String,Int,Int)=>String )
	extends ImageResizer
{
	def this( datastore:FSDatastore, imConvert:CommandLine ) = this(datastore,imConvert,null)
	
	def imArgs( infile:File, newWidth:Int, newHeight:Int, outFile:File ) = Array[String](
		infile.getPath(),
		"-filter","Box", // See note in ImageMagickResizer
		"-thumbnail",(newWidth+"x"+newHeight+"^"),
		"-gravity","Center",
		"-extent",(newWidth+"x"+newHeight),
		"-quality","85",
		outFile.getPath()
	)
	
	def resize( infile:File, newWidth:Int, newHeight:Int, outFile:File ):Process = {
		makeParentDirs( outFile )
		imConvert.start( imArgs( infile, newWidth, newHeight, outFile ) )
	}
		
	def resize( origUri:String, boxWidth:Int, boxHeight:Int ):String = {
		val origBlob = datastore( origUri )
		if( origBlob == null ) throw new Exception("Couldn't find "+origUri)
		if( !origBlob.isInstanceOf[FileBlob] ) throw new Exception("Can only work with file blobs; got a "+origBlob.getClass())
		val tempFile = datastore.tempFile(".jpg")
		val args = imArgs( origBlob.asInstanceOf[FileBlob].getFile(), boxWidth, boxHeight, tempFile )
		makeParentDirs( tempFile )
		val imResult = imConvert.start( args ).waitFor()
		if( imResult != 0 ) {
			if( fallbackImageSource != null ) {
				return fallbackImageSource( origUri, boxWidth, boxHeight )
			} else {
				throw new RuntimeException("convert returned non-zero status: "+imResult+" for arguments "+CommandLine.argumentsToString(args) )
			}
		}
		val len = tempFile.length()
		return datastore.storeAndRemove( tempFile )
	}
}
object ImageMagickCropResizer
{
	def main( args:Array[String] ) {
		var datastoreDir:File = null
		var inFilename:String = null
		var outFile:File = null
		var w = 512
		var h = 384
		
		var i = 0
		while( i < args.length ) {
			if( "-datastore".equals(args(i)) ) {
				i += 1
				datastoreDir = new File(args(i))
			} else if( "-w".equals(args(i)) ) {
				i += 1
				w = Integer.parseInt(args(i))
			} else if( "-h".equals(args(i)) ) {
				i += 1
				h = Integer.parseInt(args(i))
			} else if( "-o".equals(args(i)) ) {
				i += 1
				outFile = new File(args(i))
			} else if( !args(i).startsWith("-") ) {
				inFilename = args(i)
			} else {
				throw new RuntimeException("Unrecognised argument: "+args(i))
			}
			i += 1
		}
		
		val hm = new HashMap[(String,String),String]()
		hm( ("x","y") ) = "Z"
		hm.isDefinedAt( ("x","y") )
		hm( ("x","y") )
		
		val functionCache:FunctionCache = new MemoryFunctionCache()
		val datastore = if( datastoreDir == null ) null else new FSSHA1Datastore(datastoreDir)
		
		val imr = new ImageMagickCropResizer( datastore, ImageMagickCommands.convert )
		if( datastore == null ) {
			if( inFilename == null ) throw new RuntimeException("No input file specified")
			if( outFile == null ) throw new RuntimeException("No output file specified")
			
			imr.resize( new File(inFilename), w, h, outFile )
		} else {
			val thumbUri = imr.resize( inFilename, w, h )
			System.out.println( "Resize+crop result = "+thumbUri )
		}
	}
}
