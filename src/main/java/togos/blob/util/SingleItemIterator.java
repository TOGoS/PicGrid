package togos.blob.util;

import java.util.Iterator;

public class SingleItemIterator implements Iterator {
	protected boolean ended;
	protected Object v;
	public SingleItemIterator( Object v ) { this.v = v; }
	public boolean hasNext() { return !ended; }
	public Object next() { ended = true; return v; }
	public void remove() { throw new UnsupportedOperationException(); } 
}
