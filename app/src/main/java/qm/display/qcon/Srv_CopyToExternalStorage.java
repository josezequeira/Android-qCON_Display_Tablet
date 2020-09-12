package qm.display.qcon;

/* Libraries imported */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/* Start - main service class */
public class Srv_CopyToExternalStorage extends Service {
	
	/*** Start variables ***/
	// Debugging
	private static final String TAG = "Srv_DataStorage";
	
	// Context
    private static Context mSrvContx;
	
	// Timer members
	private static Handler timeOutHandler;
	private static final int timeOut = 4000;
	private static volatile boolean timeOutFlag;
	
	// Member thread to copy files to external storage
	private static CopyToExternalStorageThread copyFilesToExtStorageThread;
	private static String startTime;
	
	// Date and hour format
    private SimpleDateFormat FileNameDateHourFormat;
	
	// Members to broadcast messages
	public static final String ACTION_COPYFILESOK = "srv_CopyToExternalStorageOK";
	private static Intent broadCastIntent;
	
	// Constants for messenger handler of save files thread and timer thread
    public static final int MSGHDL_THR_COPY_OPENINFILESFAILS = 1;
    public static final int MSGHDL_THR_COPY_SETUPFILESFAILS = 2;
    public static final int MSGHDL_THR_COPY_EXSTORAGEFAILS = 3;
    public static final int MSGHDL_THR_COPY_EXDIRECTORYFAILS = 4;
    public static final int MSGHDL_THR_COPY_SHOWWAITMSG = 5;
    public static final int MSGHDL_THR_COPY_COPYOK = 6;
	/*** End variables ***/
	
	
	/***- Start - Methods for service life cycle -***/
	@Override
	public void onCreate() {
		Log.d(TAG, "*** ON CREATE ***");
		
		// Set context
		mSrvContx = this;
		
		// Initialize variables
		timeOutHandler = null;
		timeOutFlag = false;
		copyFilesToExtStorageThread = null;
		startTime = null;
		
		// Initialize date and hour format
		FileNameDateHourFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.UK);
		
		// Create TimeOur handler
		timeOutHandler = new Handler();
		
