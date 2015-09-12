package uk.thinkling.moonshot;

import android.content.Context;
import android.util.AttributeSet;

/**
 * SIMPLES
 * Created by ergo on 05/07/2015. Just to format the title and summary
 */
public class EditTextPreference extends android.preference.EditTextPreference {
    public EditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public EditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextPreference(Context context) {
        super(context);
    }

    @Override
    public CharSequence getSummary() {
        // Allow %s formatting string to be used in summary - should check for non-null
        final CharSequence value = getText();
        final CharSequence summary = super.getSummary();
        if (summary == null || value == null) {
             return null;
        } else {
            return String.format(summary.toString(), value);
        }
    }

    @Override
    public CharSequence getTitle() {
        String title = super.getTitle().toString();
        // Allow %s formatting string to be used in title - should check for non-null as above
        return String.format(title, getText());
    }

    @Override
    public void setText(String value) {
        super.setText(value);
        notifyChanged();
    }

}
