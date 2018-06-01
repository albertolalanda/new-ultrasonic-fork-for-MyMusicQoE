package org.moire.ultrasonic.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

import org.moire.ultrasonic.R;

//LALANDA APP COMPAT PROBLEMS

@SuppressLint("AppCompatCustomView")
public class VerticalSeekBar extends SeekBar
{
    public VerticalSeekBar( Context context )
    {
        super( context );
    }

    public VerticalSeekBar( Context context, AttributeSet attrs, int defStyle )
    {
        super( context, attrs, defStyle );
    }

    public VerticalSeekBar( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }

    protected void onSizeChanged( int w, int h, int oldw, int oldh )
    {
        super.onSizeChanged( h, w, oldh, oldw );
    }

    @Override
    protected synchronized void onMeasure( int widthMeasureSpec, int heightMeasureSpec )
    {
        super.onMeasure( heightMeasureSpec, widthMeasureSpec );
        setMeasuredDimension( getMeasuredHeight(), getMeasuredWidth() );
    }

    protected void onDraw( Canvas canvas )
    {
        canvas.rotate( -90 );
        canvas.translate( -getHeight(), 0 );

        int thumbX = updateThumbPosition();
        super.onDraw( canvas );
        //drawInfoText( canvas, thumbX );

    }

    private int updateThumbPosition()
    {
        Resources res = getResources();
        //setThumb(res.getDrawable(R.drawable.thumb));

        Rect bounds = getProgressDrawable().getBounds();
        int progressPosX = (int) ( bounds.left + ( (float) this.getProgress() / (float) this.getMax() ) * bounds.width() );
        int thumbX = progressPosX - getThumbOffset();
        Drawable thumbDrawable = null;
        //LALANDA SEE
        //https://stackoverflow.com/questions/32181705/call-requires-api-level-11current-min-is-8-android-app-activityoncreateview?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
        //this changes the api level to 16 from 14

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            thumbDrawable = getThumb();
        }*/

        thumbDrawable = res.getDrawable(R.drawable.thumb);

        //
        int top = getWidth() / 2 - thumbDrawable.getIntrinsicWidth() / 2;
        int bottom = thumbDrawable.getIntrinsicWidth() / 2 + getWidth() / 2;
        thumbDrawable.setBounds( thumbX, top, thumbX + thumbDrawable.getIntrinsicWidth(), bounds.top + bottom );
        return thumbX;
    }

    private void drawInfoText( Canvas canvas, int thumbX )
    {
        canvas.restore();
        Paint paint = new Paint();
        paint.setColor( Color.WHITE );
        paint.setStyle( Paint.Style.FILL_AND_STROKE );
        paint.setDither( true );
        paint.setAntiAlias( true );
        paint.setTextSize( 25 );
        int textX = (int) ( getWidth() / 2 - paint.measureText( "Drag the bar" ) / 2 );
        canvas.drawText( "Drag the bar " + getProgress(), textX, getHeight() - thumbX + 30, paint );
    }


    @Override
    public boolean onTouchEvent( MotionEvent event )
    {
        if ( !isEnabled() )
        {
            return false;
        }

        switch ( event.getAction() )
        {
            case MotionEvent.ACTION_UP:
                //line to make seekbar always go to top interval
                //int progress = ( getProgress() / 20 ) * 20 + 20;
                int progress = getProgress();
                setProgress( progress );
                break;
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                int i = getMax() - (int) ( getMax() * event.getY() / getHeight() );
                setProgress( i );
                onSizeChanged( getWidth(), getHeight(), 0, 0 );
                break;
        }
        return true;
    }

}