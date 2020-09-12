package qm.display.qcon;

/* Libraries imported */
import qm.display.customviews.BSRBoxView;
import qm.display.customviews.EMGBoxView;
import qm.display.customviews.ImpTextView;
import qm.display.customviews.SQIBoxView;
import qm.display.customviews.qCONBoxView;
import qm.display.customviews.qNOXBoxView;
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

/* Start main fragment */
public class FragmentScreen_02 extends Fragment {
	
	/*** Start variables ***/
	// Debugging
	private static final String TAG = "FragmentScreen_02";
	
	// Layout members
	private static qCONBoxView qCON;
	private static qNOXBoxView qNOX;
    private static EMGBoxView emg;
    private static BSRBoxView bsr;
    private static SQIBoxView sqi;
    private static ImpTextView zBox;
    private static LinearLayout graphEEGLy, graphIndexesLy;
	
    // Activity members
    private static Act_Main main;
	/*** End variables ***/
	
	
	
	/*** Constructors ***/
	public FragmentScreen_02() {
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
		View ly = inflater.inflate(R.layout.ly_screen_02, container, false);
		
		// Set initial layout members
        qCON = (qCONBoxView) ly.findViewById(R.id.qconBoxView);
        qNOX = (qNOXBoxView) ly.findViewById(R.id.qNOXBoxView);
        emg = (EMGBoxView) ly.findViewById(R.id.emgBoxView);
        bsr = (BSRBoxView) ly.findViewById(R.id.bsrBoxView);
        sqi = (SQIBoxView) ly.findViewById(R.id.sqiBoxView);
        zBox = (ImpTextView) ly.findViewById(R.id.Impedances_TextView);
        graphEEGLy = (LinearLayout) ly.findViewById(R.id.EEG_graph);
        graphIndexesLy = (LinearLayout) ly.findViewById(R.id.Indexes_graph);
        
        // Set graph on fragment
        if (main != null) {
        	qCON.setOnLongClickListener(main.alarm_qCON_OnLongClickListener);
        	main.graph_EEG_setLy(graphEEGLy);
            main.graph_Indexes_setLy(graphIndexesLy);
            main.graph_EEG_setEEGcolor(2);
            main.graph_Indexes_Show_qNOX(true);
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
			main.graph_Indexes_Show_qNOX(false);
			main.graph_EEG_removeLy();
			main.graph_Indexes_removeLy();
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
	
	// ----- Set qCON ----- \\
	public void setqNOX(String value) {
		if (qNOX != null) {
			qNOX.setText(value);
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
	
}/* End main fragment */