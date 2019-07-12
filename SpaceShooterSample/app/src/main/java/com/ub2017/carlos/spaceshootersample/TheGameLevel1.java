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
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;
import android.graphics.BitmapFactory;


import java.io.IOException;
import java.security.cert.PolicyNode;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by carlos on 10/13/2016.
 */

public class TheGameLevel1 extends Activity implements View.OnTouchListener
{
    SoundPool soundPool;
    int countBoom;
    TheView mySurfaceView;   //the surface view, where we draw
    SpriteHelper allsprites;  //class that has all the locations and sizes of the images in the sprite
    float xTouch,yTouch;     //the location when the screen is touched
    int s_width,s_height,x_height;    //the size of the surface view
    //used for which sprite to use
    int shipLoc,shipExplode,enemy1Loc,enemy2Loc,meteor1Loc,meteor2Loc,meteor3Loc,boomLoc;
    boolean hit = false; boolean enemy1ChangeLoc = false;boolean enemy2ChangeLoc = false;
    Random r = new Random();
    Point enemy1,enemy2,enemy1bullet,enemy2bullet,meteor1,meteor2,meteor3, boom,shipBullet;
    long enemyMoveTime = 0; int shipHits=0;int enemiesHits=0;
    float temp=0;
    //15 frames per seconds
    float skipTime =1000.0f/30.0f; //setting 30fps
    long lastUpdate;
    float dt;
    long currentTime;
    int seconds;
    int minutes;
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
                TheGameLevel1.this.getPackageName()+
                "/"+R.raw.mainsong);
        try {
            mp.setDataSource(TheGameLevel1.this ,theUri);
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

    private MediaPlayer.OnPreparedListener okStart = new MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();

        }
    };

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
        soundMap.put(1, sound_pool.load(TheGameLevel1.this, R.raw.enemyboom, 1));
        soundMap.put(2,sound_pool.load(TheGameLevel1.this, R.raw.gameover, 1));
        soundMap.put(3,sound_pool.load(TheGameLevel1.this, R.raw.meteorboom, 1));
        soundMap.put(4, sound_pool.load(TheGameLevel1.this, R.raw.shipboom, 1));
        soundMap.put(5,sound_pool.load(TheGameLevel1.this, R.raw.shipbullet, 1));
        soundMap.put(6, sound_pool.load(TheGameLevel1.this, R.raw.success, 1));
 }
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("OnCreate","inside oncreate method");
        setManually();

        //use a pool if there is multiple smalls sounds to be played
        setPool_API_20andDown();
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.spritesheetbetter); //not setting a static xml
        //set all the sprite locations and sizes
        allsprites = new SpriteHelper(getResources());//send the resources (only View has it)
        //make sure there is only ONE copy of the image and that the image
        //is in the drawable-nodpi. if it is not unwanted scaling might occur
        shipLoc=shipExplode=enemy1Loc=enemy2Loc=meteor1Loc=meteor2Loc=meteor3Loc = 0;
        enemy1 = new Point();
        enemy2 = new Point();
        enemy1bullet = new Point();     //used for canvas drawing location
        enemy2bullet = new Point();
        meteor1 = new Point();
        boom = new Point();
        shipBullet = new Point();
        lastUpdate = 0;         //to check against now time
        //soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC);
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
                        //look it to paint on it
                       c = holder.lockCanvas();

                        Bitmap mScaledBackground = BitmapFactory.decodeResource(getResources(),R.drawable.bkg1);
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
                            xTouch = 0;
                             sound_pool.play(4,  5, 5, 1 , 0, 1);
                            if(countBoom==0){
                                Rect enemy1Boom = new Rect((int) temp, (int) (s_height * .9 ),(int) temp+allsprites.boomSize,s_height);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==1){
                                Rect enemy1Boom = new Rect((int) temp, (int) (s_height * .9 ),(int) temp+allsprites.boomSize,s_height);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==2){
                                Rect enemy1Boom = new Rect((int) temp, (int) (s_height * .9 ),(int) temp+allsprites.boomSize,s_height);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==3){
                                Rect enemy1Boom = new Rect((int) temp, (int) (s_height * .9 ),(int) temp+allsprites.boomSize,s_height);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==4){
                                Rect enemy1Boom = new Rect((int) temp, (int) (s_height * .9 ),(int) temp+allsprites.boomSize,s_height);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==5) {
                                Rect enemy1Boom = new Rect((int) temp, (int) (s_height * .9), (int) temp + allsprites.boomSize, s_height);
                                c.drawBitmap(allsprites.space, allsprites.boomsprites[countBoom], enemy1Boom, null);

                                hit = false;
                                countBoom = 0;

                            }
                            countBoom++;
                        }
                        else
                            {
                                if(firstTimeBullet){
                                    sound_pool.play(5,  1, 1, 1 , 0, 1);
                                    firstTimeBullet = false;

                                }
                            place1 = new Rect((int) xTouch, (int) (s_height * .9),
                                    (int) xTouch + allsprites.shipVWidth, s_height);
                            c.drawBitmap(allsprites.space, allsprites.shipVSprites[shipLoc], place1, null);
                        }

                        //ship bullet

                        Rect place2 = new Rect(shipBullet.x,shipBullet.y,shipBullet.x+allsprites.shipBulletSize,shipBullet.y+allsprites.shipBulletSize);
                        c.drawBitmap(allsprites.space,allsprites.shipBulletSprite,place2,null);

                        //enemy 1
                        if (enemy1ChangeLoc) {
                            sound_pool.play(1,  5, 5, 1 , 0, 1);
                            if(countBoom==0){
                                Rect enemy1Boom = new Rect(enemy1.x+15,enemy1.y+15,enemy1.x+allsprites.enemyWidth+20,enemy1.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==1){
                                Rect enemy1Boom = new Rect(enemy1.x+15,enemy1.y+15,enemy1.x+allsprites.enemyWidth+30,enemy1.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==2){
                                Rect enemy1Boom = new Rect(enemy1.x+15,enemy1.y+15,enemy1.x+allsprites.enemyWidth+30,enemy1.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==3){
                                Rect enemy1Boom = new Rect(enemy1.x+15,enemy1.y+15,enemy1.x+allsprites.enemyWidth+30,enemy1.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==4){
                                Rect enemy1Boom = new Rect(enemy1.x+15,enemy1.y+15,enemy1.x+allsprites.enemyWidth+30,enemy1.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==5){
                                Rect enemy1Boom = new Rect(enemy1.x+15,enemy1.y+15,enemy1.x+allsprites.enemyWidth+30,enemy1.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                                changeEnemy1Location();
                                enemy1ChangeLoc = false;
                                countBoom=0;
                            }
                            countBoom++;
                        }
                        else{

                            Rect place3 = new Rect(enemy1.x,enemy1.y,enemy1.x+allsprites.enemyWidth,enemy1.y+allsprites.enemyHeight);
                            c.drawBitmap(allsprites.space,allsprites.enemy1sprites[enemy1Loc],place3,null);
                        }

                        //enemy 1 bullet

                        Rect place4 = new Rect(enemy1bullet.x,enemy1bullet.y,enemy1bullet.x+ allsprites.enemybulletWidth,
                                         enemy1bullet.y+allsprites.enemybulletHeight);
                        c.drawBitmap(allsprites.space,allsprites.enemybulletSprite,place4,null);

                        //enemy 2
                        if(enemy2ChangeLoc){
                            sound_pool.play(1,  1, 1, 1 , 0, 1);
                            if(countBoom==0){
                                Rect enemy1Boom = new Rect(enemy2.x+15,enemy2.y+15,enemy2.x+allsprites.enemyWidth+30,enemy2.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==1){
                                Rect enemy1Boom = new Rect(enemy2.x+15,enemy2.y+15,enemy2.x+allsprites.enemyWidth+30,enemy2.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==2){
                                Rect enemy1Boom = new Rect(enemy2.x+15,enemy2.y+15,enemy2.x+allsprites.enemyWidth+30,enemy2.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==3){
                                Rect enemy1Boom = new Rect(enemy2.x+15,enemy2.y+15,enemy2.x+allsprites.enemyWidth+30,enemy2.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==4){
                                Rect enemy1Boom = new Rect(enemy2.x+15,enemy2.y+15,enemy2.x+allsprites.enemyWidth+30,enemy2.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                            }
                            if(countBoom==5){
                                Rect enemy1Boom = new Rect(enemy2.x+15,enemy2.y+15,enemy2.x+allsprites.enemyWidth+30,enemy2.y+allsprites.boomSize);
                                c.drawBitmap(allsprites.space,allsprites.boomsprites[countBoom],enemy1Boom,null);
                                changeEnemy2Location();
                                enemy2ChangeLoc = false;
                                countBoom=0;
                            }
                            countBoom++;
                        }
                        else{
                            Rect place5 = new Rect(enemy2.x,enemy2.y,enemy2.x+allsprites.enemyWidth,enemy2.y+allsprites.enemyHeight);
                            c.drawBitmap(allsprites.space,allsprites.enemy2sprites[enemy2Loc],place5,null);

                        }

                        //enemy 1 bullet

                        Rect place6 = new Rect(enemy2bullet.x,enemy2bullet.y,enemy2bullet.x+ allsprites.enemybulletWidth,
                                enemy2bullet.y+allsprites.enemybulletHeight);
                        c.drawBitmap(allsprites.space,allsprites.enemybulletSprite,place6,null);



                        holder.unlockCanvasAndPost(c);

                        //change values to draw the next sprite
                        shipLoc= (shipLoc+1)%4;
                        enemy1Loc = (enemy1Loc+1)%6;
                        enemy2Loc = (enemy2Loc+1)%6;
                        meteor1Loc = (meteor1Loc+1)%4;
                        boomLoc = (boomLoc+1)%6;

                        Log.d("ship bullet","********* ship bullet y pos: "+shipBullet.y);
                        Log.d("enemy1 bullet","&&&&&&&& enemy1 bullet y pos: "+enemy1bullet.y);
                        Log.d("enemy2 bullet","%%%%%%%%  enemy2  bullet y pos: "+enemy2bullet.y);

                        shipBullet.y-=s_height/25;
                        //move bullets
                        enemy1bullet.y+=s_height/100;//change this to make it seem faster going down
                        x_height = s_height;
                        //move bullets
                        enemy2bullet.y+=x_height/100;//change this to make it seem faster going down
                       // shipBullet.y+=s_height/2;
                        //check if bullet hit enemy or meteors
                        checkHits();
                        //if enemies not hit then check bullets are out of screen
                        //check ship bullet
                         // add logic here
                        //check enemy bullet
                        if (enemy1bullet.y > s_height) {
                            Log.d("if eneme1 ","inside enemy1bullet.y");
                            //reset bullets
                            resetEnemy1Bullets();
                        }
                        if (enemy2bullet.y > s_height) {
                            Log.d("if enemy2","inside enemy1bullet.y");
                            //reset bullets
                            resetEnemy2Bullets();
                        }
                        if (shipBullet.y < 0) {
                            Log.d("if ship bullet","inside shipbullet.y");

                            //reset bullets
                            resetShipBullets();
                        }

                        lastUpdate = System.currentTimeMillis();

                    }
                }
            }
            int widthRatio(int n){
                return (int)(allsprites.shipVWidth/(allsprites.shipVHeight/s_height*.1));
            }
        };
        public void changeEnemy1Location(){
            int x1 = r.nextInt(8) + 1;
            int y1 = r.nextInt(4) + 1;

            int xlocation1 = (int)(s_width*(x1*.1));
            int ylocation1 = (int)(s_height*(y1*.1));

            enemy1.x = xlocation1;
            enemy1.y = ylocation1;

        }
        public void changeEnemy2Location(){
            int x2 = r.nextInt(8) + 1;
            int y2 = r.nextInt(4) + 1;

            int xlocation2 = (int)(s_width*(x2*.1));
            int ylocation2 = (int)(s_height*(y2*.1));

            enemy2.x = xlocation2;
            enemy2.y = ylocation2;

        }
        public void placeEnemies(){

            Log.d("placeEnemies","inside placeEnemies");
            int x1 = r.nextInt(8) + 1;
            int y1 = r.nextInt(4) + 1;

            int x2 = r.nextInt(8) + 1;
            int y2 = r.nextInt(4) + 1;



            int xlocation1 = (int)(s_width*(x1*.1));//this values will change
            int ylocation1 = (int)(s_height*(y1*.1));

            int xlocation2 = (int)(s_width*(x2*.1));//this values will change
            int ylocation2 = (int)(s_height*(y2*.1));

            enemy1.x = xlocation1;
            enemy1.y = ylocation1;
            enemy2.x = xlocation2;
            enemy2.y = ylocation2;

            //placement for the meteor for second level
            int xlocation3 = (int)(s_width*.95);//this values will change
            int ylocation3 = (int)(s_height*.5);
            meteor1.x = xlocation3;
            meteor1.y = ylocation3;

            //placement of boom, should only happen when the ship blows up
            int xlocation4 = (int)(s_width*.7);//this values will change
            int ylocation4 = (int)(s_height*.8);
            boom.x = xlocation4;
            boom.y = ylocation4;

            //this will go up just like the enemy bullets go down
            int xlocation5 = (int)(s_width*.3);//this values will change
            int ylocation5 = (int)(s_height*.8);
            shipBullet.x = xlocation5;
            shipBullet.y = ylocation5;

        }
        public void resetEnemy1Bullets()
        {
            Log.d("resetEnemyBullets","inside resetting enemybullets");

            enemy1bullet.x = enemy1.x + allsprites.enemyWidth/2 - allsprites.enemybulletWidth/2;
            enemy1bullet.y =  enemy1.y+allsprites.enemyHeight;

        }
        public void resetEnemy2Bullets()
        {
            Log.d("resetEnemyBullets","inside resetting enemybullets");


            enemy2bullet.x = enemy2.x + allsprites.enemyWidth/2 - allsprites.enemybulletWidth/2;
            enemy2bullet.y =  enemy2.y+allsprites.enemyHeight;
        }
        public void resetShipBullets()
        {
            sound_pool.play(5,  1, 1, 1 , 0, 1);
            Log.d("resetshipBullets","inside resetting enemybullets");
            shipBullet.x = (int) xTouch+ allsprites.shipVWidth/2 - allsprites.shipBulletSize/2;
            shipBullet.y =  (int)(s_height*.9);

        }

        public void checkHits()
        {
            Log.d("checkHits","inside checkHits mehtod");
            if(enemy1bullet.x>xTouch)
                if (enemy1bullet.x+allsprites.enemybulletWidth < (xTouch+allsprites.shipVWidth))
                    if(enemy1bullet.y >(s_height*.9)) // do not used yTouch since you can touch anywhere
                        if(enemy1bullet.y+allsprites.enemybulletHeight < s_height)//(yTouch+allsprites.shipVHeight))
                        {
                            shipHits++;
                            if (shipHits == 5)
                            {
                                Log.d("chekcHits", "!!!!!!!!!!!!!!!!! inside enemy1 toast shipHits: "+shipHits);

                                sound_pool.play(2,  5, 5, 1 , 0, 1);
                                mp.pause();
                                mp.seekTo(0);
                                mp.stop();
                                TheGameLevel1.this.runOnUiThread(new Runnable()
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
                            else{
                                temp = xTouch;
                            resetEnemy1Bullets();

                        }
                            hit = true;

            }

            if(enemy2bullet.x>xTouch)
                if (enemy2bullet.x+allsprites.enemybulletWidth < (xTouch+allsprites.shipVWidth))
                    if(enemy2bullet.y >(s_height*.9)) // do not used yTouch since you can touch anywhere
                        if(enemy2bullet.y+allsprites.enemybulletHeight < s_height)//(yTouch+allsprites.shipVHeight))
                        {
                            shipHits++;
                            if (shipHits == 5) {
                                Log.d("chekcHits", "!!!!!!!!!!!!!!!!! inside toast enemy2 shipHits: "+shipHits);
                                mp.pause();
                                mp.seekTo(0);
                                mp.stop();
                                sound_pool.play(2,  5, 5, 1 , 0, 1);

                                TheGameLevel1.this.runOnUiThread(new Runnable()
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
                            else{
                                Log.d("chekcHits", "!!!!!!!!!!!!!!!!! 111111111111111111111111");
                                temp = xTouch;
                                resetEnemy2Bullets();

                                }
                            hit = true;

                        }


                if (shipBullet.x+allsprites.shipBulletSize > enemy1.x)
                    if(shipBullet.x < enemy1.x+allsprites.enemyWidth) // do not used yTouch since you can touch anywhere
                        if(shipBullet.y+allsprites.bulletOriginalHeight > 0)//(yTouch+allsprites.shipVHeight))
                        if(shipBullet.y<enemy1.y+allsprites.enemyHeight-100)
                        {
                           /* System.exit(0);*/
                                if(enemiesHits == 10){
                                    //show toast and move to next stage
                                    mp.pause();
                                    mp.seekTo(0);
                                    mp.stop();
                                    sound_pool.play(6,  1, 1, 1 , 0, 1);

                                    TheGameLevel1.this.runOnUiThread(new Runnable()
                                    {
                                        public void run()
                                        {
                                          //  Toast.makeText(getApplicationContext(),"You won the game!!!",Toast.LENGTH_LONG).show();
                                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                            builder.setMessage("Total time: "+minutes+":"+seconds)
                                                    .setCancelable(false)
                                                    .setTitle("Success")
                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                                                            startActivity(intent);
                                                        }
                                                    });
                                            AlertDialog alert = builder.create();
                                            alert.show();
                                            change = false;

                                            //Do your UI operations like dialog opening or Toast here
                                        }
                                    });
                                    enemiesHits = 0;

                                }
                                else{
                                    enemy1ChangeLoc = true;
                                    resetShipBullets();
                                    enemiesHits++;
                                }

                        }
                        //check bullet from enemy 2


            if (shipBullet.x+allsprites.shipBulletSize > enemy2.x)
                if(shipBullet.x < enemy2.x+allsprites.enemyWidth) // do not used yTouch since you can touch anywhere
                    if(shipBullet.y+allsprites.bulletOriginalHeight > 0)//(yTouch+allsprites.shipVHeight))
                        if(shipBullet.y<enemy2.y+allsprites.enemyHeight-100)
                        {
                           /* System.exit(0);*/

                            if(enemiesHits == 10){
                                //show toast and move to next stage
                                mp.pause();
                                mp.seekTo(0);
                                mp.stop();
                                sound_pool.play(6,  1, 1, 1 , 0, 1);

                                TheGameLevel1.this.runOnUiThread(new Runnable()
                                {
                                    public void run()
                                    {
                                       // Toast.makeText(getApplicationContext(),"You Won the game!!!",Toast.LENGTH_LONG).show();
                                        //Do your UI operations like dialog opening or Toast here
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

                                enemiesHits = 0;
                            }
                            else{
                                enemy2ChangeLoc = true;
                                resetShipBullets();
                                enemiesHits++;
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
            resetEnemy1Bullets();
            resetEnemy2Bullets();
            resetShipBullets();

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    }




}
