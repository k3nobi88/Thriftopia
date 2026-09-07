package com.example.thriftopia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class SquareCropImageView extends View {

    private Bitmap bitmap;
    private Matrix matrix = new Matrix();

    private Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF cropRect = new RectF();

    private float scale = 1f;
    private float minScale = 1f;
    private float translateX = 0f;
    private float translateY = 0f;

    private float lastX = 0f;
    private float lastY = 0f;
    private boolean dragging = false;

    private ScaleGestureDetector scaleDetector;

    public SquareCropImageView(Context context) {
        super(context);

        overlayPaint.setColor(Color.parseColor("#AA000000"));

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(3));
        borderPaint.setColor(Color.WHITE);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        setupInitialPosition();
        invalidate();
    }

    private void setupInitialPosition() {
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        float padding = dp(24);
        float cropSize = Math.min(getWidth(), getHeight()) - padding;

        cropRect.set(
                (getWidth() - cropSize) / 2f,
                (getHeight() - cropSize) / 2f,
                (getWidth() + cropSize) / 2f,
                (getHeight() + cropSize) / 2f
        );

        minScale = Math.max(cropRect.width() / bitmap.getWidth(), cropRect.height() / bitmap.getHeight());
        scale = minScale;

        translateX = cropRect.centerX() - (bitmap.getWidth() * scale) / 2f;
        translateY = cropRect.centerY() - (bitmap.getHeight() * scale) / 2f;

        clampImage();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        setupInitialPosition();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.BLACK);

        if (bitmap == null) {
            return;
        }

        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(translateX, translateY);

        canvas.save();
        canvas.clipRect(cropRect);
        canvas.drawBitmap(bitmap, matrix, bitmapPaint);
        canvas.restore();

        canvas.drawRect(0, 0, getWidth(), cropRect.top, overlayPaint);
        canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), overlayPaint);
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint);
        canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, overlayPaint);

        canvas.drawRect(cropRect, borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                dragging = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragging && !scaleDetector.isInProgress()) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;

                    translateX += dx;
                    translateY += dy;

                    lastX = event.getX();
                    lastY = event.getY();

                    clampImage();
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
        }

        return true;
    }

    public Bitmap getCroppedBitmap(int outputSize) {
        if (bitmap == null) {
            return null;
        }

        Bitmap output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        float ratio = outputSize / cropRect.width();

        Matrix outputMatrix = new Matrix();
        outputMatrix.postScale(scale * ratio, scale * ratio);
        outputMatrix.postTranslate((translateX - cropRect.left) * ratio, (translateY - cropRect.top) * ratio);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, outputMatrix, paint);

        return output;
    }

    private void clampImage() {
        if (bitmap == null) {
            return;
        }

        float imageWidth = bitmap.getWidth() * scale;
        float imageHeight = bitmap.getHeight() * scale;

        if (imageWidth <= cropRect.width()) {
            translateX = cropRect.centerX() - imageWidth / 2f;
        } else {
            float minX = cropRect.right - imageWidth;
            float maxX = cropRect.left;

            if (translateX < minX) translateX = minX;
            if (translateX > maxX) translateX = maxX;
        }

        if (imageHeight <= cropRect.height()) {
            translateY = cropRect.centerY() - imageHeight / 2f;
        } else {
            float minY = cropRect.bottom - imageHeight;
            float maxY = cropRect.top;

            if (translateY < minY) translateY = minY;
            if (translateY > maxY) translateY = maxY;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scale;
            scale *= detector.getScaleFactor();

            if (scale < minScale) {
                scale = minScale;
            }

            if (scale > minScale * 5f) {
                scale = minScale * 5f;
            }

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            float scaleFactor = scale / oldScale;

            translateX = focusX - (focusX - translateX) * scaleFactor;
            translateY = focusY - (focusY - translateY) * scaleFactor;

            clampImage();
            invalidate();

            return true;
        }
    }
}