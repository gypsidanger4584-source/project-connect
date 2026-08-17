package com.example.login_new.alogic;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;

public class ZoomableFrameLayout extends FrameLayout {

    private float scaleFactor = 1f;
    private final float minScale = 1f, maxScale = 3f;

    private float posX = 0f, posY = 0f;
    private float lastTouchX, lastTouchY;
    private ScaleGestureDetector scaleDetector;

    public ZoomableFrameLayout(Context context) {
        super(context);
        init(context);
    }

    public ZoomableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float prevScale = scaleFactor;
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(minScale, Math.min(scaleFactor, maxScale));

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                posX += (posX - focusX) * (scaleFactor / prevScale - 1);
                posY += (posY - focusY) * (scaleFactor / prevScale - 1);

                // 최소 크기일 때 위치값 초기화
                if (scaleFactor == minScale) {
                    posX = 0f;
                    posY = 0f;
                }

                invalidate();
                requestLayout();
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        if (scaleFactor > 1f) {
            getParent().requestDisallowInterceptTouchEvent(true);
        } else {
            getParent().requestDisallowInterceptTouchEvent(false);

            if (posX != 0f || posY != 0f) {
                posX = 0f;
                posY = 0f;
                invalidate();
                requestLayout();
            }
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                if (scaleFactor > 1f) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;

                    posX += dx;
                    posY += dy;

                    clampPosition();  // 위치 제한

                    lastTouchX = event.getX();
                    lastTouchY = event.getY();

                    invalidate();
                    requestLayout();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        return true;
    }

    private void clampPosition() {
        if (getChildCount() == 0) return;

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        int contentWidth = getChildAt(0).getWidth();
        int contentHeight = getChildAt(0).getHeight();

        float scaledWidth = contentWidth * scaleFactor;
        float scaledHeight = contentHeight * scaleFactor;

        float minX = Math.min(0, viewWidth - scaledWidth);
        float maxX = 0;

        float minY = Math.min(0, viewHeight - scaledHeight);
        float maxY = 0;

        if (posX < minX) posX = minX;
        if (posX > maxX) posX = maxX;

        if (posY < minY) posY = minY;
        if (posY > maxY) posY = maxY;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();

        canvas.translate(posX, posY);
        canvas.scale(scaleFactor, scaleFactor);

        super.dispatchDraw(canvas);

        canvas.restore();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // 터치 좌표 역변환해서 자식 뷰에 전달 (마커 클릭 위해 필수)
        MotionEvent adjustedEvent = MotionEvent.obtain(event);
        adjustedEvent.setLocation(
                (event.getX() - posX) / scaleFactor,
                (event.getY() - posY) / scaleFactor
        );
        boolean handled = super.dispatchTouchEvent(adjustedEvent);
        adjustedEvent.recycle();
        return handled;
    }
}
