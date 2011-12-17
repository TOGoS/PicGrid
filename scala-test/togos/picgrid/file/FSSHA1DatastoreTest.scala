package togos.picgrid.file

import java.io.File
import junit.framework.Assert.{assertEquals,assertTrue,assertFalse}
import junit.framework.TestCase
import togos.blob.SimpleByteChunk
import togos.blob.SingleChunkByteBlob
import java.io.FileOutputStream
import togos.picgrid.BlobConversions.byteBlobAsChunkIterator

class FSSHA1DatastoreTest extends TestCase
{
	var dsDir:File = null
	var datastore:FSSHA1Datastore = null
	val testBlob = new SingleChunkByteBlob( new SimpleByteChunk( "Hello, world!".getBytes() ) )
	val testUrn = "urn:bitprint:SQ5HALIG6NCZTLXB7DNI56PXFFQDDVUZ.276TET7NAXG7FVCDQWOENOX4VABJSZ4GBV7QATQ"
	
	override def setUp() = {
		dsDir = new File("junk/datastore/data/test"+(Math.random*Integer.MAX_VALUE).toInt)
		datastore = new FSSHA1Datastore( dsDir )
	}
	
	def testStoreHelloWorld() = {
		val storedUrn = datastore.store( testBlob )
		assertEquals( testUrn, storedUrn )
		val fetchedData = datastore( testUrn )
		assertEquals( testBlob, fetchedData )
	}
	
	def testStoreBlobTwice() = {
		val f = datastore.tempFile(".txt")
		
		FileUtil.makeParentDirs(f)
		
		val fos1 = new FileOutputStream( f )
		for( c <- testBlob )  fos1.write( c.getBuffer(), c.getOffset(), c.getSize() )
		fos1.close()
		
		assertTrue( f.exists() )
		val urn1 = datastore.storeAndRemove( f )
		assertFalse( f.exists() )
		
		val fos2 = new FileOutputStream( f )
		for( c <- testBlob )  fos2.write( c.getBuffer(), c.getOffset(), c.getSize() )
		fos2.close()
		
		assertTrue( f.exists() )
		val urn2 = datastore.storeAndRemove( f )
		assertFalse( f.exists() )

		assertEquals( urn1, urn2 )
	}
}
