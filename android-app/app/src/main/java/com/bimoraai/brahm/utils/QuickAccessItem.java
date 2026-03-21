package com.bimoraai.brahm.utils;

/**
 * Data model for a single tile in the Home quick-access grid.
 *
 * @param label          Display label shown below the icon.
 * @param iconRes        Drawable resource ID for the tile icon.
 * @param targetActivity Activity class to launch when the tile is tapped.
 */
public class QuickAccessItem {

    private final String   label;
    private final int      iconRes;
    private final Class<?> targetActivity;

    public QuickAccessItem(String label, int iconRes, Class<?> targetActivity) {
        this.label          = label;
        this.iconRes        = iconRes;
        this.targetActivity = targetActivity;
    }

    public String   getLabel()          { return label; }
    public int      getIconRes()        { return iconRes; }
    public Class<?> getTargetActivity() { return targetActivity; }
}
