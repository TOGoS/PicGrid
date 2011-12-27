package togos.picgrid.store;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import togos.blob.ByteChunk;
import togos.service.Service;

/**
 * DataMap that saves data to a backing VectorDataMap asynchronously.
 * Data that has been 'put' but not yet saved is still available using
 * a temporary cache.
 */
public class AsyncDataMap implements DataMap, Service, Runnable
{
	final static class KV {
		public ByteChunk key;
		public ByteChunk value;
		
		public KV( ByteChunk k, ByteChunk v ) {
			this.key   = k;
			this.value = v;
		}
	}
	
	/** Gets sent to the writing thread to signal end of data */
	protected KV QUIT = new KV(null,null);
	
	protected BlockingQueue storeQueue = new ArrayBlockingQueue(256);
	protected VectorDataMap store;
	protected WeakHashMap storingValues = new WeakHashMap(256);
	protected Thread thread;
	
	/** Approximate number of bytes (total keys + values) to break batch inserts after. */
	public int maxBatchSize = 1024*256;
	/** Maximum number of entries to insert per batch. */
	public int maxBatchLength = 256;
	
	public AsyncDataMap(VectorDataMap store) {
		this.store = store;
	}
	
	public void put( ByteChunk key, ByteChunk value ) {
		synchronized( this ) {
			storingValues.put( key, new WeakReference(value) );
		}
		try {
			storeQueue.put(new KV(key,value));
		} catch( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}
	
	public ByteChunk get( ByteChunk key ) {
		Reference r;
		ByteChunk v;
		synchronized( this ) { r = (Reference)storingValues.get( key ); }
		v = r == null ? null : (ByteChunk)r.get();
		return v == null ? store.get(key) : v;
	}
	
	public void run() {
		KV kv;
		ByteChunk[] keys = new ByteChunk[256];
		ByteChunk[] values = new ByteChunk[256];
		boolean quit = false;
		try {
			while( !quit ) {
				kv = (KV)storeQueue.take();
				if( kv == QUIT ) return;
				// OK, this isn't totally generic; it's using sizes from SimpleListFile2:
				int size = 16+kv.key.getSize()+kv.value.getSize();
				int count = 1;
				keys[0] = kv.key;
				values[0] = kv.value;
				while( !quit && size < maxBatchSize && count < maxBatchLength && (kv = (KV)storeQueue.poll()) != null ) {
					if( kv == QUIT ) {
						quit = true;
					} else {
						keys[count] = kv.key;
						values[count] = kv.value;
						size += 16+kv.key.getSize()+kv.value.getSize();
						++count;
					}
				}
				store.multiPut( keys, values, 0, count );
			}
		} catch( InterruptedException e ) {
			// Then it's time to quit!
		} finally {
			thread = null;
		}
	}
	
	public synchronized void start() {
		if( thread == null ) {
			thread = new Thread(this);
			thread.start();
		}
	}
	
	public synchronized void haltAndJoin() {
		Thread t = thread;
		if( t != null ) {
			try {
				storeQueue.put(QUIT);
				t.join();
			} catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
			thread = null;
		}
	}
	
	public synchronized void halt() {
		if( thread != null ) {
			try {
				storeQueue.put(QUIT);
			} catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
			thread = null;
		}
	}
}
