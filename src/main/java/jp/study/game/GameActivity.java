package jp.study.game;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class GameActivity extends AppCompatActivity {
    private GameView mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new GameView(this, savedInstanceState);
        setContentView(mView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mView.stop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mView.onSaveInstanceState(outState);
    }
}
