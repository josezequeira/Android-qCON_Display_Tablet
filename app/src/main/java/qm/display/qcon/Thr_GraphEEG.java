package qm.display.qcon;

/* Libraries imported */
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

/* Start Main Class */
public class Thr_GraphEEG extends Thread {
	/*** Start variables ***/
	// Debugging
	private static final String TAG = "Thr_GraphEEG";
	
	// Context
	private static Context Contx;
	
	// Variables and objects of the library AchartEngine and graphics
	private GraphicalView EEGGraph;
	private XYSeries EEGserie;
	private XYMultipleSeriesDataset EEGDataset;
	private XYSeriesRenderer EEGserieRenderer;
    private XYMultipleSeriesRenderer EEGRenderer;
    
    // EEG Graph text and line attributes
    private static String EEGgraph_titleLabel;
    private static int EEG_Color;
    private static final int EEGgraph_lineWidth = 2;
    private static int YrefVal;
    
    // Members to receive EEG samples
    private static boolean connProtocol;	// True for OEM, false for SA
 	private int[] eegSamples;
 	private static int eegArraySize;
 	private static int[] eegArray;
    private static int eegArrayIndex;
    
    // Members to draw EEG
    private static int eegArrayCopySize;
    private static int[] eegArrayCopy;
    private static int eegArrayCopyIndex;
    private double numberOfPointsEEGChart;
    
 	// Members to set EEG drawing modes
 	private static boolean drawingModeFlag;
 	
    // Members to filter EEG
 	private static boolean filterEEGflag;
	private static int[] filtredEEG;
	private static float numSamples;
	private static final int filterOrder = 5;
	private static final int numCoeff = filterOrder + 1;
	private static int InOutSize;
	private static double[] numeratorsB;
	private static double[] denominatorsA;
	private static double[] inPrevious;
	private static double[] outPrevious;
	private static double[] in;
	private static double[] out;
    
    // Thread members
 	private static boolean threadRunning;
 	private static boolean threadWait;
    private static int ID;
    private static final int CANCEL = 1;
    private static final int INITEEGGRAPH = 2;
    private static final int GRAPH = 3;
	/*** End variables ***/
	
    
    /** --- Constructor --- **/
    public Thr_GraphEEG(String ThreadName, Context context, boolean connectionProtocol) {
    	// Set threadName
    	super(ThreadName);
    	// Write on log
    	Log.d(TAG, "--- On Thr_GraphEEG constructor ---");
    	// Set context
    	Contx = context;
		// Start thread members
		threadRunning = true;
		threadWait = true;
		ID = 0;
    	// Get resources
    	EEGgraph_titleLabel = context.getResources().getString(R.string.Graph_EEG_Lb_Title);
    	EEG_Color = context.getResources().getColor(R.color.EEG_Color_screen_qCON);
    	// Set variables according the connection protocol
    	setEEGconnProtocol(context, connectionProtocol);
    }
	
