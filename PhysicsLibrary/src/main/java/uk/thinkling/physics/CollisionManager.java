package uk.thinkling.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collisions
 * Created by ergo on 01/06/2015.
 */
public class  CollisionManager {

    private final int width;
    private final int height;
    private final double gravity;
    private final double friction;

    public List<CollisionRec> collisions = new ArrayList<>();
    List<CollisionRec> nextCollisions = new ArrayList<>();
    public List<Boundary> boundaries = new ArrayList<>();


    // constructor takes width and height as bounding box. Could be better if it takes bounding box as an object model to allow gaps and falloff etc.
    // this would make snooker simulation pockets etc. possible
    public CollisionManager(int width, int height, double friction, double gravity) {
        this.width = width;
        this.height = height;
        this.gravity = gravity;
        this.friction = friction;
    }

    // add a new boundary record to the collision manager
    // TODO Not yet used
    public void addBoundary(int width, int height, int x, int y) {
        boundaries.add(new Boundary(width,height,x,y));
    }


    //deal with all the collisions for the current timeslice and maintain list of actual collisions that occurred.
    public void collide(List<MoveObj> objs) {
        double dt = 1;
        double t;
        int listsize = objs.size();
        collisions.clear();

        //adjust speeds for friction and gravity
        //EFF may be better with a hand written counted loop
        for (MoveObj obj : objs) obj.applyFrictionGravity(friction, gravity, 0.1, width, height);

        //for this timeslice, find first collision, forward wind to that point, adjust velocities and repeat until timeslice done.
        while (dt > 0) {

            clearCollisions();

            //count for each entry in the objs array and then check for collision against all of the subsequent ones.
            //ignore if time is more than 1 (wait for next cycle)

            for (int bCount1 = 0; bCount1 < listsize; bCount1++) {
                // check wall collisions
                //EFF can check if actually moving or not.
                t = wallCollisionTime(objs.get(bCount1));
                if (t < dt && t >= 0)
                    addCollision(t, objs.get(bCount1), null);

                for (int bCount2 = bCount1 + 1; bCount2 < listsize; bCount2++) {

                    t = objCollisionTime(objs.get(bCount1), objs.get(bCount2));
                    // if a collision time due to happen, then add to the list
                    if (t < dt && t >= 0) {
                        //if (t > 0 && t < 0.001) t = 0.001; // minimum granularity
                        addCollision(t, objs.get(bCount1), objs.get(bCount2));
                    }
                }
            }



            Collections.sort(nextCollisions);

            if (noCollisions()) {
                t = dt;
            } else {
                t = nextCollisions.get(0).time;
            }

            // First, wind each object to time of first collision.
            for (MoveObj obj : objs) obj.move(t);

            // handle all the collisions due at this exact point in time and set new velocities.
            for (CollisionRec coll : nextCollisions) {
                if (coll.time > t) break; // if non-consecutive collision, ignore all subsequent collisions
                if (coll.doCollision()) {
                    //  collisions.add(coll); // add this collision to actual collision list for subsequent analysis
                }
            }
            // Now consider all possible collisions between the balls in their new positions and velocities
            // (we can avoid some of the work at this step because only the possible collisions involving the two balls whose velocities
            // changed at step 2 can be different). Find the earliest of these collisions. TODO EFF

            // update dt (becomes 1 if no collisions so then exits)
            dt -= t;


        }
    }



    private void addCollision(double time, MoveObj obja, MoveObj objb){
        // only add an entry if the same as current or if first in list/
        if (nextCollisions.isEmpty()) {
            nextCollisions.add(new CollisionRec(time, obja, objb));
        } else if (time < nextCollisions.get(0).time) {
            //if an earlier entry found, replace first - this may mean later entries still exist
            nextCollisions.get(0).time = time;
            nextCollisions.get(0).obja = obja;
            nextCollisions.get(0).objb = objb;
        } else if (time==nextCollisions.get(0).time){
            //   nextCollisions.add(new CollisionRec(time, obja, objb));
        }
    }

