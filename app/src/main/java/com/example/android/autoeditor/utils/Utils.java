package com.example.android.autoeditor.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlend;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static final String PREFERENCES_FILE = "PREFS";
    public static final String OVERWRITE_FLAG = "should_overwrite";
    public static final int CONTRAST_FILTER = 0;
    public static final int EXPOSURE_FILTER = 1;
    public static final int SATURATION_FILTER = 2;
    public static final int UNSHARP_MASK_SHARPEN = 3;
    public static final int CONVOLUTION_SHARPEN = 4;

    private static ColorMatrix contrastCm = new ColorMatrix();
    private static ColorMatrix exposureCm = new ColorMatrix();
    private static float saturation = 1;

    public static void requestMissingPermissions(Activity ctx) {
        List<String> permissionList = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {//granted perm int = 0
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissionList.isEmpty()) {
            requestPermissions(ctx, permissionList);
        }
    }

    private static void requestPermissions(Activity ctx, List<String> permissions) {
        ActivityCompat.requestPermissions(ctx, permissions.toArray(new String[permissions.size()]), 0);
    }

    public static void requestPermission(Activity ctx, String permission) {
        ActivityCompat.requestPermissions(ctx, new String[] {permission}, 0);
    }

    public static boolean allPermissionsGranted(Activity ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public static Drawable tintMyDrawable(Drawable drawable, int color) {
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, color);
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    public static String readSharedSetting(Context ctx, String settingName, String defaultValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(settingName, defaultValue);
    }

    public static boolean readSharedSetting(Context ctx, String settingName, boolean defaultValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(settingName, defaultValue);
    }

    public static void saveSharedSetting(Context ctx, String settingName, String settingValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(settingName, settingValue);
        editor.apply();
    }

    public static void saveSharedSetting(Context ctx, String settingName, boolean settingValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(settingName, settingValue);
        editor.apply();
    }

    public static Bitmap applyFilter(Context ctx, Bitmap bmp, float value, int filter){

        ColorMatrix cm = new ColorMatrix();

        switch (filter) {
            case CONTRAST_FILTER:
                value /= 3f;
                value = value < 0 ? value / 2 : value;
                float contrast = (float) Math.pow((100 + value) / 100, 2);
                float brightness = 127.5f * (1 - contrast);

                contrastCm.set(new float[]
                        {
                                contrast, 0, 0, 0, brightness,
                                0, contrast, 0, 0, brightness,
                                0, 0, contrast, 0, brightness,
                                0, 0, 0, 1, 0
                        });
                break;

            case EXPOSURE_FILTER:

                value = (float) Math.pow(2f, value/100*3);
                exposureCm.set(new float[]
                        {
                                value, 0, 0, 0, 0,
                                0, value, 0, 0, 0,
                                0, 0, value, 0, 0,
                                0, 0, 0, 1, 0
                        });
                break;

            case SATURATION_FILTER:

                value = value > 0 ?
                        (float) Math.pow(1.01, value) : (value + 100)/100;

                saturation = value;
                break;

            case UNSHARP_MASK_SHARPEN:

                return unsharpMask(bmp, value, ctx);

            case CONVOLUTION_SHARPEN:

                return convolutionSharpen(bmp, value, ctx);
        }

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        cm.setConcat(contrastCm, exposureCm);
        cm.setSaturation(saturation);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }

    public Bitmap changeBitmapBrightness(Bitmap bmp, float value)
    {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        1, 0, 0, 0, value,
                        0, 1, 0, 0, value,
                        0, 0, 1, 0, value,
                        0, 0, 0, 1, 0,
                        0, 0, 0, 0, 1
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        RectF drawRect=new RectF();
        drawRect.set(0,0,bmp.getWidth(),bmp.getHeight());

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, null, drawRect, paint);

        return ret;
    }

    private static Bitmap convolutionSharpen(Bitmap original, float progress, Context ctx) {

        if(progress < 0) {
            return blurBitmap(original, progress/-4, ctx);
        }

        float d = progress/100f;
        float o = d *  (float) Math.sqrt(2);
        float c = (o * 4) + (d * 4) + 1;

        float[] radius = new float[] {
                -d, -o, -d,
                -o, c, -o,
                -d, -o, -d
        };

        Bitmap bitmap = Bitmap.createBitmap(
                original.getWidth(), original.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript rs = RenderScript.create(ctx);

        Allocation allocIn = Allocation.createFromBitmap(rs, original);
        Allocation allocOut = Allocation.createFromBitmap(rs, bitmap);

        ScriptIntrinsicConvolve3x3 convolution
                = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        convolution.setInput(allocIn);
        convolution.setCoefficients(radius);

        convolution.forEach(allocOut);
        allocOut.copyTo(bitmap);
        rs.destroy();

        return bitmap;

    }

    private static Bitmap unsharpMask(Bitmap bmp, float value, Context ctx) {
        float radius = Math.abs(value)/4f;
        Bitmap blurred = blurBitmap(bmp, radius, ctx);

        if(value < 0) {
            return blurred;
        }

        Bitmap sub = subtractBitmaps(bmp, blurred, ctx);

        return addBitmaps(bmp, sub, ctx);
    }

    private static Bitmap addBitmaps(Bitmap orig, Bitmap blurred, Context context) {
        Bitmap out = orig.copy(orig.getConfig(), true);
        Bitmap blur = blurred.copy(blurred.getConfig(), true);
        //Create renderscript
        RenderScript rs = RenderScript.create(context);

        //Create allocation from Bitmap
        Allocation origAlloc = Allocation.createFromBitmap(rs, out);
        Allocation blurAlloc = Allocation.createFromBitmap(rs, blur);

        //Create script
        ScriptIntrinsicBlend blendScript = ScriptIntrinsicBlend.create(rs, Element.U8_4(rs));
        blendScript.forEachAdd(blurAlloc, origAlloc);
        //now add to orig

        //Copy script result into bitmap
        origAlloc.copyTo(out);

        //Destroy everything to free memory
        origAlloc.destroy();
        blurAlloc.destroy();
        blendScript.destroy();
        rs.destroy();
        return out;
    }

    private static Bitmap subtractBitmaps(Bitmap orig, Bitmap blurred, Context context) {
        Bitmap out = orig.copy(orig.getConfig(), true);
        Bitmap blur = blurred.copy(blurred.getConfig(), true);
        //Create renderscript
        RenderScript rs = RenderScript.create(context);

        //Create allocation from Bitmap
        Allocation origAlloc = Allocation.createFromBitmap(rs, out);
        Allocation blurAlloc = Allocation.createFromBitmap(rs, blur);

        //Create script
        ScriptIntrinsicBlend blendScript = ScriptIntrinsicBlend.create(rs, Element.U8_4(rs));
        blendScript.forEachSubtract(blurAlloc, origAlloc);
        //now add to orig

        //Copy script result into bitmap
        origAlloc.copyTo(out);

        //Destroy everything to free memory
        origAlloc.destroy();
        blurAlloc.destroy();
        blendScript.destroy();
        rs.destroy();
        return out;
    }

    private static Bitmap blurBitmap(Bitmap bitmap, float radius, Context context) {
        if (radius == 0) {
            return bitmap;
        }

        Bitmap out = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        //Create renderscript
        RenderScript rs = RenderScript.create(context);

        //Create allocation from Bitmap
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);

        Allocation outAlloc = Allocation.createFromBitmap(rs, out);

        //Create script
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        //Set blur radius (maximum 25.0)
        blurScript.setRadius(radius);
        //Set input for script
        blurScript.setInput(allocation);
        //Call script for output allocation
        blurScript.forEach(outAlloc);

        //Copy script result into bitmap
        outAlloc.copyTo(out);

        //Destroy everything to free memory
        allocation.destroy();
        outAlloc.destroy();
        blurScript.destroy();
        rs.destroy();
        return out;
    }
}