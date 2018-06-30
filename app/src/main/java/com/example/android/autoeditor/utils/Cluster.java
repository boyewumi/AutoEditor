package com.example.android.autoeditor.utils;

import android.os.Build;
import android.os.Handler;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.autoeditor.EditPicture;
import com.example.android.autoeditor.R;

import java.util.Locale;

public class Cluster {
    private final EditPicture activity;
    private final SeekBar seekBar;
    private TextView textView;
    private String prefix;
    private int strength = 0;

    public Cluster(EditPicture activity, int seekBarId) {
        this.activity = activity;
        seekBar = activity.findViewById(seekBarId);

        switch (seekBarId) {
            case R.id.exposure_seekbar:
                textView = activity.findViewById(R.id.exposure_label);
                prefix = activity.getResources().getString(R.string.exposure);
                break;

            case R.id.contrast_seekbar:
                textView = activity.findViewById(R.id.contrast_label);
                prefix = activity.getResources().getString(R.string.contrast);
                break;

            case R.id.sharpen_seekbar:
                textView = activity.findViewById(R.id.sharpen_label);
                prefix = activity.getResources().getString(R.string.sharpness);
                break;

            case R.id.saturation_seekbar:
                textView = activity.findViewById(R.id.saturation_label);
                prefix = activity.getResources().getString(R.string.saturation);
                break;
        }

        initSeekbar();
    }

    public SeekBar getSeekBar() {
        return seekBar;
    }

    public TextView getTextView() {
        return textView;
    }

    public int getStrength() {
        return strength;
    }

    private void updatePreview() {
        activity.updatePreview();
    }

    private void initSeekbar() {
        seekBar.setMax(200);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean inDoubleTapWindow = false;
            int initialProgress = 0;
            Handler handler = new Handler();

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                strength = progress - 100;

                updateLabel(prefix, strength);
                updatePreview();
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

                if(inDoubleTapWindow && Math.abs(initialProgress - seekBar.getProgress()) <= 10) {
                    reset();
                    return;
                }

                inDoubleTapWindow = true;

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        inDoubleTapWindow = false;
                        initialProgress = seekBar.getProgress();
                    }
                }, 250);
            }

            private void reset() {
                initialProgress = 0;
                inDoubleTapWindow = false;
                strength = 0;

                resetCluster();
                updateLabel(prefix, strength);
            }
        });

        resetCluster();
    }

    private void resetCluster() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            seekBar.setProgress(100, true);
        } else {
            seekBar.setProgress(100);
        }

        updatePreview();
    }

    private void updateLabel(String prefix, int strength) {
        textView.setText(String.format(Locale.getDefault(), prefix + " " + "%d", strength));
    }

    public interface OnFilterAdjustment {
        void updatePreview();
    }
}