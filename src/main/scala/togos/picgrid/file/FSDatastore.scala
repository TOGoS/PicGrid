package togos.picgrid.file

import java.io.File

import togos.picgrid.BlobAutoStore

/**
 * A datastore that can import files directly, possibly
 * optimizing the process somehow
 */
trait FSDatastore extends BlobAutoStore
{
	/**
	 * Return a new temporary file, preferably on the same
	 * filesystem as the repository.
	 */
	def tempFile( ext:String="" ):File
	/**
	 * Store the file into the repository, removing it
	 * from its original location.
	 * Returns the URI of the stored file.
	 */
	def storeAndRemove( tempFile:File ):String
}
