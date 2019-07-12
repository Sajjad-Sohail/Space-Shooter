package com.ub2017.carlos.spaceshootersample;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.BitmapFactory;


import java.io.IOException;
import java.security.cert.PolicyNode;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by carlos on 10/13/2016.
 */

public class TheGameLevel2 extends Activity implements View.OnTouchListener
{

    TheView mySurfaceView;   //the surface view, where we draw
    SpriteHelper allsprites;  //class that has all the locations and sizes of the images in the sprite
    float xTouch,yTouch;     //the location when the screen is touched
    int s_width,s_height,x_height;    //the size of the surface view
    //used for which sprite to use
    int shipLoc,shipExplode,meteor1Loc,meteor2Loc,meteor3Loc,boomLoc;
    boolean hit = false; boolean meteor1ChangeLoc= false;boolean meteor2ChangeLoc = false;boolean meteor3ChangeLoc = false;
    boolean meteor1HitsNoLocChange = false;boolean meteor2HitsNoLocChange = false;boolean meteor3HitsNoLocChange = false;
    Random r = new Random();
    Point meteor1,meteor2,meteor3, boom,shipBullet; int meteor1Hits=0;int meteor2Hits=0;int meteor3Hits=0;
    long enemyMoveTime = 0; int meteorsHits=0;
    TextView textView;
    //15 frames per seconds
    float skipTime =1000.0f/30.0f; //setting 30fps
    long lastUpdate;
    float dt;
    long currentTime;
    int minutes;
    int seconds;
    int countBoom;
    int spawnMeteor1;int spawnMeteor2;int spawnMeteor3; boolean firstTimeSpawn = true;
    MediaPlayer mp;
    boolean firstTimeBullet = true;
    SoundPool sound_pool;
    SparseIntArray soundMap;

