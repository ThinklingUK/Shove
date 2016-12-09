package uk.thinkling.shove;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import android.util.Log;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

import uk.thinkling.physics.MoveObj;
import uk.thinkling.physics.CollisionManager;




/**
 * Shove
 * Created by David & Ellison on 11/04/2015.
 */
public class ShoveDrawView extends View {


    /*VARIABLES*/

    MoveObj inPlay;
    List<MoveObj> objs = new ArrayList<>();
    uk.thinkling.physics.CollisionManager collider;

    int screenW, screenH, bedH, sidebar, coinR, startZone;
    int shadoff = 4; //shadow offset TODO - factor of coinR
    final double gravity = 0, friction = 0.07;
    final float coinRatio = 0.33f; // bed to radius friction (0.33 is 2 thirds,
    final float bedSpace=0.8f; // NB: includes end space and free bed before first line.
    int beds=9, maxCoins=5, bedScore=3;
    int coinsLeft = 0, winner = -1;
    int playerNum = 0;
    int currSpeed=0;
    Player[] player = new Player[2];
    boolean sounds=true, bounds=true, rebounds=true, highlight=true;

    String dynamicInstructions ="";

    MainActivity parent;
    private final GestureDetectorCompat gdc;

    static Paint linepaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    static Paint outlinepaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    static Paint shadowpaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint bmppaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static Bitmap rawbmp, bmp; // bitmap for the coin
    Matrix matrix = new Matrix(); //matrix for bitmap


    /*VARIABLES*/


