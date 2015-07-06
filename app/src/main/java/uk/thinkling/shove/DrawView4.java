package uk.thinkling.shove;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.*;

/**
 * SIMPLES
 * Created by Ellison on 11/04/2015.
 */
public class DrawView4 extends View {


    /*VARIABLES*/

    MoveObj player1;
    List<MoveObj> objs = new ArrayList<>();
    CollisionManager collider;

    int screenW;
    int screenH;

    int gameState = 0;
    final int balls = 20;
    final int startSeconds = 100;

    MainActivity parent;

    int highScore = 0;
    int score = 0;
    int timeSoFar = 0;
    int seconds = 100;

    /*VARIABLES*/


    // this is the constructor - it is called when an instance of this class is created
    public DrawView4(Context context, AttributeSet attrs) {
        super(context, attrs);
        parent = (MainActivity) this.getContext();
    }


    // this happens if the scren size changes - including the first timeit is measured. Here is where we get width and height
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        Log.d("onMeasure", w + " "+ h);
        screenW = w;
        screenH = h;
        collider = new CollisionManager(w,h, 0, 0);
        // initialise the objs array by creating a new MoveObj object for each entry in the array
    }

    /* BIT FOR TOUCHING! */
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {

        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (gameState == 1) {
                    player1.xSpeed = (int) (e.getX() - player1.x) / 10;
                    player1.ySpeed = (int) (e.getY() - player1.y) / 10;
                } else {
                    gameState = 1;
                    objs.clear();
                    objs.add(new MoveObj(100, screenW/15, screenW, screenH));
                    player1 = objs.get(0);
                    for (int bCount = 1; bCount < balls; bCount++) objs.add(new MoveObj(screenW, screenH));
                    objs.add(new MoveObj(0, screenW, screenH)); //make sure at least one white ball
                    seconds=startSeconds;
                  }
        }

        return true;
    }


    @Override
    // this is the method called when the view needs to be redrawn
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if (gameState == 0) {
            score = 0;
        }else{

            //Timer: Counts down to timeout
            timeSoFar++;
            if (timeSoFar % 25 == 0) {
                seconds--;
            }
            highScore = Math.max(highScore, score);


        /*Score's Text*/
            parent.HighScoreText.setText("High: " + highScore);
            parent.ScoreText.setText("Score: " + score);
            parent.TimeLeftText.setText("Seconds: " + seconds);

            // handle all the collisions starting at earliest time - if 2nd obj is null, then a wall collision
            // if any collisions, deal with change and play sounds etc.
            collider.collide(objs);
            for (CollisionManager.CollisionRec coll : collider.collisions) {
                if (coll.objb == null) continue; //ignore a wall collision
                if (coll.obja.type == 0) coll.objb.state = 0;
                else if (coll.objb.type == 0) coll.obja.state = 0;
                float volume = Math.min((float)coll.impactV/100,1);
                float impactS = 1; //could be 0.5 to 2.0 x speed
                parent.player.play(parent.clinkSound,volume,volume,1,0,impactS);
            }

            int balls_left = 0;

            // Once the collisions have been handled, draw each object and apply friction
            Iterator<MoveObj> i = objs.iterator();
            while (i.hasNext()) {
                MoveObj obj = i.next(); // must be called before you can call i.remove()
                balls_left += (obj.type == 0) ? 0 : 1;
                obj.draw(canvas);
                if (obj.state == 0) obj.radius -= 1;
                if (obj.radius == 0) {
                    i.remove();
                    score += 1;
                }
            }

            if (balls_left == 1) {
                score += seconds;
                seconds += startSeconds;
                for (int bCount = 1; bCount < balls; bCount++) objs.add(new MoveObj(screenW, screenH));
            }

            if (player1.radius == 0 || seconds<=0) gameState=0;
        }
    }
}