    /** --- Thread execution --- **/
    public void run() {
    	while (threadRunning) {
    		// Check if should wait
			synchronized (this) {
				while (threadWait) {
					try {
						wait();
					} catch (InterruptedException e) {
						Log.e(TAG, "Error checking if thread should wait", e);
					}
				}
			}
			// Do work
			if (ID == INITEEGGRAPH) {
				setEEGiniValues();
				// Pause thread
				Pause();
			}
			else if (ID == GRAPH) {
				// Graph EEG
				if ((eegSamples != null) && (threadRunning == true)) {
					// Set EEG samples on EEG array to process and graph, according protocol flag
					if (connProtocol == true) {
						for (int i=0; i<100; i++) {
							setEEGarray(eegSamples[i + 26]);
						}
					}
					else {
						for (int i=0; i<256; i++) {
							setEEGarray(eegSamples[i]);
						}
					}
					// Reset buffer
					eegSamples = null;
				}
				// Pause thread
				Pause();
			}
			else if (ID == CANCEL) {
				// Free graphs resources
				freeEEGgraphResoures();
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
    
    /** --- Thread initialize EEG graph --- **/
    public void initEEGgraph() {
    	ID = INITEEGGRAPH;
		synchronized (this) {
			threadWait = false;
			notify();
		}
	}
    
    /** --- Thread set EEG values to graph --- **/
    public void setEEGvaluesToGraph(int[] samples) {
    	ID = GRAPH;
    	eegSamples = samples;
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
	// ---- Set EEG variables according to connection protocol ---- \\
	public void setEEGconnProtocol(Context context, boolean connProt) {
		// Free graphs resources
		freeEEGgraphResoures();
		
		// Set connection protocol
		connProtocol = connProt;
		
		// Set the initial attributes for the graphs
		setEEGgraphAttributes(connProtocol);
		
		// Set variables according the connection protocol
    	if (connProtocol == true) {
    		YrefVal = 121;
    		eegArraySize = 100;
    		eegArrayCopySize = 300;
    		numSamples = 100;
    	}
    	else {
    		YrefVal = 31000;
    		eegArraySize = 1024;
    		eegArrayCopySize = 3072;
    		numSamples = 1024;
    	}
    	
    	// Start variables
		drawingModeFlag = true;		// False indicates erasing mode and True rolling mode
    	eegArray = new int[eegArraySize];
    	eegArrayCopy = new int[eegArrayCopySize];
    	eegArrayIndex = 0;
    	eegArrayCopyIndex = 0;
    	InOutSize = (int) (numSamples + numCoeff);
    	// Create start filter members
    	createStartFilterMembers(YrefVal);
    	
    	// Create graph
    	EEGGraph = ChartFactory.getLineChartView(context, EEGDataset, EEGRenderer);
	}
	
	// ---- Set EEG layout ---- \\
	public void setEEGlayout(LinearLayout layout) {
    	// Add graph and parameters to layout
    	if (EEGGraph != null) {
    		layout.addView(EEGGraph, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    		layout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    	}
	}
	
	// ---- Remove EEG layout ---- \\
	public void removeEEGlayout() {
		if (EEGGraph != null) {
			final ViewParent graphParent= EEGGraph.getParent();
			if (graphParent != null) {
				((ViewGroup)graphParent).removeView(EEGGraph);
			}
		}
	}
	
	// ---- Set EEG series Color ---- \\
	public void setEEGcolor(int screen) {
		if ((EEGserieRenderer != null) && (Contx != null)) {
			// Get color
			if (screen == 1) {
				EEG_Color = Contx.getResources().getColor(R.color.EEG_Color_screen_qCON);
			}
			else if (screen == 2) {
				EEG_Color = Contx.getResources().getColor(R.color.EEG_Color_screen_qNOX);
			}
			// Set color
			EEGserieRenderer.setColor(EEG_Color);
		}
	}
	
	// ---- Set EEG initial Values ---- \\
	private void setEEGiniValues() {
		Log.d(TAG, "--- On setEEGiniValues ---");
		// Set initial points on EEG
		for (int i=0; i<numberOfPointsEEGChart; i++) {
			EEGserie.add(i, YrefVal);
    		eegArrayCopy[i] = YrefVal;
    	}
		for (int i=0; i<eegArray.length; i++) {
    		eegArray[i] = YrefVal;
    	}
		// Repaint EEG graph
		repaintEEGchart();
	}
	
	// ---- Set EEGArray values ---- \\
	private void setEEGarray(int eegSample) {
    	
    	// Set the EEG sample on eegArray
		eegArray[eegArrayIndex] = eegSample;
		// Increment eegArrayIndex by 1
		eegArrayIndex = eegArrayIndex + 1;
    	
		// Check if eegArray is full
		if (eegArrayIndex >= eegArraySize) {
			// Check flag to filter EEG to graph
			if (filterEEGflag == true) {
				// filter EEG
				filtredEEG = filter(eegArray);
				// Draw EEG
				if (drawingModeFlag == true) {
					// Copy eegArray with rolling mode and graph the EEG
					setRollingMode(filtredEEG);
				}
				else {
					// Copy eegArray with erasing mode and graph the EEG
					setErasingMode(filtredEEG);
				}
			}
			else {
				// Draw EEG
				if (drawingModeFlag == true) {
					// Copy eegArray with rolling mode and graph the EEG
					setRollingMode(eegArray);
				}
				else {
					// Copy eegArray with erasing mode and graph the EEG
					setErasingMode(eegArray);
				}
			}
			
    		// Restart eegArrayIndex
    		eegArrayIndex = 0;
    		filtredEEG = null;
		}
    }
	
	
	/** --------------------- EEG filter methods --------------------- **/
	// ---- Get EEG filter flag ---- \\
	public boolean getFilterEEGflag() {
		return filterEEGflag;
	}
	
	// ---- Set EEG filter flag ---- \\
	public void setFilterEEGflag(boolean flag) {
		filterEEGflag = flag;
	}
	
	// ---- Create and start filter members ---- \\
	// Coefficients to get a low pass Filter FC = 30 Hz
	private static void createStartFilterMembers(int refCero) {
		
		// Start filter flag
		filterEEGflag = false;
		
		// Create filter members
		numeratorsB = new double[numCoeff];
		denominatorsA = new double[numCoeff];
		inPrevious = new double[numCoeff];
		outPrevious = new double[numCoeff];
		in = new double[InOutSize];
		out = new double[InOutSize];
		
		// Set numerators
		numeratorsB[0] = 0.0000049707424865653;
		numeratorsB[1] = 0.0000248537124328263;
		numeratorsB[2] = 0.0000497074248656526;
		numeratorsB[3] = 0.0000497074248656526;
		numeratorsB[4] = 0.0000248537124328263;
		numeratorsB[5] = 0.0000049707424865653;
		
		// Set denominators
		denominatorsA[0] = 1.000000000000000;
		denominatorsA[1] = -4.404554926947396;
		denominatorsA[2] = 7.791662106989460;
		denominatorsA[3] = -6.917241379058562;
		denominatorsA[4] = 3.080902229276263;
		denominatorsA[5] = -0.550608966500194;
		
		// Start members
		for (int i=0; i<numCoeff; i++) {
			inPrevious[i] = refCero;
			outPrevious[i] = refCero;
		}
	}
	
	// ---- EEG filter ---- \\
	private static int[] filter(int[] eegToProcess) {
		// Variables
		int[] eegFiltered = new int[(int) numSamples];
		
		// Set previous values to in and out arrays
		// And set next previous in values
		for (int i=0; i<numCoeff; i++) {
			in[i] = inPrevious[i];
			out[i] = outPrevious[i];
			inPrevious[i] = eegToProcess[(int) (numSamples - numCoeff + i)];
		}
		
		// Filter
		for (int i = numCoeff; i < InOutSize; i++) {
			in[i] = eegToProcess[i - numCoeff];
			out[i] = 0;
			for (int j = 0; j < numCoeff; j++) {
				out[i] = out[i] + numeratorsB[j] * in[i-j];
			}
			for (int j = 1; j < numCoeff; j++) {
				out[i] = out[i] - denominatorsA[j] * out[i-j];
			}
		}
		
		// Set next previous out values
		for (int i = 0; i < numCoeff; i++) {
			outPrevious[i] = out[(int) (numSamples + i)];
		}
		// Set array to deliver filtered signal
		for (int i = 0; i < numSamples; i++) {
			eegFiltered[i] = (int) out[i + numCoeff];
		}
		
		// Return filtered signal
		return eegFiltered;
	}
	
	
	/** --------------------- EEG drawing methods --------------------- **/
	// ---- Set EEG Y scale ---- \\
	public void setEEGYscale(int Yscale) {
		Log.d(TAG, "--- On setEEGYscale ---");
		
		// Check connection protocol
		if (connProtocol == true) {
			// Adjust Y axis scale
			switch (Yscale) { // Start Switch
			case 1:
				EEGRenderer.setYTitle(Contx.getResources().getString(R.string.ActionBar_EEGscale_25));
				EEGRenderer.setYAxisMin(YrefVal - 7);
		    	EEGRenderer.setYAxisMax(YrefVal + 7);
				break;
			case 2:
				EEGRenderer.setYTitle(Contx.getResources().getString(R.string.ActionBar_EEGscale_50));
				EEGRenderer.setYAxisMin(YrefVal - 14);
		    	EEGRenderer.setYAxisMax(YrefVal + 14);
				break;
			case 3:
				EEGRenderer.setYTitle(Contx.getResources().getString(R.string.ActionBar_EEGscale_120));
				EEGRenderer.setYAxisMin(YrefVal - 33);
		    	EEGRenderer.setYAxisMax(YrefVal + 33);
				break;
			case 4:
				EEGRenderer.setYTitle(Contx.getResources().getString(R.string.ActionBar_EEGscale_250));
				EEGRenderer.setYAxisMin(YrefVal - 68);
		    	EEGRenderer.setYAxisMax(YrefVal + 68);
				break;
			case 5:
				EEGRenderer.setYTitle(Contx.getResources().getString(R.string.ActionBar_EEGscale_475));
				EEGRenderer.setYAxisMin(YrefVal - 121);
		    	EEGRenderer.setYAxisMax(YrefVal + 134);
				break;
			default:
				break;
			} // End Switch
		}
		else {
			// Adjust Y axis scale
			switch (Yscale) { // Start Switch
			case 1:
				EEGRenderer.setYTitle(Contx.getResources().getString(R.string.ActionBar_EEGscale_25));
				EEGRenderer.setYAxisMin(YrefVal - 1724);
		    	EEGRenderer.setYAxisMax(YrefVal + 1724);
				break;
			case 2:
				EEGRenderer.setYTitle(Contx.getResources().getString(R.string.ActionBar_EEGscale_50));
				EEGRenderer.setYAxisMin(YrefVal - 3449);
		    	EEGRenderer.setYAxisMax(YrefVal + 3449);
				break;
			case 3:
				EEGRenderer.setYTitle(Contx.getResources().getString(R.string.ActionBar_EEGscale_120));
				EEGRenderer.setYAxisMin(YrefVal - 8277);
		    	EEGRenderer.setYAxisMax(YrefVal + 8277);
				break;
			case 4:
				EEGRenderer.setYTitle(Contx.getResources().getString(R.string.ActionBar_EEGscale_250));
				EEGRenderer.setYAxisMin(YrefVal - 17245);
		    	EEGRenderer.setYAxisMax(YrefVal + 17245);
				break;
			case 5:
				EEGRenderer.setYTitle(Contx.getResources().getString(R.string.ActionBar_EEGscale_475));
				EEGRenderer.setYAxisMin(YrefVal - 31000);
		    	EEGRenderer.setYAxisMax(YrefVal + 34535);
				break;
			default:
				break;
			} // End Switch
		}
	}
	
	// ---- Get drawing mode flag ---- \\
	public boolean getDrawingModeFlag() {
		return drawingModeFlag;
	}
	
	// ---- Set drawing mode flag ---- \\
	public void setDrawingModeFlag(boolean flag) {
		drawingModeFlag = flag;
		if (drawingModeFlag == false) {
			// Restart eegArrayCopyIndex
			eegArrayCopyIndex = 0;
		}
	}
	
	// ---- Set EEG Graph Attributes ---- \\
	private void setEEGgraphAttributes(boolean connProtocol) {
		Log.d(TAG, "--- On setEEGgraphAttributes ---");
		
		EEGDataset = new XYMultipleSeriesDataset();
        EEGRenderer = new XYMultipleSeriesRenderer();
        if (connProtocol == true) {
        	numberOfPointsEEGChart = 300.0;								// Equivalente a 3 seg a la velocidad de Tx del qCON
        }
        else {
        	numberOfPointsEEGChart = 3072.0;							// Equivalente a 3 seg a la velocidad de Tx del qCON
        }
        EEGRenderer.setApplyBackgroundColor(true);						// Color de fondo - aplicar
        EEGRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));	// Color de fondo - color
        EEGRenderer.setMargins(new int[] {22, 30, -30, 10});			// Margin size values, in this order: top, left, bottom, right
        EEGRenderer.setChartTitle(EEGgraph_titleLabel);					// Titulo de la grafica - Titulo
    	EEGRenderer.setChartTitleTextSize(20);							// Titulo de la grafica - Tamaño
    	
    	EEGRenderer.setShowAxes(true);									// Axes - Mostrar ejes
    	EEGRenderer.setAxesColor(Color.LTGRAY);							// Axes - color de los ejes X y Y
    	
    	EEGRenderer.setShowLabels(true);								// Labels - Mostrar labels
    	//EEGRenderer.setLabelsTextSize(16);							// Labels - Tamaño del texto
    	//EEGRenderer.setLabelsColor(Color.LTGRAY);						// Labels - Color
    	EEGRenderer.setXLabels(0);										// Labels - establece aprox el número de labels del eje X
    	//EEGRenderer.setXLabelsAlign(Align.CENTER);					// Labels - alineacion del label del eje X
    	EEGRenderer.setYLabels(0);										// Labels - establece aplox el número de labels del eje Y
    	//EEGRenderer.setYLabelsAlign(Align.RIGHT);						// Labels - alineacion del label del eje Y
    	
    	EEGRenderer.setShowLegend(false);								// Legend - Mostrar legenda
    	//EEGRenderer.setFitLegend(true);								// Legend - auto-ajustar
    	//EEGRenderer.setLegendTextSize(16);							// Legend - Tamaño del texto
    	
    	EEGRenderer.setShowGrid(false);									// Cuadricula - Mostrar cuadricula
    	EEGRenderer.setShowGridX(false);								// Cuadricula - Mostrar cuadricula en X
    	EEGRenderer.setShowGridY(false);								// Cuadricula - Mostrar cuadricula en Y
    	EEGRenderer.setShowCustomTextGrid(false);						// Cuadricula - Mostrar cuadricula del texto
    	
    	EEGRenderer.setPanEnabled(true);								// Panorama - Aplicar
    	EEGRenderer.setPanEnabled(false, true);							// Panorama - Aplicar al eje X y Y
    	//EEGRenderer.setPanLimits(new double[] {0, numberOfPointsEEGChart, -5, 65540});// Panorama - Limite de movimiento en los ejes X y Y
    	//EEGRenderer.setExternalZoomEnabled(false);					// Zoom - Desabilitar zoom externo
    	EEGRenderer.setZoomEnabled(true);								// Zoom - Aplicar
    	EEGRenderer.setZoomEnabled(false, true);						// Zoom - Aplicar a los ejes X y Y
    	EEGRenderer.setZoomButtonsVisible(false);						// Zoom - Mostrar el boton de zoom
    	//EEGRenderer.setZoomLimits(new double[] {0, numberOfPointsEEGChart, -5, 65540});// Zoom - Limite del zoom en los ejes
    	
    	EEGRenderer.setAxisTitleTextSize(18);							// Eje XY - tamaño de los titulos de los ejes X y Y
    	
    	//EEGRenderer.setXTitle("");									// Eje X - Titulo
    	EEGRenderer.setXAxisMin(0.0);									// Eje X - limite inferior
    	EEGRenderer.setXAxisMax(numberOfPointsEEGChart);				// Eje X - limite superior
    	
    	//EEGRenderer.setYTitle("");									// Eje Y - Titulo
    	//EEGRenderer.setYAxisMin(0.0);									// Eje Y - limite inferior
    	//EEGRenderer.setYAxisMax(65535.0);								// Eje Y - limite superior
    	
    	EEGserie = new XYSeries(EEGgraph_titleLabel);					// Se crea la serie
    	EEGDataset.addSeries(EEGserie);									// Se pone la serie en el conjunto de datos
    	
    	EEGserieRenderer = new XYSeriesRenderer();						// Se crea el atributo de la serie
    	EEGserieRenderer.setColor(EEG_Color);							// Color de la serie
    	EEGserieRenderer.setLineWidth(EEGgraph_lineWidth);				// Grosor de la linea
    	EEGRenderer.addSeriesRenderer(EEGserieRenderer);
	}
	
	// ---- Set EEG values with Erasing mode effect ---- \\
	private void setErasingMode(int[] eegArray) {
		
		// Copy new values
		for (int i=0; i<eegArraySize; i++) {
			// Copy EEG samples
			eegArrayCopy[eegArrayCopyIndex] = eegArray[i];
			// Increment eegArrayCopyIndex by 1
			eegArrayCopyIndex = eegArrayCopyIndex + 1;
			if (eegArrayCopyIndex >= eegArrayCopySize) {
				// Restart eegArrayCopyIndex
				eegArrayCopyIndex = 0;
			}
		}
		// Set each EEG sample to graph
    	clearSerieAddNewPoits(eegArrayCopy);
	}
	
	// ---- Set EEG values with Rolling mode effect ---- \\
    private void setRollingMode(int[] eegArray) {
    	// Variables
    	int index = eegArrayCopySize - eegArraySize;
    	
    	// Move old values to the left
    	for (int i=0; i<eegArraySize; i++) {
    		// Move one value to the left
        	for (int j=1; j<eegArrayCopySize; j++) {
        		eegArrayCopy[j-1] = eegArrayCopy[j];
        	}
    	}
    	// Copy new values at end
    	for (int i=index; i<eegArrayCopySize; i++) {
    		eegArrayCopy[i] = eegArray[i-index];
    	}
    	// Set each EEG sample to graph
    	clearSerieAddNewPoits(eegArrayCopy);
    }
	
    // ---- Clear serie and add new points to the serie ---- \\
    private void clearSerieAddNewPoits(int[] array) {
    	if (EEGserie != null) {
    		// Clear serie
    		EEGserie.clear();
    		
    		// Add new points to serie
        	for (int i=0; i<array.length; i++) {
        		EEGserie.add(i, array[i]);
        	}
    	}
    	// Repaint EEG graph
		repaintEEGchart();
    }
    
    // ---- Repaint EEG chart ---- \\
    private void repaintEEGchart() {
    	if (EEGGraph != null) {
    		EEGGraph.repaint();
    	}
    }
    
    // ---- Free EEG graphics resources ---- \\
	private void freeEEGgraphResoures() {
		Log.d(TAG, "--- On freeEEGgraphResoures ---");
		
		if ((EEGserie!= null) && (EEGDataset!= null)) {
			EEGserie.clear();
			EEGDataset.removeSeries(EEGserie);
		}
    	
		if ((EEGserieRenderer != null) && (EEGRenderer != null)) {
			EEGRenderer.removeSeriesRenderer(EEGserieRenderer);
		}
    	
    	EEGserie = null;
    	EEGserieRenderer = null;
    	EEGRenderer = null;
    	EEGDataset = null;
    	EEGGraph = null; 
	}
    
	
}/* End Main Class */