    // this is the constructor - it is called when an instance of this class is created
    public ShoveDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //if (!isInEditMode()) ;
        parent = (MainActivity) this.getContext(); //TODO - remove all references to parent to improve editor preview
        gdc = new GestureDetectorCompat(parent, new MyGestureListener()); // create the gesture detector
       // for (int f = 0; f <= score.length; f++) score[0][f][0] = score[0][f][1] = score[1][f][0] = score[1][f][1] = 0; /* set scores to zero */
    }


    // this happens if the screen size changes - including the first time - it is measured. Here is where we get width and height
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        screenW = w;
        screenH = h;
        sidebar=screenW/10;
        float strokeSize = (w/280);  // TODO this is driven by width so set in onSizeChanged
        shadowpaint.setARGB(64,0,0,0);
        linepaint.setColor(Color.parseColor("#CD7F32"));
        linepaint.setStyle(Paint.Style.STROKE);
        linepaint.setStrokeWidth(strokeSize);
        linepaint.setTextSize(50);
        linepaint.setDither(true);                    // set the dither to true
        linepaint.setStyle(Paint.Style.STROKE);       // set to STROKE
        // linepaint.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
        linepaint.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
        // linepaint.setPathEffect(new CornerPathEffect(10) );   // set the path effect when they join.
        linepaint.setAntiAlias(true);
        outlinepaint.set(linepaint);
        outlinepaint.setColor(Color.parseColor("#FFFFFF"));

        player[0]=new Player();
        player[1]=new Player();

        try {
            loadPrefs();
        } catch (Exception e){
            Log.e("LOADING PREFS",  e.getMessage());
        }

        collider = new CollisionManager(w, h, friction, gravity);


        try {
            restoreData();
        } catch (Exception ex){ //could be FileNotFoundException, IOException, ClassNotFoundException
            Log.e("deserialise",ex.toString());
        }


        //TODO - if beds changed, then stored object radius may be inaccurate
        //TODO if #beds changed (ie. in prefs) and mismatch with saved data, reset score data




    }

    /* BIT FOR TOUCHING! */
    @Override
    public boolean onTouchEvent(@NonNull final MotionEvent e) {
        return gdc.onTouchEvent(e);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";
        private final int SWIPE_MIN_DISTANCE = 120;
        private final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onDown(MotionEvent e) {
            //add a state tracker - could also require hit on a coin at start
            if (inPlay.state == 1 && e.getY()> startZone) {
                inPlay.xSpeed = inPlay.ySpeed = 0;
                inPlay.x = e.getX();
                inPlay.y = e.getY();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // effectively a Drag detector - although would need to check that we have hit a ball first in onDown
            if (inPlay.state == 1) {
                if (e2.getY() > startZone) {
                    inPlay.x = e2.getX();
                    inPlay.y = e2.getY();
                    inPlay.xSpeed = inPlay.ySpeed = 0;
                } else {
                    // gets a speed on drag
                    inPlay.xSpeed = (e2.getX()-e1.getX())/10;
                    inPlay.ySpeed = (e2.getY()-e1.getY())/10;
                }
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (inPlay.state == 1 && e2.getY()>startZone) {
                inPlay.xSpeed = velocityX / 25;
                inPlay.ySpeed = velocityY / 25;
                inPlay.rSpeed = Math.random()*20-10;
            }
            return true;
        }
    }



    @Override
    // this is the method called when the view needs to be redrawn
    // try to minimise the amount of work this needs to do.
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);


        /* Scores Text */
        if (coinsLeft >1)
            parent.HighScoreText.setText(player[playerNum].name + " - " + coinsLeft+" coins");
        else
            parent.HighScoreText.setText(player[playerNum].name + " - Last coin"); //TODO const etc

        //Portsmouth scores
        // parent.TimeLeftText.setText("P1: "+ score[0][0]);
        // parent.ScoreText.setText("P2: "+ score[1][0]);

        // beds scored so far counting
        // parent.TimeLeftText.setText("P1: "+ score[0][beds+1]);
        // parent.ScoreText.setText("P2: "+ score[1][beds+1]);



        //Draw the sidelines and Beds (NB: the screen has a 2bed endzone, 9 full beds, a 1 bed exclusion and 3 bed fling zone)
        for (int f = 0; f <= beds; f++) {
            canvas.drawLine(0, f * bedH + 2 * bedH, screenW, f * bedH + 2 * bedH, linepaint); //draw each horizontal
            //canvas.drawText("" + player[playerNum].aim[beds-f][0]+":" + player[playerNum].aim[beds-f][1], 20, f * bedH + 2.6f * bedH, linepaint); // display the aim

            if (f < beds) {
                canvas.drawText("" + (beds - f), screenW / 2, f * bedH + 2.6f * bedH, linepaint); // display the bed number

                // draw the scores in the beds
                drawScore(canvas, player[0].score[beds - f], 0, f * bedH + 2 * bedH);
                drawScore(canvas, player[1].score[beds - f], screenW - sidebar, f * bedH + 2 * bedH);
            }
        }
        //canvas.drawText("" + player[playerNum].aim[beds+1][0]+":" + player[playerNum].aim[beds+1][1], 20, 1.6f * bedH, linepaint); // display the aim


        //draw the 2 sidebars
        canvas.drawLine(sidebar, 0, sidebar, screenH, linepaint);
        canvas.drawLine(screenW - sidebar, 0, screenW - sidebar, screenH, linepaint);



        for (MoveObj obj : objs) {
            canvas.drawCircle((float) obj.x+shadoff, (float) obj.y+shadoff, coinR, shadowpaint); //draw the shadow
            matrix.reset();
            matrix.postTranslate(-coinR, -coinR);
            matrix.postRotate(obj.angle);
            matrix.postTranslate((float) obj.x, (float) obj.y);
            canvas.drawBitmap(bmp, matrix, bmppaint);

            if (highlight) {
                if (obj.state < 0) obj.draw(canvas); //TODO - move bitmap and matrix into moveObj OUTLINE IF VOIDED
                else
                    //if the coin is within a bed, highlight it. TODO - add a temporary score to the player, or only when motion stopped
                    if (between(getBed((int) obj.y), 1, beds))
                        canvas.drawCircle((float) obj.x, (float) obj.y, coinR, outlinepaint);
            }

        }
    }


    // this is the method called when recalculating positions
    public void update() {


        // handle all the collisions starting at earliest time - if 2nd obj is null, then a wall collision
        // Collision manager also moves the objects.
        collider.collide(objs);
        for (CollisionManager.CollisionRec coll : collider.collisions) {
            //TODO set pitch based on size of the objects.
            float volume = Math.min((float) coll.impactV / 100, 1); //set the volume based on impact speed
            if (coll.objb == null) {  //if a wall collision
                // if a wall collision, play sound and may void the coin
                if (sounds) parent.player.play(parent.clunkSound, volume, volume, 2, 0, 1);
                if (bounds) coll.obja.state=-1; // if boundary rules apply, set to void
            } else {
                if (sounds) parent.player.play(parent.clinkSound, volume, volume, 2, 0, 1);
            }
        }



        // Once the collisions have been handled, draw each object and apply friction
        boolean motion = false;
        for (MoveObj obj : objs) {

            //if outside the sidebars and boundary rules are on, then void the coin if already in playzone
            if (bounds && obj.state==0 && (obj.x-coinR<sidebar || obj.x+coinR>screenW-sidebar)) obj.state=-1;

            if (obj.xSpeed != 0 || obj.ySpeed != 0) {
                motion = true;
                // if there is a streamID then adjust volume else start movement sound
                float volume = Math.min( (float)Math.sqrt(obj.xSpeed*obj.xSpeed+obj.ySpeed*obj.ySpeed) / 50, 1); //set the volume based on impact speed TODO const or calc

                if (obj.movingStreamID >0) {
                    parent.player.setVolume(obj.movingStreamID,volume,volume);
                    //adjust volume
                } else {
                    if (sounds) obj.movingStreamID = parent.player.play(parent.slideSound, volume, volume, 1, -1, 1);
                }
            }
            else{
                //stop any playing sound
                if (obj.movingStreamID >0) {
                    parent.player.stop(obj.movingStreamID);
                    obj.movingStreamID=0;
                }
            }
        }

        // if AI, and there is a ball available, then fire it.
        // TODO - could place coin away from scoring coins
        // TODO - could act tactically - aim for furthest beds with lowest score.

        if (inPlay != null && inPlay.state == 1 && !motion && player[playerNum].AI) {
            motion=true;
            inPlay.state = 0;
            int Smin=player[playerNum].aim[0][0];
            int Smax=player[playerNum].aim[beds+1][1];
            int Soffset = (Smax-Smin)/player[playerNum].accuracy; // how much to potentially miss by percentage of range

            // Which bed to aim for? if I know how to hit the bed? the lowest potential score? furthest away? pick one at random?
            int lowestPotential = 2;
            int target = 0;
            for (int f = 1; f <= beds; f++) {
                player[playerNum].aim[f][0]=Math.max(player[playerNum].aim[f-1][0],player[playerNum].aim[f][0]); // cascade mins up
                player[playerNum].aim[f][1]=Math.min(player[playerNum].aim[f+1][1],player[playerNum].aim[f][1]); // cascade max down (happens over several iterations)
                if (potential(player[playerNum].score[f])<=lowestPotential) {
                    lowestPotential=potential(player[playerNum].score[f]);
                    target=f;
                }
            }

            Smin=player[playerNum].aim[target][0];
            Smax=player[playerNum].aim[target][1];

            // Built in inaccuracy as %age of min and max
            currSpeed = Math.min(player[playerNum].aim[beds+1][1],Math.max(player[playerNum].aim[0][0],(int) (Math.random()*(Smax-Smin+2*Soffset)+Smin-Soffset)));
            inPlay.xSpeed = Math.random()*screenW/100-screenW/200;
            inPlay.ySpeed = -currSpeed; //TODO - check if screen size impacts this, or friction
            inPlay.rSpeed = Math.random()*20-10; //randomise rotational
           // DEBUG Toast.makeText(getContext(), "min " + Smin+" max "+Smax + " = "+currSpeed, Toast.LENGTH_LONG).show();
        }


        // if ball in play exits the start zone, then it cannot be touched
        if (inPlay != null && inPlay.state == 1 && inPlay.y < startZone) inPlay.state = 0;

        // if motion stops, and still behind start zone,
        if (inPlay != null && !motion && inPlay.state != 1 && inPlay.y>startZone) { // not even reached first line
            inPlay.state=1; //put back into flick state
            inPlay.y=screenH - bedH; //move back to start
            // if no collisions and this is an AI, if this is a faster minimum speed, update minimum speed.
            if (player[playerNum].AI && !inPlay.hitObj && !inPlay.hitTop ) player[playerNum].aim[0][0] = Math.max(player[playerNum].aim[0][0],currSpeed);
        }

        //if there is a ball in play and all motion stops, calc intermediate or final scores and play new ball if final
        if (inPlay != null && !motion && inPlay.state != 1) {
            coinsLeft--;

            // if an AI player, get bed of current coin and set speed for a score or over and under-run
            if (player[playerNum].AI){

                float bedZone = nearestBed((int) inPlay.y);
                int bedZoneInt = (int)bedZone;
                if (bedZoneInt > beds || inPlay.hitTop) {
                    player[playerNum].aim[beds+1][1] = Math.min(player[playerNum].aim[beds+1][1],currSpeed);
                }
                else if (!inPlay.hitObj && bedZoneInt > 0) { // if no collision and within beds then consider speed as a guide

                    // if at lower end or higher end of bed, set the min or max for the bed (and potentially the board)
                    // DEBUG Toast.makeText(getContext(), "bedzoneInt is "+bedZoneInt+" bedzone is "+bedZone+" diff is "+(bedZone - bedZoneInt), Toast.LENGTH_LONG).show();

                    if ((float)(bedZone - bedZoneInt) < 0.5f) {
                        player[playerNum].aim[bedZoneInt][0] = Math.max(player[playerNum].aim[bedZoneInt][0], currSpeed);
                        player[playerNum].aim[bedZoneInt-1][1] = Math.min(player[playerNum].aim[bedZoneInt-1][1], currSpeed);
                    } else {
                        player[playerNum].aim[bedZoneInt][1] = Math.min(player[playerNum].aim[bedZoneInt][1], currSpeed);
                        player[playerNum].aim[bedZoneInt + 1][0] = Math.max(player[playerNum].aim[bedZoneInt + 1][0], currSpeed);
                    }
                }
            }


            //for each coin, determine new potential score - first set potentials to zero
            for (int f = 0; f < player[0].score.length; f++) {
                player[0].score[f][1] = 0;
                player[1].score[f][1] = 0; /* set potential scores to zero TODO - const would be clearer */
            }

            for (MoveObj obj : objs) {
                obj.hitSide=false; //TODO take the opportunity to clear hit states
                obj.hitTop=false;
                obj.hitObj=false;
                //if the coin is within a bed, score it

                int bed = getBed((int) obj.y);
                if (between(bed, 1, beds) && obj.state >= 0) { //don't include coin if voided. (ie. state is -1
                    player[playerNum].score[0][1] += bed; //add the score onto player potential total // used in portsmouth rules
                    // if already three, increment opponent up to three, else increment player up to three
                    if (!addPoint(playerNum, bed, true)) addPoint(1 - playerNum, bed, false);
                    if (potential(player[playerNum].score[beds + 1]) >= beds) winner = playerNum;
                    //THIS IS A WIN !!!!! reset all could even break the for loop!
                    // TODO would be good to have a "scoring" state where the coins and scores are animated
                }
            }

            if (coinsLeft <= 0) {
                // all coins used up - so end of turn - although in progressive, some could be returned
                // TODO - if progressive (Oxford) then return some coins

                // potential scores become real scores.
                for (int f = 0; f < player[0].score.length; f++) {
                    player[0].score[f][0] = potential(player[0].score[f]);
                    player[1].score[f][0] = potential(player[1].score[f]);
                    player[0].score[f][1] = 0;
                    player[1].score[f][1] = 0; /* set potential scores to zero TODO - const would be clearer */
                }

                playerNum = 1 - playerNum;
                coinsLeft = maxCoins;
                objs.clear();
            }


            if (winner >= 0) {
                Toast.makeText(getContext(), "Won by " + player[winner].name, Toast.LENGTH_LONG).show();

                /* set scores to zero */
                for (int f = 0; f < player[0].score.length; f++) {
                    player[0].score[f][0] = 0;
                    player[0].score[f][1] = 0;
                    player[1].score[f][0] = 0;
                    player[1].score[f][1] = 0;
                }
                playerNum = 0;
                winner = -1;
                coinsLeft = maxCoins;
                objs.clear();
            }

            inPlay = null;
        }

        if (inPlay == null) {
            // add a new coin - this could be first coin
            // TODO in combat mode we alternate playerNum
            inPlay = new MoveObj(11 + playerNum, coinR, screenW / 2, screenH - bedH, Math.random()*screenW/25-screenW/50, 0);
            inPlay.wallBounce=rebounds; //enable or disable wall bounce TODO - move into constructor
            objs.add(inPlay);
            if (sounds) parent.player.play(parent.placeSound,1,1,1,0,1);
        }

    }

    private void drawScore(Canvas c, int[] score, float x, float y){

        // draw lines for the points with a horizontal when bed is filled
        // should perhaps be off the canvas and use other views to handle this.

        int div = sidebar/bedScore; // this splits the verts EFF TODO precalc this plus the 0.9f and 1.1f multiple
        // draw real scores first
        for (int i = 1; i <= score[0]; i++) {
            if (i == bedScore) //if this is a closed bed then use horizontal
                c.drawLine(x + sidebar*0.15f, y + bedH * 0.45f, x+sidebar*0.85f, y + bedH * 0.55f, outlinepaint);
            else
                c.drawLine(x + i * div*1.1f, y + bedH * 0.2f, x + i * div * 0.9f, y + bedH * 0.8f, outlinepaint);
        }

        // then, add on potential score
        for (int i = score[0]+1; i <= potential(score); i++) {
            if (i == bedScore) //if this is a closed bed then use horizontal
                c.drawLine(x + sidebar*0.15f, y + bedH * 0.45f, x+sidebar*0.85f, y + bedH * 0.55f, linepaint);
            else
                c.drawLine(x + i * div*01.1f, y + bedH * 0.2f, x + i * div * 0.9f, y + bedH * 0.8f, linepaint);
        }

    }

    private int getBed(int pos){
        if (pos>startZone) return -1; // not even reached first line TODO - this only looks at midpoint, should +coinR
        if ((pos+coinR)< 2*bedH) return 99; // in the endzone
        if ((pos-coinR)%bedH>coinR) return 0; //overlapping. so no score
        return (beds+2-((pos-coinR)/bedH));
    }

    private float nearestBed(int pos) {
        if (pos - coinR > startZone) return -1; // not even reached first line
        if ((pos + coinR) < 2 * bedH) return 99; // in the endzone
        return (beds+3 - ((float) pos / bedH));
    }

    private boolean addPoint(int playerN, int bed, boolean scorer){
        // if the bed is full cannot add so return false - point may go to opponent
        if (potential(player[playerN].score[bed]) == bedScore) return false;

        // if the bed will not get filled then add to potential score
        if (potential(player[playerN].score[bed]) < bedScore-1){
            player[playerN].score[bed][1]++;
            return true;
        }

        // remaining case is that a bed will get filled. This cannot be allowed if a non-scorer would win.
        if (!scorer && potential(player[playerN].score[beds + 1]) >= beds - 1) return false;

        player[playerN].score[bed][1]++;
        player[playerN].score[beds + 1][1]++;
        return true;
    }

    private int potential(int[] points){
        return points[0]+points[1];
    }

    private boolean between(int val, int min, int max){
        return (val>=min && val<=max);
    }




    public void saveData() throws IOException {
        File file = new File(getContext().getCacheDir(), getContext().getString(R.string.File_Name_Objects));
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
        os.writeObject(objs);
        os.close();
        file = new File(getContext().getCacheDir(), getContext().getString(R.string.File_Name_Players)); //TODO change to Player
        os = new ObjectOutputStream(new FileOutputStream(file));
        os.writeObject(player);
        os.close();
    }

    public void restoreData() throws IOException,ClassNotFoundException {
        coinsLeft = maxCoins; // this only gets used if no restore. NB: will get reduced by one if new game
        File file = new File(getContext().getCacheDir(), getContext().getString(R.string.File_Name_Objects));
        ObjectInputStream is = new ObjectInputStream(new FileInputStream(file));
        objs = (ArrayList) is.readObject();
        int lastCoinPlayed = objs.size()-1;
        if (lastCoinPlayed >= 0) {
            inPlay = objs.get(lastCoinPlayed);
            playerNum=inPlay.type-11;
        }
        coinsLeft=maxCoins-lastCoinPlayed;
        file = new File(getContext().getCacheDir(), getContext().getString(R.string.File_Name_Players)); //TODO change to Player
        is = new ObjectInputStream(new FileInputStream(file));
        player = (Player[]) is.readObject();
        Toast.makeText(getContext(), "Loading Player Data", Toast.LENGTH_LONG).show();
        //TODO New game wipes the AI aim data

    }

    // TODO - pull in the cache here - rather than the onStart()
    // This is called onResume - so after any view of settings or after a pause etc.
    public void loadPrefs() throws ClassCastException  {
        //Load lists from file or set defaults TODO set defaults as consts
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        // store names and AI settings from prefs
        player[0].name=preferences.getString("pref_player1", "Player 1");
        player[0].accuracy=Integer.parseInt(preferences.getString("pref_player1AI", "0"));
        player[0].AI=player[0].accuracy>0; //TODO should be in setter
        player[1].name=preferences.getString("pref_player2", "Player 2");
        player[1].accuracy=Integer.parseInt(preferences.getString("pref_player2AI", "0"));
        player[1].AI=player[1].accuracy>0; //TODO should be in setter

        sounds = preferences.getBoolean("pref_sounds", true);
        bounds = preferences.getBoolean("pref_bounds", true);
        rebounds = preferences.getBoolean("pref_rebounds", true);
        highlight = preferences.getBoolean("pref_highlight", true);


        maxCoins = Integer.parseInt(preferences.getString("pref_maxcoins", "5"));
        bedScore = Integer.parseInt(preferences.getString("pref_bedscore", "3"));
        beds = Integer.parseInt(preferences.getString("pref_beds", "9"));

        //TODO should check to see if beds or bedScore previously set and have now changed. These changes should really trigger a restart
        //Number of beds then affects bed size, coin size etc.

        bedH = Math.round(screenH * bedSpace / (beds + 3)); //2 extra beds for end and free space after flickzone
        coinR=Math.round(bedH * coinRatio);
        startZone=(beds+2)*bedH+coinR;

        rawbmp = BitmapFactory.decodeResource(getResources(), R.drawable.coin67);
        bmp = Bitmap.createScaledBitmap(rawbmp, coinR * 2, coinR * 2, true);
        bmppaint.setFilterBitmap(true);

        //TODO if #beds changed (ie. in prefs) and mismatch with saved data, reset score data
        //TODO move to strings
        dynamicInstructions = String.format(getContext().getString(R.string.str_instructions), beds, maxCoins, bedScore, rebounds?"bounce":"fall", bounds?"not be":"be");

        //Also if bedScore changes then scoring might fail - best to restart in these cases - or all cases?
        //TODO does exit reset lose all calculated zones? - should save these
        if (player[0].score.length!=beds+2) {
            player[0]=new Player(player[0].name, player[0].accuracy);
            player[1]=new Player(player[1].name, player[1].accuracy);
        }

    }


    public class Player implements Serializable{
        public String name = "Player";
        public boolean AI = false;
        public int accuracy = 0; //This is percentage offset each direction 10 is fairly inaccurate 30 is pretty good 100 is sharp
        public int[][] aim = new int[beds+2][2]; //[bed - bed zero is the fling zone, bed+1 is the outzone][min|max]
        public int[][] score = new int[beds+2][2]; //[bed - bed zero is for point score and final bed is for tracking completed][actual|potential]

        public Player() {
            for (int i=0; i<aim.length; i++){
                aim[i][0]=0;
                aim[i][1]=screenH/10; //TODO factor should be affected by friction
            }
            //Toast.makeText(getContext(), "Taking Aim "+aim.length+" "+beds+" "+screenH/10, Toast.LENGTH_LONG).show();
        }

        public Player(String name, int accuracy) {
            this();
            this.name = name;
            this.AI = accuracy>0;
            this.accuracy = accuracy;
        }
    }
}


