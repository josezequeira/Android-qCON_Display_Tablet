package qm.display.qcon;

/* Libraries imported */
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.LinearLayout;

/* Start Main Class */
public class Thr_GraphIndexes extends Thread {
	
	/*** Start variables ***/
	// Debugging
	private static final String TAG = "Thr_GraphIndexes";
	
	// Variables and objects of the library AchartEngine and graphics
	private static GraphicalView IndexesGraph;
    private static XYMultipleSeriesDataset IndexesDataset;
    private static XYMultipleSeriesRenderer IndexesRenderer;
    private static XYSeries QCONserie;
    private static XYSeries QNOXserie;
    private static XYSeries EMGserie;
    private static XYSeries BSRserie;
    private static XYSeries SQIserie;
    private static XYSeriesRenderer QCONserieRenderer, QNOXserieRenderer, EMGserieRenderer, BSRserieRenderer, SQIserieRenderer;
    
    // Indicators text and lines attributes
    private static int qCON_Color;
    private static int qNOX_Color;
    private static int EMG_Color;
    private static int BSR_Color;
    private static int SQI_Color;
    private static int serieQCON_lineWidth = 5;
    private static int serieQNOX_lineWidth = 4;
    private static int serieEMG_lineWidth = 3;
    private static int serieBSR_lineWidth = 4;
    private static int serieSQI_lineWidth = 2;
    
    // Members to graph
    private static boolean IndexesShowOnUiFlag, SQIshowOnUIFlag;
    private static double numberOfPointsIndexesChart;
    private static long IndexesEjeXtick;
    private static int qCON;
    private static int qNOX;
    private static int EMG;
    private static int BSR;
    private static int SQI;
    private static boolean qNOX_ShowSerie;
    
	// Members
 	private static volatile boolean threadRunning;
 	private static volatile boolean threadWait;
    private static int ID;
    private static final int CANCEL = 0;
    private static final int SETVALUESTOGRAPH = 1;
	/*** End variables ***/
	
