package qm.display.qcon;

/* Libraries imported */
import android.util.Log;

/* Start - main class */
public class Class_CircularBuffer {
	
	/*** Start variables ***/
	// Debugging
	private static final String TAG = "Class_CircularBuffer";
	
	// Members
    private byte[] byteBufferFiFo;
    private short[] shortBufferFiFo;
    private int[] intBufferFiFo;
    private long[] longBufferFiFo;
    private float[] floatBufferFiFo;
    private int bufferFiFoSize;
    private int elementsOccupied;
    private int inIndex;
    private int outIndex;
	/*** End variables ***/
	
	
	/** --- Class constructor --- **/
	public Class_CircularBuffer(int buffSize, String bufferType) {
		Log.d(TAG, "--- On class constructor ---");
		
		// Start members
        if (bufferType.equals("byte")) {
        	byteBufferFiFo = new byte[buffSize];
        }
        else if(bufferType.equals("short")) {
        	shortBufferFiFo = new short[buffSize];
        }
        else if(bufferType.equals("int")) {
        	intBufferFiFo = new int[buffSize];
        }
        else if(bufferType.equals("long")) {
        	longBufferFiFo = new long[buffSize];
        }
        else if(bufferType.equals("float")) {
        	floatBufferFiFo = new float[buffSize];
        }
        
        bufferFiFoSize = buffSize;
        elementsOccupied = 0;
        inIndex = 0;
        outIndex = 0;
	}
	
	
	/** --------------------- Methods --------------------- **/
	// ///// CircularBuffer - Reset \\\\\ \\
    public synchronized void reset() {
    	// Reset some members
        elementsOccupied = 0;
        inIndex = 0;
        outIndex = 0;
    }
    
    // ///// CircularBuffer - Getter of the buffer size \\\\\ \\
    public synchronized int bufferSize() {
    	return bufferFiFoSize;
    }
    
    // ///// CircularBuffer - Getter of the elements in the circular buffer \\\\\ \\
    public synchronized int numberOfElements() {
    	// Variables
        int value;
        
        value = elementsOccupied;
        
        return value;
    }
    
    
    // ///// CircularBuffer - Store in byte circular buffer \\\\\ \\
    public synchronized void storeByte(byte inValue) {
    	try {
    		// Check if should wait
            while (elementsOccupied == bufferFiFoSize) {
            	wait();
            }
            // Store in buffer FiFo
            byteBufferFiFo[inIndex] = inValue;
            inIndex = (inIndex + 1) % bufferFiFoSize;
            elementsOccupied++;
            // Notify for stop of waiting
            notify();
    	}
    	catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }
    
    // ///// CircularBuffer - Store in short circular buffer \\\\\ \\
    public synchronized void storeShort(short inValue) {
    	try {
    		// Check if should wait
            while (elementsOccupied == bufferFiFoSize) {
            	wait();
            }
            // Store in buffer FiFo
            shortBufferFiFo[inIndex] = inValue;
            inIndex = (inIndex + 1) % bufferFiFoSize;
            elementsOccupied++;
            // Notify for stop of waiting
            notify();
    	}
    	catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }
    
    // ///// CircularBuffer - Store in integer circular buffer \\\\\ \\
    public synchronized void storeInt(int inValue) {
    	try {
    		// Check if should wait
            while (elementsOccupied == bufferFiFoSize) {
            	wait();
            }
            // Store in buffer FiFo
            intBufferFiFo[inIndex] = inValue;
            inIndex = (inIndex + 1) % bufferFiFoSize;
            elementsOccupied++;
            // Notify for stop of waiting
            notify();
    	}
    	catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }
    
    // ///// CircularBuffer - Store in long circular buffer \\\\\ \\
    public synchronized void storeLong(long inValue) {
    	try {
    		// Check if should wait
            while (elementsOccupied == bufferFiFoSize) {
            	wait();
            }
            // Store in buffer FiFo
            longBufferFiFo[inIndex] = inValue;
            inIndex = (inIndex + 1) % bufferFiFoSize;
            elementsOccupied++;
            // Notify for stop of waiting
            notify();
    	}
    	catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }
    
    // ///// CircularBuffer - Store in float circular buffer \\\\\ \\
    public synchronized void storeFloat(float inValue) {
    	try {
    		// Check if should wait
            while (elementsOccupied == bufferFiFoSize) {
            	wait();
            }
            // Store in buffer FiFo
            floatBufferFiFo[inIndex] = inValue;
            inIndex = (inIndex + 1) % bufferFiFoSize;
            elementsOccupied++;
            // Notify for stop of waiting
            notify();
    	}
    	catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }
    
    
    // ///// CircularBuffer - Store byte at the end and lose old values from the head of circular buffer \\\\\ \\
    public synchronized void storeByteAndLoseOldValues(byte inValue) {
    	// Check if buffer is full
        while (elementsOccupied == bufferFiFoSize) {
        	// Get out a value from the buffer FiFo
        	outIndex = (outIndex + 1) % bufferFiFoSize;
            elementsOccupied--;
        }
        // Store in buffer FiFo
        byteBufferFiFo[inIndex] = inValue;
        inIndex = (inIndex + 1) % bufferFiFoSize;
        elementsOccupied++;
    }
    
