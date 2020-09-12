package qm.display.qcon;

/* Libraries imported */
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/* Start - main service class */
public class Srv_BT_ConnectionSynchronization extends Service {
	/*** Start variables ***/
	// Debugging
	private static final String TAG = "Srv_BT_ConnectionSynchronization";
	
	// Timer members
	private static Handler timeOutHandler;
	private static final int timeOut = 6000;
	private static boolean registerReceiverFlag;
	
	// Members for device connection
	private static boolean connProtocol;	// True for OEM, false for SA
	private static BluetoothAdapter btAdapter;
	private static String MAC_Address;
	private static ConnectThread connectThread;
	
	// Members to read from device
	private static ReadThread readDevice;
	
	// Circular buffer to store data read and then to synchronize
	private static Class_CircularBuffer fifoBufferToSynch;
	
	// Members to synchronize data read from device
	private static SynchronizeDataThread synchData;
	
	// Members to broadcast data synchronized
	public static final String ACTION_SEND_DATASYNCH = "sendDataSynch";
	private static Intent broadCastIntent;
	private static Bundle extraData;
	private static final String key_dataSynch = "dataArray";
	
	// Members to store data
	private static Class_StoreBinaryFileThread saveBinFileThread;
	private static boolean createNewBinFileFlag;
	/*** End variables ***/
	
	
	/***- Start - Methods for service life cycle -***/
	@Override
	public void onCreate() {
		Log.d(TAG, "*** ON CREATE ***");
		
		// Initialize time Out members
		timeOutHandler = null;
		registerReceiverFlag = false;
		// Initialize device connection variables
		connProtocol = false;
		MAC_Address = null;
		btAdapter = null;
		connectThread = null;
		// Initialize variables to read from the device
		readDevice = null;
		// Initialize circular buffer to store data read and then to extract
		fifoBufferToSynch = null;
		// Initialize variables to extract data read from device
		synchData = null;
		// Initialize variables to send data extracted
		broadCastIntent = null;
		extraData = null;
		// Initialize variables to store data
		saveBinFileThread = null;
		createNewBinFileFlag = false;
		
		// Create TimeOur handler
		timeOutHandler = new Handler();
		
		// Create circular buffer
		fifoBufferToSynch = new Class_CircularBuffer(4096, "byte");
		
		// Create members to broadcast data extracted
		broadCastIntent = new Intent(ACTION_SEND_DATASYNCH);
		extraData = new Bundle();
		
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "*** ON START-COMMAND ***");
		
		// Get BT adapter
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// Get data from intent
		if (intent != null) {
			connProtocol = intent.getBooleanExtra("connProtocolFlag", false);
			createNewBinFileFlag = intent.getBooleanExtra("createNewBinFileFlag", true);
			MAC_Address = intent.getStringExtra("btDevMACaddress");
		}
		else {
			connProtocol = false;
			createNewBinFileFlag = false;
			MAC_Address = null;
		}
		
		if ((btAdapter != null) && (MAC_Address != null)) {
			// Start data storage thread
			startStoreBinayFileThread(this, createNewBinFileFlag);
			// Start synchronization thread
			startSynchonizeDataThread(connProtocol);
			// Start connect thread
			startConnectThread(btAdapter, MAC_Address, connProtocol);
			// Start receiver
			startReceiver();
			// Start timeOut to detect if main activity is alive
			startTimeOut();
		}
		else {
			// Stop service
			stopSelf();
		}
		
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
		// Stop read thread
		stopReadDataThread();
		// Stop synchronization thread
		stopSynchonizeDataThread();
		// Stop data storage thread
		stopStoreBinayFileThread();
		// Stop connect thread
		stopConnectThread();
		// Clear extra data
		if (extraData != null) {
			extraData = null;
		}
		// Clear broadcast
		if (broadCastIntent != null) {
			broadCastIntent = null;
		}
		
