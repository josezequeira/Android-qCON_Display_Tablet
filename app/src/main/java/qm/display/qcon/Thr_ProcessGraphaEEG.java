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
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

/* Start Main Class */
public class Thr_ProcessGraphaEEG extends Thread {
	/*** Start variables ***/
	// Debugging
	private static final String TAG = "Thr_ProcessGraphaEEG";
	
	// Context
	private static Context Contx;
	
	// Layout
	private static LinearLayout graphsLy;
	
	// Variables and objects of the library AchartEngine and graphics
	private GraphicalView aEEG_Graph;
	private XYSeries aEEG_serie;
	private XYMultipleSeriesDataset aEEG_Dataset;
	private XYSeriesRenderer aEEG_serieRenderer;
    private XYMultipleSeriesRenderer aEEG_Renderer;
    
    // aEEG Graph text and line attributes
    private static String aEEGgraph_titleLabel;
    private static int aEEG_Color;
    private static final int aEEGgraph_lineWidth = 2;
    
    // Members to receive EEG samples and flags from qCON
  	private int[] eegSamples;
  	private static final int eegArraySize = 1024;
  	private static int[] eegArray;
    private static int eegArrayIndex;
    
    // Members to draw aEEG
    private static double windowsPointsXaxis;
    private static double windowsPointsYaxis;
    private static boolean widthMeasureFlag;
    private static double[] aEEGarray;
    private static int aEEGarrayIndexH;
    private static int aEEGarrayIndexT;
    private static int samplesToMean;
    private static double[] aEEGarrayToDraw;
    public static final int xSCALE30MIN = 1;
    public static final int xSCALE1h = 2;
    public static final int xSCALE2h = 3;
    public static final int xSCALE4h = 4;
    public static final int xSCALE12h = 5;
    public static final int xSCALE24h = 6;
    private static boolean x30minFlag;
    private static boolean x1hourFlag;
    private static boolean x2hourFlag;
    private static boolean x4hourFlag;
    private static boolean x12hourFlag;
    private static boolean x24hourFlag;
    
    // Members to filter EEG to calculate aEEG
    private static int[] aEEG_filtredEEG;
    private static final float aEEG_numSamples = 1024;
    private static final int aEEG_filterOrder = 5;
	private static final int aEEG_numCoeff = aEEG_filterOrder + 1;
	private static final int aEEG_InOutSize = (int) (aEEG_numSamples + aEEG_numCoeff);
	private static double[] aEEG_numeratorsB;
	private static double[] aEEG_denominatorsA;
	private static double[] aEEG_inPrevious;
	private static double[] aEEG_outPrevious;
	private static double[] aEEG_in;
	private static double[] aEEG_out;
	private static double aEEG;
	private static double aEEG_previous;
	
