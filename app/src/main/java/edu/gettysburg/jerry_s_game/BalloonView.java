package edu.gettysburg.jerry_s_game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.HashMap;

/**
 * Created by parker on 11/28/17.
 */

public class BalloonView extends View {
    private int nBalloons = 40;
    private int speed = 7;
    private int width;
    private int height;
    Canvas canvas1;
    private HashMap<Rect, Integer> balloons = new HashMap<Rect, Integer>();
    private ArrayList<ImageView> balloonSelector = new ArrayList<ImageView>();

    private Handler handler = new Handler();
    private Timer timer = new Timer();

    boolean isGameOver = false;
    int totalScore = 0;

    static final Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public final Drawable[] balloonsDrawable = new Drawable[9];
    public final Bitmap[] bitmaps = new Bitmap[9];
    public Bitmap bitmapPopped = null;
    Rect[] destRects = new Rect[nBalloons];
    Integer[] destBalloons = new Integer[nBalloons];
    TreeMap<Integer, Integer> balloonTouched = new TreeMap<Integer, Integer>();
    TreeSet<Integer> balloonDisappeared = new TreeSet<Integer>();
    HashMap<Integer, Integer> balloonPoints = new HashMap<Integer, Integer>();
    int balloonHeight;
    int balloonWidth;

    Rect srcRect;
    Rect destRect;

    //black, purple, blue, green, yellow, orange, red
    public static final Integer[] imageResIds = new Integer[]{0, R.drawable.black,
            R.drawable.purple,R.drawable.blue,R.drawable.green, R.drawable.yellow,
            R.drawable.orange,R.drawable.red, R.drawable.death};
    public static final int[] points = {0, 11, 9, 7, 5, 3, 2, 1, -1};

    Resources res = null;

    static{
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Paint.Style.FILL);
    }

    public BalloonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        res = context.getResources();
        init();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        this.width = width;
        this.height = height;
        super.onSizeChanged(width, height, oldw, oldh);
    }

    private void init(){
        isGameOver = false;

        //width = getWidth(); // does not work, need to fix
        //height = getHeight(); // TODO/width = 1000;
        width = 1080;
        height = 1584;

        //Log.i("width", "" + width);
        //Log.i("height", "" + height);

        for (int i = 1; i < imageResIds.length; i++){
            balloonsDrawable[i] = res.getDrawable(imageResIds[i]);
            bitmaps[i] = BitmapFactory.decodeResource(res, imageResIds[i]);
        }
        int resIDPopped = R.drawable.popped;
        bitmapPopped = BitmapFactory.decodeResource(res, resIDPopped);

        balloonHeight = (bitmaps[1].getHeight() - 1) / 3;
        balloonWidth = (bitmaps[1].getWidth() - 1) / 3;

        //make n random balloons with random rectangles
        Random rand = new Random();

        for (int i = 0; i < nBalloons; i++) {
            int a = rand.nextInt(8) + 1;
            int left = rand.nextInt(width - balloonWidth);
            int top = rand.nextInt(2 * height);

            Rect dRect = new Rect(left, top, left + balloonWidth, top + balloonHeight);
            destRects[i] = dRect;
            destBalloons[i] = a; //so we can pair the color with the balloon

            balloonPoints.put(i,points[a]);
           // canvas1.drawBitmap(bitmaps[a], srcRect, dRect, paint);
        }
        srcRect = new Rect(0, 0, bitmaps[1].getWidth() - 1, bitmaps[1].getHeight() - 1);
        destRect = new Rect(0, 0, balloonWidth, balloonHeight);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        //Log.i("in onDraw!!", "in onDraw!!!" );

        canvas1 = canvas;
        width = getWidth();
        height = getHeight();

        canvas.drawRect(0,0,width,height,whitePaint);
        for (int i = 0; i < nBalloons; i++) {
            if (balloonTouched.containsKey(i) || balloonDisappeared.contains(i)) {
                if (balloonTouched.containsKey(i)) {
                    canvas1.drawBitmap(bitmapPopped, srcRect, destRects[i], paint);
                }
            }
            else {
                canvas1.drawBitmap(bitmaps[destBalloons[i]], srcRect, destRects[i], paint);
            }
        }



        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        changePos();
                        invalidate();
                    }
                });
            }
        }, 20, Integer.MAX_VALUE);

//        timer.schedule(new TimerTask() {
//            public void run() {
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        addBalloons();
//                    }
//                });
//            }
//        }, 0, 1000);

        // makeBalloonsClickable();
    }

    public void changePos(){
        //Log.i("changePos", " ");
        for (int i = 0; i < nBalloons; i++){
            Rect rect = destRects[i];

            if (rect.bottom < 0){
                destRects[i] = new Rect(rect.left, rect.top + 2*height, rect.right, rect.bottom + 2*height);
                // reset balloon bitmap to make them appear again
                if (balloonTouched.containsKey(i)) {
                    balloonTouched.remove(i);
                }
                if (balloonDisappeared.contains(i)) {
                    balloonDisappeared.remove(i);
                }
                // if they are on the top, we move them to the bottom. Below where the screen is
            }
            else {
                destRects[i] = new Rect(rect.left, rect.top - speed, rect.right, rect.bottom - speed); // move them to the top
                // for animation: update balloons' bitmaps
                if (balloonTouched.containsKey(i)) {
                    if (balloonTouched.get(i) > 10) {
                        balloonTouched.remove(i);
                        balloonDisappeared.add(i);
                    }
                    else {
                        int times = balloonTouched.get(i);
                        balloonTouched.put(i, times + 1);
                    }
                }
            }
        }

    //    Random rand = new Random();
    //    int a = rand.nextInt(8) + 1;
        //int left = rand.nextInt(width - balloonWidth);
        //int top = rand.nextInt(height - balloonHeight);

        // Rect dRect = new Rect(left, top, left + balloonWidth, top + balloonHeight);
     //   canvas1.drawBitmap(bitmaps[a], srcRect, destRect, paint);
        //canvas1.drawBitmap(bitmaps[a], srcRect, dRect, paint);


    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isGameOver && event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            int touchX = (int)event.getX();
            int touchY = (int)event.getY();
            for (int i = 0; i < nBalloons; ++i) {
                Rect rect = destRects[i];
                int balloonIndex = destBalloons[i];
                if (rect.contains(touchX, touchY)) {
                    Log.i("onTouchEvent","points " + balloonPoints.get(i));
                    balloonTouched.put(i,0);
                    totalScore += balloonPoints.get(i);
                    Log.i("onTouchEvent","totalScore " + totalScore);
                }
            }
        }
        return true;
    }

    /*public void makeBalloonsClickable() {

        for (ImageView balloon : balloons) {

            balloon.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    Log.i("touched", "balloon touched");
                    //     balloon.setVisibility(View.INVISIBLE);
                    balloonTouched(view);
                    return false;
                }
            });
        }
    }

    public void balloonTouched(View view) {
        view.setVisibility(View.INVISIBLE);
    }*/
}