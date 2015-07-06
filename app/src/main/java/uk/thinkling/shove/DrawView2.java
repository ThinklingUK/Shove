package uk.thinkling.shove;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * SIMPLES
 * Created by Ellison on 11/04/2015.
 */
public class DrawView2 extends View {


    /*VARIABLES*/

    MoveObj player1;
    List<MoveObj> objs = new ArrayList<>();
    final int balls = 25;
    final double gravity = 1, friction = 0;

    CollisionManager collider;

    int screenW;
    int screenH;

    MainActivity parent;

    int highScore = 0;
    int score = 3;
    int timeSoFar = 0;

    /*VARIABLES*/


    // this is the constructor - it is called when an instance of this class is created
    public DrawView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        parent = (MainActivity) this.getContext();
    }


    // this happens if the scren size changes - including the first timeit is measured. Here is where we get width and height
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        Log.d("onMeasure", w + " "+ h);
        screenW = w;
        screenH = h;

        collider = new CollisionManager(w, h, friction, gravity);

        // initialise the objs array by creating a new MoveObj object for each entry in the array
        // TODO - maybe only do this if null
        player1 = new MoveObj(100, 40, w, h);
        objs.add(player1);
        for (int bCount = 1; bCount < balls; bCount++) objs.add(new MoveObj(screenW, screenH));

    }

    /* BIT FOR TOUCHING! */
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {

        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (score>0){
                    score--;
                    player1.xSpeed = (int) (e.getX() - player1.x) / 10;
                    player1.ySpeed = (int) (e.getY() - player1.y) / 10;
                }
        }

        return true;
    }


    @Override
    // this is the method called when the view needs to be redrawn
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        //Timer: Every 10 Seconds Add 1 To Score
        timeSoFar++;
        if(timeSoFar% 250 == 0){
           score++;
           if(score > highScore)    highScore = score;
        }

        /*Score's Text*/
        parent.HighScoreText.setText("High Score: "+highScore);
        parent.ScoreText.setText("Score: "+ score);
        parent.TimeLeftText.setText("Seconds: "+Math.round(timeSoFar/25));

        // handle all the collisions starting at earliest time - if 2nd obj is null, then a wall collision
        //if any collisions,
        collider.collide(objs);
        for (CollisionManager.CollisionRec coll : collider.collisions) {
            if (coll.objb == null) continue; //ignore a wall collision
            if (coll.obja.type == 100 && coll.objb.type == 0) score++;
        }

        for (MoveObj obj : objs) {
            obj.draw(canvas);
        }

     }
}
