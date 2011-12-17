package togos.picgrid.app

import togos.picgrid.webserver.WebServer
import togos.mf.api.Callable
import togos.mf.api.Response
import togos.mf.api.Request
import scala.collection.mutable.ArrayBuffer
import scala.collection.TraversableLike
import togos.blob.ByteBlob
import togos.mf.base.BaseResponse
import togos.picgrid.image.ImageInfoExtractor
import togos.mf.api.ResponseCodes
import togos.picgrid.file.FSSHA1Datastore
import java.io.File

class WebPathToURNSourceAdapter( val source:(String=>ByteBlob) ) extends (String=>ByteBlob)
{
	val pattern = """/raw/([^/]+)/.*""".r
	
	def apply( webPath:String ) = webPath match {
		case pattern(uri) =>
			System.err.println("Look fer " + uri)
			source(uri)
		case _ =>
			System.err.println("No match: " + webPath)
			null
	}
}

class BlobFunctionCallable( val blobFunction:(String=>ByteBlob) ) extends Callable {
	def call( req:Request ):Response = {
		val b:ByteBlob = blobFunction(req.getResourceName())
		if( b == null ) return BaseResponse.RESPONSE_UNHANDLED
		
		val fmt = ImageInfoExtractor.extractImageType(b)
		val ct = if( fmt == null ) null else fmt.mimeType;
		new BaseResponse( ResponseCodes.NORMAL, b, ct )
	}
}

object WebServerCommand
{
	def main( args:Array[String] ) {
		val repoPaths = new ArrayBuffer[String]
		
		var i = 0
		while( i < args.length ) {
			if( "-repo".equals(args(i)) ) {
				i += 1
				repoPaths += args(i)
			} else {
				System.err.println("Unrecognised arument: "+args(i) )
				System.exit(1)
			}
			i += 1
		}
		
		val datastorePaths = new ArrayBuffer[String]
		for( repoPath <- repoPaths ) {
			for( f <- new File(repoPath + "/data" ).listFiles ) {
				if( f.isDirectory() ) {
					System.err.println( "Datastore " + f.getPath() )
					datastorePaths += f.getPath()
				}
			}
		}
		
		val resourceCallable = new BlobFunctionCallable(new WebPathToURNSourceAdapter(new FSSHA1Datastore( null, datastorePaths.toList )))
		
		val ws = new WebServer()
		ws.addRequestHandler( resourceCallable )
		ws.run()
	}
}

object PicGridCommand {
	type Command = { def main( args:Array[String] ):Unit }
	
	def main( args:Array[String] ) {
		val cmdName = args(0)
		val cmd:Command = cmdName match {
			case "compose" => GridifyCommand
			case "webserve" => WebServerCommand
		} 
		cmd.main( args.slice(1,args.length) )
	}
}
