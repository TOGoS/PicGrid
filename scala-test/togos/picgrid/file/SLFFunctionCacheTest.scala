package togos.picgrid.file

import java.io.File

import junit.framework.Assert.assertEquals
import junit.framework.TestCase
import togos.picgrid.BlobConversions.byteChunkAsString
import togos.picgrid.BlobConversions.stringAsByteChunk

class SLFFunctionCacheTest extends TestCase
{
	val testCacheDir = new File("junk/test-cache.slf")
	val slffc1 = new SLFFunctionCache( testCacheDir )
	val slffc2 = new SLFFunctionCache( testCacheDir )
	
	override def setUp() {
		FileUtil.deltree( testCacheDir )
	}
	
	def testAddSomeValues() {
		slffc1( "some-cache:some-value" ) = "two dozen"
		assertEquals( "two dozen", slffc1( "some-cache:some-value" ):String )
		slffc1.flush()
		Thread.sleep(200)
		assertEquals( "two dozen", slffc2( "some-cache:some-value" ):String )
		assertEquals( null, slffc2( "some-cache:some-palue" ):String )
		slffc2( "some-cache:some-palue" ) = "cats"
		assertEquals( "cats", slffc2( "some-cache:some-palue" ):String )
	}
}