    private void clearCollisions(){
       // nextCollisions.clear();
        if (nextCollisions.isEmpty()) nextCollisions.add(new CollisionRec(9999,null,null));
        nextCollisions.get(0).time=9999;
    }

    private boolean noCollisions(){
        //return nextCollisions.isEmpty();
        return nextCollisions.get(0).time==9999;
    }


    /// Calculate the time of impact of an object with bounding walls.
    /// Returns: Positive Time if a collision will occur, else negative (ie. has already occurred or no collision.
    /// TODO enable top wall collision only - possibly use boundaries (next function)
    public double wallCollisionTime(MoveObj obj) {

        //detect earliest wall collision, ie. biggest dt (from y or X strike)
        double dt=9999;
        if (!obj.wallBounce) return dt; //if the obj has wall bounce disabled, never return a collision time.
        if (obj.x - obj.radius + obj.xSpeed < 0)
            dt = Math.min(dt, (obj.x - obj.radius) / -obj.xSpeed);
        else if (obj.x + obj.radius + obj.xSpeed > width)
            dt = Math.min(dt, (width - obj.x - obj.radius) / obj.xSpeed);

        if (obj.y - obj.radius + obj.ySpeed < 0)
            dt = Math.min(dt, (obj.y - obj.radius) / -obj.ySpeed);
        else if (obj.y + obj.radius + obj.ySpeed > height)
            dt = Math.min(dt, (height - obj.y - obj.radius) / obj.ySpeed);

        return dt;

    }


    /// Calculate the time of impact of an object with bounding walls.
    /// Returns: Positive Time if a collision will occur, else negative (ie. has already occurred or no collision.
    public double boundaryCollisionTime(MoveObj obj) {

        //detect earliest wall collision, ie. biggest dt (from y or X strike)
        double dt=9999;
        if (!obj.wallBounce) return dt; //if the obj has wall bounce disabled, never return a collision time.

        for (Boundary b : boundaries) {


            if (obj.x - obj.radius + obj.xSpeed < 0)
                dt = Math.min(dt, (obj.x - obj.radius) / -obj.xSpeed);
            else if (obj.x + obj.radius + obj.xSpeed > width)
                dt = Math.min(dt, (width - obj.x - obj.radius) / obj.xSpeed);

            if (obj.y - obj.radius + obj.ySpeed < 0)
                dt = Math.min(dt, (obj.y - obj.radius) / -obj.ySpeed);
            else if (obj.y + obj.radius + obj.ySpeed > height)
                dt = Math.min(dt, (height - obj.y - obj.radius) / obj.ySpeed);

        }
        return dt;
    }

