package qm.display.qcon;

/* Libraries imported */
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

/* Start - main class */
public class Class_StoreTextFileThread extends Thread {
	/*** Start variables ***/
	// Debugging
	private static final String TAG = "Class_StoreTextFileThread";
	
	// Context
	private Context Contx;
	
	// Thread members
	private boolean threadRunning;
	private boolean threadWait;
	private int ID;
	private static final int CANCEL = 1;
	private static final int WRITETXT = 2;
	private static final int WRITETXTEVENT = 3;
	
	// Store internal text file members
	private PackageManager pkManager;
	private String appVersionName;
	private FileOutputStream fosINTxt;
	private double[] indexesReceived;
	private boolean indexFlagReceived;
	private String currentSaveTime, eventTextReceived;
	/*** End variables ***/
	
	
	/** --- Thread constructor --- **/
	public Class_StoreTextFileThread(String ThreadName, Context context, String fileName, boolean orientationChangeFlag, boolean connectionType, boolean connectionProtocol, String FW, String SN) {
		// Set threadName
		super(ThreadName);
		// Write on log
		Log.d(TAG, "--- On Class_StoreTextFileThread constructor ---");
		
		// Set context
		Contx = context;
		// Start thread members
		threadRunning = true;
		threadWait = true;
		ID = 0;
		// Get application version name
		pkManager = context.getPackageManager();
		try {
			appVersionName = pkManager.getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Error getting version code", e);
			appVersionName = "";
		}
		// Start file members
		indexesReceived = null;
		indexFlagReceived = false;
		currentSaveTime = null;
		eventTextReceived = null;
		// Create internal binary file
		createInternalFile(context, fileName, orientationChangeFlag, connectionType, connectionProtocol, FW, SN);
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
			if (ID == WRITETXT) {
				if (indexesReceived.length > 3) {
					saveIndexesOnLog(currentSaveTime, indexesReceived, indexFlagReceived);
				}
				// Pause thread
				Pause();
			}
			else if (ID == WRITETXTEVENT) {
				if (indexesReceived.length > 3) {
					saveIndexesEventsOnLog(currentSaveTime, indexesReceived, indexFlagReceived, eventTextReceived);
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
	
	/** --- Thread resume to write indexes on file --- **/
	public void WritelogFile(String currentTime, double[] indexes, boolean indexesFlag) {
		ID = WRITETXT;
		currentSaveTime = currentTime;
		indexesReceived = indexes;
		indexFlagReceived = indexesFlag;
		synchronized (this) {
			threadWait = false;
			notify();
		}
	}
	
	/** --- Thread resume to write indexes and annotation on file --- **/
	public void WritelogEventFile(String currentTime, double[] indexes, boolean indexesFlag, String eventText) {
		ID = WRITETXTEVENT;
		currentSaveTime = currentTime;
		indexesReceived = indexes;
		indexFlagReceived = indexesFlag;
		eventTextReceived = eventText;
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
	// --- Creates internal storage files to write --- \\
	private void createInternalFile(Context context, String filename, boolean orientationChangeFlag, boolean connectionType, boolean connectionProtocol, String FW, String SN) {
		Log.d(TAG, "--- On create internal text file ---");
		
		// Create internal files depending if we want to erase the existing file or
		// if we want to write data to the end of existing file.
		if (orientationChangeFlag == false) {
			// Create a new file and erase the existing file with the same name.
			try {
				fosINTxt = Contx.openFileOutput(filename, Context.MODE_PRIVATE);
			}
			catch (FileNotFoundException e) {
				Log.e(TAG,"Error trying to create internal storage");
			}
			// Write log header
			if (fosINTxt != null) {
				logHeader(connectionType, connectionProtocol, FW, SN);
			}
		}
		else if (orientationChangeFlag == true) {
			// The file should exists then writes data at the end of the file.
			try {
				fosINTxt = Contx.openFileOutput(filename, Context.MODE_APPEND);
			}
			catch (FileNotFoundException e) {
				Log.e(TAG,"Error trying to create internal storage");
			}
		}
	}
	
	// --- Writes internal storage log file --- \\
	private void writeLog(String data) {
		// Set data to save
		String dataline = data + "\n";
		// Write data to file
		if (fosINTxt != null) {
			try {
				fosINTxt.write(dataline.getBytes());
			} catch (IOException e) {
				Log.e(TAG, "Error writing TXT file", e);
				// Close file
				if (fosINTxt != null) {
					try {
						fosINTxt.flush();
						fosINTxt.close();
					} catch (IOException e1) {
						Log.e(TAG, "Error closing TXT file", e1);
					}
				}
			}
		}
	}
	
	// --- Create and writes internal log file header --- \\
	private void logHeader(boolean connectionType, boolean connectionProtocol, String FW, String SN) {
		Log.d(TAG, "--- On write log header ---");
		
		// variables
		String header;
		
		// Create log
		header = Contx.getResources().getString(R.string.FileSave_Header_01) + ";";
		writeLog(header);
		header = Contx.getResources().getString(R.string.FileSave_Header_02) + ": " + FW + ";";
		writeLog(header);
		header = Contx.getResources().getString(R.string.FileSave_Header_03) + ": " + SN + ";";
		writeLog(header);
		if (connectionProtocol == true) {
			header = Contx.getResources().getString(R.string.FileSave_Header_04) + ": " + "OEM" + ";";
		}
		else {
			header = Contx.getResources().getString(R.string.FileSave_Header_04) + ": " + "SA" + ";";
		}
		writeLog(header);
		header = Contx.getResources().getString(R.string.FileSave_Header_05) + ";";
		writeLog(header);
		header = Contx.getResources().getString(R.string.FileSave_Header_06) + ": " + Build.MODEL + ";";
		writeLog(header);
		header = Contx.getResources().getString(R.string.FileSave_Header_07) + ": " + Build.SERIAL + ";";
		writeLog(header);
		
		header = Contx.getResources().getString(R.string.FileSave_Header_08) + ";";
		writeLog(header);
		header = Contx.getResources().getString(R.string.FileSave_Header_09) + " " +  Build.VERSION.RELEASE + ";";
		writeLog(header);
		header = Contx.getResources().getString(R.string.app_name) + " v" + appVersionName + ";";
		writeLog(header);
		
		header = Contx.getResources().getString(R.string.FileSave_Header_10) + ";";
		writeLog(header);
		if (connectionType == true) {
			header = Contx.getResources().getString(R.string.FileSave_Header_11) + ": " + "BT" + ";";
		}
		else {
			header = Contx.getResources().getString(R.string.FileSave_Header_11) + ": " + "USB" + ";";
		}
		writeLog(header);
		writeLog("");
		
		header = Contx.getResources().getString(R.string.FileSave_Header_12_Time) + "         ; " +
				Contx.getResources().getString(R.string.FileSave_Header_13_qCON) + "; " +
				Contx.getResources().getString(R.string.FileSave_Header_14_qNOX) + ";  " +
				Contx.getResources().getString(R.string.FileSave_Header_15_EMG) + ";  " +
				Contx.getResources().getString(R.string.FileSave_Header_16_BSR) + ";  " +
				Contx.getResources().getString(R.string.FileSave_Header_17_SQI) + "; " +
				Contx.getResources().getString(R.string.FileSave_Header_18_Zneg) + "; " +
				Contx.getResources().getString(R.string.FileSave_Header_19_Zref) + "; " +
				Contx.getResources().getString(R.string.FileSave_Header_20_Zpos) + "; " +
				Contx.getResources().getString(R.string.FileSave_Header_21_aEEG) + "; " +
				Contx.getResources().getString(R.string.FileSave_Header_22_Annotations) + ";";
		writeLog(header);
	}
	
	// --- Saves current indexes at internal log file --- \\
	private void saveIndexesOnLog(String CurrentTime, double[] datos, boolean correctValueToStoreFlag) {
		// Variables
		String logFormatRules = Contx.getResources().getString(R.string.FileSave_FormatLogIndexes);
		String msgRes;
		String logLine;
		
		// Check flag, if true is a correct value of indexes
		if (correctValueToStoreFlag == true) {
			logLine = String.format(logFormatRules, CurrentTime, datos[0], datos[1], datos[2], datos[3], datos[4], datos[5], datos[6], datos[7], datos[8], " ");
			writeLog(logLine);
		}
		// Is false, is not a correct value
		else {
			// qCON disconnected
			if (datos[0] == -1) {
				msgRes = Contx.getResources().getString(R.string.FileSave_qCONdisconnectedText);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, " ");
				writeLog(logLine);
			}
			// Artifact
			else if (datos[0] == 128) {
				msgRes = Contx.getResources().getString(R.string.Packet_qCON_Artifact);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, " ");
				writeLog(logLine);
			}
			// Impedance
			else if (datos[0] == 200) {
				msgRes = Contx.getResources().getString(R.string.Packet_qCON_Impedance);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, " ");
				writeLog(logLine);
			}
			// Lead off test
			else if (datos[0] == 220) {
				msgRes = Contx.getResources().getString(R.string.Packet_qCON_LeadOffTest);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, " ");
				writeLog(logLine);
			}
			// Lead off
			else if (datos[0] == 225) {
				msgRes = Contx.getResources().getString(R.string.Packet_qCON_LeadOff);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, " ");
				writeLog(logLine);
			}
			// Unknown
			else {
				msgRes = Contx.getResources().getString(R.string.FileSave_qCONunknownText);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, " ");
				writeLog(logLine);
			}
		}
	}
	
	// --- Saves current indexes and the text of text box at internal log file --- \\
	private void saveIndexesEventsOnLog(String CurrentTime, double[] datos, boolean correctValueToStoreFlag, String eventText) {
		// Variables
		String logFormatRules = Contx.getResources().getString(R.string.FileSave_FormatLogIndexes);
		String msgRes;
		String logLine;
		
		// Check flag, if true is a correct value of indexes
		if (correctValueToStoreFlag == true) {
			logLine = String.format(logFormatRules, CurrentTime, datos[0], datos[1], datos[2], datos[3], datos[4], datos[5], datos[6], datos[7], datos[8], eventText);
			writeLog(logLine);
		}
		// Is false, is not a correct value
		else {
			// qCON disconnected
			if (datos[0] == -1) {
				msgRes = Contx.getResources().getString(R.string.FileSave_qCONdisconnectedText);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, eventText);
				writeLog(logLine);
			}
			// Artifact
			else if (datos[0] == 128) {
				msgRes = Contx.getResources().getString(R.string.Packet_qCON_Artifact);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, eventText);
				writeLog(logLine);
			}
			// Impedance
			else if (datos[0] == 200) {
				msgRes = Contx.getResources().getString(R.string.Packet_qCON_Impedance);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, eventText);
				writeLog(logLine);
			}
			// Lead off test
			else if (datos[0] == 220) {
				msgRes = Contx.getResources().getString(R.string.Packet_qCON_LeadOffTest);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, eventText);
				writeLog(logLine);
			}
			// Lead off
			else if (datos[0] == 225) {
				msgRes = Contx.getResources().getString(R.string.Packet_qCON_LeadOff);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, eventText);
				writeLog(logLine);
			}
			// Unknown
			else {
				msgRes = Contx.getResources().getString(R.string.FileSave_qCONunknownText);
				logLine = String.format(logFormatRules, CurrentTime, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, msgRes, eventText);
				writeLog(logLine);
			}
		}
	}
	
	// --- Closes internal files --- \\
	private void closeInternalFile() {
		Log.d(TAG, "--- On close internal files ---");
		
		// Close internal storage TXT file
		if (fosINTxt != null) {
			try {
				fosINTxt.flush();
				fosINTxt.close();
			} catch (IOException e1) {
				Log.e(TAG, "Error closing TXT file", e1);
			}
		}
	}
	
	
}/* End - main class */