    /** --- Constructor --- **/
    public Thr_GraphIndexes(String ThreadName, Context context) {
    	super(ThreadName);
    	Log.d(TAG, "--- On Thr_GraphIndexes constructor ---");
    	
    	// Start members
		threadRunning = true;
		threadWait = true;
		ID = 2;
		
		// Set members to graph
		IndexesShowOnUiFlag = true;
		SQIshowOnUIFlag = true;
		qCON = qNOX = EMG = BSR = SQI = 0;
		qNOX_ShowSerie = false;
		
		// Set color resources
		qCON_Color = context.getResources().getColor(R.color.qCON_Color);
		qNOX_Color = context.getResources().getColor(R.color.qNOX_Color);
		EMG_Color = context.getResources().getColor(R.color.EMG_Color);
		BSR_Color = context.getResources().getColor(R.color.BSR_Color);
		SQI_Color = context.getResources().getColor(R.color.SQI_Color);
		
		// Set the initial attributes for the graph
		setIndexesgraphAttributes();
		
		// Create graph
		if (IndexesGraph == null) {
    		IndexesGraph = ChartFactory.getLineChartView(context, IndexesDataset, IndexesRenderer);
    	}
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
			if (ID == SETVALUESTOGRAPH) {
				// Put indexes values on series
				updateIndexesGraphParameters(qCON, qNOX, EMG, BSR, SQI, IndexesShowOnUiFlag, SQIshowOnUIFlag);
				// Pause thread
				Pause();
			}
			else if (ID == CANCEL) {
				// Free graph resources
				freeIndexesGraphResoures();
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
    	ID = 2;
		synchronized (this) {
			threadWait = true;
		}
	}
    
    /** --- Thread set indexes values to graph --- **/
    public void SetIndexesValuesToGraph(double[] indexes, boolean indexesFlag, boolean sqiFlag) {
    	ID = SETVALUESTOGRAPH;
    	IndexesShowOnUiFlag = indexesFlag;
		SQIshowOnUIFlag = sqiFlag;
		qCON = (int) indexes[0];
		qNOX = (int) indexes[1];
		EMG = (int) indexes[2];
		BSR = (int) indexes[3];
		SQI = (int) indexes[4];
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
	
	
	/** Setters and Getters **/
	public double[] getqCONSerie() {
		return getSerieToArray(QCONserie);
	}
	public void setqCONSerie(double[] Serie) {
		if (Serie != null) {
			// Size of the series
			int serieSize = Serie.length;
			
			if (serieSize > 0) {
				// Fill currentQCONserie with Series items
				for (int i=0; i<serieSize-1; i++) {
					QCONserie.add(i, Serie[i]);
				}
			}
			
			// set IndexesEjeXtick
			IndexesEjeXtick = serieSize;
		}
	}

	public double[] getqNOXSerie() {
		return getSerieToArray(QNOXserie);
	}
	public void setqNOXSerie(double[] Serie) {
		if (Serie != null) {
			// Size of the series
			int serieSize = Serie.length;
			
			if (serieSize > 0) {
				// Fill currentQCONserie with Series items
				for (int i=0; i<serieSize-1; i++) {
					QNOXserie.add(i, Serie[i]);
				}
			}
			
			// set IndexesEjeXtick
			IndexesEjeXtick = serieSize;
		}
	}
	
	public double[] getEMGserie() {
		return getSerieToArray(EMGserie);
	}
	public void setEMGSerie(double[] Serie) {
		if (Serie != null) {
			// Size of the series
			int serieSize = Serie.length;
			
			if (serieSize > 0) {
				// Fill currentEMGserie with Series items
				for (int i=0; i<serieSize-1; i++) {
					EMGserie.add(i, Serie[i]);
				}
			}
			
			// set IndexesEjeXtick
			IndexesEjeXtick = serieSize;
		}
	}
	
	public double[] getBSRserie() {
		return getSerieToArray(BSRserie);
	}
	public void setBSRSerie(double[] Serie) {
		if (Serie != null) {
			// Size of the series
			int serieSize = Serie.length;
			
			if (serieSize > 0) {
				// Fill currentBSRserie with Series items
				for (int i=0; i<serieSize-1; i++) {
					BSRserie.add(i, Serie[i]);
				}
			}
			
			// set IndexesEjeXtick
			IndexesEjeXtick = serieSize;
		}
	}
	
	public double[] getSQIserie() {
		return getSerieToArray(SQIserie);
	}
	public void setSQISerie(double[] Serie) {
		if (Serie != null) {
			// Size of the series
			int serieSize = Serie.length;
			
			if (serieSize > 0) {
				// Fill currentSQIserie with Series items
				for (int i=0; i<serieSize-1; i++) {
					SQIserie.add(i, Serie[i]);
				}
			}
			
			// set IndexesEjeXtick
			IndexesEjeXtick = serieSize;
		}
	}
	
	
	/** --------------------- Methods --------------------- **/
	// ---- Set indexes layout ---- \\
	public void setIndexesLayout(LinearLayout layout) {
		// Add graph and parameters to layout
		if (IndexesGraph != null) {
    		layout.addView(IndexesGraph, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    		layout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    	}
	}
	
	// ---- Remove indexes layout ---- \\
	public void removeIndexesLayout() {
		if (IndexesGraph != null) {
			final ViewParent graphParent= IndexesGraph.getParent();
			if (graphParent != null) {
				((ViewGroup)graphParent).removeView(IndexesGraph);
			}
		}
	}
	
	// ---- Show qNOX Series ---- \\
	public void qNOXserie_Show() {
		// Add serie to dataset and renderer
		IndexesDataset.addSeries(QNOXserie);
		IndexesRenderer.addSeriesRenderer(QNOXserieRenderer);
		// Set flag
		qNOX_ShowSerie = true;
	}
	
	// ---- Hide qNOX Series ---- \\
	public void qNOXserie_Hide() {
		// Remove serie from renderer and dataset
		IndexesRenderer.removeSeriesRenderer(QNOXserieRenderer);
		IndexesDataset.removeSeries(QNOXserie);
		// Set flag
		qNOX_ShowSerie = false;
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
	
	// ---- Indexes Graph - Set Attributes ---- \\
	private void setIndexesgraphAttributes() {
		Log.d(TAG, "--- On setIndexesgraphAttributes ---");
		
		IndexesEjeXtick = 0;												// Se inicializa el contador del eje X
		
		IndexesDataset = new XYMultipleSeriesDataset();
    	IndexesRenderer = new XYMultipleSeriesRenderer();
    	numberOfPointsIndexesChart = 900.0;									// Equivalente a 15 min con un timer a 1 seg
    	IndexesRenderer.setApplyBackgroundColor(true);						// Color de fondo - aplicar
    	IndexesRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));	// Color de fondo - color
    	IndexesRenderer.setMargins(new int[] {20, 40, -50, 5});			// Margin size values, in this order: top, left, bottom, right
    	
    	//IndexesRenderer.setChartTitle(IndicatorsGraph_titleLabel);		// Graph title
    	//IndexesRenderer.setChartTitleTextSize(14);						// Graph title size
    	
    	IndexesRenderer.setShowAxes(true);									// Axes - Mostrar ejes
    	IndexesRenderer.setAxesColor(Color.LTGRAY);							// Axes - color de los ejes X y Y
    	
    	IndexesRenderer.setShowLabels(true);								// Labels - Mostrar labels
    	IndexesRenderer.setLabelsColor(Color.LTGRAY);						// Labels - Color
    	IndexesRenderer.setXLabels(0);										// Labels - establece aprox el número de labels del eje X
    	//IndexesRenderer.setXLabelsAlign(Align.CENTER);					// Labels - alineacion del label del eje X
    	IndexesRenderer.setYLabels(6);										// Labels - establece aplox el número de labels del eje Y
    	IndexesRenderer.setYLabelsAlign(Align.RIGHT);						// Labels - alineacion del label del eje Y
    	IndexesRenderer.setLabelsTextSize(24);								// Labels - Size of the text
    	
    	IndexesRenderer.setShowLegend(false);								// Legend - Mostrar legenda
    	//IndexesRenderer.setFitLegend(true);								// Legend - Sets if the legend should size to fit
    	//IndexesRenderer.setLegendTextSize(22);							// Legend - Size of the text
    	//IndexesRenderer.setLegendHeight(-50);								// Legend - Sets the legend height, in pixels.
    	
    	IndexesRenderer.setShowGrid(true);									// Grid - Mostrar cuadricula
    	IndexesRenderer.setShowGridX(true);									// Grid - Mostrar cuadricula en X
    	IndexesRenderer.setShowGridY(true);									// Grid - Mostrar cuadricula en Y
    	IndexesRenderer.setShowCustomTextGrid(false);						// Grid - Mostrar cuadricula del texto
    	
    	IndexesRenderer.setPanEnabled(true);								// Panorama - Aplicar
    	IndexesRenderer.setPanEnabled(false, true);						// Panorama - Aplicar al eje X y Y
    	IndexesRenderer.setPanLimits(new double[] {0, numberOfPointsIndexesChart, -2, 102});// Panorama - Limite de movimiento en los ejes X y Y
    	//IndexesRenderer.setExternalZoomEnabled(false);					// Zoom - Desabilitar zoom externo
    	IndexesRenderer.setZoomEnabled(true);								// Zoom - Aplicar
    	IndexesRenderer.setZoomEnabled(false, true);						// Zoom - Aplicar a los ejes X y Y
    	IndexesRenderer.setZoomButtonsVisible(true);						// Zoom - Mostrar el boton de zoom
    	IndexesRenderer.setZoomLimits(new double[] {0, numberOfPointsIndexesChart, 0, 100});// Zoom - Limite del zoom en los ejes
    	
    	IndexesRenderer.setAxisTitleTextSize(20);							// Eje XY - Size of the text
    	
    	//IndexesRenderer.setXTitle("");									// Eje X - Titulo
    	IndexesRenderer.setXAxisMin(0.0);									// Eje X - limite inferior
    	IndexesRenderer.setXAxisMax(numberOfPointsIndexesChart);			// Eje X - limite superior
    	
    	//IndexesRenderer.setYTitle("");									// Eje Y - Titulo
    	IndexesRenderer.setYAxisMin(0.0);									// Eje Y - limite inferior
    	IndexesRenderer.setYAxisMax(100.0);									// Eje Y - limite superior
    	IndexesRenderer.setYAxisAlign(Align.LEFT, 0);						// Eje Y - alineacion del eje
    	
    	QCONserie = new XYSeries("");										// Se crea la serie
    	QNOXserie = new XYSeries("");										// Se crea la serie
    	EMGserie = new XYSeries("");										// Se crea la serie
    	BSRserie = new XYSeries("");										// Se crea la serie
    	SQIserie = new XYSeries("");										// Se crea la serie
    	
    	IndexesDataset.addSeries(QCONserie);								// Se pone la serie en el conjunto de datos
    	IndexesDataset.addSeries(EMGserie);									// Se pone la serie en el conjunto de datos
    	IndexesDataset.addSeries(BSRserie);									// Se pone la serie en el conjunto de datos
    	IndexesDataset.addSeries(SQIserie);									// Se pone la serie en el conjunto de datos
    	
    	QCONserieRenderer = new XYSeriesRenderer();							// Se crea el atributo de la serie
    	QCONserieRenderer.setColor(qCON_Color);								// Color de la serie
    	QCONserieRenderer.setLineWidth(serieQCON_lineWidth);				// Grosor de linea
    	
    	QNOXserieRenderer = new XYSeriesRenderer();							// Se crea el atributo de la serie
    	QNOXserieRenderer.setColor(qNOX_Color);								// Color de la serie
    	QNOXserieRenderer.setLineWidth(serieQNOX_lineWidth);				// Grosor de linea
    	
    	EMGserieRenderer = new XYSeriesRenderer();							// Se crea el atributo de la serie
    	EMGserieRenderer.setColor(EMG_Color);								// Color de la serie
    	EMGserieRenderer.setLineWidth(serieEMG_lineWidth);					// Grosor de linea
    	
    	BSRserieRenderer = new XYSeriesRenderer();							// Se crea el atributo de la serie
    	BSRserieRenderer.setColor(BSR_Color);								// Color de la serie
    	BSRserieRenderer.setLineWidth(serieBSR_lineWidth);					// Grosor de linea
    	
    	SQIserieRenderer = new XYSeriesRenderer();							// Se crea el atributo de la serie
    	SQIserieRenderer.setColor(SQI_Color);								// Color de la serie
    	SQIserieRenderer.setLineWidth(serieSQI_lineWidth);					// Grosor de linea
    	
    	IndexesRenderer.addSeriesRenderer(QCONserieRenderer);
    	IndexesRenderer.addSeriesRenderer(EMGserieRenderer);
    	IndexesRenderer.addSeriesRenderer(BSRserieRenderer);
    	IndexesRenderer.addSeriesRenderer(SQIserieRenderer);
	}
	
	// ---- Indexes Graph - Update Parameters ---- \\
    private void updateIndexesGraphParameters(int qCON, int qNOX, int EMG, int BSR, int SQI, boolean IndexesShowOnUiFlag, boolean SQIshowOnUIFlag) {
    	
    	// Add X and Y value acording to the flag
    	if (IndexesShowOnUiFlag == true) {
    		QCONserie.add(IndexesEjeXtick, qCON);
    		QNOXserie.add(IndexesEjeXtick, qNOX);
    		EMGserie.add(IndexesEjeXtick, EMG);
    		BSRserie.add(IndexesEjeXtick, BSR);
    		SQIserie.add(IndexesEjeXtick, SQI);
    	} else {
    		QCONserie.add(IndexesEjeXtick, MathHelper.NULL_VALUE);
    		QNOXserie.add(IndexesEjeXtick, MathHelper.NULL_VALUE);
    		EMGserie.add(IndexesEjeXtick, MathHelper.NULL_VALUE);
    		BSRserie.add(IndexesEjeXtick, MathHelper.NULL_VALUE);
        	if (SQIshowOnUIFlag == true) {
        		SQIserie.add(IndexesEjeXtick, SQI);
        	}
        	else {
        		SQIserie.add(IndexesEjeXtick, MathHelper.NULL_VALUE);
        	}
    	}
    	// Increment X
    	IndexesEjeXtick++;
    	// Keep a constant number of points by removing them from the left
    	if (QCONserie.getItemCount() > numberOfPointsIndexesChart) {
    		QCONserie.remove(0);
    		if (QNOXserie.getItemCount() > numberOfPointsIndexesChart) {
    			QNOXserie.remove(0);
    		}
    		if (EMGserie.getItemCount() > numberOfPointsIndexesChart) {
    			EMGserie.remove(0);
    		}
    		if (BSRserie.getItemCount() > numberOfPointsIndexesChart) {
    			BSRserie.remove(0);
    		}
    		if (SQIserie.getItemCount() > numberOfPointsIndexesChart) {
    			SQIserie.remove(0);
    		}
        	// Adjust X axis scale
        	if (IndexesRenderer != null) {
        		IndexesRenderer.setXAxisMin(IndexesEjeXtick-1 - numberOfPointsIndexesChart);
            	IndexesRenderer.setXAxisMax(IndexesEjeXtick-1);
        	}
    	}
    	
    	// Repaint indexes graph
		repaintIndexesChart();
    }
	
    // ---- Indexes Graph - Repaint indexes chart ---- \\
    private static void repaintIndexesChart() {
    	if (IndexesGraph != null) {
			IndexesGraph.refreshDrawableState();
    		IndexesGraph.repaint();
    	}
    }
    
	// ---- Indexes Graph - Free resources ---- \\
	private void freeIndexesGraphResoures() {
		Log.d(TAG, "--- On freeIndexesGraphResoures ---");
		
    	QCONserie.clear();
    	QNOXserie.clear();
    	EMGserie.clear();
    	BSRserie.clear();
    	SQIserie.clear();
    	IndexesRenderer.removeSeriesRenderer(QCONserieRenderer);
    	IndexesRenderer.removeSeriesRenderer(EMGserieRenderer);
    	IndexesRenderer.removeSeriesRenderer(BSRserieRenderer);
    	IndexesRenderer.removeSeriesRenderer(SQIserieRenderer);
    	IndexesDataset.removeSeries(QCONserie);
    	IndexesDataset.removeSeries(EMGserie);
    	IndexesDataset.removeSeries(BSRserie);
    	IndexesDataset.removeSeries(SQIserie);
    	if (qNOX_ShowSerie == true) {
    		IndexesRenderer.removeSeriesRenderer(QNOXserieRenderer);
    		IndexesDataset.removeSeries(QNOXserie);
    	}
    	IndexesGraph = null;
	}
	
}/* End Main Class */