    /// Calculate the time of closest approach of two moving circles.  Also determine if the circles collide.
    /// Returns: Positive Time if a collision will occur, else negative (ie. has already occurred) or no collision.
    private double objCollisionTime(MoveObj obj1, MoveObj obj2)
    {
        // vector Pab - ie. the distance of the 2 centre positions
        double dX = obj2.x - obj1.x, dY = obj2.y - obj1.y;

        // vector Vab - ie. the vector of the relative speed to one another
        double vX = obj2.xSpeed - obj1.xSpeed, vY = obj2.ySpeed - obj1.ySpeed;

        //squares of radius, current distance, new distance and velocity vector
        double radSum = obj1.radius + obj2.radius;
        double radDiff = Math.abs(obj1.radius - obj2.radius);
        double radSq = radSum*radSum;
        double distSq = dX*dX+dY*dY;
        double vectSq = vX*vX+vY*vY;
        double newDistSq=(dX+vX)*(dX+vX)+(dY+vY)*(dY+vY);

        //if an object is within the other
        double dist = Math.sqrt(distSq);
        double newDist = Math.sqrt(newDistSq);

        // WITHIN CIRCLE COLLISION HANDLING.
        // the distance is less than the difference in radii, then one circle is within the other
/*
        if (radDiff>dist) {
            // determine if running at tangent to circle - then deal.
            // if internal collision will happen, time to occur is ratio of gap to distance travelled
            if (radDiff < newDist) return (radDiff - dist) / Math.sqrt(vectSq);
            else return -1;
        }
*/

        //if hypotenuse will be less than radii, then collision will happen at ratio of radius to distance travelled
        if (newDistSq<radSq) return (dist-radSum)/Math.sqrt(vectSq);

        //additional check to catch passing balls

        // check for overlap of the circles - catches passing balls
        // the velocity from the relative velocity dot product
        double a = vectSq;
        double b = 2 * (dX*vX+dY*vY);
        double c = (distSq) - (radSq);

        // The quadratic discriminant.
        double discriminant = b * b - 4 * a * c;

        // Case 1:
        // If the discriminant is negative, then there are no real roots, so there is no collision.
        // NB: The time of closest approach would be the average of the imaginary roots, ie:  t = -b / 2a
        if (discriminant < 0) return -1;

        // Case 2 and 3:
        // If the discriminant is zero, then there is exactly one real root, meaning that the circles just grazed each other.  If the
        // discriminant is positive, then there are two real roots, meaning that the circles penetrate each other.  In that case, the
        // smallest of the two roots is the initial time of impact.  We handle these two cases identically.
        double t0 = (-b + Math.sqrt(discriminant)) / (2 * a);
        double t1 = (-b - Math.sqrt(discriminant)) / (2 * a);
        return Math.min(t0,t1);

        // We also have to check if the time to impact is negative.  If it is negative, then that means that the collision
        // occurred in the past.  Since we're only concerned about future events, we say that no collision occurs if t < 0.

    }


    // record for holding a potential or actual collision event
    public class CollisionRec implements Comparable<CollisionRec>{
        double time;
        public MoveObj obja;
        public MoveObj objb;
        public double impactV = 0;
        // could store the event type instead of relying upon objb


        private CollisionRec(double time, MoveObj obja, MoveObj objb) {
            this.time = time;
            this.obja = obja;
            this.objb = objb;
        }

        //Implement the natural order for this class - ie. event time
        public int compareTo(CollisionRec c)
        {
            return (this.time < c.time ? -1 : 1);
        }

