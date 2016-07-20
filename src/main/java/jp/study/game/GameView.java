package jp.study.game;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by daiki on 2016/07/08.
 */
public class GameView extends TextureView implements TextureView.SurfaceTextureListener, View.OnTouchListener{
    private Thread mThread;
    volatile private boolean mIsRunnable;
    volatile private float mTouchedX;
    volatile private float mTouchedY;
    private ArrayList<DrawableItem> mItemList;
    private ArrayList<Block> mBlockList;
    private Pad mPad;
    private float mPadHalfWidth;
    private Ball mBall;
    private float mBallRadius;
    private float mBlockWidth;
    private float mBlockHeight;
    static final int BLOCK_COUNT = 100;
    private int mLife;
    private long mGameStartTime;
    private Handler mHandler;
    private static final String KEY_LIFE = "life";
    private static final String KEY_GAME_START_TIME = "game_start_time";
    private static final String KEY_BALL = "ball";
    private static final String KEY_BLOCK = "block";
    private final Bundle mSavedInstanceState;


    public GameView(final Context context, Bundle savedInstanceState) {
        super(context);
        setSurfaceTextureListener(this);
        setOnTouchListener(this);
        mSavedInstanceState = savedInstanceState;
        mHandler = new Handler(){
            // UI Threadで実行されるHandler
            @Override
            public void handleMessage(Message message) {
                // 実行する処理
                Intent intent = new Intent(context, ClearActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.putExtras(message.getData());
                context.startActivity(intent);
            }
        };
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        readyObjects(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        readyObjects(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        synchronized (this) {
            return true;
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void start() {
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Paint paint = new Paint();
                ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.FILL);
                int collisionTime = 0;
                int soundIndex = 0;
                Vibrator vibrator = (Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);
                while (true) {
                    long startTime = System.currentTimeMillis();
                    synchronized (GameView.this) {
                        if (!mIsRunnable) {
                            break; // ループを終了する
                        }
                        Canvas canvas = lockCanvas();
                        // アプリ実行中繰り返し呼ばれる
                        if (canvas == null) {
                            continue;
                        }
                        canvas.drawColor(Color.BLACK);
                        float padLeft = mTouchedX - mPadHalfWidth;
                        float padRight = mTouchedX + mPadHalfWidth;
                        mPad.setLeftRight(padLeft, padRight);
                        mBall.move();
                        float ballTop = mBall.getY() - mBallRadius;
                        float ballLeft = mBall.getX() - mBallRadius;
                        float ballBottom = mBall.getY() + mBallRadius;
                        float ballRight = mBall.getX() + mBallRadius;

                        if (ballLeft < 0 && mBall.getSpeedX() < 0 || ballRight >= getWidth() && mBall.getSpeedX() > 0) {
                            mBall.setSpeedX(-mBall.getSpeedX()); // 横方向の壁にぶつかったので横の速度を反転
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_0, 10);
                            vibrator.vibrate(3);
                        }
                        if (ballTop < 0) {
                            mBall.setSpeedY(-mBall.getSpeedY()); // 縦方向の壁にぶつかったので縦の速度を反転
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_0, 10);
                            vibrator.vibrate(3);
                        }
                        if (ballTop > getHeight()) {
                            if (mLife > 0) {
                                mLife--;
                                mBall.reset();
                            } else {
                                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE);
                                vibrator.vibrate(3);
                                unlockCanvasAndPost(canvas);
                                Message message = Message.obtain();
                                Bundle bundle = new Bundle();
                                bundle.putBoolean(ClearActivity.EXTRA_IS_CLEAR, false);
                                bundle.putInt(ClearActivity.EXTRA_BLOCK_COUNT, getBlockCount());
                                bundle.putLong(ClearActivity.EXTRA_TIME, System.currentTimeMillis() - mGameStartTime);
                                message.setData(bundle);
                                mHandler.sendMessage(message);
                                return;
                            }
                        }
                        // ブロックとボールの衝突判定処理
                        Block leftBlock = getBlock(ballLeft, mBall.getY());
                        Block topBlock = getBlock(mBall.getX(), ballTop);
                        Block rightBlock = getBlock(ballRight, mBall.getY());
                        Block bottomBlock = getBlock(mBall.getX(), ballBottom);
                        boolean isCollision = false;
                        // ぶつかっているブロックが存在したら衝突判定を行う
                        if (leftBlock != null) {
                            mBall.setSpeedX(-mBall.getSpeedX());
                            leftBlock.collision();
                            isCollision = true;
                        }
                        if (topBlock != null) {
                            mBall.setSpeedY(-mBall.getSpeedY());
                            topBlock.collision();
                            isCollision = true;
                        }
                        if (rightBlock != null) {
                            mBall.setSpeedX(-mBall.getSpeedX());
                            rightBlock.collision();
                            isCollision = true;
                        }
                        if (bottomBlock != null) {
                            mBall.setSpeedX(-mBall.getSpeedX());
                            bottomBlock.collision();
                            isCollision = true;
                        }
                        if (isCollision) {
                            // ブロックにぶつかった場合
                            if (collisionTime > 0) {
                                // 一定期間内にぶつかった場合音を変える
                                if (soundIndex < 15) {
                                    soundIndex++;
                                }
                            } else {
                                // 一定時間内にぶつかっていない場合音を戻す
                                soundIndex = 1;
                            }
                            collisionTime = 10;
                            toneGenerator.startTone(soundIndex, 10);
                            vibrator.vibrate(3);
                        } else if (collisionTime > 0) {
                            // ブロックにぶつかっていない場合、音が変わる残り時間を減らす
                            collisionTime--;
                        }

                        // パッドとボールの衝突判定処理
                        float padTop = mPad.getTop();
                        float ballSpeedY = mBall.getSpeedY();
                        if (ballBottom > padTop && ballBottom - ballSpeedY < padTop && padLeft < ballRight && padRight > ballLeft) {
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_0, 10);
                            vibrator.vibrate(3);
                            if (ballSpeedY < mBlockHeight / 3) {
                                ballSpeedY *= -1.05f;
                            } else {
                                ballSpeedY = -ballSpeedY;
                            }
                            float ballSpeedX = mBall.getSpeedX() + (mBall.getX() - mTouchedX) / 10;
                            if (ballSpeedX > mBlockWidth / 5) {
                                ballSpeedX = mBlockWidth / 5;
                            }
                            mBall.setSpeedY(ballSpeedY);
                            mBall.setSpeedX(ballSpeedX);
                        }
                        for (DrawableItem item : mItemList) {
                            item.draw(canvas, paint);
                        }
                        unlockCanvasAndPost(canvas);
                        if (isCollision && getBlockCount() == 0) {
                            Message message = Message.obtain();
                            Bundle bundle = new Bundle();
                            bundle.putBoolean(ClearActivity.EXTRA_IS_CLEAR, true);
                            bundle.putInt(ClearActivity.EXTRA_BLOCK_COUNT, 0);
                            bundle.putLong(ClearActivity.EXTRA_TIME, System.currentTimeMillis() - mGameStartTime);
                            message.setData(bundle);
                            mHandler.sendMessage(message);
                        }
                    }
                    long sleepTime = 16 - (System.currentTimeMillis() - startTime);
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {

                        }
                    }
                }
                toneGenerator.release(); // toneGeneratorをリリースする
            }
        });
        mIsRunnable = true;
        mThread.start();
    }

    public void stop() {
        mIsRunnable = false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mTouchedX = event.getX();
        mTouchedY = event.getY();
        return true;
    }

    public void readyObjects(int width, int height) {
        mBlockWidth = width / 10;
        mBlockHeight = height / 20;
        mItemList =  new ArrayList<DrawableItem>();
        mBlockList = new ArrayList<Block>();
        for (int i = 0; i < BLOCK_COUNT; i++) {
            float blockTop = i / 10 * mBlockHeight;
            float blockLeft = i % 10 * mBlockWidth;
            float blockBottom = blockTop + mBlockHeight;
            float blockRight = blockLeft + mBlockWidth;
            mBlockList.add(new Block(blockTop, blockLeft, blockBottom, blockRight));
        }
        mItemList.addAll(mBlockList);
        mPad = new Pad(height * 0.8f, height * 0.85f);
        mItemList.add(mPad);
        mPadHalfWidth = width / 10;
        mBallRadius = width < height ? width / 40 : height / 40;
        mBall = new Ball(mBallRadius, width / 2, height / 2);
        mItemList.add(mBall);
        mLife = 5;
        mGameStartTime = System.currentTimeMillis();
        if (mSavedInstanceState != null) {
            mLife = mSavedInstanceState.getInt(KEY_LIFE);
            mGameStartTime = mSavedInstanceState.getLong(KEY_GAME_START_TIME);
            mBall.restore(mSavedInstanceState.getBundle(KEY_BALL), width, height);
            for (int i = 0; i < BLOCK_COUNT; i++) {
                mBlockList.get(i).restore(mSavedInstanceState.getBundle(KEY_BLOCK + String.valueOf(i)));
            }
        }
    }

    private Block getBlock(float x, float y) {
        int index = (int)(x / mBlockWidth) + (int)(y / mBlockHeight) * 10;
        if (0 <= index && index < BLOCK_COUNT) {
            Block block = (Block)mItemList.get(index);
            if (block.isExist()) {
                return block;
            }
        }
        return null;
    }

    private int getBlockCount(){
        int count = 0;
        for (Block block :mBlockList) {
            if (block.isExist()) {
                count++;
            }
        }
        return count;
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_LIFE, mLife);
        outState.putLong(KEY_GAME_START_TIME, mGameStartTime);
        outState.putBundle(KEY_BALL, mBall.sava(getWidth(), getHeight()));
        for (int i = 0; i < BLOCK_COUNT; i++) {
            outState.putBundle(KEY_BLOCK + String.valueOf(i), mBlockList.get(i).save());
        }
    }
}
