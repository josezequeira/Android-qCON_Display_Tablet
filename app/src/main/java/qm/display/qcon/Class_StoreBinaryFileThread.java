package qm.display.qcon;

/* Libraries imported */
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.Context;
import android.util.Log;

/* Start - main class */
public class Class_StoreBinaryFileThread extends Thread {
	/*** Start variables ***/
	// Debugging
	private static final String TAG = "Class_StoreBinaryFileThread";
	
	// Thread members
	private boolean threadRunning;
	private boolean threadWait;
	private int ID;
	private static final int CANCEL = 1;
	private static final int WRITEBIN = 2;
	private static final int WRITEINTTOBIN = 3;
	
	// Store internal binary file members
	private FileOutputStream fosINBin;
	private byte[] bytebufferReceived;
	private int[] intbufferReceived;
	/*** End variables ***/
	
	
	/** --- Thread constructor --- **/
	public Class_StoreBinaryFileThread(String ThreadName, Context context, String fileName, boolean createNewFileFlag) {
		// Set threadName
		super(ThreadName);
		// Write on log
		Log.d(TAG, "--- On Class_StoreBinaryFileThread constructor ---");
		
		// Start thread members
		threadRunning = true;
		threadWait = true;
		ID = 0;
		// Start file members 
		fosINBin = null;
		bytebufferReceived = null;
		intbufferReceived = null;
		// Create internal binary file
		createInternalFile(context, fileName, createNewFileFlag);
	}
	
	
	/** --- Thread execution --- **/
	@Override
	public void run() {
		while (threadRunning == true) {
			// Check if should wait
			synchronized (this) {
				while (threadWait) {
					try {
						wait();
					}
					catch (InterruptedException e) {
						Log.e(TAG, "Error checking if thread should wait", e);
					}
				}
			}
			// Do work
			if (ID == WRITEBIN) {
				if (bytebufferReceived.length > 2) {
					// Store byte buffer
					writeBin(bytebufferReceived);
					// Reset buffer
					bytebufferReceived = null;
				}
				// Pause thread
				Pause();
			}
			else if (ID == WRITEINTTOBIN) {
				if (intbufferReceived.length > 2) {
					// Set binary array size
					bytebufferReceived = new byte[intbufferReceived.length];
					// Convert integer to byte
					for (int i=0; i<intbufferReceived.length; i++) {
						bytebufferReceived[i] = (byte) intbufferReceived[i];
					}
					// Store byte buffer
					writeBin(bytebufferReceived);
					// Reset buffer
					intbufferReceived = null;
					bytebufferReceived = null;
				}
				// Pause thread
				Pause();
			}
			else if (ID == CANCEL) {
				closeInternalFile();
				// Set thread running flag to false
				threadRunning = false;
			}
			else {
				// Pause thread
				Pause();
			}
		}
	}
	
	/** --- Thread Pause --- **/
	public void Pause() {
		ID = 0;
		synchronized (this) {
			threadWait = true;
		}
	}
	
	/** --- Thread resume to write bin file --- **/
	public void WriteBinFile(byte[] buffer) {
		ID = WRITEBIN;
		bytebufferReceived = buffer;
		synchronized (this) {
			threadWait = false;
			notify();
		}
	}
	
	/** --- Thread resume to write integer values to binary file --- **/
	public void WriteIntToBinFile(int[] buffer) {
		ID = WRITEINTTOBIN;
		intbufferReceived = buffer;
		synchronized (this) {
			threadWait = false;
			notify();
		}
	}
	
	/** --- Thread cancel --- **/
	public void cancel() {
		ID = CANCEL;
		synchronized (this) {
			threadWait = false;
			notify();
		}
	}
	
	
	/** --------------------- Methods --------------------- **/
	// --- Create internal binary file --- \\
	private void createInternalFile(Context context, String filename, boolean createNewFileFlag) {
		Log.d(TAG, "--- On create internal binay file ---");
		
		// Create internal files depending if we want to erase the existing file or
		// if we want to write data to the end of existing file.
		if (createNewFileFlag == true) {
			// Create a new file and erase the existing file with the same name.
			try {
				fosINBin = context.openFileOutput(filename, Context.MODE_PRIVATE);
			}
			catch (FileNotFoundException e) {
				Log.e(TAG,"Error trying to create internal storage");
			}
		}
		else {
			// The file should exists then writes data at the end of the file.
			try {
				fosINBin = context.openFileOutput(filename, Context.MODE_APPEND);
			}
			catch (FileNotFoundException e) {
				Log.e(TAG,"Error trying to create internal storage");
			}
		}
	}
	
	// --- Writes internal storage binary file --- \\
	private void writeBin(byte[] buffer) {
		if (fosINBin != null) {
			try {
				fosINBin.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "Error writing BIN file", e);
				// Close file
				closeInternalFile();
			}
		}
	}
	
	// --- Closes internal files --- \\
	private void closeInternalFile() {
		Log.d(TAG, "--- On close internal files ---");
		
		// Close internal storage BIN file
		if (fosINBin != null) {
			try {
				fosINBin.flush();
				fosINBin.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing BIN file", e);
			}
		}
	}
	
	
}/* End - main class */