        // deal with a collision at point of impact by changing the velocities
        public boolean doCollision() {
            final double ed=0.7; // elasticity factor set less than 1 to drop off
            double tX=0, tY=0;

            //could be a wall bounce
            if (objb == null) {
                // Makes object bounce on edges by reversing velocity (if it was about to hit - this introduces errors at high speed.
                if (obja.x + obja.xSpeed <= obja.radius || obja.x + obja.xSpeed + obja.radius >= width) {
                    obja.xSpeed = -obja.xSpeed * ed;
                    tY=obja.xSpeed>0?1:-1; // for rotation
                    obja.hitSide=true;
                }

                if (obja.y + obja.ySpeed <= obja.radius || obja.y + obja.ySpeed + obja.radius >= height) {
                    obja.ySpeed = -obja.ySpeed * ed;
                    tX=obja.ySpeed>0?-1:1; //for rotation NB: as Y goes down the screen this is inverted
                    obja.hitTop=true;
                }

                double vt=obja.xSpeed*tX+obja.ySpeed*tY;
                obja.rSpeed+=vt/obja.radius*30; //EFF fixed number to convert speed into rotation

                // store impact velocity for sound
                impactV = Math.sqrt(obja.xSpeed * obja.xSpeed + obja.ySpeed * obja.ySpeed);

                return true; //true if tracking wall bounces
            }

            double dX = objb.x - obja.x, dY = objb.y - obja.y;
            double mass_ratio = obja.mass / objb.mass;
            double vX = objb.xSpeed - obja.xSpeed;
            double vY = objb.ySpeed - obja.ySpeed;

            impactV = Math.sqrt(vX * vX + vY * vY); //used for sound of impact


/*
            //Method 1 -
            double dvx2, norm, fy21, sign;
            double mass_ratio = objb.mass / obja.mass;


            if ((vX * dX + vY * dY) >= 0)
                return false;  //return false with unchanged speeds if balls are not approaching

            // to avoid a zero divide; (for single precision calculations, 1.0E-12 should be replaced by a larger value). **************
            fy21 = 1.0E-12 * Math.abs(dY);
            if (Math.abs(dX) < fy21) {
                sign = (dX < 0) ? -1 : 1;
                dX = (float) (fy21 * sign);
            }

            //     ***  update velocities ***
            norm = dY / dX;
            dvx2 = -2 * (vX + norm * vY) / ((1 + norm * norm) * (1 + mass_ratio));
            objb.xSpeed += dvx2;
            objb.ySpeed += norm * dvx2;
            obja.xSpeed -= mass_ratio * dvx2;
            obja.ySpeed -= norm * mass_ratio * dvx2;*/


            // Method 2 - allows for inelastic collision
            double distance = Math.sqrt(dX * dX + dY * dY);
            //EFF as is this at point of collision, is this not just radius sum

            // Unit vector in the direction of the collision
            double ax = dX / distance, ay = dY / distance;

            // Projection of the velocities in these axes
            double va1 = (obja.xSpeed * ax + obja.ySpeed * ay), vb1 = (-obja.xSpeed * ay + obja.ySpeed * ax);
            double va2 = (objb.xSpeed * ax + objb.ySpeed * ay), vb2 = (-objb.xSpeed * ay + objb.ySpeed * ax);

            // New velocities in these axes (after collision): ed<=1, for elastic collision ed=1
            double vaP1 = va1 + (1 + ed) * (va2 - va1) / (1 + mass_ratio);
            double vaP2 = va2 + (1 + ed) * (va1 - va2) / (1 + 1/mass_ratio);


            // record that the objects collided
            obja.hitObj=true;
            objb.hitObj=true;

            // Apply the projections
            obja.xSpeed = vaP1 * ax - vb1 * ay;
            obja.ySpeed = vaP1 * ay + vb1 * ax;// new vx,vy for ball 1 after collision
            objb.xSpeed = vaP2 * ax - vb2 * ay;
            objb.ySpeed = vaP2 * ay + vb2 * ax;// new vx,vy for ball 2 after collision


            // Calculate rotational values
            tX=dY; tY=-dX; //TODO can we lose this? - ie. fold into calculation below?
            double vt=vX*tX+vY*tY;
            obja.rSpeed-=vt/obja.radius; //TODO - some form of factor to reduce impact surface friction
            objb.rSpeed-=vt/objb.radius;

            double threshold = 0.5; //NB: larger than 0.5
            // if below movement threshold, begin sleep countdown. This can be reset by movement.
            if (obja.xSpeed > threshold || obja.xSpeed < -threshold || obja.ySpeed > threshold+gravity || obja.ySpeed < -threshold) obja.movestate=5; //TODO const
            if (objb.xSpeed > threshold || objb.xSpeed < -threshold || objb.ySpeed > threshold || objb.ySpeed < -threshold) objb.movestate=5; //TODO const

/*
            First get the surface tangent from the surface normal: t = (ny, -nx)
            the normal vector n is: n=〈 x2−x1, y2−y1〉
            the tangent vector equal to the negative of the y component of the unit normal vector,
            and make the y component of the unit tangent vector equal to the x component of the unit
            normal vector (y2-y1, x1-x2)

            Then you can get the velocity component along the surface as vt = v dot t.
            NB:             a · b = ax × bx + ay × by
            NB: |a| is the magnitude (length) of vector a

            Now you can calculate the rotation of the ball: w = |(normal * r) cross vt|, where r is the radius of the ball.
            In two dimensions, the analog of the cross product for a=(ax,ay) and  b=(bx,by) is a x b	=	ax*by-ay*bx
            Magnitude of cross product  |a×b| = |a||b| sinθ
            Also take into account the existing rotation
*/



//TODO - limit velocities so they're not too high (e.g. screen size/25)
            return false;

        }

    }

    // record for holding a boundary - TODO not yet used
    public class Boundary{
        public int width;
        public int height;
        public int x;
        public int y;


        public Boundary(int width, int height, int x, int y) {
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
        }
    }





}
