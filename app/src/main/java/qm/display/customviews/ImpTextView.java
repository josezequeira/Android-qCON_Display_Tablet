package qm.display.customviews;

/* Libraries imported */
import qm.display.qcon.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/* Start main class */
public class ImpTextView extends View {
	
	/*** Start variables ***/
	// Debugging
	//private static final String TAG = "ImpTextView";
	
	// ---- Members variables ---- \\
	private static int mViewHeight;
	private static int mViewWidth;
	private static int lb_Height;
	private static int negZ_XPoint;
	private static int refZ_XPoint;
	private static int posZ_XPoint;
	private static int textWidth;
	private static int lb_text_YPoint;
	private static int vl_text_YPoint;
	private static float descent;
	
	// Label Background
	private static int lb_Background_Size;
	private static int lb_Background_Color;
	
	// Border rectangle
	//private static RectF borderRectangle;
	private static int borderRectangleColor;
	private static int borderRectangleWidth;
	
	// Label text
	private static String NegZ_lb_text;
	private static int NegZ_lb_Text_Color;
	private static boolean NegZ_lb_Text_BelowBaseLine;
	private static int NegZ_lb_textSize;
	private static int NegZ_lb_textSizeCalc;
	
	private static String RefZ_lb_text;
	private static int RefZ_lb_Text_Color;
	private static boolean RefZ_lb_Text_BelowBaseLine;
	private static int RefZ_lb_textSize;
	private static int RefZ_lb_textSizeCalc;
	
	private static String PosZ_lb_text;
	private static int PosZ_lb_Text_Color;
	private static boolean PosZ_lb_Text_BelowBaseLine;
	private static int PosZ_lb_textSize;
	private static int PosZ_lb_textSizeCalc;
	
	// Value text
	private static String NegZ_vl_text;
	private static int NegZ_vl_Text_Color;
	private static boolean NegZ_vl_Text_BelowBaseLine;
	private static int NegZ_vl_textSize;
	private static int NegZ_vl_textSizeCalc;
	
	private static String RefZ_vl_text;
	private static int RefZ_vl_Text_Color;
	private static boolean RefZ_vl_Text_BelowBaseLine;
	private static int RefZ_vl_textSize;
	private static int RefZ_vl_textSizeCalc;
	
	private static String PosZ_vl_text;
	private static int PosZ_vl_Text_Color;
	private static boolean PosZ_vl_Text_BelowBaseLine;
	private static int PosZ_vl_textSize;
	private static int PosZ_vl_textSizeCalc;
	
	// Paints
	private static Paint lb_backgroundPaint;
	private static Paint borderRectanglePaint;
	private static Paint lb_TextPaint;
	private static Paint vl_TextPaint;
	private static Rect textBoundaries;
	/*** End variables ***/
	
	
	/** ---- Class constructors ---- **/
	public ImpTextView(Context context) {
		super(context);
		
		// Set text values
		NegZ_lb_text = "";
		RefZ_lb_text = "";
		PosZ_lb_text = "";
		NegZ_vl_text = "";
		RefZ_vl_text = "";
		PosZ_vl_text = "";
		
		// Initialize paints
		init(context);
	}
	
	public ImpTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// Set text values
		NegZ_lb_text = "";
		RefZ_lb_text = "";
		PosZ_lb_text = "";
		NegZ_vl_text = "";
		RefZ_vl_text = "";
		PosZ_vl_text = "";
		
        // This call uses R.styleable.ImpTextView, which is an array of
        // the custom attributes that were declared in values/attrs.xml.
		TypedArray attributes = context.getTheme().obtainStyledAttributes(
				attrs, R.styleable.ImpTextView, 0, 0);
		
