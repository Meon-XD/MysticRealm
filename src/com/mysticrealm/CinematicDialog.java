package com.mysticrealm;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;

public class CinematicDialog extends Dialog {
    public interface Listener {
        void onCinematicFinished();
    }

    private final int videoRes;
    private final Listener listener;
    private VideoView video;
    private boolean finished;
    private boolean wasPlaying;

    public CinematicDialog(Context context, int videoRes, Listener listener) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.videoRes = videoRes;
        this.listener = listener;
        setCanceledOnTouchOutside(false);
        setCancelable(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout root = new FrameLayout(getContext());
        root.setBackgroundColor(Color.BLACK);

        video = new AspectVideoView(getContext());
        FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        vp.gravity = Gravity.CENTER;
        root.addView(video, vp);

        TextView skip = new TextView(getContext());
        skip.setText("Lewati  >>");
        skip.setTextColor(Color.WHITE);
        skip.setTextSize(18);
        skip.setGravity(Gravity.CENTER);
        skip.setPadding(32, 14, 32, 14);
        skip.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        sp.gravity = Gravity.TOP | Gravity.END;
        sp.setMargins(0, 24, 32, 0);
        root.addView(skip, sp);
        skip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });

        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                dismiss();
            }
        });
        video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                dismiss();
                return true;
            }
        });

        setContentView(root);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.black);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        Uri uri = Uri.parse("android.resource://" + getContext().getPackageName()
                + "/" + videoRes);
        video.setVideoURI(uri);
        video.start();
    }

    public void pause() {
        if (video != null && video.isPlaying()) {
            wasPlaying = true;
            video.pause();
        }
    }

    public void resume() {
        if (video != null && wasPlaying) {
            wasPlaying = false;
            video.start();
        }
    }

    @Override
    public void dismiss() {
        if (finished) return;
        finished = true;
        try {
            if (video != null) video.stopPlayback();
        } catch (Exception ignored) {
        }
        super.dismiss();
        if (listener != null) listener.onCinematicFinished();
    }

    @Override
    public void onBackPressed() {
        dismiss();
    }

    private static class AspectVideoView extends VideoView {
        AspectVideoView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            if (width <= 0 || height <= 0) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            int w = width;
            int h = height;
            if (width * 9 > height * 16) {
                w = height * 16 / 9;
            } else {
                h = width * 9 / 16;
            }
            setMeasuredDimension(w, h);
        }
    }
}