    // ///// CircularBuffer - Store short at the end and lose old values from the head of circular buffer \\\\\ \\
    public synchronized void storeShortAndLoseOldValues(short inValue) {
    	// Check if buffer is full
        while (elementsOccupied == bufferFiFoSize) {
        	// Get out a value from the buffer FiFo
        	outIndex = (outIndex + 1) % bufferFiFoSize;
            elementsOccupied--;
        }
    	// Store in buffer FiFo
        shortBufferFiFo[inIndex] = inValue;
        inIndex = (inIndex + 1) % bufferFiFoSize;
        elementsOccupied++;
    }
    
    // ///// CircularBuffer - Store integer at the end and lose old values from the head of circular buffer \\\\\ \\
    public synchronized void storeIntAndLoseOldValues(int inValue) {
    	// Check if buffer is full
        while (elementsOccupied == bufferFiFoSize) {
        	// Get out a value from the buffer FiFo
        	outIndex = (outIndex + 1) % bufferFiFoSize;
            elementsOccupied--;
        }
    	// Store in buffer FiFo
        intBufferFiFo[inIndex] = inValue;
        inIndex = (inIndex + 1) % bufferFiFoSize;
        elementsOccupied++;
    }
    
    // ///// CircularBuffer - Store long at the end and lose old values from the head of circular buffer \\\\\ \\
    public synchronized void storeLongAndLoseOldValues(long inValue) {
    	// Check if buffer is full
        while (elementsOccupied == bufferFiFoSize) {
        	// Get out a value from the buffer FiFo
        	outIndex = (outIndex + 1) % bufferFiFoSize;
            elementsOccupied--;
        }
    	// Store in buffer FiFo
        longBufferFiFo[inIndex] = inValue;
        inIndex = (inIndex + 1) % bufferFiFoSize;
        elementsOccupied++;
    }
    
    // ///// CircularBuffer - Store float at the end and lose old values from the head of circular buffer \\\\\ \\
    public synchronized void storeFloatAndLoseOldValues(float inValue) {
    	// Check if buffer is full
        while (elementsOccupied == bufferFiFoSize) {
        	// Get out a value from the buffer FiFo
        	outIndex = (outIndex + 1) % bufferFiFoSize;
            elementsOccupied--;
        }
    	// Store in buffer FiFo
        floatBufferFiFo[inIndex] = inValue;
        inIndex = (inIndex + 1) % bufferFiFoSize;
        elementsOccupied++;
    }
    
    
    // ///// CircularBuffer - Read byte from circular buffer \\\\\ \\
    public synchronized byte readByte() {
    	// Variables
        byte outValue = 0;
    	try {
    		// Check if should wait
            while (elementsOccupied == 0) {
            	wait();
            }
            // readByte from buffer FiFo
            outValue = byteBufferFiFo[outIndex];
            outIndex = (outIndex + 1) % bufferFiFoSize;
            elementsOccupied--;
            // Notify for stop of waiting
            notify();
    	}
    	catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    	// Return value
        return outValue;
    }
    
    // ///// CircularBuffer - Read short from circular buffer \\\\\ \\
    public synchronized short readShort() {
    	// Variables
    	short outValue = 0;
    	try {
    		// Check if should wait
            while (elementsOccupied == 0) {
            	wait();
            }
            // readByte from buffer FiFo
            outValue = shortBufferFiFo[outIndex];
            outIndex = (outIndex + 1) % bufferFiFoSize;
            elementsOccupied--;
            // Notify for stop of waiting
            notify();
    	}
    	catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    	// Return value
        return outValue;
    }
    
    // ///// CircularBuffer - Read integer from circular buffer \\\\\ \\
    public synchronized int readInt() {
    	// Variables
    	int outValue = 0;
    	try {
    		// Check if should wait
            while (elementsOccupied == 0) {
            	wait();
            }
            // readByte from buffer FiFo
            outValue = intBufferFiFo[outIndex];
            outIndex = (outIndex + 1) % bufferFiFoSize;
            elementsOccupied--;
            // Notify for stop of waiting
            notify();
    	}
    	catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    	// Return value
        return outValue;
    }
    
    // ///// CircularBuffer - Read long from circular buffer \\\\\ \\
    public synchronized long readLong() {
    	// Variables
        long outValue = 0;
        try {
        	// Check if should wait
            while (elementsOccupied == 0) {
                wait();
            }
            // readByte from buffer FiFo
            outValue = longBufferFiFo[outIndex];
            outIndex = (outIndex + 1) % bufferFiFoSize;
            elementsOccupied--;
            // Notify for stop of waiting
            notify();
        }
        catch (InterruptedException e) {
    		e.printStackTrace();
    	}
        // Return value
        return outValue;
    }
    
    // ///// CircularBuffer - Read float from circular buffer \\\\\ \\
    public synchronized float readFloat() {
    	// Variables
        float outValue = 0;
        try {
        	// Check if should wait
            while (elementsOccupied == 0) {
                wait();
            }
            // readByte from buffer FiFo
            outValue = floatBufferFiFo[outIndex];
            outIndex = (outIndex + 1) % bufferFiFoSize;
            elementsOccupied--;
            // Notify for stop of waiting
            notify();
        }
        catch (InterruptedException e) {
    		e.printStackTrace();
    	}
        // Return value
        return outValue;
    }
    
}/* End - main class */