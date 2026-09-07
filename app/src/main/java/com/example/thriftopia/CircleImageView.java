package com.example.thriftopia;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class CircleImageView extends AppCompatImageView {

    private Path circlePath = new Path();

    public CircleImageView(Context context) {
        super(context);
        init();
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.CENTER_CROP);
    }

    @Override
    public void draw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        float radius = Math.min(width, height) / 2f;

        circlePath.reset();
        circlePath.addCircle(width / 2f, height / 2f, radius, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(circlePath);
        super.draw(canvas);
        canvas.restore();
    }
}