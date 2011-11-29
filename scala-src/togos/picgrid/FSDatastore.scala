package togos.picgrid
import java.io.File

/**
 * A datastore that can import files directly, possibly
 * optimizing the process somehow
 */
trait FSDatastore extends Datastore
{
	def tempFile( ext:String="" ):File
	def storeAndDelete( tempFile:File ):String
}