		// Retrieve the values from the TypedArray and store into
        // fields of this class.
		try {
			// Label background
			lb_Background_Size = attributes.getInteger(R.styleable.ImpTextView_imp_label_Background_Size, 50);
			lb_Background_Color = attributes.getColor(R.styleable.ImpTextView_imp_label_Background_Color, Color.BLACK);
			// Border rectangle
			borderRectangleColor = attributes.getColor(R.styleable.ImpTextView_imp_line_color, Color.MAGENTA);
			borderRectangleWidth = attributes.getInteger(R.styleable.ImpTextView_imp_line_width, 3);
			// Label texts
			NegZ_lb_text = attributes.getString(R.styleable.ImpTextView_imp_negZ_label_Text);
			NegZ_lb_Text_Color = attributes.getColor(R.styleable.ImpTextView_imp_negZ_label_Text_Color, Color.WHITE);
			NegZ_lb_Text_BelowBaseLine = attributes.getBoolean(R.styleable.ImpTextView_imp_negZ_label_Text_BelowBaseLine, false);
			NegZ_lb_textSize = attributes.getInteger(R.styleable.ImpTextView_imp_negZ_label_Text_Size, 10);
			
			RefZ_lb_text = attributes.getString(R.styleable.ImpTextView_imp_refZ_label_Text);
			RefZ_lb_Text_Color = attributes.getColor(R.styleable.ImpTextView_imp_refZ_label_Text_Color, Color.WHITE);
			RefZ_lb_Text_BelowBaseLine = attributes.getBoolean(R.styleable.ImpTextView_imp_refZ_label_Text_BelowBaseLine, false);
			RefZ_lb_textSize = attributes.getInteger(R.styleable.ImpTextView_imp_refZ_label_Text_Size, 10);
			
			PosZ_lb_text = attributes.getString(R.styleable.ImpTextView_imp_posZ_label_Text);
			PosZ_lb_Text_Color = attributes.getColor(R.styleable.ImpTextView_imp_posZ_label_Text_Color, Color.WHITE);
			PosZ_lb_Text_BelowBaseLine = attributes.getBoolean(R.styleable.ImpTextView_imp_posZ_label_Text_BelowBaseLine, false);
			PosZ_lb_textSize = attributes.getInteger(R.styleable.ImpTextView_imp_posZ_label_Text_Size, 10);
			// Value texts
			NegZ_vl_text = attributes.getString(R.styleable.ImpTextView_imp_negZ_value_Text);
			NegZ_vl_Text_Color = attributes.getColor(R.styleable.ImpTextView_imp_negZ_value_Text_Color, Color.WHITE);
			NegZ_vl_Text_BelowBaseLine = attributes.getBoolean(R.styleable.ImpTextView_imp_negZ_value_Text_BelowBaseLine, false);
			NegZ_vl_textSize = attributes.getInteger(R.styleable.ImpTextView_imp_negZ_value_Text_Size, 10);
			
			RefZ_vl_text = attributes.getString(R.styleable.ImpTextView_imp_refZ_value_Text);
			RefZ_vl_Text_Color = attributes.getColor(R.styleable.ImpTextView_imp_refZ_value_Text_Color, Color.WHITE);
			RefZ_vl_Text_BelowBaseLine = attributes.getBoolean(R.styleable.ImpTextView_imp_refZ_value_Text_BelowBaseLine, false);
			RefZ_vl_textSize = attributes.getInteger(R.styleable.ImpTextView_imp_refZ_value_Text_Size, 10);
			
			PosZ_vl_text = attributes.getString(R.styleable.ImpTextView_imp_posZ_value_Text);
			PosZ_vl_Text_Color = attributes.getColor(R.styleable.ImpTextView_imp_posZ_value_Text_Color, Color.WHITE);
			PosZ_vl_Text_BelowBaseLine = attributes.getBoolean(R.styleable.ImpTextView_imp_posZ_value_Text_BelowBaseLine, false);
			PosZ_vl_textSize = attributes.getInteger(R.styleable.ImpTextView_imp_posZ_value_Text_Size, 10);
		} finally {
			// Release the TypedArray so that it can be reused.
			attributes.recycle();
		}
		// Initialize paints
		init(context);
	}
	
	
	/** ---- View methods ---- **/
	/* --- Create members objects --- */
	private void init(Context context) {
		
		// Set initial global values
		mViewHeight = 0;
		mViewWidth = 0;
		textWidth = 0;
		lb_Height = 0;
		NegZ_lb_textSizeCalc = 0;
		RefZ_lb_textSizeCalc = 0;
		PosZ_lb_textSizeCalc = 0;
		NegZ_vl_textSizeCalc = 0;
		RefZ_vl_textSizeCalc = 0;
		PosZ_vl_textSizeCalc = 0;
		negZ_XPoint = 0;
		refZ_XPoint = 0;
		posZ_XPoint = 0;
		lb_text_YPoint = 0;
		vl_text_YPoint = 0;
		descent = 0;
		
		// Label background
		lb_backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		lb_backgroundPaint.setColor(lb_Background_Color);
		lb_backgroundPaint.setStyle(Style.FILL);
		
		// Lines
		borderRectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		borderRectanglePaint.setColor(borderRectangleColor);
		borderRectanglePaint.setStrokeWidth(borderRectangleWidth);
		borderRectanglePaint.setStyle(Style.STROKE);
		
		// Label text
		lb_TextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		lb_TextPaint.setTextAlign(Align.CENTER);
		
		// Value text
		vl_TextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		vl_TextPaint.setTextAlign(Align.CENTER);
		
		// Rectangle for text boundaries
		textBoundaries = new Rect();
		
		// Rectangle for view boundaries
		//borderRectangle = new RectF();
	}
	
	/* --- Calculate the available height space --- */
	private int calcHeight(int heightSpec) {
		
		int result = 100; // Default heights
		
		int mode = MeasureSpec.getMode(heightSpec);
		int limit = MeasureSpec.getSize(heightSpec);
		
		if (mode == MeasureSpec.AT_MOST) {
			result = limit;
		} 
		else if (mode == MeasureSpec.EXACTLY) {
			result = limit;
		}
		
		return result;
	}
	
	/* --- Calculate the available width space --- */
	private int calcWidth(int widthSpec) {
		
		int result = 200; // Default width
		
		int mode = MeasureSpec.getMode(widthSpec);
		int limit = MeasureSpec.getSize(widthSpec);
		
		if (mode == MeasureSpec.AT_MOST) {
			result = limit;
		}
		else if (mode == MeasureSpec.EXACTLY) {
			result = limit;
		}
		
		return result;
	}
	
	/** --- Calculate the view dimension --- */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		// Try to get all available space at view
		int width = calcWidth(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
		int height = calcHeight(heightMeasureSpec) - getPaddingBottom() - getPaddingTop();
		
		// Set width and height global values
		mViewWidth = width;
		mViewHeight = height;
		
		setMeasuredDimension(width, height);
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	/** --- Draw the view --- */
	@Override
	protected void onDraw(Canvas canvas) {
		
		// Calculate label background height
		lb_Height = mViewHeight/(100/lb_Background_Size);
		
		// Calculate text max width
		textWidth = (mViewWidth/3);
		
		// Calculate labels and values xAxis point
		posZ_XPoint = (mViewWidth/3)/2;
		refZ_XPoint = mViewWidth/2;
		negZ_XPoint = mViewWidth - posZ_XPoint;
		
		// Calculate labels and values yAxis point
		lb_text_YPoint = lb_Height/2;
		vl_text_YPoint = lb_Height + (mViewHeight - lb_Height)/2;
		
		// ---- Draw label background rectangle ---- \\
		//canvas.drawRect(0, 0, mViewWidth, lb_Height, lb_backgroundPaint);
		
		// ---- Draw border rectangle ---- \\
		//borderRectangle.set(0, lb_text_YPoint, mViewWidth-1, mViewHeight-1);
		//canvas.drawRoundRect(borderRectangle, 5, 5, borderRectanglePaint);
		
		// ----- Draw label text ----- \\
		// -- Positive impedance label --
		// Set text size and color
		PosZ_lb_textSizeCalc = lb_Height - PosZ_lb_textSize;
		lb_TextPaint.setTextSize(PosZ_lb_textSizeCalc);
		lb_TextPaint.setColor(PosZ_lb_Text_Color);
		// Measure text boundaries
		lb_TextPaint.getTextBounds(PosZ_lb_text, 0, PosZ_lb_text.length(), textBoundaries);
		// Change text size until match view width
		while (textBoundaries.width() > textWidth-20) {
			PosZ_lb_textSizeCalc = PosZ_lb_textSizeCalc - 1;
			lb_TextPaint.setTextSize(PosZ_lb_textSizeCalc);
			lb_TextPaint.getTextBounds(PosZ_lb_text, 0, PosZ_lb_text.length(), textBoundaries);
		}
		// Measure distance below text baseline
		descent = lb_TextPaint.descent();
		if (PosZ_lb_Text_BelowBaseLine == false) {
			descent = 0;
		}
		// Draw text boundaries rectangle
		canvas.drawRect(posZ_XPoint - (textBoundaries.width()/2) - 3,
						lb_text_YPoint - (textBoundaries.height()/2),
						posZ_XPoint+(textBoundaries.width()/2) + 4,
						lb_text_YPoint + (textBoundaries.height()/2),
						lb_backgroundPaint);
		// Draw text
		canvas.drawText(PosZ_lb_text, posZ_XPoint,
						lb_text_YPoint + (textBoundaries.height()/2) - descent,
						lb_TextPaint);
		
		// -- Reference impedance label --
		// Set text size and color
		RefZ_lb_textSizeCalc = lb_Height - RefZ_lb_textSize;
		lb_TextPaint.setTextSize(RefZ_lb_textSizeCalc);
		lb_TextPaint.setColor(RefZ_lb_Text_Color);
		// Measure text boundaries
		lb_TextPaint.getTextBounds(RefZ_lb_text, 0, RefZ_lb_text.length(), textBoundaries);
		// Change text size until match view width
		while (textBoundaries.width() > textWidth-20) {
			RefZ_lb_textSizeCalc = RefZ_lb_textSizeCalc - 1;
			lb_TextPaint.setTextSize(RefZ_lb_textSizeCalc);
			lb_TextPaint.getTextBounds(RefZ_lb_text, 0, RefZ_lb_text.length(), textBoundaries);
		}
		// Measure distance below text baseline
		descent = lb_TextPaint.descent();
		if (RefZ_lb_Text_BelowBaseLine == false) {
			descent = 0;
		}
		// Draw text boundaries rectangle
		canvas.drawRect(refZ_XPoint - (textBoundaries.width()/2) - 3,
						lb_text_YPoint - (textBoundaries.height()/2),
						refZ_XPoint + (textBoundaries.width()/2) + 4,
						lb_text_YPoint + (textBoundaries.height()/2),
						lb_backgroundPaint);
		// Draw text
		canvas.drawText(RefZ_lb_text, refZ_XPoint,
						lb_text_YPoint + (textBoundaries.height()/2) - descent,
						lb_TextPaint);
		
		// -- Negative impedance label --
		// Set text size and color
		NegZ_lb_textSizeCalc = lb_Height - NegZ_lb_textSize;
		lb_TextPaint.setTextSize(NegZ_lb_textSizeCalc);
		lb_TextPaint.setColor(NegZ_lb_Text_Color);
		// Measure text boundaries
		lb_TextPaint.getTextBounds(NegZ_lb_text, 0, NegZ_lb_text.length(), textBoundaries);
		// Change text size until match view width
		while (textBoundaries.width() > textWidth-20) {
			NegZ_lb_textSizeCalc = NegZ_lb_textSizeCalc - 1;
			lb_TextPaint.setTextSize(NegZ_lb_textSizeCalc);
			lb_TextPaint.getTextBounds(NegZ_lb_text, 0, NegZ_lb_text.length(), textBoundaries);
		}
		// Measure distance below text baseline
		descent = lb_TextPaint.descent();
		if (NegZ_lb_Text_BelowBaseLine == false) {
			descent = 0;
		}
		// Draw text boundaries rectangle
		canvas.drawRect(negZ_XPoint - (textBoundaries.width()/2) - 3,
						lb_text_YPoint - (textBoundaries.height()/2),
						negZ_XPoint + (textBoundaries.width()/2) + 4,
						lb_text_YPoint + (textBoundaries.height()/2),
						lb_backgroundPaint);
		// Draw text
		canvas.drawText(NegZ_lb_text, negZ_XPoint,
						lb_text_YPoint + (textBoundaries.height()/2) - descent,
						lb_TextPaint);
		
		
		// ----- Draw value text ----- \\
		// -- Positive impedance value --
		// Set text size and color
		PosZ_vl_textSizeCalc = mViewHeight - lb_Height - PosZ_vl_textSize;
		vl_TextPaint.setTextSize(PosZ_vl_textSizeCalc);
		vl_TextPaint.setColor(PosZ_vl_Text_Color);
		// Measure text boundaries
		vl_TextPaint.getTextBounds(PosZ_vl_text, 0, PosZ_vl_text.length(), textBoundaries);
		// Change text size until match view width
		while (textBoundaries.width() > textWidth-5) {
			PosZ_vl_textSizeCalc = PosZ_vl_textSizeCalc - 1;
			vl_TextPaint.setTextSize(PosZ_vl_textSizeCalc);
			vl_TextPaint.getTextBounds(PosZ_vl_text, 0, PosZ_vl_text.length(), textBoundaries);
		}
		// Measure distance below text baseline
		descent = vl_TextPaint.descent();
		if (PosZ_vl_Text_BelowBaseLine == false) {
			descent = 0;
		}
		// Draw text
		canvas.drawText(PosZ_vl_text, posZ_XPoint,
						vl_text_YPoint + (textBoundaries.height()/2) - descent,
						vl_TextPaint);
		
		// -- Reference impedance value --
		// Set text size and color
		RefZ_vl_textSizeCalc = mViewHeight - lb_Height - RefZ_vl_textSize;
		vl_TextPaint.setTextSize(RefZ_vl_textSizeCalc);
		vl_TextPaint.setColor(RefZ_vl_Text_Color);
		// Measure text boundaries
		vl_TextPaint.getTextBounds(RefZ_vl_text, 0, RefZ_vl_text.length(), textBoundaries);
		// Change text size until match view width
		while (textBoundaries.width() > textWidth-5) {
			RefZ_vl_textSizeCalc = RefZ_vl_textSizeCalc - 1;
			vl_TextPaint.setTextSize(RefZ_vl_textSizeCalc);
			vl_TextPaint.getTextBounds(RefZ_vl_text, 0, RefZ_vl_text.length(), textBoundaries);
		}
		// Measure distance below text baseline
		descent = vl_TextPaint.descent();
		if (RefZ_vl_Text_BelowBaseLine == false) {
			descent = 0;
		}
		// Draw text
		canvas.drawText(RefZ_vl_text, refZ_XPoint,
						vl_text_YPoint + (textBoundaries.height()/2) - descent,
						vl_TextPaint);
		
		// -- Negative impedance value --
		// Set text size and color
		NegZ_vl_textSizeCalc = mViewHeight - lb_Height - NegZ_vl_textSize;
		vl_TextPaint.setTextSize(NegZ_vl_textSizeCalc);
		vl_TextPaint.setColor(NegZ_vl_Text_Color);
		// Measure text boundaries
		vl_TextPaint.getTextBounds(NegZ_vl_text, 0, NegZ_vl_text.length(), textBoundaries);
		// Change text size until match view width
		while (textBoundaries.width() > textWidth-5) {
			NegZ_vl_textSizeCalc = NegZ_vl_textSizeCalc - 1;
			vl_TextPaint.setTextSize(NegZ_vl_textSizeCalc);
			vl_TextPaint.getTextBounds(NegZ_vl_text, 0, NegZ_vl_text.length(), textBoundaries);
		}
		// Measure distance below text baseline
		descent = vl_TextPaint.descent();
		if (NegZ_vl_Text_BelowBaseLine == false) {
			descent = 0;
		}
		// Draw text
		canvas.drawText(NegZ_vl_text, negZ_XPoint,
						vl_text_YPoint + (textBoundaries.height()/2) - descent,
						vl_TextPaint);
	}
	
	/* --- Repaint hole view --- */
	private void repaint() {
		this.invalidate();
		this.requestLayout();
	}
	
	/* --- Set negative impedance value text and repaint view --- */
	public synchronized void setNegZText(String text) {
		// Set text
		NegZ_vl_text = text;
		// Repaint view
		repaint();
	}
	
	/* --- Set reference impedance value text and repaint view --- */
	public synchronized void setRefZText(String text) {
		// Set text
		RefZ_vl_text = text;
		// Repaint view
		repaint();
	}
	
	/* --- Set positive impedance value text and repaint view --- */
	public synchronized void setPosZText(String text) {
		// Set text
		PosZ_vl_text = text;
		// Repaint view
		repaint();
	}
	
}/* End main class */