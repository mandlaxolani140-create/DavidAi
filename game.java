package com.example.cargame;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.RelativeLayout.LayoutParams;

public class MainActivity extends Activity {

    ImageView car;
    float x, y, angle = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MAIN LAYOUT (Background)
        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundColor(Color.parseColor("#2E7D32"));

        // CAR IMAGE
        car = new ImageView(this);
        car.setImageResource(R.drawable.car); // put your car.png in res/drawable
        LayoutParams carParams = new LayoutParams(200, 100);
        carParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(car, carParams);

        // CONTROL BUTTONS
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.CENTER);
        LayoutParams buttonParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonParams.setMargins(0, 0, 0, 50);

        // LEFT BUTTON
        Button left = new Button(this);
        left.setText("←");
        buttonLayout.addView(left);

        // UP & DOWN in vertical layout
        LinearLayout vertical = new LinearLayout(this);
        vertical.setOrientation(LinearLayout.VERTICAL);
        vertical.setGravity(Gravity.CENTER);
        Button up = new Button(this);
        up.setText("↑");
        Button down = new Button(this);
        down.setText("↓");
        vertical.addView(up);
        vertical.addView(down);
        buttonLayout.addView(vertical);

        // RIGHT BUTTON
        Button right = new Button(this);
        right.setText("→");
        buttonLayout.addView(right);

        layout.addView(buttonLayout, buttonParams);
        setContentView(layout);

        // INITIAL POSITION
        layout.post(() -> {
            x = car.getX();
            y = car.getY();
        });

        // MOVEMENT LOGIC
        up.setOnClickListener(v -> moveCar(10));
        down.setOnClickListener(v -> moveCar(-10));
        left.setOnClickListener(v -> rotateCar(-15));
        right.setOnClickListener(v -> rotateCar(15));
    }

    private void moveCar(float distance) {
        float rad = (float) Math.toRadians(angle);
        x += (float) Math.cos(rad) * distance;
        y += (float) Math.sin(rad) * distance;
        car.setX(x);
        car.setY(y);
    }

    private void rotateCar(float degrees) {
        angle += degrees;
        car.setRotation(angle);
    }
}