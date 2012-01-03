package togos.picgrid.app

import togos.picgrid.store.MigratingStore
import togos.picgrid.FunctionCache
import togos.picgrid.file.SLFFunctionCache
import togos.picgrid.file.SLF2FunctionCache
import togos.picgrid.MemoryFunctionCache
import scala.collection.mutable.ListBuffer
import togos.picgrid.image.ImageMagickCommands
import java.io.File
import togos.picgrid.file.FSSHA1Datastore
import java.io.Writer
import java.io.OutputStreamWriter
import togos.picgrid.file.FileUtil
import java.io.FileWriter
import togos.picgrid.image.ImageInfoExtractor
import togos.picgrid.image.ImageMagickCropResizer
import togos.picgrid.RowlyGridificationMethod
import togos.picgrid.Gridifier
import togos.picgrid.CompoundImageRasterizer
import togos.picgrid.CompoundImageHTMLizer

object GridifyCommand
{
	def getCache( dir:String, name:String ):FunctionCache = {
		if( dir != null ) {
			new MigratingStore(
				new SLFFunctionCache( new File(dir+"/"+name+".slf") ),
				new SLF2FunctionCache( new File(dir+"/"+name+".slf2") )
			)
		} else {
			new MemoryFunctionCache()
		}
	}
	
	def main( args:Array[String] ) {
		var datastoreDir:String = null
		var datasources:ListBuffer[String] = new ListBuffer[String]()
		var functionCacheDir:String = null
		var verbose = false
		var i = 0
		var target:String = null
		var refStoragePath:String = null
		while( i < args.length ) {
			args(i) match {
				case "-v" =>
					verbose = true
				case "-convert-path" =>
					i += 1
					ImageMagickCommands.convertPath = args(i)
				case "-function-cache-dir" =>
					i += 1
					functionCacheDir = args(i)
				case "-datastore" =>
					i += 1
					datastoreDir = args(i) 
				case "-datasource" =>
					i += 1
					datasources += args(i)
				case "-ms-datasource" => // Multi-sector datasource (e.g. ccouch/data)
					i += 1
					val msd = new File(args(i))
					for( f <- msd.listFiles() ) if( f.isDirectory() ) {
						datasources += f.getPath();
					}
				case "-resource-log" =>
					i += 1
					refStoragePath = args(i)
				case arg if !arg.startsWith("-") =>
					target = arg
				case arg =>
					System.err.println("Error: Unrecognised argument: "+arg)
					System.exit(1)
			}
			i += 1
		}
		
		if( datastoreDir == null ) {
			throw new RuntimeException("No -datastore directory specified")
		}
		val datastore = new FSSHA1Datastore(new File(datastoreDir), datasources.toList)
		if( target == null ) {
			System.err.println("Error: You must specify a target")
			System.exit(1)
		}
		
		var resource:Process = null
		var refWriter:Writer = null
		val refLogger:String=>Unit = if( refStoragePath == null ) {
			((a:String) => {})
		} else if( refStoragePath.equals("-") ) {
			((a:String) => System.out.println(a) )
		} else if( refStoragePath.startsWith("|") ) {
			val command = refStoragePath.substring(1).trim()
			((a:String) => {
				if( refWriter == null ) {
					resource = Runtime.getRuntime().exec(command)
					refWriter = new OutputStreamWriter( resource.getOutputStream() )
					new Thread() {
						override def run() {
							val is = resource.getInputStream()
							var z = 0
							val buf = new Array[Byte](65536)
							while( z != -1 ) {
								z = is.read( buf )
								if( z > 0 ) System.out.write( buf, 0, z )
							}
							is.close()
						}
					}.start()
				}
				refWriter.write( a + "\n" )
				refWriter.flush()
			})
		} else {
			((a:String) => {
				if( refWriter == null ) {
					val refFile:File = new File(refStoragePath)
					FileUtil.makeParentDirs( refFile )
					refWriter = new FileWriter( refFile )
				}
				refWriter.write( a + "\n" )
			})
		}
		
		val imageInfoExtractor = new ImageInfoExtractor( getCache(functionCacheDir, "image-dimensions"), datastore )
		val resizer = new ImageMagickCropResizer( datastore, ImageMagickCommands.convert )
		val gridificationMethod = new RowlyGridificationMethod
		val gridifier = new Gridifier( getCache(functionCacheDir, "gridification"), datastore, imageInfoExtractor, gridificationMethod )
		val rasterizer:(String=>String) = new CompoundImageRasterizer( getCache(functionCacheDir, "rasterize"), datastore, imageInfoExtractor, resizer, ImageMagickCommands.convert )
		val loggingRasterizer = { a:String =>
			val res = rasterizer(a)
			refLogger(res)
			res
		}
		
		val htmlizer = new CompoundImageHTMLizer( getCache(functionCacheDir, "htmlization"), datastore, imageInfoExtractor, rasterizer, refLogger )
		
		val centry = gridifier.gridifyDir( target, null )
		if( centry == null ) {
			System.err.println("No images found!")
			return
		}
		
		val cinfo = centry.info
		
		if( verbose ) {
			System.out.println( "# Compound image URI" )
			System.out.println( cinfo.uri )
			
			val rasterizationUri = rasterizer( cinfo.uri )
			System.out.println( "# Rasterization" )
			System.out.println( rasterizationUri )
		}
		
		val pageUri = htmlizer.pagify( cinfo.uri )
		refLogger( pageUri )
		
		if( verbose ) System.out.println( "# Page" )
		System.out.println( pageUri );
		
		if( refWriter != null ) refWriter.close()
		if( resource != null ) resource.waitFor()
		
		if( verbose ) {
			System.out.println( "# Page (again, in case it scrolled away)" )
			System.out.println( pageUri );
		}
		
	}
}
