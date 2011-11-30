package togos.picgrid

import junit.framework.Assert.assertEquals
import junit.framework.TestCase

class RDFDirectoryParserTest extends TestCase
{
	val testRdf = """
		<Directory xmlns="http://ns.nuke24.net/ContentCouch/" xmlns:bz="http://bitzi.com/xmlns/2002/01/bz-core#" xmlns:dc="http://purl.org/dc/terms/" xmlns:rdf="http://www.w3.org/1999/02/2
2-rdf-syntax-ns#">
			<entries rdf:parseType="Collection">
				<DirectoryEntry>
					<name>PersonalAppealFromBrandonHarris-2011.11.29.png</name>
					<target>
						<Blob rdf:about="urn:bitprint:KQQFB5YMTIYVMYVF3ESVNHIJ5EBM3MUU.IKTPI7EXHAZSNC7KDSTCP5NQPHOWHFQKBII4V6I">
							<bz:fileLength>275441</bz:fileLength>
						</Blob>
					</target>
					<dc:modified>2011-11-30 01:13:15 GMT</dc:modified>
				</DirectoryEntry>
				<DirectoryEntry>
					<name>TMCMG</name>
					<target>
						<Directory rdf:about="x-rdf-subject:urn:bitprint:GF4VGLOGJ57N3BVJYVB5DD65GUO6ERUT.B4WVE4P75KUF7KTYKP6F6HX7R5LHN4FCMXCTXEQ"/>
					</target>
				</DirectoryEntry>
				<DirectoryEntry>
					<name>TagSuggestions-2011.03.24.png</name>
					<target>
						<Blob rdf:about="urn:bitprint:UNSITUZCY3LG24Y3CSMDMM2AY4Y53KGS.2MAISDKLE3KEWP7LZVJBER4SN2FL2HP6S2VGFKI">
							<bz:fileLength>35429</bz:fileLength>
						</Blob>
					</target>
					<dc:modified>2011-03-25 03:53:23 GMT</dc:modified>
				</DirectoryEntry>
				<DirectoryEntry>
					<name>TaggedInSomePhotos-2011.10.09.png</name>
					<target>
						<Blob rdf:about="urn:bitprint:TLKTQ5MCIGG3PA4VTAOBKGXLX7BYMMIA.IH5H6F7SNB4FVV37SOSQ46HGFOEXEUXJSKPYMQY">
							<bz:fileLength>108563</bz:fileLength>
						</Blob>
					</target>
					<dc:modified>2011-10-10 05:52:52 GMT</dc:modified>
				</DirectoryEntry>
			</entries>
		</Directory>
	"""
	
	def testParseRdf() {
		val expectedUris = Map(
			"PersonalAppealFromBrandonHarris-2011.11.29.png" -> "urn:bitprint:KQQFB5YMTIYVMYVF3ESVNHIJ5EBM3MUU.IKTPI7EXHAZSNC7KDSTCP5NQPHOWHFQKBII4V6I",
			"TMCMG" -> "x-rdf-subject:urn:bitprint:GF4VGLOGJ57N3BVJYVB5DD65GUO6ERUT.B4WVE4P75KUF7KTYKP6F6HX7R5LHN4FCMXCTXEQ",
			"TagSuggestions-2011.03.24.png" -> "urn:bitprint:UNSITUZCY3LG24Y3CSMDMM2AY4Y53KGS.2MAISDKLE3KEWP7LZVJBER4SN2FL2HP6S2VGFKI",
			"TaggedInSomePhotos-2011.10.09.png" -> "urn:bitprint:TLKTQ5MCIGG3PA4VTAOBKGXLX7BYMMIA.IH5H6F7SNB4FVV37SOSQ46HGFOEXEUXJSKPYMQY" 
		)
		val expectedTypes = Map(
			"PersonalAppealFromBrandonHarris-2011.11.29.png" -> DirectoryObjectClass.Blob,
			"TMCMG" -> DirectoryObjectClass.Directory,
			"TagSuggestions-2011.03.24.png" -> DirectoryObjectClass.Blob,
			"TaggedInSomePhotos-2011.10.09.png" -> DirectoryObjectClass.Blob 
		)
		val entries = RDFDirectoryParser.parseDirectory(testRdf)
		assertEquals( 4, entries.length )
		for( item <- entries ) {
			assertEquals( expectedUris(item.name), item.targetUri )
			assertEquals( expectedTypes(item.name), item.targetClass )
		}
	}
}