	// Thread members
 	private static boolean threadRunning;
 	private static boolean threadWait;
    private static int ID;
    private static final int CANCEL = 1;
    private static final int PROCESS = 2;
	/*** End variables ***/
	
    
    /** --- Constructor --- **/
    public Thr_ProcessGraphaEEG(String ThreadName, Context context, int xAxisScale) {
    	// Set threadName
    	super(ThreadName);
    	// Write on log
    	Log.d(TAG, "--- On Thr_ProcessGraphaEEG constructor ---");
    	
    	// Set context
    	Contx = context;
		
    	// Start thread members
		threadRunning = true;
		threadWait = true;
		ID = 0;
    	
		// Get resources
    	aEEGgraph_titleLabel = Contx.getResources().getString(R.string.Graph_aEEG_Lb_Title);
    	aEEG_Color = Contx.getResources().getColor(R.color.aEEG_Color);
    	
    	// Set the initial attributes for the graphs
		setaEEGgraphAttributes();
    	
		// Create and add graph parameters to layout
    	if (aEEG_Graph == null) {
    		aEEG_Graph = ChartFactory.getLineChartView(Contx, aEEG_Dataset, aEEG_Renderer);
    	}
    	
    	// Start variables
    	eegArray = new int[eegArraySize];
    	eegArrayIndex = 0;
    	aEEG = 0;
    	aEEG_previous = 0;
    	widthMeasureFlag = false;
    	aEEGarray = new double[3600*24];
    	aEEGarrayIndexT = 0;
    	aEEGarrayIndexH = 0;
    	samplesToMean = 0;
    	setaEEGxAxisScale(xAxisScale);
    	
        // Create start filter members
    	aEEG_createStartFilterMembers();
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
			if (ID == PROCESS) {
				// Process EEG
				if ((eegSamples != null) && (threadRunning == true)) {
					// Set EEG samples on EEG array to process
					// and set qCON value
					for (int i=0; i<256; i++) {
						setEEGarray(eegSamples[i], eegSamples[259]);
					}
					
					// Reset buffer
					eegSamples = null;
				}
				// Pause thread
				Pause();
			}
			else if (ID == CANCEL) {
				// Free graphs resources
				freeaEEGgraphResoures();
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
    
    /** --- Thread set EEG values to process --- **/
    public void setEEGvaluesToProcess(int[] samples) {
    	ID = PROCESS;
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
    
    
    /** --------------------- Setters and Getters --------------------- **/
	public double[] getaEEGarray() {
		return aEEGarray;
	}
	public void setaEEGarray(double[] array) {
		if (array != null) {
			aEEGarray = array;
		}
	}
	
	public int getaEEGarrayIndexH() {
		return aEEGarrayIndexH;
	}
	public void setaEEGarrayIndexH(int indexH) {
		aEEGarrayIndexH = indexH;
	}
	
	public int getaEEGarrayIndexT() {
		return aEEGarrayIndexT;
	}
	public void setaEEGarrayIndexT(int indexT) {
		aEEGarrayIndexT = indexT;
	}
	
 	public double[] getaEEGserie() {
		return getSerieToArray(aEEG_serie);
	}
	public void setaEEGserie(double[] Serie) {
		if (Serie != null) {
			// Size of the series
			int serieSize = Serie.length;
			
			if (serieSize > 0) {
				// Fill current cEEG serie
				for (int i=0; i<serieSize-1; i++) {
					aEEG_serie.add(i, Serie[i]);
				}
			}
		}
	}
	
	public double getaEEGvalue() {
		return aEEG;
	}
    
	
	/** --------------------- Methods --------------------- **/
	// ---- Set aEEG layout ---- \\
	public void setaEEGlayout(LinearLayout layout) {
    	// Add graph and parameters to layout
    	if (aEEG_Graph != null) {
    		graphsLy = layout;
    		graphsLy.addView(aEEG_Graph, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    		graphsLy.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    		// Change width measure flag
    		widthMeasureFlag = false;
    	}
	}
	
	// ---- Remove aEEG layout ---- \\
	public void removeaEEGlayout() {
		if (aEEG_Graph != null) {
			final ViewParent graphParent= aEEG_Graph.getParent();
			if (graphParent != null) {
				((ViewGroup)graphParent).removeView(aEEG_Graph);
			}
		}
	}
	
 	// ---- Set aEEG xAxix scale ---- \\
  	public void setaEEGxAxisScale(int scale) {
  		// Set the selected xAxis scale
  		if (scale == xSCALE30MIN) {
  			// Set xAxis flags
  			x30minFlag = true;
  	        x1hourFlag = false;
  	        x2hourFlag = false;
  	        x4hourFlag = false;
  	        x12hourFlag = false;
  	        x24hourFlag = false;
  	        // Reset cEEGarrayIndexH
  	        aEEGarrayIndexH = -1;
  		}
  		else if (scale == xSCALE1h) {
  			// Set xAxis flags
  			x30minFlag = false;
  	        x1hourFlag = true;
  	        x2hourFlag = false;
  	        x4hourFlag = false;
  	        x12hourFlag = false;
  	        x24hourFlag = false;
  	        // Reset cEEGarrayIndexH
  	        aEEGarrayIndexH = -1;
  		}
  		else if (scale == xSCALE2h) {
  			// Set xAxis flags
  			x30minFlag = false;
  	        x1hourFlag = false;
  	        x2hourFlag = true;
  	        x4hourFlag = false;
  	        x12hourFlag = false;
  	        x24hourFlag = false;
  	        // Reset cEEGarrayIndexH
  	        aEEGarrayIndexH = -1;
  		}
  		else if (scale == xSCALE4h) {
  			// Set xAxis flags
  			x30minFlag = false;
  	        x1hourFlag = false;
  	        x2hourFlag = false;
  	        x4hourFlag = true;
  	        x12hourFlag = false;
  	        x24hourFlag = false;
  	        // Reset cEEGarrayIndexH
  	        aEEGarrayIndexH = -1;
  		}
  		else if (scale == xSCALE12h) {
  			// Set xAxis flags
  			x30minFlag = false;
  	        x1hourFlag = false;
  	        x2hourFlag = false;
  	        x4hourFlag = false;
  	        x12hourFlag = true;
  	        x24hourFlag = false;
  	        // Reset cEEGarrayIndexH
  	        aEEGarrayIndexH = -1;
  		}
  		else if (scale == xSCALE24h) {
  			// Set xAxis flags
  			x30minFlag = false;
  	        x1hourFlag = false;
  	        x2hourFlag = false;
  	        x4hourFlag = false;
  	        x12hourFlag = false;
  	        x24hourFlag = true;
  	        // Reset cEEGarrayIndexH
  	        aEEGarrayIndexH = -1;
  		}
  	}
 	
 	// ---- Set EEGArray values ---- \\
 	private void setEEGarray(int eegSample, int qconCode) {
     	
     	// Set the EEG sample on eegArray
 		eegArray[eegArrayIndex] = eegSample;
 		// Increment eegArrayIndex by 1
 		eegArrayIndex = eegArrayIndex + 1;
     	
 		// Check if eegArray is full
 		if (eegArrayIndex >= eegArraySize) {
 			// Check qCON code
 			if (qconCode > 100) {
 				// Set previous value of aEEG to graph
 	 			setAEEGarray(aEEG_previous);
 			}
 			else {
 				// filter EEG for calculate the aEEG
 	 			aEEG_filtredEEG = aEEG_filter(eegArray);
 	 			// Process the EEG filtered to get cEEG value
 	 			aEEG = eegAmplitude(aEEG_filtredEEG);
 	 			// Set cEEG previous value
 	 			aEEG_previous = aEEG;
 	 			// Set cEEG sample on cEEGarray to graph cEEG
 	 			setAEEGarray(aEEG);
 			}
 			
     		// Restart eegArrayIndex
     		eegArrayIndex = 0;
     		aEEG_filtredEEG = null;
 		}
     }
 	
 	// ---- Get Series To Array ---- \\
    private double[] getSerieToArray(XYSeries serie) {
    	
    	// Size of the series
    	int serieSize = serie.getItemCount();
    	
    	// Create double array
    	double[] serieArray = new double[serieSize];
    	
    	// Fill serieArray with series items
    	for (int i=0; i<serieSize-1; i++) {
    		serieArray[i] = serie.getY(i);
    	}
    	
    	// Return serieArray
    	return serieArray;
    }
    
    
    /** --------------------- aEEG filter and process methods --------------------- **/
	// ---- Create and start filter members ---- \\
	// Coefficients to get a low pass Filter FC = 15 Hz
	private static void aEEG_createStartFilterMembers() {
		
		// Create filter members
		aEEG_numeratorsB = new double[aEEG_numCoeff];
		aEEG_denominatorsA = new double[aEEG_numCoeff];
		aEEG_inPrevious = new double[aEEG_numCoeff];
		aEEG_outPrevious = new double[aEEG_numCoeff];
		aEEG_in = new double[aEEG_InOutSize];
		aEEG_out = new double[aEEG_InOutSize];
		
		// Set numerators
		aEEG_numeratorsB[0] = 0.00000017845770622360;
		aEEG_numeratorsB[1] = 0.00000089228853111800;
		aEEG_numeratorsB[2] = 0.00000178457706223600;
		aEEG_numeratorsB[3] = 0.00000178457706223600;
		aEEG_numeratorsB[4] = 0.00000089228853111800;
		aEEG_numeratorsB[5] = 0.00000017845770622360;
		
		// Set denominators
		aEEG_denominatorsA[0] = 1.000000000000000;
		aEEG_denominatorsA[1] = -4.702186629293726;
		aEEG_denominatorsA[2] = 8.852611681997509;
		aEEG_denominatorsA[3] = -8.340774904459835;
		aEEG_denominatorsA[4] = 3.932676200182633;
		aEEG_denominatorsA[5] = -0.742320637779982;
		
		// Start members
		for (int i=0; i<aEEG_numCoeff; i++) {
			aEEG_inPrevious[i] = 31000;
			aEEG_outPrevious[i] = 31000;
		}
	}
	
	// ---- EEG filter ---- \\
	private static int[] aEEG_filter(int[] eegToProcess) {
		// Variables
		int[] eegFiltered = new int[(int) aEEG_numSamples];
		
		// Set previous values to in and out arrays
		// And set next previous in values
		for (int i=0; i<aEEG_numCoeff; i++) {
			aEEG_in[i] = aEEG_inPrevious[i];
			aEEG_out[i] = aEEG_outPrevious[i];
			aEEG_inPrevious[i] = eegToProcess[(int) (aEEG_numSamples-aEEG_numCoeff+i)];
		}
		
		// Filter
		for (int i = aEEG_numCoeff; i < aEEG_InOutSize; i++) {
			aEEG_in[i] = eegToProcess[i-aEEG_numCoeff];
			aEEG_out[i] = 0;
			for (int j = 0; j < aEEG_numCoeff; j++) {
				aEEG_out[i] = aEEG_out[i] + aEEG_numeratorsB[j] * aEEG_in[i-j];
			}
			for (int j = 1; j < aEEG_numCoeff; j++) {
				aEEG_out[i] = aEEG_out[i] - aEEG_denominatorsA[j] * aEEG_out[i-j];
			}
		}
		
		// Set next previous out values
		for (int i = 0; i < aEEG_numCoeff; i++) {
			aEEG_outPrevious[i] = aEEG_out[(int) (aEEG_numSamples+i)];
		}
		// Set array to deliver filtered signal
		for (int i = 0; i < aEEG_numSamples; i++) {
			eegFiltered[i] = (int) aEEG_out[i+aEEG_numCoeff];
		}
		
		// Return filtered signal
		return eegFiltered;
	}
	
	// ---- Calculate EEG mean ---- \\
	private static double eegMean(int[] eegFiltered) {
		// Variables
		double sum = 0.0;
		double mean = 0.0;
		
		// Sum of all EEG samples
		for (int i=0; i<aEEG_numSamples; i++) {
			sum = sum + eegFiltered[i];
		}
		// Calculate mean
		mean = sum/aEEG_numSamples;
		
		return mean;
	}
	
	// ---- Process EEG amplitude (aEEG) ---- \\
	private static double eegAmplitude(int[] eegFiltered) {
		// Variables
		double eegMean;
		double sum = 0.0;
		double amplitude = 0.0;
		
		// Get the mean
		eegMean = eegMean(eegFiltered);
		
		// Subtract DC level and get EEG summation
		for (int i=0; i<aEEG_numSamples; i++) {
			sum = sum + (Math.abs(eegFiltered[i] - eegMean));
		}
		
		// Calculate EEG amplitude
		amplitude = sum/aEEG_numSamples;
		
		return amplitude;
	}
	
	
	/** --------------------- aEEG drawing methods --------------------- **/
	// ---- Set aEEG Graph Attributes ---- \\
	private void setaEEGgraphAttributes() {
		Log.d(TAG, "--- On setaEEGgraphAttributes ---");
		
		// Members creation
		aEEG_Dataset = new XYMultipleSeriesDataset();
		aEEG_serie = new XYSeries(aEEGgraph_titleLabel);
		aEEG_serieRenderer = new XYSeriesRenderer();
		aEEG_Renderer = new XYMultipleSeriesRenderer();
		
		// ---------- Set initial variables ----------
        //windowsPointsXaxis = 3600.0;									// Numero de puntos de la ventana en el eje X
		windowsPointsXaxis = 1024.0;
        windowsPointsYaxis = 2000.0;									// Numero de puntos de la ventana en el eje Y
        
        // ---------- Add series to data set ----------
    	aEEG_Dataset.addSeries(aEEG_serie);								// Se pone la serie en el conjunto de datos
        
    	// ---------- Series customization ----------
    	aEEG_serieRenderer.setColor(aEEG_Color);						// Color de la serie
    	aEEG_serieRenderer.setLineWidth(aEEGgraph_lineWidth);			// Grosor de la linea
    	
        // ---------- Chart elements customization ----------
    	aEEG_Renderer.addSeriesRenderer(aEEG_serieRenderer);			// Agregar atributos de la serie a la ventana
        aEEG_Renderer.setApplyBackgroundColor(true);					// Color de fondo - aplicar
        aEEG_Renderer.setBackgroundColor(Color.argb(100, 50, 50, 50));	// Color de fondo - color
        aEEG_Renderer.setMargins(new int[] {22, 40, -40, 5});			// Margin size values, in this order: top, left, bottom, right
        aEEG_Renderer.setChartTitle(aEEGgraph_titleLabel);				// Titulo de la grafica - Titulo
    	aEEG_Renderer.setChartTitleTextSize(20);						// Titulo de la grafica - Tamaño
    	
    	aEEG_Renderer.setShowAxes(true);								// Axes - Mostrar ejes
    	aEEG_Renderer.setAxesColor(Color.LTGRAY);						// Axes - color de los ejes X y Y
    	
    	aEEG_Renderer.setShowLabels(true);								// Labels - Mostrar labels
    	aEEG_Renderer.setLabelsTextSize(18);							// Labels - Tamaño del texto
    	aEEG_Renderer.setLabelsColor(Color.LTGRAY);						// Labels - Color
    	aEEG_Renderer.setXLabels(0);									// Labels - establece aprox el número de labels del eje X
    	aEEG_Renderer.setXLabelsAlign(Align.CENTER);					// Labels - alineacion del label del eje X
    	aEEG_Renderer.setYLabels(3);									// Labels - establece aplox el número de labels del eje Y
    	aEEG_Renderer.setYLabelsAlign(Align.RIGHT);						// Labels - alineacion del label del eje Y
    	
    	aEEG_Renderer.setShowLegend(false);								// Legend - Mostrar legenda
    	
    	aEEG_Renderer.setShowGrid(false);								// Cuadricula - Mostrar cuadricula
    	aEEG_Renderer.setShowGridX(false);								// Cuadricula - Mostrar cuadricula en X
    	aEEG_Renderer.setShowGridY(false);								// Cuadricula - Mostrar cuadricula en Y
    	aEEG_Renderer.setShowCustomTextGrid(false);						// Cuadricula - Mostrar cuadricula del texto
    	
    	aEEG_Renderer.setPanEnabled(true);								// Panorama - Aplicar
    	aEEG_Renderer.setPanEnabled(true, true);						// Panorama - Aplicar al eje X y Y
    	//aEEG_Renderer.setPanLimits(new double[] {0, maxPointsChart, -5, windowsPointsYaxis});// Panorama - Limite de movimiento en los ejes X y Y
    	//aEEG_Renderer.setExternalZoomEnabled(false);					// Zoom - Desabilitar zoom externo
    	aEEG_Renderer.setZoomEnabled(true);								// Zoom - Aplicar
    	aEEG_Renderer.setZoomEnabled(true, true);						// Zoom - Aplicar a los ejes X y Y
    	aEEG_Renderer.setZoomButtonsVisible(true);						// Zoom - Mostrar el boton de zoom
    	//aEEG_Renderer.setZoomLimits(new double[] {0, windowsPointsXaxis, -5, windowsPointsYaxis});// Zoom - Limite del zoom en los ejes
    	
    	aEEG_Renderer.setAxisTitleTextSize(18);							// Eje XY - tamaño de los titulos de los ejes X y Y
    	
    	//aEEG_Renderer.setXTitle("");									// Eje X - Titulo
    	aEEG_Renderer.setXAxisMin(0.0);									// Eje X - limite inferior de la ventana
    	aEEG_Renderer.setXAxisMax(windowsPointsXaxis);					// Eje X - limite superior de la ventana
    	
    	//aEEG_Renderer.setYTitle("");									// Eje Y - Titulo
    	aEEG_Renderer.setYAxisMin(0.0);									// Eje Y - limite inferior de la ventana
    	aEEG_Renderer.setYAxisMax(windowsPointsYaxis);					// Eje Y - limite superior de la ventana
    	//aEEG_Renderer.setYAxisAlign(Align.LEFT, 0);					// Eje Y - alineacion del eje
    	
	}
	
	// ---- Measure aEEG Graph Layout width to set number of points on X axis ---- \\
	private void measureLyWidth() {
		// Variables
		double temp1 = 0.0;
		double temp2 = 0.0;
		
		// Get Layout width and set windowsPointsXaxis on first time
		if ((widthMeasureFlag == false) && (graphsLy != null) && (aEEG_Renderer != null)) {
			// Calculate the X points
			windowsPointsXaxis = graphsLy.getWidth() - 45;
			temp1 = 1800/windowsPointsXaxis;
			temp2 = Math.round(temp1);
			windowsPointsXaxis = 1800/temp2;
			// Set X points
			aEEG_Renderer.setXAxisMax(windowsPointsXaxis);
			// Change flag
			widthMeasureFlag = true;
			Log.i(TAG, "------------------------ Here! ---------------------------");
			Log.i(TAG, "Width= " + String.valueOf(windowsPointsXaxis));
		}
	}
	
	// ---- Calculate aEEG scale array to draw ---- \\
	private static double[] calcaEEGtoDraw(double[] samples, int totalPoints, int samplestomean, int end) {
		// Variables
		double sum;
		double mean;
		double[] aEEGtoDraw;
		
		int steps = totalPoints/samplestomean;
		int start = end - totalPoints;
		int midleEnd = start + samplestomean;
		
		if (steps < 0) {
			steps = 0;
		}
		
		aEEGtoDraw = new double[steps];
		
		for (int i=0; i<steps; i++) {
			// Clear variables
			sum = 0.0;
			mean = 0.0;
			
			// Sum of EEG samples
			for (int j=start; j<midleEnd; j++) {
				sum = sum + samples[j];
			}
			// Calculate mean
			mean = sum/samplestomean;
			
			// Set aEEG mean to array
			aEEGtoDraw[i] = mean;
			
			// Reset start and midleEnd
			start = midleEnd;
			midleEnd = start + samplestomean;
		}
		
		return aEEGtoDraw;
	}
	
	// ---- Calculate aEEG scale array to draw 2 ---- \\
	private static double[] calcaEEGtoDraw2(double[] samples, int samplestomean, int end) {
		// Variables
		double sum;
		double mean;
		double[] aEEGtoDraw;
		
		int steps = end/samplestomean;
		int start = 0;
		int midleEnd = samplestomean;
		
		if (steps < 0) {
			steps = 0;
		}
		
		aEEGtoDraw = new double[steps];
		
		for (int i=0; i<steps; i++) {
			// Clear variables
			sum = 0.0;
			mean = 0.0;
			
			// Sum of EEG samples
			for (int j=start; j<midleEnd; j++) {
				sum = sum + samples[j];
			}
			// Calculate mean
			mean = sum/samplestomean;
			
			// Set cEEG mean to array
			aEEGtoDraw[i] = mean;
			
			// Reset start and midleEnd
			start = midleEnd;
			midleEnd = start + samplestomean;
		}
		
		return aEEGtoDraw;
	}
	
	// ---- Set aEEGArray values ---- \\
	private void setAEEGarray(double aEEGsample) {
		
		if (aEEGarrayIndexT >= aEEGarray.length) {
			// Move one value to the left
			for (int i=1; i<aEEGarray.length; i++) {
				aEEGarray[i-1] = aEEGarray[i];
        	}
			// Set the aEEG sample at the end of aEEGarray
			aEEGarray[aEEGarray.length - 1] = aEEGsample;
			// Decrement aEEGarrayIndexH
			aEEGarrayIndexH = aEEGarrayIndexH - 1;
		}
		else {
			// Set the aEEG sample on aEEGarray
			aEEGarray[aEEGarrayIndexT] = aEEGsample;
			// Increment aEEGarrayIndex by 1
			aEEGarrayIndexT = aEEGarrayIndexT + 1;
		}
		
		//Check time flags to graph aEEG
		if (x30minFlag == true) {
			// Get samples to mean
			samplesToMean = (int) (1800/windowsPointsXaxis);
			
			if (aEEGarrayIndexT >= 1800) {
				if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
					// Calculate aEEG array to draw
					aEEGarrayToDraw = calcaEEGtoDraw(aEEGarray, 1800, samplesToMean, aEEGarrayIndexT);
					// Clear aEEG serie
					clearaEEGserie();
					// Add the aEEG array calculated to aEEG serie
					setaEEGarrayToSerie(aEEGarrayToDraw);
					// Repaint aEEG serie
					repaintaEEGserie();
					// Set aEEGarrayIndexH
					aEEGarrayIndexH = aEEGarrayIndexT;
				}
			}
			else if (aEEGarrayIndexH < 0) {
				// Calculate aEEG array to draw
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				// Clear aEEG serie
				clearaEEGserie();
				// Add the aEEG array calculated to aEEG serie
				setaEEGarrayToSerie(aEEGarrayToDraw);
				// Repaint aEEG serie
				repaintaEEGserie();
				// Set aEEGarrayIndexH
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
			else if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
				// Calculate aEEG array to draw
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				// Clear aEEG serie
				clearaEEGserie();
				// Add the aEEG array calculated to aEEG serie
				setaEEGarrayToSerie(aEEGarrayToDraw);
				// Repaint aEEG serie
				repaintaEEGserie();
				// Set aEEGarrayIndexH
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
		}
		else if (x1hourFlag == true) {
			// Get samples to mean
			samplesToMean = (int) (3600/windowsPointsXaxis);
			
			if (aEEGarrayIndexT >= 3600) {
				if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
					aEEGarrayToDraw = calcaEEGtoDraw(aEEGarray, 3600, samplesToMean, aEEGarrayIndexT);
					clearaEEGserie();
					setaEEGarrayToSerie(aEEGarrayToDraw);
					repaintaEEGserie();
					aEEGarrayIndexH = aEEGarrayIndexT;
				}
			}
			else if (aEEGarrayIndexH < 0) {
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				clearaEEGserie();
				setaEEGarrayToSerie(aEEGarrayToDraw);
				repaintaEEGserie();
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
			else if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				clearaEEGserie();
				setaEEGarrayToSerie(aEEGarrayToDraw);
				repaintaEEGserie();
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
		}
		else if (x2hourFlag == true) {
			// Get samples to mean
			samplesToMean = (int) (7200/windowsPointsXaxis);
			
			if (aEEGarrayIndexT >= 7200) {
				if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
					aEEGarrayToDraw = calcaEEGtoDraw(aEEGarray, 7200, samplesToMean, aEEGarrayIndexT);
					clearaEEGserie();
					setaEEGarrayToSerie(aEEGarrayToDraw);
					repaintaEEGserie();
					aEEGarrayIndexH = aEEGarrayIndexT;
				}
			}
			else if (aEEGarrayIndexH < 0) {
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				clearaEEGserie();
				setaEEGarrayToSerie(aEEGarrayToDraw);
				repaintaEEGserie();
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
			else if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				clearaEEGserie();
				setaEEGarrayToSerie(aEEGarrayToDraw);
				repaintaEEGserie();
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
		}
		else if (x4hourFlag == true) {
			// Get samples to mean
			samplesToMean = (int) (14400/windowsPointsXaxis);
			
			if (aEEGarrayIndexT >= 14400) {
				if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
					aEEGarrayToDraw = calcaEEGtoDraw(aEEGarray, 14400, samplesToMean, aEEGarrayIndexT);
					clearaEEGserie();
					setaEEGarrayToSerie(aEEGarrayToDraw);
					repaintaEEGserie();
					aEEGarrayIndexH = aEEGarrayIndexT;
				}
			}
			else if (aEEGarrayIndexH < 0) {
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				clearaEEGserie();
				setaEEGarrayToSerie(aEEGarrayToDraw);
				repaintaEEGserie();
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
			else if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				clearaEEGserie();
				setaEEGarrayToSerie(aEEGarrayToDraw);
				repaintaEEGserie();
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
		}
		else if (x12hourFlag == true) {
			// Get samples to mean
			samplesToMean = (int) (43200/windowsPointsXaxis);
			
			if (aEEGarrayIndexT >= 43200) {
				if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
					aEEGarrayToDraw = calcaEEGtoDraw(aEEGarray, 43200, samplesToMean, aEEGarrayIndexT);
					clearaEEGserie();
					setaEEGarrayToSerie(aEEGarrayToDraw);
					repaintaEEGserie();
					aEEGarrayIndexH = aEEGarrayIndexT;
				}
			}
			else if (aEEGarrayIndexH < 0) {
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				clearaEEGserie();
				setaEEGarrayToSerie(aEEGarrayToDraw);
				repaintaEEGserie();
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
			else if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				clearaEEGserie();
				setaEEGarrayToSerie(aEEGarrayToDraw);
				repaintaEEGserie();
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
		}
		else if (x24hourFlag == true) {
			// Get samples to mean
			samplesToMean = (int) (86400/windowsPointsXaxis);
			
			if (aEEGarrayIndexT >= 86400) {
				if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
					aEEGarrayToDraw = calcaEEGtoDraw(aEEGarray, 86400, samplesToMean, aEEGarrayIndexT);
					clearaEEGserie();
					setaEEGarrayToSerie(aEEGarrayToDraw);
					repaintaEEGserie();
					aEEGarrayIndexH = aEEGarrayIndexT;
				}
			}
			else if (aEEGarrayIndexH < 0) {
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				clearaEEGserie();
				setaEEGarrayToSerie(aEEGarrayToDraw);
				repaintaEEGserie();
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
			else if ((aEEGarrayIndexT-aEEGarrayIndexH) >= samplesToMean) {
				aEEGarrayToDraw = calcaEEGtoDraw2(aEEGarray, samplesToMean, aEEGarrayIndexT);
				clearaEEGserie();
				setaEEGarrayToSerie(aEEGarrayToDraw);
				repaintaEEGserie();
				aEEGarrayIndexH = aEEGarrayIndexT;
			}
		}
	}
	
	// ---- Clear aEEG serie ---- \\
	private void clearaEEGserie() {
		if (aEEG_serie != null) {
			aEEG_serie.clear();
		}
	}
	
	// ---- Add aEEG array to aEEG serie ---- \\
	private void setaEEGarrayToSerie(double[] arrayToDraw) {
		// Add aEEG array to aEEG serie
		if (aEEG_serie != null) {
			for (int i=0; i<arrayToDraw.length; i++) {
				aEEG_serie.add(i, arrayToDraw[i]);
			}
		}
	}
	
	// ---- Repaint aEEG serie ---- \\
	private void repaintaEEGserie() {
		// Repaint aEEG chart
		if (aEEG_Graph != null) {
			aEEG_Graph.repaint();
		}
		// Measure layout width
		measureLyWidth();
	}
	
	// ---- Free aEEG graphics resources ---- \\
	private void freeaEEGgraphResoures() {
		Log.d(TAG, "--- On freeaEEGgraphResoures ---");
		
    	aEEG_serie.clear();
    	aEEG_Renderer.removeSeriesRenderer(aEEG_serieRenderer);
    	aEEG_Dataset.removeSeries(aEEG_serie);
    	aEEG_Graph = null; 
	}
	
	
}/* End Main Class */