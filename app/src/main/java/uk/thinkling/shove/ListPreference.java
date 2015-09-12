package uk.thinkling.shove;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;

/**
 * SIMPLES
 * Created by ergo on 05/07/2015.
 */
public class ListPreference extends android.preference.ListPreference {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public ListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListPreference(Context context) {
        super(context);
    }

    @Override
    public void setValue(String value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            super.setValue(value);
        } else {
            String oldValue = getValue();
            super.setValue(value);
            if (!TextUtils.equals(value, oldValue)) {
                notifyChanged();
            }
        }
    }
}
