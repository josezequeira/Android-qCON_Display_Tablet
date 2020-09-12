package qm.display.qcon;

/* Libraries imported */
import qm.display.customviews.BSRBoxView;
import qm.display.customviews.EMGBoxView;
import qm.display.customviews.ImpTextView;
import qm.display.customviews.SQIBoxView;
import qm.display.customviews.qCONBoxView;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/* Start main fragment */
public class FragmentScreen_03 extends Fragment {
	
	/*** Start variables ***/
	// Debugging
	private static final String TAG = "FragmentScreen_01";
	
	// Layout members
    private static qCONBoxView qCON;
    private static EMGBoxView emg;
    private static BSRBoxView bsr;
    private static SQIBoxView sqi;
    private static ImpTextView zBox;
    private static LinearLayout graphEEGLy, graphaEEGLy;
    private static TextView xAxis_aEEG_graph;
    private static int aEEGxScaleSelected;
    
    // Activity members
    private static Act_Main main;
	/*** End variables ***/
	
	
	
	/*** Constructors ***/
    public FragmentScreen_03() {
    	// Empty constructor
	}
	
    
	
	/***- Start - Methods for fragment life cycle -***/
	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG, "----- onAttach -----");
		
		super.onAttach(activity);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "----- onCreate -----");
		
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "----- onCreateView -----");
		
		// Get main activity
		main = (Act_Main) getActivity();
		
		// Get the layout for this fragment
		View ly = inflater.inflate(R.layout.ly_screen_03, container, false);
		
		// Set initial layout members
        qCON = (qCONBoxView) ly.findViewById(R.id.qconBoxView);
        emg = (EMGBoxView) ly.findViewById(R.id.emgBoxView);
        bsr = (BSRBoxView) ly.findViewById(R.id.bsrBoxView);
        sqi = (SQIBoxView) ly.findViewById(R.id.sqiBoxView);
        zBox = (ImpTextView) ly.findViewById(R.id.Impedances_TextView);
        graphEEGLy = (LinearLayout) ly.findViewById(R.id.EEG_graph);
        graphaEEGLy = (LinearLayout) ly.findViewById(R.id.aEEG_graph);
        xAxis_aEEG_graph = (TextView) ly.findViewById(R.id.aEEG_graph_xAxis);
        
        // Set graph on fragment
        if (main != null) {
        	qCON.setOnLongClickListener(main.alarm_qCON_OnLongClickListener);
        	main.graph_EEG_setLy(graphEEGLy);
            main.graph_EEG_setEEGcolor(2);
        	main.graph_aEEG_setLy(graphaEEGLy);
        	aEEGxScaleSelected = main.graph_aEEG_getXscaleSelected();
        	setaEEGxAxisLebel(aEEGxScaleSelected);
        }
        
		return ly;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.d(TAG, "----- onActivityCreated -----");
		
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onStart() {
		Log.d(TAG, "----- onStart -----");
		
		super.onStart();
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, "----- onResume -----");
		
		super.onResume();
	}
	
	@Override
	public void onPause() {
		Log.d(TAG, "----- onPause -----");
		
		super.onPause();
	}
	
	@Override
	public void onStop() {
		Log.d(TAG, "----- onStop -----");
		
		super.onStop();
	}
	
	@Override
	public void onDestroyView() {
		Log.d(TAG, "----- onDestroyView -----");
		
		// Remove graphs from fragment
		if (main != null) {
			main.graph_EEG_removeLy();
			main.graph_aEEG_removeLy();
		}
		
		super.onDestroyView();
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "----- onDestroy -----");
		
		super.onDestroy();
	}
	
	@Override
	public void onDetach() {
		Log.d(TAG, "----- onDetach -----");
		
		super.onDetach();
	}
	/***- End - Methods for fragment life cycle -***/
	
	
	/** --------------------- Methods --------------------- **/
	// ----- Set qCON ----- \\
	public void setqCON(String value) {
		if (qCON != null) {
			qCON.setText(value);
		}
	}
	
	// ----- qCON visual alarm ----- \\
	public void eneableQCONvisualAlarm() {
		if (qCON != null) {
			qCON.setTextColor(Color.BLACK);
			qCON.setBackgroundColor(Color.YELLOW);
		}
	}
	public void disableQCONvisualAlarm() {
		if ((qCON != null) && (main != null)) {
			qCON.setTextColor(main.getResources().getColor(R.color.qCON_Color));
			qCON.setBackgroundResource(R.drawable.qcon_rectangle);
		}
	}
	
	// ----- Set EMG ----- \\
	public void setEMG(String value) {
		if (emg != null) {
			emg.setText(value);
		}
	}
	
	// ----- Set BSR ----- \\
	public void setBSR(String value) {
		if (bsr != null) {
			bsr.setText(value);
		}
	}
	
	// ----- Set SQI ----- \\
	public void setSQI(int value, boolean flag) {
		if ((sqi != null) && (main != null)) {
			if ((value < 50) && (flag == false)) {
				sqi.setText(main.getResources().getString(R.string.MSg_LowSQI));
				sqi.setTypeface(Typeface.DEFAULT_BOLD);
				sqi.setTextColor(Color.BLACK);
				sqi.setBackgroundColor(Color.GREEN);
			}
			else if ((value < 50) && (flag == true)) {
				sqi.setText(String.valueOf(value));
				sqi.setTypeface(Typeface.DEFAULT);
				sqi.setTextColor(Color.GREEN);
				sqi.setBackgroundResource(R.drawable.sqi_rectangle);
			}
			else {
				sqi.setText(String.valueOf(value));
				sqi.setTypeface(Typeface.DEFAULT);
				sqi.setTextColor(Color.GREEN);
				sqi.setBackgroundResource(R.drawable.sqi_rectangle);
			}
		}
	}
	public void setSQI(String value, boolean flag) {
		if (sqi != null) {
			sqi.setText(value);
			sqi.setTypeface(Typeface.DEFAULT);
			sqi.setTextColor(Color.GREEN);
			sqi.setBackgroundResource(R.drawable.sqi_rectangle);
		}
	}
	
	// ----- Set zNEG ----- \\
	public void setZNEG(String value) {
		if (zBox != null) {
			zBox.setNegZText(value);
		}
	}
	
	// ----- Set zREF ----- \\
	public void setZREF(String value) {
		if (zBox != null) {
			zBox.setRefZText(value);
		}
	}
	
	// ----- Set zPOS ----- \\
	public void setZPOS(String value) {
		if (zBox != null) {
			zBox.setPosZText(value);
		}
	}
	
	// ----- Set aEEG X axis label ----- \\
	public void setaEEGxAxisLebel(int value) {
		if (xAxis_aEEG_graph != null) {
			if (value == 1) {
				xAxis_aEEG_graph.setText(R.string.Graph_aEEG_Lb_xAxis_30min);
			}
			else if (value == 2) {
				xAxis_aEEG_graph.setText(R.string.Graph_aEEG_Lb_xAxis_1h);
			}
			else if (value == 3) {
				xAxis_aEEG_graph.setText(R.string.Graph_aEEG_Lb_xAxis_2h);
			}
			else if (value == 4) {
				xAxis_aEEG_graph.setText(R.string.Graph_aEEG_Lb_xAxis_4h);
			}
			else if (value == 5) {
				xAxis_aEEG_graph.setText(R.string.Graph_aEEG_Lb_xAxis_12h);
			}
			else {
				xAxis_aEEG_graph.setText(R.string.Graph_aEEG_Lb_xAxis_24h);
			}
		}
	}
	
}/* End main fragment */