package com.example.thriftopia;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class BottomNavHelper {

    public static final String PAGE_HOME = "home";
    public static final String PAGE_EXPLORE = "explore";
    public static final String PAGE_MESSAGES = "messages";
    public static final String PAGE_PROFILE = "profile";
    public static final String PAGE_SELL = "sell";

    public static void setup(Activity activity, String currentPage) {
        SuspendedUserGuard.checkCurrentSession(activity);

        LinearLayout navHome = activity.findViewById(R.id.navHome);
        LinearLayout navExplore = activity.findViewById(R.id.navExplore);
        LinearLayout navSell = activity.findViewById(R.id.navSell);
        LinearLayout navMessages = activity.findViewById(R.id.navMessages);
        LinearLayout navProfile = activity.findViewById(R.id.navProfile);

        ImageView navHomeIcon = activity.findViewById(R.id.navHomeIcon);
        ImageView navExploreIcon = activity.findViewById(R.id.navExploreIcon);
        ImageView navSellIcon = activity.findViewById(R.id.navSellIcon);
        ImageView navMessagesIcon = activity.findViewById(R.id.navMessagesIcon);
        ImageView navProfileIcon = activity.findViewById(R.id.navProfileIcon);

        TextView navHomeText = activity.findViewById(R.id.navHomeText);
        TextView navExploreText = activity.findViewById(R.id.navExploreText);
        TextView navSellText = activity.findViewById(R.id.navSellText);
        TextView navMessagesText = activity.findViewById(R.id.navMessagesText);
        TextView navProfileText = activity.findViewById(R.id.navProfileText);

        if (navHome == null || navExplore == null || navSell == null || navMessages == null || navProfile == null) {
            return;
        }

        int normalColor = activity.getResources().getColor(R.color.text_secondary_light);
        int activeColor = activity.getResources().getColor(R.color.accent_lime);
        int sellColor = activity.getResources().getColor(R.color.black);

        setNavState(navHomeIcon, navHomeText, currentPage.equals(PAGE_HOME), normalColor, activeColor);
        setNavState(navExploreIcon, navExploreText, currentPage.equals(PAGE_EXPLORE), normalColor, activeColor);
        setNavState(navMessagesIcon, navMessagesText, currentPage.equals(PAGE_MESSAGES), normalColor, activeColor);
        setNavState(navProfileIcon, navProfileText, currentPage.equals(PAGE_PROFILE), normalColor, activeColor);

        if (navSellIcon != null) navSellIcon.setColorFilter(sellColor);
        if (navSellText != null) navSellText.setTextColor(sellColor);

        navHome.setOnClickListener(v -> {
            if (!currentPage.equals(PAGE_HOME)) {
                activity.startActivity(new Intent(activity, HomeActivity.class));
                activity.finish();
            }
        });

        navExplore.setOnClickListener(v -> {
            if (!currentPage.equals(PAGE_EXPLORE)) {
                activity.startActivity(new Intent(activity, ExploreActivity.class));
                activity.finish();
            }
        });

        navSell.setOnClickListener(v -> showSellQuickMenu(activity));

        navMessages.setOnClickListener(v -> {
            if (!currentPage.equals(PAGE_MESSAGES)) {
                activity.startActivity(new Intent(activity, ChatListActivity.class));
                activity.finish();
            }
        });

        navProfile.setOnClickListener(v -> {
            if (!currentPage.equals(PAGE_PROFILE)) {
                activity.startActivity(new Intent(activity, ProfileActivity.class));
                activity.finish();
            }
        });
    }

    private static void setNavState(ImageView icon, TextView text, boolean isActive, int normalColor, int activeColor) {
        int color = isActive ? activeColor : normalColor;
        if (icon != null) icon.setColorFilter(color);
        if (text != null) text.setTextColor(color);
    }

    private static void showSellQuickMenu(Activity activity) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));

        LinearLayout normalBox = createSellOption(activity, "Normally", R.drawable.ic_sell_normal);
        LinearLayout auctionBox = createSellOption(activity, "Auction", R.drawable.ic_auction_gavel);

        LinearLayout.LayoutParams normalParams = new LinearLayout.LayoutParams(
                dp(activity, 126),
                dp(activity, 58)
        );
        normalParams.setMargins(0, 0, dp(activity, 10), 0);
        normalBox.setLayoutParams(normalParams);

        LinearLayout.LayoutParams auctionParams = new LinearLayout.LayoutParams(
                dp(activity, 126),
                dp(activity, 58)
        );
        auctionBox.setLayoutParams(auctionParams);

        container.addView(normalBox);
        container.addView(auctionBox);

        PopupWindow popupWindow = new PopupWindow(
                container,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(dp(activity, 12));
        }

        normalBox.setOnClickListener(v -> {
            popupWindow.dismiss();

            Intent intent = new Intent(activity, SellItemActivity.class);
            intent.putExtra("sellMode", "normal");
            activity.startActivity(intent);
        });

        auctionBox.setOnClickListener(v -> {
            popupWindow.dismiss();

            Intent intent = new Intent(activity, SellItemActivity.class);
            intent.putExtra("sellMode", "auction");
            activity.startActivity(intent);
        });

        View rootView = activity.getWindow().getDecorView();

        popupWindow.showAtLocation(
                rootView,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
                0,
                dp(activity, 88)
        );
    }

    private static LinearLayout createSellOption(Activity activity, String label, int iconRes) {
        LinearLayout option = new LinearLayout(activity);
        option.setOrientation(LinearLayout.HORIZONTAL);
        option.setGravity(Gravity.CENTER);
        option.setBackgroundResource(R.drawable.bg_sell_quick_option);
        option.setClickable(true);
        option.setFocusable(true);
        option.setPadding(dp(activity, 12), 0, dp(activity, 12), 0);

        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        icon.setColorFilter(activity.getResources().getColor(R.color.accent_lime));

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dp(activity, 22),
                dp(activity, 22)
        );
        iconParams.setMargins(0, 0, dp(activity, 8), 0);
        icon.setLayoutParams(iconParams);

        TextView text = new TextView(activity);
        text.setText(label);
        text.setTextSize(13);
        text.setTextColor(activity.getResources().getColor(R.color.text_primary_light));
        text.setTypeface(null, Typeface.BOLD);
        text.setGravity(Gravity.CENTER);

        option.addView(icon);
        option.addView(text);

        return option;
    }

    private static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density);
    }
}