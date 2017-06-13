package com.edu4java.android.killthemall;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class GameView extends SurfaceView {
    private GameLoopThread gameLoopThread;
    private List<Sprite> sprites = new ArrayList<Sprite>();
    private List<TempSprite> temps = new ArrayList<TempSprite>();
    private long lastClick;
    private Bitmap bmpBlood;
    private Star star;

    private SoundPool sounds;
    private int destroy;
    private int destroy_me;
    private int start_shoot;
    private int msprite = 6;

    private SensorManager sensorManager;
    private SensorEventListener sensorListener;
    private Sensor sensor;
    private float[] mags;
    private float[] accels;
    private float[] gravity;
    private float[] magnetic;

    public GameView(Context context) {
        super(context);

        gameLoopThread = new GameLoopThread(this);
        getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                boolean retry = true;
                gameLoopThread.setRunning(false);
                while (retry) {
                    try {
                        gameLoopThread.join();
                        retry = false;
                    } catch (InterruptedException e) {}
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                createSprites();
                gameLoopThread.setRunning(true);
                gameLoopThread.start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
            }
        });
        bmpBlood = BitmapFactory.decodeResource(getResources(), R.drawable.blood1);
        sounds = new SoundPool(10,  AudioManager.STREAM_MUSIC,0);
        destroy = sounds.load(context, R.raw.destroy,1);
        destroy_me = sounds.load(context, R.raw.destroy_me,1);
        start_shoot = sounds.load(context, R.raw.star_shoot, 1);

        sensorManager = (SensorManager) this.getContext().getSystemService(Context.SENSOR_SERVICE);
        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        mags = event.values.clone();
                        break;
                    case Sensor.TYPE_ACCELEROMETER:
                        accels = event.values.clone();
                        break;
                }

                if (mags != null && accels != null) {
                    gravity = new float[9];
                    magnetic = new float[9];
                    SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
                    float[] outGravity = new float[9];
                    float[] values = new float[3];
                    SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X, SensorManager.AXIS_Z, outGravity);
                    SensorManager.getOrientation(outGravity, values);

                    float roll = 1/(values[0]) * 1000;
                    float pitch = -1 * values[1] * 500;
                    Log.d("roll", Float.toString(roll));
                    mags = null;
                    accels = null;
                    for (Sprite sprite : sprites) {
                        if (sprite.goodOne) {
                            sprite.setSpeed(roll, pitch);
                        }
                    }

                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void createSprites() {
        sprites.add(createSprite(R.drawable.bad1, false));
        sprites.add(createSprite(R.drawable.bad2, false));
//        sprites.add(createSprite(R.drawable.bad3, false));
//        sprites.add(createSprite(R.drawable.bad4, false));
//        sprites.add(createSprite(R.drawable.bad5, false));
//        sprites.add(createSprite(R.drawable.bad6, false));
//        sprites.add(createSprite(R.drawable.good1, true));
//        sprites.add(createSprite(R.drawable.good2, true));
//        sprites.add(createSprite(R.drawable.good3, true));
//        sprites.add(createSprite(R.drawable.good4, true));
        sprites.add(createSprite(R.drawable.good5, true));
//        sprites.add(createSprite(R.drawable.good6, true));
    }

    private Sprite createSprite(int resouce, boolean good) {
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), resouce);
        return new Sprite(this, bmp, good);
    }

    private void endGame(Canvas canvas, String text){
        gameLoopThread.setRunning(false);
        Typeface tf = Typeface.create("Helvetica",Typeface.BOLD);
        Paint paint = new Paint();
        paint.setTypeface(tf);
        paint.setColor(Color.RED);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(40);
        canvas.drawText(text, canvas.getWidth()/2, canvas.getHeight()/2, paint);
        sensorManager.unregisterListener(sensorListener);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Boolean gameOver = false;
        int goodSpriteNo = 0;
        int badSpriteNo = 0;
        List<Sprite> spritesToRemove = new ArrayList<Sprite>();

        canvas.drawColor(Color.BLACK);
        if (star != null && star.getVisible())
            star.onDraw(canvas);

        for (Sprite spriteA : sprites) {
            if (spriteA.goodOne) {
                for (Sprite spriteB : sprites) {
                    if (!spriteB.goodOne) {
                        if (spriteA.isCollision(spriteB.getX(), spriteB.getY())) {
                            spritesToRemove.add(spriteA);
                            sounds.play(destroy_me, 1.0f, 1.0f, 0,0,1.5f);

                        }
                    }
                }
            }
        }

        if (star != null && star.getVisible()) {
            for (Sprite spriteB : sprites) {
                if (!spriteB.goodOne) {
                    if (spriteB.isCollision(star.getX(), star.getY())) {
                        spritesToRemove.add(spriteB);
                        sounds.play(destroy, 1.0f, 1.0f, 0,0,1.5f);

                        star = null;
                        break;
                    }
                }
            }
        }

        for (Sprite sprite : spritesToRemove) {
            temps.add(new TempSprite(temps, this, sprite.getX(), sprite.getY(), bmpBlood));
            sprites.remove(sprite);
        }

        for (int i = temps.size() - 1; i >= 0; i--) {
            temps.get(i).onDraw(canvas);
        }

        for (Sprite sprite : sprites) {
            sprite.onDraw(canvas);
            if (sprite.goodOne) goodSpriteNo++;
            else badSpriteNo++;
        }

        if (badSpriteNo == 0) {
            gameOver = true;
            endGame(canvas, "The winner are Good Guys");
        }

        if (goodSpriteNo == 0) {
            gameOver = true;
            endGame(canvas, "The winner are Bad Guys");
        }

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (System.currentTimeMillis() - lastClick > 300) {
            lastClick = System.currentTimeMillis();

            synchronized (getHolder()) {

                Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.star);
                for (Sprite sprite : sprites) {
                    if (sprite.goodOne) {
                        star = new Star(this, bmp, sprite.getX(), sprite.getY(),
                                sprite.getXSpeed()*3, sprite.getYSpeed()*3);
                        sounds.play(start_shoot, 1.0f, 1.0f, 0,0,1.5f);

                    }
                }
            }

        }

        return true;
    }
}