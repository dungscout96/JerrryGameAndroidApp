package edu.gettysburg.jerry_s_game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    private int nBalloons = 1000;
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
    int error = 0;

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
    int totalLine = 0;

    Rect srcRect;
    //Rect destRect;

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

    private void init(){
        isGameOver = false;

        // TODO get actual width and height
        width = 1080;
        height = 1584;
        // we need to somehow be able to fix this: init() gets called before onDraw.
        // we can't get the width or height of the screen unless we are on that method
        // thus, we can't randomize where our balloons start before drawing them (absurd?)
        // unless we assume some width and height.
        // Maybe to fix it, we could not initialize the balloons and create an if statement in the
        // onDraw method such that when we update the width and the height (at the beginning 0)
        // we create new balloons.
        // So far it generates the balloons at the given width and height

        for (int i = 1; i < imageResIds.length; i++){
            balloonsDrawable[i] = res.getDrawable(imageResIds[i]);
            bitmaps[i] = BitmapFactory.decodeResource(res, imageResIds[i]);
        }

        int resIDPopped = R.drawable.popped;
        bitmapPopped = BitmapFactory.decodeResource(res, resIDPopped);

        balloonHeight = (bitmaps[1].getHeight() - 1) / 3;
        balloonWidth = (bitmaps[1].getWidth() - 1) / 3;

        //make n random balloons with random rectangles
        generateNew(width, height);

        srcRect = new Rect(0, 0, bitmaps[1].getWidth() - 1, bitmaps[1].getHeight() - 1);
    }

    private void generateNew(int width, int height) {
    //    System.out.println("456789132456789456123456");
        int total = 0;
        int limit = nBalloons;
        Random rand = new Random();
        while (total < limit) {
            int per_line = rand.nextInt(4) + 2;
            if (total + per_line > limit) per_line = limit - total;
            int[] colorsBall = new int[per_line];

            int length = width / per_line;
            int maxWidth = length - balloonWidth;
            for (int i = 0; i < per_line; i++) {
                int ranLeft = rand.nextInt(maxWidth);
                colorsBall[i] = ranLeft + length * i;

            }


            for (int i = 0; i < per_line; i++) {
                int deviateHeight = balloonHeight * totalLine + rand.nextInt(balloonHeight * 2);
                Rect ran = new Rect(colorsBall[i], height + deviateHeight,
                        colorsBall[i] + balloonWidth, height + balloonHeight + deviateHeight);

                int a = rand.nextInt(8) +1;
                destBalloons[total + i] = a;
                destRects[total + i] = ran;
                balloonPoints.put(total+i,points[a]);
            }
            total += per_line;
            totalLine++;

        }
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        canvas1 = canvas;

        LinearLayout layout = new LinearLayout(getContext());
        TextView missView = new TextView(getContext());
        missView.setVisibility(View.VISIBLE);
        missView.setText("       Misses " + error);
        missView.setTextSize(30);
        missView.setTextColor(Color.RED);
        missView.setGravity(Gravity.RIGHT);

        TextView scoreView = new TextView(getContext());
        scoreView.setVisibility(View.VISIBLE);
        scoreView.setText("Your Score " + totalScore);
        scoreView.setTextSize(30);
        scoreView.setTextColor(Color.BLACK);
        scoreView.setGravity(Gravity.LEFT);
        layout.addView(scoreView);
        layout.addView(missView);
        layout.measure(canvas.getWidth(), 20);
        layout.layout(0, 0, canvas.getWidth(), 20);
        layout.setGravity(Gravity.CENTER);

        canvas.drawRect(0,scoreView.getHeight(),width,height,whitePaint);

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
        }, 5, Integer.MAX_VALUE);

        layout.draw(canvas);
        // makeBalloonsClickable();
    }

    public void changePos(){
        for (int i = 0; i < nBalloons; i++){
            Rect rect = destRects[i];

            if (rect.bottom < 0){
                destRects[i] = new Rect(rect.left, rect.top + totalLine * balloonHeight, rect.right, rect.bottom + totalLine * balloonHeight);
                // reset balloon bitmap to make them appear again
                if (!balloonTouched.containsKey(i) && !balloonDisappeared.contains(i)) {
                    if (destBalloons[i] != 8 && balloonPoints.get(i) != -1) {
                        error++;
                        Log.i("error", "" + error);
                        if (error >= 20) {
                            endgame();
                            return;
                        }
                    }
                }
               else if (balloonTouched.containsKey(i)) {
                    balloonTouched.remove(i);
                }
                 else  {
                    balloonDisappeared.remove(i);
                }
                // if they are on the top, we move them to the bottom. Below where the screen is

                // TODO update visibility here to make them visible again
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
    }

    // TODO make balloons clickable
    /* make them invisible if clickable and make them visible again in changePos()
        also, set a counter for the score
     */

    /*
     // TODO set a timer
    We have to be able to lose somehow, right? And if we lose, save the High Score
     */

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isGameOver && event.getActionMasked() == MotionEvent.ACTION_DOWN || !isGameOver && event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            int touchX = (int)event.getX();
            int touchY = (int)event.getY();
            for (int i = 0; i < nBalloons; ++i) {
                Rect rect = destRects[i];
                int balloonIndex = destBalloons[i];
                if (rect.contains(touchX, touchY) && !balloonDisappeared.contains(i)){
                    Log.i("onTouchEvent","points " + balloonPoints.get(i));
                    balloonTouched.put(i,0);
                    totalScore += balloonPoints.get(i);

                    if (balloonPoints.get(i) == -1){
                        endgame();
                    }

                    Log.i("onTouchEvent","totalScore " + totalScore);
                }
            }
        }
        return true;
    }

    public void endgame(){
        Context context = getContext();
        int savedHighScore = 0;
        try {
            FileInputStream fis = context.getApplicationContext().openFileInput("high_score");
            DataInputStream in = new DataInputStream(fis);
            savedHighScore = in.readInt();
//            savedHighScore = fis.read();
            Log.i("endGame", "Current high score: " + savedHighScore);
            fis.close();
            in.close();
            if (savedHighScore < totalScore) {
                try {
                    FileOutputStream fos = context.getApplicationContext().openFileOutput("high_score", Context.MODE_PRIVATE);
                    DataOutputStream out = new DataOutputStream(fos);
                    out.writeInt(totalScore);
                    fos.close();
                    out.close();
                    Log.i("endGame", "wrote to high_score " + totalScore);
                }
                catch (FileNotFoundException e) {

                }
            }
            else {
                Log.i("endGame", "Should have updated");
            }
        }
        catch (Exception e) {
            try {
                Log.i("endGame", "write to high_score first time");
                FileOutputStream fos = context.getApplicationContext().openFileOutput("high_score", Context.MODE_PRIVATE);
                DataOutputStream out = new DataOutputStream(fos);
//                fos.write(totalScore);
                out.writeInt(totalScore);
                fos.close();
                out.close();
            }
            catch (Exception e1) {

            }
        }
        Toast.makeText(getContext(),"Game Over",Toast.LENGTH_SHORT).show();
        MainActivity homeActivity = (MainActivity) getContext();
        homeActivity.gameOver(totalScore);
        // get score
        // assign to highscore
    }

}