package com.aselalee.bluetoothjoystick;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class JoyStickClass {
	public static final int STICK_NONE = 0;
	public static final int STICK_UP = 1;
	public static final int STICK_UPRIGHT = 2;
	public static final int STICK_RIGHT = 3;
	public static final int STICK_DOWNRIGHT = 4;
	public static final int STICK_DOWN = 5;
	public static final int STICK_DOWNLEFT = 6;
	public static final int STICK_LEFT = 7;
	public static final int STICK_UPLEFT = 8;

	public static final int DISTANCE_STEP_0 = 0;
	public static final int DISTANCE_STEP_1 = 1;
	public static final int DISTANCE_STEP_2 = 2;
	public static final int DISTANCE_STEP_3 = 3;
	public static final int DISTANCE_STEP_4 = 4;
	public static final int DISTANCE_STEP_5 = 5;

	private int OFFSET = 0;
	
	private Context mContext;
	private ViewGroup mLayout;
	private LayoutParams params;
	private int stick_width, stick_height;
	
	private int position_x = 0, position_y = 0, min_distance = 0;
	private float distance = 0, angle = 0;
	
	private DrawCanvas draw;
	private Paint paint;
	private Bitmap stick;
	
	private boolean touch_state = false;
	
	public JoyStickClass (Context context, ViewGroup layout, int stick_res_id) {
		mContext = context;

		stick = BitmapFactory.decodeResource(mContext.getResources(),
				stick_res_id);
		
        stick_width = stick.getWidth();
        stick_height = stick.getHeight();
		
        draw = new DrawCanvas(mContext);
        paint = new Paint();
		mLayout = layout;
		params = mLayout.getLayoutParams();
	}
	
	public void drawStick(MotionEvent arg1) {
		position_x = (int) (arg1.getX() - (params.width / 2));
		position_y = (int) (arg1.getY() - (params.height / 2));
	    distance = (float) Math.sqrt(Math.pow(position_x, 2) + Math.pow(position_y, 2));
	    angle = (float) cal_angle(position_x, position_y);

		if(arg1.getAction() == MotionEvent.ACTION_DOWN) {
			if(distance <= (params.width / 2) - OFFSET) {
				draw.position(arg1.getX(), arg1.getY());
				draw();
				touch_state = true;
			}
		} else if(arg1.getAction() == MotionEvent.ACTION_MOVE && touch_state) {
			if(distance <= (params.width / 2) - OFFSET) {
				draw.position(arg1.getX(), arg1.getY());
				draw();
			} else if(distance > (params.width / 2) - OFFSET){
				float x = (float) (Math.cos(Math.toRadians(cal_angle(position_x, position_y))) * ((params.width / 2) - OFFSET));
				float y = (float) (Math.sin(Math.toRadians(cal_angle(position_x, position_y))) * ((params.height / 2) - OFFSET));
				x += (params.width / 2);
				y += (params.height / 2);
				draw.position(x, y);
				draw();
			} else {
				mLayout.removeView(draw);
			}
		} else if(arg1.getAction() == MotionEvent.ACTION_UP) {
			mLayout.removeView(draw);
			touch_state = false;
		}
	}
	
	public float getDistance() {
        if (touch_state) {
            if (distance > min_distance && distance < ((params.width / 2) - OFFSET)) {
                return distance;
            }
            else if (distance >= ((params.width / 2) - OFFSET)){
                return ((params.width / 2) - OFFSET);
            }
        }
		return 0;
	}

	public int get6StepDistance() {
		int step = DISTANCE_STEP_0;
		int dist = (int) getDistance();
		int minStep = ((params.width / 2) - OFFSET) / 6;
		if (dist <= minStep) {
			step = DISTANCE_STEP_0;
		}
		else if (dist > minStep && dist <= (minStep * 2)) {
			step = DISTANCE_STEP_1;
		}
		else if (dist > (minStep * 2) && dist <= (minStep * 3)) {
			step = DISTANCE_STEP_2;
		}
		else if (dist > (minStep * 3) && dist <= (minStep * 4)) {
			step = DISTANCE_STEP_3;
		}
		else if (dist > (minStep * 4) && dist <= (minStep * 5)) {
			step = DISTANCE_STEP_4;
		}
		else { // (dist > (minStep * 5))
			step = DISTANCE_STEP_5;
		}
		return step;
	}

	public String get6StepDistanceAsString() {
		int step = get6StepDistance();
		String str = "";
		switch (step) {
			case DISTANCE_STEP_0:
				str = "Step 0";
				break;
			case DISTANCE_STEP_1:
				str = "Step 1";
				break;
			case DISTANCE_STEP_2:
				str = "Step 2";
				break;
			case DISTANCE_STEP_3:
				str = "Step 3";
				break;
			case DISTANCE_STEP_4:
				str = "Step 4";
				break;
			case DISTANCE_STEP_5:
				str = "Step 5";
				break;
		}
		return str;
	}

	public int get8Direction() {
		if(distance > min_distance && touch_state) {
			if(angle >= 247.5 && angle < 292.5 ) {
				return STICK_UP;
			} else if(angle >= 292.5 && angle < 337.5 ) {
				return STICK_UPRIGHT;
			} else if(angle >= 337.5 || angle < 22.5 ) {
				return STICK_RIGHT;
			} else if(angle >= 22.5 && angle < 67.5 ) {
				return STICK_DOWNRIGHT;
			} else if(angle >= 67.5 && angle < 112.5 ) {
				return STICK_DOWN;
			} else if(angle >= 112.5 && angle < 157.5 ) {
				return STICK_DOWNLEFT;
			} else if(angle >= 157.5 && angle < 202.5 ) {
				return STICK_LEFT;
			} else if(angle >= 202.5 && angle < 247.5 ) {
				return STICK_UPLEFT;
			}
		} else if(distance <= min_distance && touch_state) {
			return STICK_NONE;
		}
		return 0;
	}

    public void setMinimumDistance(int minDistance) {
        min_distance = minDistance;
    }

	public String get8DirectionAsSting() {
		int dir = get8Direction();
		String str = "";
		switch (dir) {
			case STICK_UP:
				str = "Up";
				break;
			case STICK_UPRIGHT:
				str = "Up Right";
				break;
			case STICK_RIGHT:
				str = "Right";
				break;
			case STICK_DOWNRIGHT:
				str = "Down Right";
				break;
			case STICK_DOWN:
				str = "Down";
				break;
			case STICK_DOWNLEFT:
				str = "Down Left";
				break;
			case STICK_LEFT:
				str = "Left";
				break;
			case STICK_UPLEFT:
				str = "Up Left";
				break;
			case STICK_NONE:
				str = "Center";
				break;
		}
		return str;
	}

	public String get8DirectionAsCmd() {
		int dir = get8Direction();
		String str = "";
		switch (dir) {
			case STICK_UP:
				str = "UU";
				break;
			case STICK_UPRIGHT:
				str = "UR";
				break;
			case STICK_RIGHT:
				str = "RR";
				break;
			case STICK_DOWNRIGHT:
				str = "DR";
				break;
			case STICK_DOWN:
				str = "DD";
				break;
			case STICK_DOWNLEFT:
				str = "DL";
				break;
			case STICK_LEFT:
				str = "LL";
				break;
			case STICK_UPLEFT:
				str = "UL";
				break;
			case STICK_NONE:
				str = "SS";
				break;
			default:
				str = "SS";
		}
		return str;
	}

	public void setOffset(int offset) {
		OFFSET = offset;
	}


	public void setStickSize(int size) {
        stick = Bitmap.createScaledBitmap(stick, size, size, false);
        stick_width = stick.getWidth();
        stick_height = stick.getHeight();
	}
	
	public void setLayoutSize(int size) {
		params.width = size;
		params.height = size;
	}
	
	private double cal_angle(float x, float y) {
		if(x >= 0 && y >= 0)
			return Math.toDegrees(Math.atan(y / x));
	    else if(x < 0 && y >= 0)
	    	return Math.toDegrees(Math.atan(y / x)) + 180;
	    else if(x < 0 && y < 0)
	    	return Math.toDegrees(Math.atan(y / x)) + 180;
	    else if(x >= 0 && y < 0) 
	    	return Math.toDegrees(Math.atan(y / x)) + 360;
		return 0;
	}
	 
	private void draw() {
		try {
			mLayout.removeView(draw);
		} catch (Exception e) { }
		mLayout.addView(draw);
	}
	 
	private class DrawCanvas extends View{
	 	float x, y;
	 	
	 	private DrawCanvas(Context mContext) {
	         super(mContext);
	     }
	     
	     public void onDraw(Canvas canvas) {
	         canvas.drawBitmap(stick, x, y, paint);
	     }
	     
	     private void position(float pos_x, float pos_y) {
	     	x = pos_x - (stick_width / 2);
	     	y = pos_y - (stick_height / 2);
	     }
	 }
}
