package togos.picgrid.file;

import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/**
 * Run several of these at once to see what happens
 * when multiple processes access the same file at once!
 */
public class RandomAccessFileTest
{
	public static void main( String[] args ) {
		try {
			RandomAccessFile raf = new RandomAccessFile("junk/raf", "rw");
			
			FileLock l = raf.getChannel().lock();
			if( raf.length() == 0 ) {
				raf.seek(0);
				raf.write(0);
			}
			l.release();
			
			while( true ) {
				Thread.sleep(2000);
				l = raf.getChannel().lock();
				raf.seek(0);
				int thing = raf.read();
				System.out.println(thing);
				raf.seek(0);
				raf.write(thing+1);
				l.release();
			}
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
	}
}
