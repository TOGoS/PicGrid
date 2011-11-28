package togos.picgrid

import junit.framework.TestCase
import togos.mf.base.SimpleByteChunk
import java.io.File
import junit.framework.Assert.assertEquals

class FSSHA1DatastoreTest extends TestCase
{
	var dsDir:File = null
	var datastore:Datastore = null
	val testBlob = new SimpleByteBlob( new SimpleByteChunk( "Hello, world!".getBytes() ) )
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
}
