package com.mysticrealm;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private GameView view;
    private Game game;
    private CinematicDialog cinematic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        hideSystemUi();
        game = new Game(this);
        view = new GameView(this, game);
        setContentView(view);
        view.requestFocus();
    }

    private void hideSystemUi() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
            if (view != null) view.requestFocus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (cinematic != null) {
            cinematic.resume();
        } else {
            game.onResume();
        }
        if (view != null) view.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cinematic != null) cinematic.pause();
        if (view != null) view.stop();
        game.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cinematic != null) {
            cinematic.dismiss();
            cinematic = null;
        }
        if (game != null) game.shutdown();
    }

    public void playCinematic(String kind, final Runnable onDone) {
        if (cinematic != null) return;
        int res = "ending".equals(kind) ? R.raw.cinematic_ending : R.raw.cinematic_opening;
        cinematic = new CinematicDialog(this, res, new CinematicDialog.Listener() {
            public void onCinematicFinished() {
                cinematic = null;
                onDone.run();
            }
        });
        cinematic.show();
    }

    @Override
    public void onBackPressed() {
        if (game != null && game.onBack()) return;
        super.onBackPressed();
    }
}
