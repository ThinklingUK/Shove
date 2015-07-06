package uk.thinkling.shove;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/**
 * SIMPLES
 * Created by Ellison on 11/04/2015.
 */
public class DrawView extends View {


    /*VARIABLES*/

    Random rnd = new Random();
    Ball youBall = new Ball();
    Ball[] ball = new Ball[50];
    int screenW;
    int screenH;

    int gameState = 0;

    MainActivity parent;

    int highScore = 0;
    int score = 0;
    int timeSoFar = 0;

    /*VARIABLES*/

    // make a new Paint object and store it in myPaint
    Paint paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint paint3 = new Paint(Paint.ANTI_ALIAS_FLAG);

    // this is the constructor - it is called when an instance of this class is created
    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);

        parent = (MainActivity) this.getContext();

        // Use Color.parseColor to define HTML colors and set the color of the myPaint Paint object
        paint1.setColor(Color.parseColor("#000000"));
        paint2.setColor(Color.parseColor("#FFFF00"));
        paint3.setColor(Color.parseColor("#FFFFFF"));


        youBall.x = 80;
        youBall.y = 80;
        youBall.xSpeed = 0;
        youBall.ySpeed = 0;
        youBall.radius = 40;
        youBall.mass = 4 / 3 * 3.142 * 40 * 40 * 40;
        youBall.type = 1;

        // initialise the ball array by creating a new Ball object for each entry in the array
        for (int bCount = 0; bCount < ball.length; bCount++) ball[bCount] = new Ball();
        // above can also be written in this form
        // for(Ball b : ball) b = new Ball();


    }

    /* BIT FOR TOUCHING! */

    @Override
    public boolean onTouchEvent(MotionEvent e) {


        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:

               if(gameState == 1) {
                   youBall.xSpeed = (int) (e.getX() - youBall.x) / 10;
                   youBall.ySpeed = (int) (e.getY() - youBall.y) / 10;
               } else {

                   gameState = 1;

               }



            }
        return true;
    }


    @Override
    // this is the method called when the view needs to be redrawn
    protected void onDraw(Canvas canvas) {
        screenW = this.getWidth();
        screenH = this.getHeight();
        super.onDraw(canvas);

        if (gameState == 1) {
            //Timer: Every 10 Seconds Add 1 To Score
            timeSoFar++;
            //time so far goes up by 25 every second so check if two seconds have passed we use time so far % 50
            if (timeSoFar % 50 == 0) {

                score++;

                if (score > highScore) {

                    highScore = score;

                }

            }


        /*Score's Text*/

            parent.HighScoreText.setText("High Score: " + highScore);
            parent.ScoreText.setText("Score: " + score);
            parent.TimeLeftText.setText("Seconds: " + Math.round(timeSoFar / 25));

            // count for each entry in the ball array and then check for collision against all of the subsequent ones.
            // Once the collisions have been handled, move each ball, then draw it.
            // remember that the .move()  function has been programmed to detect wall collisions
            for (int bCount1 = 0; bCount1 < ball.length; bCount1++) {
                for (int bCount2 = bCount1 + 1; bCount2 < ball.length; bCount2++) {
                    ballCollision(ball[bCount1], ball[bCount2]);
                }

                ball[bCount1].move();
                canvas.drawCircle(ball[bCount1].x, ball[bCount1].y, ball[bCount1].radius, ball[bCount1].paint);
            }


            // Check Hero Ball For Collisions
            for (int bCount = 0; bCount < ball.length; bCount++) {

                if (ballCollision(youBall, ball[bCount])) {


                    if (ball[bCount].type == 0) {

                        score = 0;
                        timeSoFar = 0;
                        gameState = 0;

                    }


                }


            }
            //Makes ball bounce on edges
            youBall.move();

            //draw the thinkling
            canvas.drawCircle(youBall.x, youBall.y, youBall.radius, paint1);
            canvas.drawCircle(youBall.x + youBall.radius / 3, youBall.y - youBall.radius / 3, youBall.radius / 4, paint3);
            canvas.drawCircle(youBall.x - youBall.radius / 3, youBall.y - youBall.radius / 3, youBall.radius / 4, paint3);

        } else {
            parent.HighScoreText.setText("New game");
        }

    }


    /* Makes Ball Class */

    // Gives Variable Name And Type
    public class Ball {

        int type;

        int x;
        int y;
        int xSpeed;  // if these are int, then the speed will gradually slow down due to rounding.
        int ySpeed;
        int radius;
        double mass;
        Paint paint = new Paint();

        // This is the default constructor - it sets the data for the variables to random values
        public Ball() {

            type = rnd.nextInt(10); // get a random integer from 0 to 9 for the 'type'

            if (type == 0) {
                paint.setColor(Color.parseColor("#FFFFFF"));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5f);
                radius = 30;
            } else {
                paint.setARGB(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                radius = rnd.nextInt(40) + 10;
                type = 1;
            }

            x = rnd.nextInt(500) + radius;
            y = rnd.nextInt(800) + radius;
            xSpeed = rnd.nextInt(25) - 12;
            ySpeed = rnd.nextInt(25) - 12;
            mass = 4 / 3 * 3.142 * radius * radius * radius;
        }

        public void move() {

            if(type == 0 && rnd.nextInt(250)>248){

                xSpeed = rnd.nextInt(100) - 50;
                ySpeed = rnd.nextInt(100) - 50;
                Log.d("random fire", "on ball");
            }

            //Makes ball bounce on edges
            if (x + xSpeed < radius || x + xSpeed + radius > screenW) {
                xSpeed = -xSpeed;
            }
            if (y + ySpeed < radius || y + ySpeed + radius > screenH) {
                ySpeed = -ySpeed;
            }
            x += xSpeed;
            y += ySpeed;

        }
        //

    }

    public boolean ballCollision(Ball a, Ball b) {

        double mass_ratio, dvx2, norm, xSpeedDiff, ySpeedDiff, fy21, sign;

        float xDist = b.x - a.x;
        float yDist = b.y - a.y;
        float radiusSum = a.radius + b.radius;


        // if the balls are touching - ie. x-squared + y-squared is less that radius+radius-squared TODO - could do bounding-box check
        if ((xDist * xDist + yDist * yDist) < (radiusSum * radiusSum)) {

            mass_ratio = b.mass / a.mass;
            xSpeedDiff = b.xSpeed - a.xSpeed;
            ySpeedDiff = b.ySpeed - a.ySpeed;

            if ((xSpeedDiff * xDist + ySpeedDiff * yDist) >= 0)
                return false;  //return old velocities if balls are not approaching
            // to avoid a zero divide; (for single precision calculations, 1.0E-12 should be replaced by a larger value). **************
            fy21 = 1.0E-12 * Math.abs(yDist);
            if (Math.abs(xDist) < fy21) {
                sign = (xDist < 0) ? -1 : 1;
                xDist = (float) (fy21 * sign);
            }

            //     ***  update velocities ***
            norm = yDist / xDist;
            dvx2 = -2 * (xSpeedDiff + norm * ySpeedDiff) / ((1 + norm * norm) * (1 + mass_ratio));
            b.xSpeed += dvx2;
            b.ySpeed += norm * dvx2;
            a.xSpeed -= mass_ratio * dvx2;
            a.ySpeed -= norm * mass_ratio * dvx2;

            //Log.d("Collide!", "The balls have a distance of" + dist);
            return true; // collision detected, so return true

        }
        //balls not touching so return false
        return false;

    }


}