		// Create members to broadcast data
		broadCastIntent = new Intent(ACTION_COPYFILESOK);
		
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "*** ON START-COMMAND ***");
		
		// Get star time for the file name
		startTime = FileNameDateHourFormat.format(new Date());
		// Start members
		startReceiver();
		startCopyToExternalStorageThread(this);
		// Start timeOut to detect if main activity is alive
		startTimeOut();
		
		// We want this service to continue running until it is explicitly stopped, so return sticky
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "*** ON IBINDER ***");
		return null;
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "*** ON DESTROY ***");
		
		// --- Release resources --- \\
		// Stop and destroy TimeOut
		stopTimeOut();
		timeOutHandler = null;
		// Stop receivers
		stopReceiver();
		// Stop threads
		stopCopyToExternalStorageThread();
		
		super.onDestroy();
	}
	/***- End - Methods for service life cycle -***/
	
	
	/** --------------------- Stuff of timer to copy data on external files --------------------- **/
	/* ---- Start TimeOut ---- */
	private void startTimeOut() {
		if (timeOutHandler != null) {
			timeOutHandler.removeCallbacks(timeOutTask);
			timeOutHandler.postDelayed(timeOutTask, timeOut);
		}
	}
	/* ---- Stop TimeOut ---- */
	private void stopTimeOut() {
		if (timeOutHandler != null) {
			timeOutHandler.removeCallbacks(timeOutTask);
		}
	}
	/* ---- TimeOut task if there is not data received ---- */
	private final Runnable timeOutTask = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "*** On timeOutTask ***");
			
			// Set flag
			timeOutFlag = true;
			
			// Copy internal files to external files
			if (copyFilesToExtStorageThread != null) {
				copyFilesToExtStorageThread.CopyInFilesToExFiles();
			}
		}
	};
	
	
	/** --------------------- Stuff to receive messages --------------------- **/
	/* ---- Register broadcast receiver ---- */
	private void startReceiver() {
		registerReceiver(mainActivityReceiver, new IntentFilter(Act_Main.ACTION_MAINISALIVE));
		registerReceiver(srv_CopyToExStorageReceiver, new IntentFilter(Act_Main.ACTION_COPYTOEXTERNALFILES));
	}
	/* ---- Unregister USB broadcast receiver ---- */
	private void stopReceiver() {
		unregisterReceiver(mainActivityReceiver);
		unregisterReceiver(srv_CopyToExStorageReceiver);
	}
	
	/* ---- Broadcast receiver to receive message from main activity that is alive ---- */
	private final BroadcastReceiver mainActivityReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Stop timeOut
			stopTimeOut();
			// Start timeOut
			startTimeOut();
		}
	};
	
	/* ---- Broadcast receiver to trigger copy internal files to external files ---- */
	private final BroadcastReceiver srv_CopyToExStorageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Stop timeOut
			stopTimeOut();
			// Copy internal files to external files
			if (copyFilesToExtStorageThread != null) {
				copyFilesToExtStorageThread.CopyInFilesToExFiles();
			}
		}
	};
	
	
	/** --------------------- Thread to copy files to external storage --------------------- **/
	/* ---- Create and start thread to copy files to external storage ---- */
	private void startCopyToExternalStorageThread(Context context) {
		// Set internal file names and extension to copy to external storage
		String[] fileNameExtension = {"qCONBINFile", "bin",
									  "qCONTXTFile", "txt"};
		
		// Create thread
		copyFilesToExtStorageThread = new CopyToExternalStorageThread("Thr_CopyFilesToExtStorage",
																	context,
																	msgHdlThrSaveFiles,
																	fileNameExtension);
		// Start thread
		copyFilesToExtStorageThread.start();
	}
	/* ---- Stop and destroy thread copy files to external storage ---- */
	private void stopCopyToExternalStorageThread() {
		if (copyFilesToExtStorageThread != null) {
			copyFilesToExtStorageThread.cancel();
			copyFilesToExtStorageThread = null;
		}
		
	}
	/* ---- Thread to copy files to external storage ---- */
	private class CopyToExternalStorageThread extends Thread {
		/*** Start variables ***/
		// Context
		private Context Contx;
		
		// Message handler
		private Handler messenger;
		
		// Thread members
		private boolean threadRunning;
		private boolean threadWait;
		private int ID;
		private static final int CANCEL = 1;
		private static final int COPYINFILESTOEXFILES = 2;
		
		// Member to store on external storage
		private String[] filesNameAndExtension;
		private int binCounter, txtCounter;
		private FileInputStream fisInternal;
		private boolean openOK;
		private boolean setupExFileOK;
		private boolean copyFileOK;
		private boolean mExternalStorageWriteable;
		private File sdDir;
		private File appDir;
		private File file;
		private OutputStream fosExternal;
		private final int BUFF_SIZE = 1024*10;
		private final byte[] copyBuffer = new byte[BUFF_SIZE];
		/*** End variables ***/
		
		
		/** --- Thread constructor --- **/
		public CopyToExternalStorageThread(String ThreadName, Context context, Handler msgHandler, String[] NamesAndExtension) {
			// Set threadName
			super(ThreadName);
			// Write on log
			Log.d(TAG, "--- On CopyToExternalStorageThread constructor ---");
			
			// Set context
			Contx = context;
			// Set messenger handler
			messenger = msgHandler;
			// Start thread members
			threadRunning = true;
			threadWait = true;
			ID = 0;
			// Start copy to external storage files members
			filesNameAndExtension = NamesAndExtension;
			binCounter = 0;
			txtCounter = 0;
			fisInternal = null;
			openOK = false;
			setupExFileOK = false;
			copyFileOK = false;
			mExternalStorageWriteable = false;
			sdDir = null;
			appDir = null;
			file = null;
			fosExternal = null;
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
				if (ID == COPYINFILESTOEXFILES) {
					// Check for file name and extensions
					if (filesNameAndExtension != null) {
						// Get fileNameTime
						String fileNameTime;
						if ((startTime != null) && (!startTime.isEmpty())) {
							fileNameTime = startTime;
						}
						else {
							fileNameTime = FileNameDateHourFormat.format(new Date());
						}
						// Get number of files to copy
						int numFilesToCopy = (filesNameAndExtension.length)/2;
						
						if (timeOutFlag == true) {
							// Show wait message
							messenger.obtainMessage(MSGHDL_THR_COPY_SHOWWAITMSG).sendToTarget();
						}
						
						// Start copy each internal file to external file
						for (int i=0; i<numFilesToCopy; i++) {
							// Try to open internal file to copy
							openOK = openInternalFile(filesNameAndExtension[2*i]);
							// If open internal file is OK
							if (openOK == true) {
								// Setup external file to copy
								setupExFileOK = setupExternalFile(fileNameTime, filesNameAndExtension[(2*i)+1]);
								// If setup external file it is OK
								if (setupExFileOK == true) {
									// Copy internal file to external file
									copyFileOK = copyFile(fisInternal, fosExternal);
									
									if (copyFileOK == false) {
										Log.w(TAG, "Can't copy file");
										// Do nothing for now
									}
									else {
										// Delete internal file
										deleteInternalFile(filesNameAndExtension[2*i]);
									}
								}
								// If setup external file fails
								else {
									Log.w(TAG, "Can't setup external files");
									// show setup external file fails message
									messenger.obtainMessage(MSGHDL_THR_COPY_SETUPFILESFAILS).sendToTarget();
								}
							}
							// If open internal file fails
							else {
								Log.w(TAG, "Can't open internal files");
								// Show can't open internal files message
								messenger.obtainMessage(MSGHDL_THR_COPY_OPENINFILESFAILS).sendToTarget();
							}
						}
						// End copy each file
					}
					else {
						Log.e(TAG, "Files name and extensions are missing");
					}
					
					if (timeOutFlag == true) {
						// Show "copy is done" message
						messenger.obtainMessage(MSGHDL_THR_COPY_COPYOK).sendToTarget();
						// Stop service
						stopSelf();
					}
					else {
						// Sleep for a few moment
						try {
							Thread.sleep(2000);
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
						// Notify to the UI thread that the copy has finished
						broadcastMsgDataisStored(broadCastIntent);
					}
					
					// Pause thread
					Pause();
				}
				else if (ID == CANCEL) {
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
		
		/** --- Thread resume to copy files to external storage --- **/
		public void CopyInFilesToExFiles() {
			ID = COPYINFILESTOEXFILES;
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
		// --- Open internal file --- \\
		private boolean openInternalFile(String fileName) {
			// Variables
			boolean OpenFilesOk = true;
			
			// Open internal file
			try {
				fisInternal = Contx.openFileInput(fileName);
			} catch (FileNotFoundException e) {
				Log.e(TAG,"Error trying to open internal file: " + fileName, e);
				OpenFilesOk = false;
			}
			
			return OpenFilesOk;
		}
		
		// ---- Setup external file ---- \\
		private boolean setupExternalFile(String CurrentTime, String fileExtension) {
			Log.d(TAG, "--- On setup external files ---");
			
			// Variables
			boolean allOK;
			
			// Check external storage
			if (checkExternalStorage() == true) {
				// Check external directory
				if (checkExternalDirectoy() == true) {
					// Create external file
					createExternalFile(CurrentTime, fileExtension);
					// Set flag
					allOK = true;
				}
				else {
					// Set flag
					allOK = false;
					// Show message external directory could not be created or could not be written
					messenger.obtainMessage(MSGHDL_THR_COPY_EXDIRECTORYFAILS).sendToTarget();
				}
			}
			else {
				// Set flag
				allOK = false;
				// Show message external storage not detected
				messenger.obtainMessage(MSGHDL_THR_COPY_EXSTORAGEFAILS).sendToTarget();
			}
			return allOK;
		}
		
		// --- Checking external storage availability --- \\
		private boolean checkExternalStorage() {
			Log.d(TAG, "--- On check external storage ---");
			// Get external storage state
			String state = Environment.getExternalStorageState();
			
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				// We can read and write the media
				mExternalStorageWriteable = true;
			}
			 else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
				 // We can only read the media
				 mExternalStorageWriteable = false;
			 }
			 else {
				 // Something else is wrong. It may be one of many other states, but all we need
				 // to know is we can neither read nor write.
				 mExternalStorageWriteable = false;
			 }
			return mExternalStorageWriteable;
		}
		
		// --- Create external storage directory --- \\
		private boolean checkExternalDirectoy() {
			Log.d(TAG, "--- On check external directoy ---");
			
			// Variables
			boolean allOK;
			boolean createDirOK = false;
			
			// Get directory name from resources
			String directory = Contx.getResources().getString(R.string.FileSave_Directory);
			// File directory
			sdDir = new File(Environment.getExternalStorageDirectory().getPath());
			// Application directory
			appDir = new File(sdDir.getAbsolutePath() + "/" + directory);
			
			// Check if directory exists, if not exists create the directory
			if (appDir.exists() == false) {
				// Create directory
				createDirOK = appDir.mkdir();
				// Check if was well created
				if (createDirOK == false) {
					Log.e(TAG, "ERROR: /qCONFiles directory could not be created");
					allOK = false;
				}
				else {
					// Check if we can write at the directory
					if (appDir.canWrite() == false) {
						// We can not write at the directory
						Log.e(TAG, "ERROR: /qCONFiles directory created and could not be written");
						allOK = false;
					}
					else {
						// We can write at the directory
						Log.d(TAG, "/qCONFiles directory created and can be written");
						allOK = true;
					}
				}
			}
			else {
				// Directory exists, check if we can write at the directory
				if (appDir.canWrite() == false) {
					// We can not write at the directory
					Log.e(TAG, "ERROR: /qCONFiles directory could not be written");
					allOK = false;
				}
				else {
					// We can write at the directory
					Log.d(TAG, "/qCONFiles directory can be written");
					allOK = true;
				}
			}
			return allOK;
		}
		
		// --- Create external storage file --- \\
		private void createExternalFile(String CurrentTime, String fileExtension) {
			// Variables
			String fileName;
			
			// Set file name
			if (fileExtension.equals("bin")) {
				// Increment binCounter
				binCounter++;
				if (binCounter > 1) {
					fileName = CurrentTime + "_" + Contx.getResources().getString(R.string.FileSave_BinName) + String.valueOf(binCounter) + ".bin";
				}
				else {
					fileName = CurrentTime + "_" + Contx.getResources().getString(R.string.FileSave_BinName) + ".bin";
				}
			}
			else if (fileExtension.equals("txt")) {
				// Increment txtCounter
				txtCounter++;
				if (txtCounter > 1) {
					fileName = CurrentTime + "_" + Contx.getResources().getString(R.string.FileSave_TxTName) + String.valueOf(txtCounter) + ".txt";
				}
				else {
					fileName = CurrentTime + "_" + Contx.getResources().getString(R.string.FileSave_TxTName) + ".txt";
				}
			}
			else {
				fileName = CurrentTime + "_" + "qCONcustom" + "." + fileExtension;
			}
			
			// Create external storage file
			if (appDir.exists() && appDir.canWrite()) {
				file = new File(appDir.getAbsolutePath() + "/" + fileName);
				// Create file
				try {
					file.createNewFile();
				} catch (IOException e) {
					Log.e(TAG,"ERROR: could not create the file", e);
				}
				// Create file OutputStream
				if (file.exists() && file.canWrite()) {
					try {
						fosExternal = new FileOutputStream(file);
					} catch (FileNotFoundException e) {
						Log.e(TAG, "Error: File not found", e);
					}
				}
			}
			else {
				Log.e(TAG, "ERROR: could not create external storage files");
			}
		}
		
		// ---- Copy internal storage file to external storage file ---- \\
		private boolean copyFile(FileInputStream in, OutputStream out) {
			Log.d(TAG, "--- On copy files ---");
			
			// Variables
			boolean flag = false;
			
			try {
				// Copy block
				while (true) {
					synchronized (copyBuffer) {
						int amountRead = in.read(copyBuffer);
						if (amountRead == -1) {
							break;
						}
						out.write(copyBuffer, 0, amountRead);
					}
				}
			}
			catch (IOException e) {
				Log.e(TAG,"Error on copyFiles", e);
			}
			finally {
				if (in != null) {
					flag = true;
					try {
						in.close();
					} catch (IOException e) {
						Log.e(TAG, "Error trying to close in file on copyFiles");
						flag = false;
					}
				}
				if (out != null) {
					flag = true;
					try {
						out.flush();
						out.close();
					} catch (IOException e) {
						Log.e(TAG, "Error trying to close out file on copyFiles");
						flag = false;
					}
				}
			}
			return flag;
		}
		
		// ---- Delete internal file ---- \\
		private void deleteInternalFile(String internalfileName) {
			// Variables
			boolean deleteFileFlag = false;
			
			// Delete file
			deleteFileFlag = Contx.deleteFile(internalfileName);
			
			if (deleteFileFlag == false) {
				Log.e(TAG, "Error deleting internal file");
			}
		}
		
	}
	
	
	/** --------------------- Stuff to broadcast messages --------------------- **/
	// ---- Broadcast Data ---- \\
 	private void broadcastMsgDataisStored(Intent intent) {
 		// BroadCast data into intent
 		if (intent != null) {
 			sendBroadcast(intent);
 		}
 	}
	
 	
 	/** --------------------- Stuff of messages handler --------------------- **/
 	private static Handler msgHdlThrSaveFiles = new Handler() {
 		@Override
        public void handleMessage(Message msg) {
    		// Start Switch
    		switch (msg.what) {
    		case MSGHDL_THR_COPY_OPENINFILESFAILS:
    			// Show "can't open internal files" message
				Toast.makeText(mSrvContx, R.string.Toast_SaveFile_CopyInternalFiles, Toast.LENGTH_LONG).show();
				break;
    		case MSGHDL_THR_COPY_SETUPFILESFAILS:
    			// Show message "Error trying to setup external storage files"
				Toast.makeText(mSrvContx, R.string.Toast_SaveFile_SetupExternalFilesError, Toast.LENGTH_LONG).show();
				break;
    		case MSGHDL_THR_COPY_EXSTORAGEFAILS:
    			// Show message "external storage not detected"
				Toast.makeText(mSrvContx, R.string.Toast_SaveFile_SDcardNotDetected, Toast.LENGTH_LONG).show();
				break;
    		case MSGHDL_THR_COPY_EXDIRECTORYFAILS:
    			// Show message "external directory could not be created or could not be written"
				Toast.makeText(mSrvContx, R.string.Toast_SaveFile_CreateDirectotyError, Toast.LENGTH_LONG).show();
				break;
    		case MSGHDL_THR_COPY_SHOWWAITMSG:
    			// Show "wait" message
				Toast.makeText(mSrvContx, R.string.Toast_SaveFile_CopyExternalFiles_Wait, Toast.LENGTH_SHORT).show();
    			break;
    		case MSGHDL_THR_COPY_COPYOK:
    			// Show "copy is done" message
				Toast.makeText(mSrvContx, R.string.Toast_SaveFile_CopyExternalFiles_Done, Toast.LENGTH_SHORT).show();
    			break;
    		default:
    			// Do nothing
    			break;
    		}// End Switch
    	}
 	};
 	
}/* End - main service class */