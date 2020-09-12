package qm.display.customviews;

/* Libraries imported */
import qm.display.qcon.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

/* Start main class */
public class qNOXBoxView extends View {
	
	/*** Start variables ***/
	// Debugging
	//private static final String TAG = "qNOXBoxView";
	
	// Members variables
	private static int mViewHeight;
	private static int mViewWidth;
	private static int lb_Height;
	private static int text_xAxisPoint;
	private static int lb_yAxisPoint;
	private static int vl_yAxisPoint;
	private static float descent;
	
	private static int lb_Background_Size;
	private static int lb_Background_Color;
	private static String lb_text;
	private static int lb_Text_Color;
	private static boolean lb_Text_BelowBaseLine;
	private static float lb_textSize;
	
	private static String vl_text;
	private static int vl_Text_Color;
	private static boolean vl_Text_BelowBaseLine;
	private static float vl_textSize;
	
	private static Paint lb_BackgroundPaint;
	private static Paint lb_TextPaint;
	private static Paint vl_TextPaint;
	private static Rect textBoundaries;
	/*** End variables ***/
	
	
	/** ---- Class constructors ---- **/
	public qNOXBoxView(Context context) {
		super(context);
		
		// Set text values
		lb_text = "";
		vl_text = "";
		
		// Initialize paints
		init(context);
	}
	
	public qNOXBoxView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// Set text values
		lb_text = "";
		vl_text = "";
		
		// attrs contains the raw values for the XML attributes
        // that were specified in the layout, which don't include
        // attributes set by styles or themes, and which may have
        // unresolved references. Call obtainStyledAttributes()
        // to get the final values for each attribute.
        //
        // This call uses R.styleable.qNOXBoxView, which is an array of
        // the custom attributes that were declared in values/attrs.xml.
		TypedArray attributes = context.getTheme().obtainStyledAttributes(
				attrs, R.styleable.qNOXBoxView, 0, 0);
		
		try {
			// Retrieve the values from the TypedArray and store into
	        // fields of this class.
			lb_Background_Size = attributes.getInteger(R.styleable.qNOXBoxView_qNOX_label_Background_Size, 25);
			lb_Background_Color = attributes.getColor(R.styleable.qNOXBoxView_qNOX_label_Background_Color, Color.RED);
			lb_text = attributes.getString(R.styleable.qNOXBoxView_qNOX_label_Text);
			lb_Text_Color = attributes.getColor(R.styleable.qNOXBoxView_qNOX_label_Text_Color, Color.BLACK);
			lb_Text_BelowBaseLine = attributes.getBoolean(R.styleable.qNOXBoxView_qNOX_label_Text_BelowBaseLine, false);
			vl_text = attributes.getString(R.styleable.qNOXBoxView_qNOX_value_Text);
			vl_Text_Color = attributes.getColor(R.styleable.qNOXBoxView_qNOX_value_Text_Color, Color.RED);
			vl_Text_BelowBaseLine = attributes.getBoolean(R.styleable.qNOXBoxView_qNOX_value_Text_BelowBaseLine, false);
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
		lb_Height = 0;
		text_xAxisPoint = 0;
		lb_yAxisPoint = 0;
		vl_yAxisPoint = 0;
		descent = 0;
		
		// Label background
		lb_BackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		lb_BackgroundPaint.setColor(lb_Background_Color);
		lb_BackgroundPaint.setStyle(Style.FILL);
		// Label text
		lb_TextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		lb_TextPaint.setColor(lb_Text_Color);
		lb_TextPaint.setTextAlign(Align.CENTER);
		// Value text
		vl_TextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		vl_TextPaint.setColor(vl_Text_Color);
		vl_TextPaint.setTextAlign(Align.CENTER);
		
		// Rectangle for text boundaries
		textBoundaries = new Rect();
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
		
		// Calculate labels and values xAxis point
		text_xAxisPoint = mViewWidth/2;
		// Calculate labels and values yAxis point
		lb_yAxisPoint = lb_Height/2;
		vl_yAxisPoint = lb_Height + (mViewHeight - lb_Height)/2;
		
		// -- Draw label background rectangle -- \\
		canvas.drawRect(0, 0, mViewWidth, lb_Height, lb_BackgroundPaint);
		
		// ----- Draw label text ----- \\
		// Set text size
		lb_textSize = lb_Height - 10;
		lb_TextPaint.setTextSize(lb_textSize);
		// Measure text boundaries
		lb_TextPaint.getTextBounds(lb_text, 0, lb_text.length(), textBoundaries);
		// Change text size until match view width
		while (textBoundaries.width() > mViewWidth-20) {
			lb_textSize = lb_textSize -1;
			lb_TextPaint.setTextSize(lb_textSize);
			lb_TextPaint.getTextBounds(lb_text, 0, lb_text.length(), textBoundaries);
		}
		// Measure distance below text baseline
		descent = lb_TextPaint.descent();
		if (lb_Text_BelowBaseLine == false) {
			descent = 0;
		}
		// Draw label text
		canvas.drawText(lb_text, text_xAxisPoint,
						lb_yAxisPoint + (textBoundaries.height()/2) - descent,
						lb_TextPaint);
		
		// ----- Draw value text ----- \\
		vl_textSize = mViewHeight - lb_Height;
		vl_TextPaint.setTextSize(vl_textSize);
		// Measure text boundaries
		vl_TextPaint.getTextBounds(vl_text, 0, vl_text.length(), textBoundaries);
		// Change text size until match view width
		while (textBoundaries.width() > mViewWidth-15) {
			vl_textSize = vl_textSize - 1;
			vl_TextPaint.setTextSize(vl_textSize);
			vl_TextPaint.getTextBounds(vl_text, 0, vl_text.length(), textBoundaries);
		}
		// Measure distance below text baseline
		descent = vl_TextPaint.descent();
		if (vl_Text_BelowBaseLine == false) {
			descent = 0;
		}
		// Draw value text
		canvas.drawText(vl_text, text_xAxisPoint,
						vl_yAxisPoint + (textBoundaries.height()/2) - descent,
						vl_TextPaint);
	}
	
	/* --- Repaint hole view --- */
	private void repaint() {
		this.invalidate();
		this.requestLayout();
	}
	
	/* --- Set text value and repaint view --- */
	public synchronized void setText(String text) {
		// Set text
		vl_text = text;
		// Repaint view
		repaint();
	}
	
	/* --- Set value text color --- */
	public synchronized void setTextColor(int color) {
		if (vl_TextPaint != null) {
			vl_TextPaint.setColor(color);
		}
	}
	
	/* --- Set value text type face --- */
	public synchronized void setTypeface (Typeface typeface) {
		if (vl_TextPaint != null) {
			vl_TextPaint.setTypeface(typeface);
		}
	}
	
	
}/* End main class */