    void setManually()
    {
        mp = new MediaPlayer();
        mp.reset();//reset it to be reused (cannot be reset if mp done with above method
        //set the source ( the song)
        //Uniform Resource Identifier is used to "identify a resource"
        Uri theUri = Uri.parse("android.resource://"+
                TheGameLevel2.this.getPackageName()+
                "/"+R.raw.mainsong);
        try {
            mp.setDataSource(TheGameLevel2.this ,theUri);
            //set it to be ready for playback, this one blocks until is done(not recommended for streams or large files)
            //mp.prepare();
            mp.prepareAsync();//doesnt block, mainly for streams
            mp.setOnPreparedListener(okStart);//invokes when is ready to start,
            mp.setOnCompletionListener(mainDone);//invokes when is reaches the end
            //calling play when is not done can cause the app to crash
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    //Start Media Player Listener
    private MediaPlayer.OnPreparedListener okStart = new MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();

        }
    };
    //Stop Media Player Listener
    private MediaPlayer.OnCompletionListener mainDone = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            mp.stop();
        }
    };
    void setPool_API_20andDown()
    {

        sound_pool = new SoundPool(4, AudioManager.STREAM_MUSIC,0);//0 is audio quality, not used (check google!)
        //https://developer.android.com/reference/android/media/SoundPool.html
        //create HashMap of sounds and pre-load sounds
        HashMap<Integer,Integer> soundMap = new HashMap<Integer, Integer>();
        soundMap.put(1, sound_pool.load(TheGameLevel2.this, R.raw.enemyboom, 1));
        soundMap.put(2,sound_pool.load(TheGameLevel2.this, R.raw.gameover, 1));
        soundMap.put(3,sound_pool.load(TheGameLevel2.this, R.raw.meteorboom, 1));
        soundMap.put(4, sound_pool.load(TheGameLevel2.this, R.raw.shipboom, 1));
        soundMap.put(5,sound_pool.load(TheGameLevel2.this, R.raw.shipbullet, 1));
        soundMap.put(6, sound_pool.load(TheGameLevel2.this, R.raw.success, 1));


    }


    protected void onCreate(Bundle savedInstanceState) {
        Log.d("OnCreate","inside oncreate method");
        super.onCreate(savedInstanceState);
        setManually();

        //use a pool if there is multiple smalls sounds to be played
        setPool_API_20andDown();
        //setContentView(R.layout.spritesheetbetter); //not setting a static xml
        //set all the sprite locations and sizes
        allsprites = new SpriteHelper(getResources());//send the resources (only View has it)
        //make sure there is only ONE copy of the image and that the image
        //is in the drawable-nodpi. if it is not unwanted scaling might occur
        shipLoc=shipExplode=meteor1Loc=meteor2Loc=meteor3Loc = 0;

        meteor1 = new Point();
        meteor2 = new Point();
        meteor3 = new Point();
        boom = new Point();
        shipBullet = new Point();
        lastUpdate = 0;         //to check against now time

        //hide the actoinbar and make it fullscreen
        hideAndFull();
        //our custom view
        mySurfaceView = new TheView(this);
        mySurfaceView.setOnTouchListener(this); //now we can touch the screen
        setContentView(mySurfaceView);

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                //not passing an xml, using the surfaceview as the layout
                //custom way of setting the size of the surfaceview
                mySurfaceView.startGame();
            }
        },2000);


    }

    public void hideAndFull()
    {

        Log.d("hideAndFull","inside hideAndFull method");
        ActionBar bar = getActionBar();
        bar.hide();
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        Log.d("onTouch","inside onTouch method");
        switch(motionEvent.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                xTouch = motionEvent.getX();
                yTouch = motionEvent.getY();
                break;
            case MotionEvent.ACTION_UP:
                xTouch = motionEvent.getX();
                yTouch = motionEvent.getY();
                view.performClick();//to get rid of the message, mimicking a click
                break;
            case MotionEvent.ACTION_MOVE:
                xTouch = motionEvent.getX();
                yTouch = motionEvent.getY();
                break;
        }
        return true;
    }

    //surface view used so we can draw is dedicated made for drawing
    //View is updated in main thread while SurfaceView is updated in another thread.
    public class TheView extends SurfaceView implements SurfaceHolder.Callback {
        //resize and edit pixels in a surface. Holds the display
        SurfaceHolder holder;
        Boolean change = true;
        Thread gameThread;
        Canvas c/* = new Canvas((BitmapFactory.decodeResource(getResources(),R.drawable.bkg1)).copy(Bitmap.Config.ARGB_8888, true))*/;
        Rect place1;

        public TheView(Context context) {

            super(context);

            Log.d("TheView","inside TheView constructor");
            //get this holder
            holder = getHolder();//gets the surfaceview surface
            holder.addCallback(this);
            gameThread = new Thread(runn);
        }

        Runnable runn = new Runnable() {
            @Override
            public void run() {

                while (change == true) {
                    Log.d("while","inside while loop");
                    //perform drawing, does it have a surface?
                    if (!holder.getSurface().isValid()) {
                        Log.d("while if","inside if surface isvalid");
                        continue;
                    }

                    dt = System.currentTimeMillis() - lastUpdate;

                    if (dt >= skipTime) {
                        //Lock the canvas to paint
                        c = holder.lockCanvas();

                        Bitmap mScaledBackground = BitmapFactory.decodeResource(getResources(),R.drawable.bkg2);
                        Rect cut = new Rect(0,0,mScaledBackground.getWidth(),mScaledBackground.getHeight());
                        Rect place = new Rect(0,0,s_width, s_height);
                        c.drawBitmap(mScaledBackground,cut,place,null);
                        Paint paint = new Paint();
                        paint.setColor(Color.WHITE);
                        paint.setTextSize(50);
                        long millis = System.currentTimeMillis() - currentTime;
                        seconds = (int) (millis / 1000);
                        minutes = seconds / 60;
                        seconds = seconds % 60;
                        c.drawText(String.format("%d:%02d", minutes, seconds), s_width-150, 65, paint);

                        if(hit)
                        {
                            sound_pool.play(4,  5, 5, 1 , 0, 1);
                            if(countBoom==0){
                                Rect enemy1Boom = new Rect(0+15,(int) yTouch+15,(int)(s_width*.1),(int)yTouch + allsprites.shipHHeight);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==1){
                                Rect enemy1Boom = new Rect(0+15,(int) yTouch+15,(int)(s_width*.1),(int)yTouch + allsprites.shipHHeight);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==2){
                                Rect enemy1Boom = new Rect(0+15,(int) yTouch+15,(int)(s_width*.1),(int)yTouch + allsprites.shipHHeight);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==3){
                                Rect enemy1Boom = new Rect(0+15,(int) yTouch+15,(int)(s_width*.1),(int)yTouch + allsprites.shipHHeight);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==4){
                                Rect enemy1Boom = new Rect(0+15,(int) yTouch+15,(int)(s_width*.1),(int)yTouch + allsprites.shipHHeight);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==5){
                                Rect enemy1Boom = new Rect(0+15,(int) yTouch+15,(int)(s_width*.1),(int)yTouch + allsprites.shipHHeight);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                                changeMeteor1Location();
                                hit = false;
                                countBoom=0;
                                mp.pause();
                                mp.seekTo(0);
                                mp.stop();
                                sound_pool.play(2,  5, 5, 1 , 0, 1);

                                TheGameLevel2.this.runOnUiThread(new Runnable()
                                {
                                    public void run()
                                    {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setMessage("Sorry. You lost the game!!!")
                                                .setCancelable(false)
                                                .setTitle("Lost")
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        //intent
                                                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                                                        startActivity(intent);
                                                    }
                                                });
                                        AlertDialog alert = builder.create();
                                        alert.show();
                                        change = false;
                                    }
                                });
                         }
                            countBoom++;
                        }
                        else{

                            if(firstTimeBullet){
                                sound_pool.play(5,  1, 1, 1 , 0, 1);
                                firstTimeBullet = false;

                            }
                            place1 =  new Rect(0,(int) yTouch,(int)(s_width*.1),(int)yTouch + allsprites.shipHHeight);
                            c.drawBitmap(allsprites.space, allsprites.shipHSprites[shipLoc], place1, null);
                        }

                        //ship bullet
                        Rect place2 = new Rect(shipBullet.x,shipBullet.y,shipBullet.x+allsprites.shipBulletSize,shipBullet.y+allsprites.shipBulletSize);
                        c.drawBitmap(allsprites.space,allsprites.shipBulletSprite,place2,null);

                        //enemy 1
                        if (meteor1ChangeLoc) {
                            sound_pool.play(1,  5, 5, 1 , 0, 1);

                            if(countBoom==0){
                                Rect enemy1Boom = new Rect(meteor1.x+30,meteor1.y,(int)meteor1.x+(int)allsprites.meteorSize,(int)meteor1.y+(int)allsprites.meteorSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==1){
                                Rect enemy1Boom = new Rect(meteor1.x+30,meteor1.y,(int)meteor1.x+(int)allsprites.meteorSize,(int)meteor1.y+(int)allsprites.meteorSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }if(countBoom==2){
                                Rect enemy1Boom = new Rect(meteor1.x+30,meteor1.y,(int)meteor1.x+(int)allsprites.meteorSize,(int)meteor1.y+(int)allsprites.meteorSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }if(countBoom==3){
                                Rect enemy1Boom = new Rect(meteor1.x+30,meteor1.y,(int)meteor1.x+(int)allsprites.meteorSize,(int)meteor1.y+(int)allsprites.meteorSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }if(countBoom==4){
                                Rect enemy1Boom = new Rect(meteor1.x+30,meteor1.y,(int)meteor1.x+(int)allsprites.meteorSize,(int)meteor1.y+(int)allsprites.meteorSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }if(countBoom==5){
                                Rect enemy1Boom = new Rect(meteor1.x+30,meteor1.y,(int)meteor1.x+(int)allsprites.meteorSize,(int)meteor1.y+(int)allsprites.meteorSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                                meteor1ChangeLoc = false;
                                changeMeteor1Location();
                                countBoom=0;
                            }
                            countBoom++;

                        }

                        else
                        {
                           Rect place3 = new Rect(meteor1.x,meteor1.y,meteor1.x+(int)allsprites.meteorSize,(int)meteor1.y+(int)allsprites.meteorSize);
                           c.drawBitmap(allsprites.space,allsprites.meteorsprites[meteor1Loc],place3,null);
                        }

                        //enemy 2
                        if (meteor2ChangeLoc)
                        {
                            sound_pool.play(1,  5, 5, 1 , 0, 1);

                            if(countBoom==0){
                                Rect enemy2Boom = new Rect(meteor2.x+30,meteor2.y,(int)meteor2.x+(int)allsprites.meteorSize,(int)meteor2.y+(int)allsprites.meteorSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                            }
                            if(countBoom==1){
                                Rect enemy2Boom = new Rect(meteor2.x+30,meteor2.y,(int)meteor2.x+(int)allsprites.meteorSize,(int)meteor2.y+(int)allsprites.meteorSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                            }if(countBoom==2){
                            Rect enemy2Boom = new Rect(meteor2.x+30,meteor2.y,(int)meteor2.x+(int)allsprites.meteorSize,(int)meteor2.y+(int)allsprites.meteorSize);
                            c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                        }if(countBoom==3){
                            Rect enemy2Boom = new Rect(meteor2.x+30,meteor2.y,(int)meteor2.x+(int)allsprites.meteorSize,(int)meteor2.y+(int)allsprites.meteorSize);
                            c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                        }if(countBoom==4){
                            Rect enemy2Boom = new Rect(meteor2.x+30,meteor2.y,(int)meteor2.x+(int)allsprites.meteorSize,(int)meteor2.y+(int)allsprites.meteorSize);
                            c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                        }if(countBoom==5){
                            Rect enemy2Boom = new Rect(meteor2.x+30,meteor2.y,(int)meteor2.x+(int)allsprites.meteorSize,(int)meteor2.y+(int)allsprites.meteorSize);
                            c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                            meteor2ChangeLoc = false;
                            changeMeteor2Location();
                            countBoom=0;
                        }
                            countBoom++;

                        }
                        else{

                        Rect place3 = new Rect(meteor2.x,meteor2.y,meteor2.x+(int)allsprites.meteorSize,(int)meteor2.y+(int)allsprites.meteorSize);
                        c.drawBitmap(allsprites.space,allsprites.meteorsprites[meteor2Loc],place3,null);
                        }

                        //meteor 3 location
                        if (meteor3ChangeLoc)
                        {

                            if(countBoom==0){
                                Rect enemy2Boom = new Rect(meteor3.x+30,meteor3.y,(int)meteor3.x+(int)allsprites.meteorSize,(int)meteor3.y+(int)allsprites.meteorSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                            }
                            if(countBoom==1){
                                Rect enemy2Boom = new Rect(meteor3.x+30,meteor3.y,(int)meteor3.x+(int)allsprites.meteorSize,(int)meteor3.y+(int)allsprites.meteorSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                            }if(countBoom==2){
                            Rect enemy2Boom = new Rect(meteor3.x+30,meteor3.y,(int)meteor3.x+(int)allsprites.meteorSize,(int)meteor3.y+(int)allsprites.meteorSize);
                            c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                        }if(countBoom==3){
                            Rect enemy2Boom = new Rect(meteor3.x+30,meteor3.y,(int)meteor3.x+(int)allsprites.meteorSize,(int)meteor3.y+(int)allsprites.meteorSize);
                            c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                        }if(countBoom==4){
                            Rect enemy2Boom = new Rect(meteor3.x+30,meteor3.y,(int)meteor3.x+(int)allsprites.meteorSize,(int)meteor3.y+(int)allsprites.meteorSize);
                            c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                        }if(countBoom==5){
                            Rect enemy2Boom = new Rect(meteor3.x+30,meteor3.y,(int)meteor3.x+(int)allsprites.meteorSize,(int)meteor3.y+(int)allsprites.meteorSize);
                            c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy2Boom,null);
                            meteor3ChangeLoc = false;
                            changeMeteor3Location();
                            countBoom=0;
                        }
                            countBoom++;

                        }
                        else{

                          Rect place3 = new Rect(meteor3.x,meteor3.y,meteor3.x+(int)allsprites.meteorSize,(int)meteor3.y+(int)allsprites.meteorSize);
                          c.drawBitmap(allsprites.space,allsprites.meteorsprites[meteor3Loc],place3,null);
                        }



                        holder.unlockCanvasAndPost(c);
                        //change values to draw the next sprite
                        shipLoc= (shipLoc+1)%4;
                        meteor1Loc = (meteor1Loc+1)%4;
                        meteor2Loc = (meteor2Loc+1)%4;
                        meteor3Loc = (meteor3Loc+1)%4;
                        boomLoc = (boomLoc+1)%6;

                        shipBullet.x+=s_width/30;
                        //move bullets
                        meteor1.x-=s_width/150;//change this to make it seem faster going down
                        if(firstTimeSpawn){
                            spawnMeteor1 = (int)yTouch;
                            spawnMeteor2 = (int)yTouch;
                            spawnMeteor3 = (int)yTouch;
                            firstTimeSpawn = false;
                        }
                        //do somthing with meteor1.y spawnX

                        if(spawnMeteor1>meteor1.y){
                            meteor1.y+=meteor1.y/165;
                         }
                        if(spawnMeteor1<meteor1.y){
                            meteor1.y-=meteor1.y/165;
                        }
                        x_height = s_height;
                        //move bullets
                        meteor2.x-=s_width/150;//change this to make it seem faster going down
                        if(spawnMeteor2>meteor2.y){
                            meteor2.y+=meteor2.y/165;
                        }
                        if(spawnMeteor2<meteor2.y){
                            meteor2.y-=meteor2.y/165;
                        }
                        meteor3.x-=s_width/150;//change this to make it seem faster going down
                        if(spawnMeteor3>meteor3.y){
                            meteor3.y+=meteor3.y/165;
                        }
                        if(spawnMeteor3<meteor3.y){
                            meteor3.y-=meteor3.y/165;
                        }
                        // shipBullet.y+=s_height/2;
                        //check if bullet hit enemy or meteors
                        checkHits();
                        //if enemies not hit then check bullets are out of screen
                        //check ship bullet
                        // add logic here
                        //check enemy bullet
                        if (meteor1.x <= 0) {
                            Log.d("if eneme1 ","inside enemy1bullet.y");

                            changeMeteor1Location();
                        }
                        if (meteor2.x <= 0) {
                            Log.d("if eneme1 ","inside enemy1bullet.y");

                            changeMeteor2Location();
                        }
                        if (meteor3.x <= 0) {
                            Log.d("if eneme1 ","inside enemy1bullet.y");

                            changeMeteor3Location();
                        }
                        if (shipBullet.x > s_width) {
                            Log.d("if ship bullet","inside shipbullet.y");

                            //reset bullets
                          resetShipBullets();
                        }

                        lastUpdate = System.currentTimeMillis();
                        Log.d("lastUpdate","TIME TIME TIME: lastUpdate: "+lastUpdate);

                    }
                }
            }
            int widthRatio(int n){
                return (int)(allsprites.shipVWidth/(allsprites.shipVHeight/s_height*.1));
            }
        };
        public void changeMeteor1Location()
        {
            spawnMeteor1 = (int)yTouch;
            int x1 = (int)s_width-(int)allsprites.meteorOriginalSize;
            int y1 = r.nextInt(8)+1;

            int xlocation1 = (int)(s_width*.90);
            int ylocation1 = (int)(s_height*(y1*.1));

            meteor1.x = xlocation1;
            meteor1.y = ylocation1;


        }
        public void changeMeteor2Location(){
            spawnMeteor2 = (int)yTouch;
            int x2 = (int)s_width-(int)allsprites.meteorOriginalSize;
            int y2 = r.nextInt(8)+1;

            int xlocation2 = (int)(s_width*.90);
            int ylocation2 = (int)(s_height*(y2*.1));

            meteor2.x = xlocation2;
            meteor2.y = ylocation2;

        }
        public void changeMeteor3Location(){
            spawnMeteor3 = (int)yTouch;
            int x3 = (int)s_width-(int)allsprites.meteorOriginalSize;
            int y3 = r.nextInt(8)+1;

            int xlocation3 = (int)(s_width*.90);
            int ylocation3 = (int)(s_height*(y3*.1));

            meteor3.x = xlocation3;
            meteor3.y = ylocation3;

        }
        public void placeEnemies(){



            int x1 = (int)s_width-(int)allsprites.meteorOriginalSize;
            int y1 = r.nextInt(s_height);

            int xlocation1 = (int)(s_width*.90);
            int ylocation1 = (int)(s_height*.10);

            meteor1.x = xlocation1;
            meteor1.y = ylocation1;

            int x2 = (int)s_width-(int)allsprites.meteorOriginalSize;
            int y2 = r.nextInt(s_height);

            int xlocation2 = (int)(s_width*.90);
            int ylocation2 = (int)(s_height*.40);

            meteor2.x = xlocation2;
            meteor2.y = ylocation2;

            int x3 = (int)s_width-(int)allsprites.meteorOriginalSize;
            int y3 = r.nextInt(s_height);

            int xlocation3 = (int)(s_width*.90);
            int ylocation3 = (int)(s_height*.70);

            meteor3.x = xlocation3;
            meteor3.y = ylocation3;

            //this will go up just like the enemy bullets go down
            int xlocation5 = (int)(allsprites.shipHWidth);//this values will change
            int ylocation5 = (int)(s_height*.5);
            shipBullet.x = xlocation5;
            shipBullet.y = ylocation5;

        }


        public void resetShipBullets()
        {
            sound_pool.play(5,  1, 1, 1 , 0, 1);
            shipBullet.x = (int)(allsprites.shipHOriginalWidth);
            shipBullet.y =  (int) yTouch+ (int)allsprites.shipHHeight/2 - allsprites.shipBulletSize/2;/*shipBullet.y+allsprites.shipHHeight*/

        }


        public void checkHits()
        {

            if   ( ((meteor1.y+allsprites.meteorSize>yTouch) && (meteor1.y+allsprites.meteorSize<yTouch+allsprites.shipHHeight)) ||
                    (  (meteor1.y<yTouch+allsprites.shipHHeight) && (meteor1.y>yTouch)) ||
                   ((meteor1.y<yTouch) && (meteor1.y+allsprites.meteorSize > yTouch+allsprites.shipHHeight)) )
                  if(meteor1.x <=allsprites.shipHOriginalWidth-50) // do not used yTouch since you can touch anywhere

                        {
                          hit=true;
                        }



            if   ( ((meteor2.y+allsprites.meteorSize>yTouch) && (meteor2.y+allsprites.meteorSize<yTouch+allsprites.shipHHeight)) ||
                    (  (meteor2.y<yTouch+allsprites.shipHHeight) && (meteor2.y>yTouch)) ||
                    ((meteor2.y<yTouch) && (meteor2.y+allsprites.meteorSize > yTouch+allsprites.shipHHeight)) )


                if(meteor2.x <=allsprites.shipHOriginalWidth) // do not used yTouch since you can touch anywhere
                       /* if(meteor1.x+allsprites.meteorOriginalSize> s_width)*///(yTouch+allsprites.shipVHeight))
                {
                    hit=true;
                }


            if   ( ((meteor3.y+allsprites.meteorSize>yTouch) && (meteor3.y+allsprites.meteorSize<yTouch+allsprites.shipHHeight)) ||
                    (  (meteor3.y<yTouch+allsprites.shipHHeight) && (meteor3.y>yTouch)) ||
                    ((meteor3.y<yTouch) && (meteor3.y+allsprites.meteorSize > yTouch+allsprites.shipHHeight)) )


                if(meteor3.x <=allsprites.shipHOriginalWidth) // do not used yTouch since you can touch anywhere
                       /* if(meteor1.x+allsprites.meteorOriginalSize> s_width)*///(yTouch+allsprites.shipVHeight))
                {
                    hit=true;
                }

            if (shipBullet.y+allsprites.shipBulletSize > meteor1.y)
                if(shipBullet.y < meteor1.y+allsprites.meteorSize) // do not used yTouch since you can touch anywhere
                    if(shipBullet.x+allsprites.bulletOriginalHeight > 0)//(yTouch+allsprites.shipVHeight))
                        if(shipBullet.x>meteor1.x)
                        {

                            if(meteorsHits == 10){
                                //show toast and move to next stage
                                mp.pause();
                                mp.seekTo(0);
                                mp.stop();
                                sound_pool.play(6,  5, 5, 1 , 0, 1);
                                TheGameLevel2.this.runOnUiThread(new Runnable()
                                {
                                    public void run()
                                    {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setMessage("Total time: "+minutes+":"+seconds)
                                                .setCancelable(false)
                                                .setTitle("Success")
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        //intent
                                                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                                                        startActivity(intent);
                                                    }
                                                });
                                        AlertDialog alert = builder.create();
                                        alert.show();
                                        change = false;
                                      }
                                });
                                meteorsHits = 0;

                            }
                            if(meteor1Hits==2){
                                meteor1ChangeLoc = true;
                                resetShipBullets();
                                meteorsHits++;
                                meteor1Hits=0;
                            }
                            else
                                {
                                    meteor1HitsNoLocChange = true;
                                    meteor1Hits++;
                                    resetShipBullets();
                            }

                        }


            if (shipBullet.y+allsprites.shipBulletSize > meteor2.y)
                if(shipBullet.y < meteor2.y+allsprites.meteorSize) // do not used yTouch since you can touch anywhere
                    if(shipBullet.x+allsprites.bulletOriginalHeight > 0)//(yTouch+allsprites.shipVHeight))
                        if(shipBullet.x>meteor2.x)
                        {

                            if(meteorsHits == 10){
                                //show toast and move to next stage
                                mp.pause();
                                mp.seekTo(0);
                                mp.stop();
                                sound_pool.play(6,  5, 5, 1 , 0, 1);
                                TheGameLevel2.this.runOnUiThread(new Runnable()
                                {
                                    public void run()
                                    {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setMessage("Total time: "+minutes+":"+seconds)
                                                .setCancelable(false)
                                                .setTitle("Success")
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        //intent
                                                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                                                        startActivity(intent);
                                                    }
                                                });
                                        AlertDialog alert = builder.create();
                                        alert.show();
                                        change = false;
                                    }
                                });
                                meteorsHits = 0;

                            }
                            if(meteor2Hits==2){
                                meteor2ChangeLoc = true;
                                resetShipBullets();
                                meteorsHits++;
                                meteor2Hits=0;
                            }
                            else
                            {
                                meteor2HitsNoLocChange = true;
                                meteor2Hits++;
                                resetShipBullets();
                            }


                        }

            if (shipBullet.y+allsprites.shipBulletSize > meteor3.y)
                if(shipBullet.y < meteor3.y+allsprites.meteorSize) // do not used yTouch since you can touch anywhere
                    if(shipBullet.x+allsprites.bulletOriginalHeight > 0)//(yTouch+allsprites.shipVHeight))
                        if(shipBullet.x>meteor3.x)
                        {

                            if(meteorsHits == 10){

                                mp.pause();
                                mp.seekTo(0);
                                mp.stop();
                                sound_pool.play(6,  1, 1, 1 , 0, 1);
                                TheGameLevel2.this.runOnUiThread(new Runnable()
                                {
                                    public void run()
                                    {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setMessage("Total time: "+minutes+":"+seconds)
                                                .setCancelable(false)
                                                .setTitle("Success")
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        //intent
                                                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                                                        startActivity(intent);
                                                    }
                                                });
                                        AlertDialog alert = builder.create();
                                        alert.show();
                                        change = false;
                                    }
                                });
                                meteorsHits = 0;

                            }
                            if(meteor3Hits==2){
                               meteor3ChangeLoc = true;
                                resetShipBullets();
                                meteorsHits++;
                                meteor3Hits=0;
                            }
                            else
                            {
                                meteor3HitsNoLocChange = true;
                                meteor3Hits++;
                                resetShipBullets();
                            }
                        }


        }
        public void moveEnemy()//set a new random location after 1 second
        {

        }
        public void startGame()
        {
            currentTime = System.currentTimeMillis();
            gameThread.start();
        }
        public void gameDone(){
            change = false;
            //clean the surface and show the menu by removing fullscreen
        }
        // three methods for the surfaceview
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder)
        {

        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int pixelFormat, int width, int height) {
            Log.d("surfaceChanged","inside surfacechanged method");
            s_width = width;
            s_height = height;
            allsprites.setSizes(s_width,s_height);
            //add  the enemy bullets
            placeEnemies();
            resetShipBullets();

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    }




}
