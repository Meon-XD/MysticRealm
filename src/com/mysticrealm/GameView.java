package com.mysticrealm;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View implements Choreographer.FrameCallback {
    private final Game game;
    private boolean running;
    private long lastNanos;

    public GameView(Context context, Game game) {
        super(context);
        this.game = game;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setKeepScreenOn(true);
    }

    public void start() {
        if (running) return;
        running = true;
        lastNanos = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }

    public void stop() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;
        float dt;
        if (lastNanos == 0) {
            dt = 1f / 60f;
        } else {
            dt = (frameTimeNanos - lastNanos) / 1000000000f;
        }
        lastNanos = frameTimeNanos;
        if (dt > 0.05f) dt = 0.05f;
        if (dt <= 0f) dt = 1f / 60f;
        game.update(dt);
        invalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        game.draw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        game.resize(w, h);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE
                && event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) {
            game.mouseMoved(event.getX(), event.getY());
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (game.inputActive || event.getRepeatCount() == 0 || isGameKey(keyCode)) {
            if (game.keyDown(keyCode)) return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        game.keyUp(keyCode);
        return game.inputActive || isGameKey(keyCode) || super.onKeyUp(keyCode, event);
    }

    private static boolean isGameKey(int code) {
        return code == KeyEvent.KEYCODE_W || code == KeyEvent.KEYCODE_A
                || code == KeyEvent.KEYCODE_S || code == KeyEvent.KEYCODE_D
                || code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_SPACE || code == KeyEvent.KEYCODE_1
                || code == KeyEvent.KEYCODE_2 || code == KeyEvent.KEYCODE_3
                || code == KeyEvent.KEYCODE_SHIFT_LEFT || code == KeyEvent.KEYCODE_SHIFT_RIGHT
                || code == KeyEvent.KEYCODE_Q || code == KeyEvent.KEYCODE_ESCAPE
                || code == KeyEvent.KEYCODE_BACK;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        int tool = ev.getPointerCount() > 0 ? ev.getToolType(0) : MotionEvent.TOOL_TYPE_UNKNOWN;
        if (tool == MotionEvent.TOOL_TYPE_MOUSE) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    game.mouseDown(MotionEvent.BUTTON_PRIMARY, ev.getX(), ev.getY());
                    return true;
                case MotionEvent.ACTION_UP:
                    game.mouseUp(MotionEvent.BUTTON_PRIMARY, ev.getX(), ev.getY());
                    return true;
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_HOVER_MOVE:
                    game.mouseMoved(ev.getX(), ev.getY());
                    return true;
                case MotionEvent.ACTION_BUTTON_PRESS:
                    if (ev.getActionButton() != MotionEvent.BUTTON_PRIMARY) {
                        game.mouseDown(ev.getActionButton(), ev.getX(), ev.getY());
                    }
                    return true;
                case MotionEvent.ACTION_BUTTON_RELEASE:
                    if (ev.getActionButton() != MotionEvent.BUTTON_PRIMARY) {
                        game.mouseUp(ev.getActionButton(), ev.getX(), ev.getY());
                    }
                    return true;
                default:
                    break;
            }
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int idx = ev.getActionIndex();
                game.pointerDown(ev.getPointerId(idx), ev.getX(idx), ev.getY(idx));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int n = ev.getPointerCount();
                for (int i = 0; i < n; i++) {
                    game.pointerMove(ev.getPointerId(i), ev.getX(i), ev.getY(i));
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int idx = ev.getActionIndex();
                game.pointerUp(ev.getPointerId(idx), ev.getX(idx), ev.getY(idx));
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                game.pointerCancel();
                break;
            }
            default:
                break;
        }
        return true;
    }
}
