package jp.study.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * Created by daiki on 2016/07/08.
 */
public class Block implements DrawableItem{
    private final float mTop;
    private final float mLeft;
    private final float mBottom;
    private final float mRight;
    private int mHard;
    private boolean mIsCollision = false; // 衝突状態を記録するフラグ
    private boolean mIsExist = true; // ブロックが破壊されていないか
    private static final String KEY_HARD = "hard";

    public Block(float top, float left, float bottom, float right) {
        mTop = top;
        mLeft = left;
        mBottom = bottom;
        mRight = right;
        mHard = 1;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (mIsExist) {
            // 耐久力が０以上のときのみ
            if (mIsCollision) {
                mHard--;
                mIsCollision = false;
                if (mHard <= 0) {
                    mIsExist = false;
                    return;
                }
            }
            // 塗りつぶし部分を描画
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(mLeft, mTop, mRight, mBottom, paint);
            // 枠線部分を描画
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            canvas.drawRect(mLeft, mTop, mRight, mBottom, paint);
        }
    }

    public void collision(){
        mIsCollision = true; // 衝突したことだけを記録し、実際の破壊はdraw()で行う
    }

    /*
     ブロックが破壊されていないか
     @return 破壊されていない場合true
     */
    public boolean isExist(){
        return mIsExist;
    }

    /*
     Bundleに状態を保存する
     @return 保存すべき状態が格納されたBundle
     */
    public Bundle save() {
        Bundle outState = new Bundle();
        outState.putInt(KEY_HARD, mHard);
        return outState;
    }

    /*
     Bundleから状態を復元する
     @param inState 復元すべき状態が格納されたBundle
     */
    public void restore(Bundle inState) {
        mHard = inState.getInt(KEY_HARD);
        mIsExist = mHard > 0;
    }
}