    	super.onDestroy();
	}
	/***- End - Methods for service life cycle -***/
	
	
	/** --------------------- Stuff of time Out --------------------- **/
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
			
			// Stop service
			stopSelf();
		}
	};
	
	
	/** --------------------- Stuff to receive messages --------------------- **/
	/* ---- Register broadcast receiver ---- */
	private void startReceiver() {
		registerReceiver(mainActivityReceiver, new IntentFilter(Act_Main.ACTION_MAINISALIVE));
		registerReceiverFlag = true;
	}
	/* ---- Unregister USB broadcast receiver ---- */
	private void stopReceiver() {
		if (registerReceiverFlag == true) {
			unregisterReceiver(mainActivityReceiver);
			registerReceiverFlag = false;
		}
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
	
	
	/** --------------------- Stuff to connect with BT device --------------------- **/
	/* ---- Create and start thread to connect with device ---- */
	private void startConnectThread(BluetoothAdapter btAdapter, String MAC_ADDRESS, boolean protocolFlag) {
		// Create connect thread
		connectThread = new ConnectThread("Thr_ConnectBTdevice", btAdapter, MAC_ADDRESS, protocolFlag);
		// Start thread
		connectThread.start();
	}
	/* ---- Stop and destroy thread to connect with device ---- */
	private void stopConnectThread() {
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}
	}
	/* ---- Thread to connect with BT device ---- */
	private class ConnectThread extends Thread {
		/*** Start variables ***/		
		// BT members
		private BluetoothAdapter bt_Adapter;
		private String mac_Address;
		private BluetoothDevice bt_Device;
		private UUID bt_uuid;
		private boolean protocolFlag;
		private BluetoothSocket bt_Socket;
		/*** End variables ***/
		
		
		/** --- Thread constructor --- **/
 		public ConnectThread(String ThreadName, BluetoothAdapter btAdapter, String MAC_ADDRESS, boolean protFlag) {
			// Set threadName
			super(ThreadName);
			// Write on log
			Log.d(TAG, "--- On ConnectThread constructor ---");
			
			// Start members
			bt_Adapter = btAdapter;
			mac_Address = MAC_ADDRESS;
			bt_Device = null;
			bt_uuid = null;
			protocolFlag = protFlag;
			bt_Socket = null;
		}
		
 		
		/** --- Thread execution --- **/
		@Override
		public void run() {
			Log.d(TAG, "--- On ConnectThread run ---");
			
			// Use the SPP UUID
			bt_uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
			
			// Get the BluetoothDevice object
			bt_Device = bt_Adapter.getRemoteDevice(mac_Address);
			
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
            	bt_Socket = bt_Device.createRfcommSocketToServiceRecord(bt_uuid);
            }
            catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            
			// Cancel discovery because it will slow down the connection
			bt_Adapter.cancelDiscovery();
			
			if (bt_Socket != null) {
	            try {
	                // This is a blocking call and will only return on a successful connection or an exception
	            	bt_Socket.connect();
	            }
	            catch (IOException e) {
	            	Log.e(TAG, "BT connection failure", e);
	                // Connection failed
	                return;
	            }
	            
	            // Start read thread
	            startReadDataThread(bt_Socket, protocolFlag);
			}
		}
		
		/** --- Thread cancel --- **/
		public void cancel() {
			// Close socket
			if (bt_Socket != null) {
				try {
					bt_Socket.close();
	            }
				catch (IOException e) {
	                Log.e(TAG, "close() of connect socket failed", e);
	            }
			}
		}
		
	}
	
	
	/** --------------------- Stuff to read data from BT device --------------------- **/
	/* ---- Create and start thread to read data from device ---- */
	private void startReadDataThread(BluetoothSocket socket, boolean protocolFlag) {
		// Create read thread
		readDevice = new ReadThread("Thr_ReadBTdevice", fifoBufferToSynch, socket, protocolFlag);
		// Start thread
		readDevice.start();
	}
	/* ---- Stop and destroy thread read data from device ---- */
	private void stopReadDataThread() {
		if (readDevice != null) {
			readDevice.cancel();
			readDevice = null;
		}
	}
	/* ---- Thread to read data from device ---- */
	// And store data read on circular buffer to synchronize
	// And resume thread to synchronize data on circular buffer
	// And store data read on binary file
	private class ReadThread extends Thread {
		
		/*** Start variables ***/
		// Thread members
		private boolean threadRunning;
		
		// Circular buffer where the data received are stored
		private Class_CircularBuffer fifoBuffer;
		
		// Members to read
		private BluetoothSocket mmSocket;
		private InputStream mmInStream;
		private BufferedInputStream mmBUffInStream;
		byte[] readBuffer;
		private int bytesToRead;
		private int bytesToRead_0_Counter;
		private boolean protocolFLag;
		/*** End variables ***/
		
		
		/** --- Thread constructor --- **/
		public ReadThread(String ThreadName, Class_CircularBuffer fifoBufferSynch, BluetoothSocket socket, boolean protFlag) {
			// Set threadName
			super(ThreadName);
			// Write on log
			Log.d(TAG, "--- On ReadThread constructor ---");
			// Start thread members
			threadRunning = true;
			// Set circular buffer
			fifoBuffer = fifoBufferSynch;
			// Start members to read
			mmSocket = socket;
			mmInStream = null;
			mmBUffInStream = null;
			bytesToRead = 0;
			bytesToRead_0_Counter = 0;
			protocolFLag = protFlag;
		}
		
		
		/** --- Thread execution --- **/
		@Override
		public void run() {
			
			// Get the BluetoothSocket input stream
            try {
            	mmInStream = mmSocket.getInputStream();
            }
            catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
			
            // Create buffered input stream
            mmBUffInStream = new BufferedInputStream(mmInStream, 2048);
            
			while (threadRunning == true) {
				
				try {
					// Create read buffer
					readBuffer = new byte[mmBUffInStream.available()];
					
					// Read from the InputStream
					bytesToRead = mmBUffInStream.read(readBuffer, 0, readBuffer.length);
					
					// Check if data available in greater than 0
					if (bytesToRead > 0) {
						// Reset counter of no bytes available in the buffer
						bytesToRead_0_Counter = 0;
						
						// Store data read on circular buffer to synchronize
						if (fifoBuffer != null) {
							for (int i=0; i<bytesToRead; i++) {
								fifoBuffer.storeByte(readBuffer[i]);
							}
						}
						
						// Resume to synchronize data stored on circular buffer
						if (synchData != null) {
							synchData.SynchDataOnCircularBuffer();
						}
						
						// Resume to store data read on binary file
						if (saveBinFileThread != null) {
							saveBinFileThread.WriteBinFile(readBuffer);
						}
						
						// Erase read buffer
						readBuffer = null;
						// Reset bytesToRead
						bytesToRead = 0;
					}
					else {
						// Increment counter of no bytes available in the buffer
						bytesToRead_0_Counter = bytesToRead_0_Counter + 1;
					}
					
				}
				catch (IOException e) {
					threadRunning = false;
					e.printStackTrace();
				}
				
				// Sleep according protocol, for OEM = true, for SA = false
				if (protocolFLag == true) {
					// Sleep for 1000 milliseconds
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				else {
					// Sleep for 250 milliseconds
					try {
						Thread.sleep(250);
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				// If no data is available stop thread and service
				if (bytesToRead_0_Counter > 4) {
					threadRunning = false;
					// Stop service
					stopSelf();
				}
				
			}
			
		}
		
		/** --- Thread cancel --- **/
		public void cancel() {
			synchronized (this) {
				// Exit thread
				threadRunning = false;
				if (mmSocket != null) {
					// Close socket
					try {
		                mmSocket.close();
		            }
					catch (IOException e) {
		                Log.e(TAG, "close() of connect socket failed", e);
		            }
				}
			}
		}
		
	}
	
	
	/** --------------------- Stuff to synchronize data stored on circular buffer --------------------- **/
	/* ---- Create and start thread to synchronize data on circular buffer ---- */
	private void startSynchonizeDataThread(boolean protocolFLag) {
		// Create thread to synchronize data on circular
		synchData = new SynchronizeDataThread("Thr_SynchDataOnCircularBuffer", fifoBufferToSynch, protocolFLag);
		// Start thread
		synchData.start();
	}
	/* ---- Stop and destroy thread to synchronize data on circular buffer ---- */
	private void stopSynchonizeDataThread() {
		if (synchData != null) {
			synchData.cancel();
			synchData = null;
		}
	}
	/* ---- Thread to synchronize data on circular buffer ---- */
	// And broadcast synchronized data
	private class SynchronizeDataThread extends Thread {
		
		/*** Start variables ***/
		// Thread members
		private boolean threadRunning;
		private boolean threadWait;
		private int ID;
		private static final int CANCEL = 1;
		private static final int SYNCHDATA = 2;
		
		// Circular buffer where the data received are stored
		private Class_CircularBuffer fifoBuffer;
		
		// Members to synchronize
		private boolean protocolFlag;
        private int[] synchBuffer;
        private volatile boolean synchFlag;
        
        // Members to extract
        private int[] eegArray;
        private int eegArrayIndex;
        
    	// Member array to send
    	private int[] dataExtracted;
		/*** End variables ***/
		
		
		/** --- Thread constructor --- **/
		public SynchronizeDataThread(String ThreadName, Class_CircularBuffer fifoBufferSynch, boolean protFlag) {
			// Set threadName
			super(ThreadName);
			// Write on log
			Log.d(TAG, "--- On SynchronizeDataThread constructor ---");
			// Start thread members
			threadRunning = true;
			threadWait = true;
			ID = 0;
			// Set circular buffer
			fifoBuffer = fifoBufferSynch;
			// Start members according of protocol flag,  for OEM = true, for SA = false
			protocolFlag = protFlag;
			if (protocolFlag == true) {
				// Start members to synchronize
				synchBuffer = new int[180];
	            for (int i = 0; i < synchBuffer.length; i++) {
	                synchBuffer[i] = 0;
	            }
			}
			else {
				// Start members to synchronize
	            synchBuffer = new int[8];
	            for (int i = 0; i < synchBuffer.length; i++) {
	                synchBuffer[i] = 0;
	            }
	            // Start members to extract
	            eegArrayIndex = 0;
	            eegArray = new int[256];
	            for (int i = 0; i < eegArray.length; i++) {
	            	eegArray[i] = 0;
	            }
	            // Start member to send
	            int len = eegArray.length + 16;
	            dataExtracted = new int[len];
	            for (int i=0; i<len; i++) {
	            	dataExtracted[i] = 0;
	            }
			}
			synchFlag = true;
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
				if (ID == SYNCHDATA) {
					// Synch according protocol flag, for OEM = true, for SA = false
					if (protocolFlag == true) {
						// Synchronize data on circular buffer
						synchData_OEM();
					}
					else {
						// Synchronize data on circular buffer
						synchData_SA();
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
		
		/** --- Thread Resume --- **/
		public void SynchDataOnCircularBuffer() {
			ID = SYNCHDATA;
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
		// ---- Convert signed byte to unsigned byte ---- \\
		private int signedByteToUnsignedByte(byte byteValue) {
			// Set byte value to integer value between 0 and 255
			int result;
			
			result = byteValue & (0xff);
			
			return result;
		}
		
		// ---- Synchronize data data on circular buffer with SA Protocol ---- \\
		private void synchData_SA() {
			// Variables
            boolean crcFlag;
            
            // While number of elements in FiFo buffer is greater or equal than 8
            // synchronize data received from qCON by the header and CRC and send result to UI
            while (fifoBuffer.numberOfElements() >= 8) {
            	while (fifoBuffer.numberOfElements() > 0) {
            		// Set CRC flag to false
                    crcFlag = false;
                    // Check synchronization flag
                    if (synchFlag == true) {
                        // Read 8 elements from FiFo buffer and storeByte them at synchronization buffer
                        for (int i = 0; i < synchBuffer.length; i++) {
                            synchBuffer[i] = signedByteToUnsignedByte(fifoBuffer.readByte());
                        }
                    }
                    else {
                        // Move each element at synchronization buffer one position to the left
                        for (int i = 0; i < synchBuffer.length - 1; i++) {
                            synchBuffer[i] = synchBuffer[i + 1];
                        }
                        // Read 1 element from FiFo buffer and storeByte it at the end of synchronization buffer
                        synchBuffer[synchBuffer.length - 1] = signedByteToUnsignedByte(fifoBuffer.readByte());
                    }
                    
                    // Set CRC flag by verifying the CRC of synchronization buffer
                    crcFlag = checkPkCRC16(synchBuffer);
                    // If CRC flag is true
                    if (crcFlag == true) {
                        // Set synchronization flag to true
                        synchFlag = true;
                        
                        // Extract synchronized data by protocol and send to UI
                        extractDataAndPrepareToSend(synchBuffer);
                        
                        // Exit while
                        break;
                    }
                    // If CRC flag is false
                    else {
                        // Set synchronization flag to false
                        synchFlag = false;
                    }
            	}
            }
		}
		
		// ---- Synchronize data data on circular buffer with OEM Protocol ---- \\
		private void synchData_OEM() {
			// Variables
            boolean headerFlag;
            boolean crcFlag;

            // While number of elements in FiFo buffer is greater or equal than 180
            // synchronize data received from qCON by the header and CRC and send result to UI
            while (fifoBuffer.numberOfElements() >= 180) {
                while (fifoBuffer.numberOfElements() > 0) {
                    // Set header flag and CRC flag to false
                    headerFlag = false;
                    crcFlag = false;

                    // Check synchronization flag
                    if (synchFlag == true) {
                        // Read 180 elements from FiFo buffer and store them at synchronization buffer
                        for (int i = 0; i < synchBuffer.length; i++) {
                        	synchBuffer[i] = signedByteToUnsignedByte(fifoBuffer.readByte());
                        }
                    }
                    else {
                        // Move each element at synchronization buffer one position to the left
                        for (int i = 0; i < synchBuffer.length - 1; i++) {
                            synchBuffer[i] = synchBuffer[i + 1];
                        }
                        // Read 1 element from FiFo buffer and store it at the end of synchronization buffer
                        synchBuffer[synchBuffer.length - 1] = signedByteToUnsignedByte(fifoBuffer.readByte());
                    }

                    // Set header flag by verifying the header of synchronization buffer
                    headerFlag = checkPkHeader(synchBuffer[0], synchBuffer[1], synchBuffer[2]);
                    // If header flag is true
                    if (headerFlag == true) {
                        // Set CRC flag by verifying the CRC of synchronization buffer
                        crcFlag = checkPkCRC16(synchBuffer);
                        
                        // If CRC flag is true
                        if (crcFlag == true) {
                            // Set synchronization flag to true
                            synchFlag = true;
                            
                            // Broadcast synch data
                            setArrayDataToSend(extraData, key_dataSynch, synchBuffer, broadCastIntent);
                            broadcastData(broadCastIntent);
                            
                            // Exit while
                            break;
                        }
                        // If CRC flag is false
                        else {
                            // Set synchronization flag to false
                            synchFlag = false;
                        }
                    }
                    // If header flag is false
                    else {
                        // Set synchronization flag to true
                        synchFlag = false;
                    }
                }// End while
            }// End while
		}
		
		// ---- Check if the readByte packet is synchronized by header ----\\
		private boolean checkPkHeader(int h1, int h2, int h3) {
			if (h1 == 102) {
                if (h2 == 102) {
                    if (h3 == 102) {
                        // header OK
                        return true;
                    }
                    else {
                        // header wrong
                        return false;
                    }
                }
                else {
                    // header wrong
                    return false;
                }
            }
            else {
                // header wrong
                return false;
            }
		}
		
		// ---- Check if the readByte packet is synchronized by CRC16 ----\\
		private boolean checkPkCRC16(int[] inPacket) {
            // Variables
            int crcCalc;
            int crcInPk;

            // Calculate the CRC
            crcCalc = CRC16(inPacket);
            // Calculate received CRC
            if (protocolFlag == true) {
            	crcInPk = 256 * inPacket[178] + inPacket[179];
            }
            else {
            	crcInPk = 256 * inPacket[6] + inPacket[7];
            }
            // Compare crcCalc and crcInpk
            if (crcCalc == crcInPk) {
                // CRC OK
                return true;
            }
            else {
                // CRC wrong
                return false;
            }
        }
		
		// ---- Calculate the CRC16 of the readByte packet ----\\
		private int CRC16(int[] databloq) {
			// Table of CRC 16
	    	int[] tabla = {
	            0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
	            0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
	            0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
	            0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
	            0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
	            0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
	            0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
	            0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
	            0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
	            0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
	            0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
	            0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
	            0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
	            0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
	            0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
	            0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
	            0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
	            0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
	            0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
	            0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
	            0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
	            0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
	            0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
	            0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
	            0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
	            0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
	            0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
	            0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
	            0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
	            0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
	            0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
	            0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040
	        };
	    	
	    	/* Calculation of CRC16
	    	 * Were:
	    	 *   ^ = XOR
	    	 *   & = AND
	    	 *   >>> = op1 >>> op2, shifts the bits of op1 to right by the number on op2 (unsigned)
	    	 *   >> = op1 >> op2, shifts the bits of op1 to right by the number on op2
	    	 *   0xff = 255
	    	 */
	        int crc = 0x0000;
	        for (int k=0; k < databloq.length - 2; k++) {
	            crc = (crc >> 8) ^ tabla[ (crc & 0xFF) ^ databloq[k] ];
	        }
	        // Return calculated CRC16
			return crc;
		}
		
		// ---- Extract synchronized data by SA protocol and prepare it to broadcast ----\\
		private void extractDataAndPrepareToSend(int[] synchPacket) {
			// Variables
	    	int MSBEEGsample1;
	    	int LSBEEGsample1;
	    	int MSBEEGsample2;
	    	int LSBEEGsample2;
	    	int EEGSample1;
	    	int EEGSample2;
	    	
			// Get data
			switch (synchPacket[0]) {// Start switch
			case 0:
				// Character 1 of the qCON serial number
				dataExtracted[264] = synchPacket[1];
				break;
			case 1:
				// Character 2 of the qCON serial number
				dataExtracted[265] = synchPacket[1];
				break;
			case 2:
				// Character 3 of the qCON serial number
				dataExtracted[266] = synchPacket[1];
				break;
			case 3:
				// Character 4 of the qCON serial number
				dataExtracted[267] = synchPacket[1];
				break;
			case 4:
				// Character 5 of the qCON serial number
				dataExtracted[268] = synchPacket[1];
				break;
			case 5:
				// Character 1 of the qCON firmware
				dataExtracted[269] = synchPacket[1];
				break;
			case 6:
				// Character 2 of the qCON firmware
				dataExtracted[270] = synchPacket[1];
				break;
			case 7:
				// Character 3 of the qCON firmware
				dataExtracted[271] = synchPacket[1];
				break;
			case 119:
				// QNOX
				dataExtracted[260] = synchPacket[1];
				break;
			case 120:
				// BlockCounter
				// Do nothing
				break;
			case 121:
				// Negative Impedance
				dataExtracted[256] = synchPacket[1];
				break;
			case 122:
				// Reference Impedance
				dataExtracted[257] = synchPacket[1];
				break;
			case 123:
				// Positive Impedance
				dataExtracted[258] = synchPacket[1];
				break;
			case 124:
				// QCON
				dataExtracted[259] = synchPacket[1];
				break;
			case 125:
				// EMG
				dataExtracted[261] = synchPacket[1];
				break;
			case 126:
				// BSR
				dataExtracted[262] = synchPacket[1];
				break;
			case 127:
				// SQI
				dataExtracted[263] = synchPacket[1];
				break;
			default:
				break;
			}// End Switch
			
			// Get the EEG samples
	    	MSBEEGsample1 = synchPacket[2];
			LSBEEGsample1 = synchPacket[3];
			MSBEEGsample2 = synchPacket[4];
			LSBEEGsample2 = synchPacket[5];
			EEGSample1 = 256 * MSBEEGsample1 + LSBEEGsample1;
			EEGSample2 = 256 * MSBEEGsample2 + LSBEEGsample2;
			
			// Set EEG sample 1 on eegArray
			eegArray[eegArrayIndex] = EEGSample1;
			// Increment index
			eegArrayIndex++;
			// Set EEG sample 2 on eegArray
			eegArray[eegArrayIndex] = EEGSample2;
			// Increment index
			eegArrayIndex++;
			
			// Reset eegArray index, prepare data to send, and broadcast data
			if (eegArrayIndex > eegArray.length-1) {
				// Reset index
				eegArrayIndex = 0;
				// Set EEG
				for (int i=0; i<eegArray.length; i++) {
					dataExtracted[i] = eegArray[i];
				}
				// Broadcast extracted data
                setArrayDataToSend(extraData, key_dataSynch, dataExtracted, broadCastIntent);
                broadcastData(broadCastIntent);
			}
		}
		
	}
	
	
	/** --------------------- Stuff to broadcast synchronized data --------------------- **/
	// ---- Set array of data to broadcast ---- \\
	private static void setArrayDataToSend(Bundle bundle, String key, int[] data, Intent intent) {
		// Check each variable
		if ((bundle != null) && (key != null) && (data != null) && (intent != null)) {
			// Put data into extraData (Bundle)
			bundle.putIntArray(key, data);
			// Put extraData (Bundle) into broadCastIntent
	    	intent.putExtras(bundle);
		}
	}
	// ---- Broadcast data ---- \\
	private void broadcastData(Intent intent) {
		// BroadCast data into intent
		if (intent != null) {
			sendBroadcast(intent);
		}
	}
	
	
	/** --------------------- Stuff to store data --------------------- **/
	/* ---- Create and start threads to store binary files ---- */
	private void startStoreBinayFileThread(Context context, boolean createNewFileFlag) {
		// Create thread
		saveBinFileThread = new Class_StoreBinaryFileThread("Thr_StoreBinFile", context, "qCONBINFile", createNewFileFlag);
		// Start thread
		saveBinFileThread.start();
	}
	/* ---- Stop and destroy threads to store binary files ---- */
	private void stopStoreBinayFileThread() {
		if (saveBinFileThread != null) {
			saveBinFileThread.cancel();
			saveBinFileThread = null;
		}
	}	
	
}/* End - main service class */