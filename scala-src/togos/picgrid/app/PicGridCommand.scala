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
		val datastorePaths = new ArrayBuffer[String]
		
		var i = 0
		while( i < args.length ) {
			if( "-repo".equals(args(i)) ) {
				i += 1
				for( f <- new File(args(i) + "/data" ).listFiles ) {
					if( f.isDirectory() ) {
						datastorePaths += f.getPath()
					}
				}
			} else {
				System.err.println("Unrecognized argument: "+args(i) )
				System.exit(1)
			}
			i += 1
		}
		
		val resourceCallable = new BlobFunctionCallable(new WebPathToURNSourceAdapter(new FSSHA1Datastore( null, datastorePaths.toList )))
		val indexCallable = new Callable() {
			def call( req:Request ):Response = {
				if( req.getResourceName() != "/" ) return BaseResponse.RESPONSE_UNHANDLED
				
				new BaseResponse( ResponseCodes.NORMAL,
					"To request a resource, use a URL of the format:\n" +
					"   raw/<urn>/<filename>\n" +
					"\n" +
					"For example:\n" +
					"   raw/urn:bitprint:PEFT4NFUHPH2EH5O7ARWA3KOO37WXKFK.MJZBF6ETFXRZ6JE4M5QONFZZEAKUWUYDS3TPBMI/151612-GEDC8683.JPG\n"
				);
			}
		}
		
		val ws = new WebServer()
		ws.addRequestHandler( indexCallable )
		ws.addRequestHandler( resourceCallable )
		ws.run()
	}
}

object PicGridCommand {
	type Command = { def main( args:Array[String] ):Unit }
	
	def main( args:Array[String] ) {
		if( args.length == 0 ) {
			System.err.println("No sub-command given")
			System.exit(1)
		}
		
		val subCmdName = args(0)
		val subCmdArgs = args.slice(1,args.length)
		subCmdName match {
		case "compose" =>
			GridifyCommand.main(subCmdArgs)
		case "webserve" =>
			WebServerCommand.main(subCmdArgs)
		case _ =>
			System.err.println("Unrecognised command: "+subCmdName)
			System.exit(1)
			null
		} 
	}
}
