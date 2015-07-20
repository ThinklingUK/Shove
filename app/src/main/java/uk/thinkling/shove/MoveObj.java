package uk.thinkling.shove;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

/**
 * SIMPLES
 * Created by ergo on 29/05/2015.
 */ /* Makes MoveObj Class */
// Gives Variable Name And Type - NB: because we want to be able to save the state in a file (serializable)
// some of the variables (object properties) are marked as transient and so will not get serialized - e.g. Paint

public class MoveObj implements Serializable {
    int type;
    double x;
    double y;
    double xSpeed;  // if these are int, then the speed will gradually slow down due to rounding.
    double ySpeed;
    double rSpeed; //rotational speed
    int radius;
    transient int movingStreamID = 0;
    double mass;
    int state = 1;
    boolean wallBounce = true;
    float angle = 0; // rotational angle of the object
    int attack; // power used in collisions
    int defense; // defence used in collisions
    transient Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    transient static Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    static Random rnd = new Random();

    public MoveObj(int type, int radius, float x, float y, double xSpeed, double ySpeed) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.xSpeed = xSpeed;
        this.ySpeed = ySpeed;
        this.radius = radius;
        this.mass= 4 / 3 * 3.142 * radius * radius;

        setPaint();

    }

    private void setPaint() {
        switch (type) {
            case 0:
                paint.setColor(Color.parseColor("#FFFFFF"));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5f);
                break;

            case 11: //p1 ball
            case 12: //p2 ball
                paint.setColor(Color.parseColor("#ff0000"));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                break;

            case 100: //hero ball
                paint.setColor(Color.parseColor("#000000"));
                stroke.setColor(Color.parseColor("#FFFFFF"));
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2f);
                break;

            default:
                paint.setARGB(255, rnd.nextInt(200)+55, rnd.nextInt(200)+55, rnd.nextInt(200)+55);
        }

        paint.setAntiAlias(true);
    }

    // Based on radius, sets random position and speed
    public MoveObj(int type, int radius, int screenW, int screenH) {
        this(type, radius, rnd.nextInt(screenW - radius * 2) + radius, rnd.nextInt(screenH - radius * 2) + radius, rnd.nextInt(25) - 12, rnd.nextInt(25) - 12); // get a random integer from 0 to 9 for the 'type'
    }

    // This is the type specific constructor - it sets radius to random values dependent on type
    public MoveObj(int type, int screenW, int screenH) {
        this(type, type==0?screenW/20:rnd.nextInt(screenW/30) + screenW/60, screenW, screenH); // get a random integer for the radius
    }

    // This is the default constructor - it sets the type to random values
    public MoveObj(int screenW, int screenH) {
        this(rnd.nextInt(10), screenW, screenH); // get a random integer from 0 to 9 for the 'type'
    }

    // this is used when serializing - can store Colour Int from Paint object
    private void writeObject(ObjectOutputStream out)throws IOException {
        out.defaultWriteObject();
        out.writeObject(paint.getColor());
        //TODO should also include stroke etc.
    }

    // this is used when de-serializing - can recreate Paint object from Colour
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        paint = new Paint();
        paint.setColor((int) in.readObject());
        setPaint();
        Log.d("deserialize", this.toString());
    }

    public String toString() {
        return "MoveObj{" +
                "type=" + type +
                ", x=" + x +
                ", y=" + y +
                ", xSpeed=" + xSpeed +
                ", ySpeed=" + ySpeed +
                ", radius=" + radius +
                '}';
    }

    public void draw(Canvas canvas) {

        switch (type) {

/*                case 0:
                canvas.drawCircle(x, y, radius, paint);
                break;*/

            case 100:
                canvas.drawCircle((float)x, (float)y, radius, paint);
                canvas.drawCircle((float)x, (float)y, radius, stroke);
                canvas.drawCircle((float)x + radius / 3, (float)y - radius / 3, radius / 4, stroke);
                canvas.drawCircle((float)x - radius / 3, (float)y - radius / 3, radius / 4, stroke);
                break;

/*          case 11:
            case 12:
                //special case for shove hapenny - drawing is done in DrawView with bitmaps should hold bitmap ref within obj
                break;
                */

            default:
                canvas.drawCircle((float) x, (float) y, radius, paint);
                //double radians = Math.toRadians(angle);
                //canvas.drawLine((float)x, (float)y, (float)(x + radius* Math.cos(radians)), (float)(y + radius* Math.sin(radians)), paint);
        }
    }

    public void applyFrictionGravity(double friction, double gravity, double rFriction, int screenW, int screenH) {

        // slow the object based on friction and accelerate down based on gravity
        xSpeed *= 1 - friction;
        ySpeed *= 1 - friction;
        rSpeed *= 1 - friction;
        ySpeed += gravity;

        if (Math.abs(xSpeed) < 0.1 && Math.abs(ySpeed) < 0.1) xSpeed = ySpeed = 0;

        // also zero speed if out of bounds
        if (x < -radius || x>screenW+radius || y < - radius || y > screenH+radius) xSpeed = ySpeed = 0;

        // NB: TODO gravity may be cancelled out before it can build esp if friction and gravity used
    }

    public void move(double time) {
        // move the object based on speed
        x += xSpeed*time;
        y += ySpeed*time;
        angle += rSpeed*time;
    }
}




