package io.particle.android.sdk.devicesetup.model;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.utils.ParticleSetupConstants;

/**
 * Use this class to customize the setup screens.
 * This way you can customize more then one device.
 * <p>
 * The values should only be Resources IDs, not actual objects (Strings, Colors, Drawables...)
 * By the default, all variables are initialized with the resource ids from customization.xml file
 * <p>
 * Note that you should use this class mostly to change things like deviceName, deviceImage, etc.
 * For stuff like text colors and others that probably will not change, it's advised to use the old method, overriding the customization.xml file
 */

public class DeviceCustomization implements Parcelable {

    private int brandName = R.string.brand_name;
    private int appName = R.string.app_name;
    private int deviceName = R.string.device_name;
    private int deviceImage = R.drawable.device_image;
    private int deviceImageSmall = R.drawable.device_image_small;

    private int brandImageHorizontal = R.drawable.brand_image_horizontal;
    private int brandImageVertical = R.drawable.brand_image_vertical;
    private int screenBackground = R.drawable.screen_background;
    private int brandImageBackgroundColor = R.color.brand_image_background_color;

    private int modeButtonName = R.string.mode_button_name;
    private int listenModeLedColorName = R.string.listen_mode_led_color_name;
    private int networkNamePrefix = R.string.network_name_prefix;

    private int termsOfServiceUri = R.string.terms_of_service_uri;
    private int privacyPolicyUri = R.string.privacy_policy_uri;
    private int forgotPasswordUri = R.string.forgot_password_uri;
    private int troubleshootingUri = R.string.troubleshooting_uri;

    //FIXME: The customization.xml file says: support static HTML for all these
    private int termsOfServiceHtmlFile = R.string.terms_of_service_html_file;
    private int privacyPOlicyHtmlFile = R.string.privacy_policy_html_file;
    private int getForgotPasswordHtmlFile = R.string.forgot_password_html_file;
    private int troubleshootingHtmlFile = R.string.troubleshooting_html_file;

    private int showSignUpPageFinePrint = R.bool.show_sign_up_page_fine_print;

    private int pageBackgroundColor = R.color.page_background_color;
    private int formFieldBackgroundColor = R.color.form_field_background_color;
    private int normalTextColor = R.color.normal_text_color;
    private int linkTextColor = R.color.link_text_color;
    private int linkTextBackground = R.color.link_text_bg;
    private int errorTextColor = R.color.error_text_color;

    private int elementBackgroundColor = R.color.element_background_color;
    private int elementBackgroundColorDark = R.color.element_background_color_dark;
    private int elementTextColor = R.color.element_text_color;
    private int elementTextDisabledColor = R.color.element_text_disabled_color;

    /*
    The font names below are the paths to the font you want to use, relative to the assets
        folder.

         e.g.: if you've put comic_sans.ttf in src/main/assets/fonts folder,
          then below, then the string value setted here should be like

            <string name="my_font">fonts/comic_sans.ttf</string>
     */

    private int normalTextFontName = R.string.normal_text_font_name;
    private int boldTextFontName = R.string.bold_text_font_name;
    private int italicTextFontName = R.string.italic_text_font_name;
    private int headerTextFontName = R.string.header_text_font_name;

    private int organization = R.bool.organization;
    private int organizationSlug = R.string.organization_slug;
    private int productSlug = R.string.product_slug;

    //FIXME: The customization.xml file says: apparently these values aren't customizable on the iOS app?
    private int indicatorLight = R.string.indicator_light;
    private int powerOnAction = R.string.power_on_action;


    //FIXME: The customization.xml file says: figure out how these should relate to the colors defined above
    private int primaryColor = R.color.element_background_color;
    private int primaryColorDark = R.color.primary_color_dark;
    private int accentColor = R.color.element_background_color;
    private int systemBarBackground = R.color.system_bar_bg;

    private int edittextFromElementGap = R.dimen.edittext_form_element_gap;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //Getters and Setters:

    public int getBrandName() {
        return brandName;
    }

    public void setBrandName(int brandName) {
        this.brandName = brandName;
    }

    public int getAppName() {
        return appName;
    }

    public void setAppName(int appName) {
        this.appName = appName;
    }

    public int getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(int deviceName) {
        this.deviceName = deviceName;
    }

    public int getDeviceImage() {
        return deviceImage;
    }

    public void setDeviceImage(int deviceImage) {
        this.deviceImage = deviceImage;
    }

    public int getDeviceImageSmall() {
        return deviceImageSmall;
    }

    public void setDeviceImageSmall(int deviceImageSmall) {
        this.deviceImageSmall = deviceImageSmall;
    }

    public int getBrandImageHorizontal() {
        return brandImageHorizontal;
    }

    public void setBrandImageHorizontal(int brandImageHorizontal) {
        this.brandImageHorizontal = brandImageHorizontal;
    }

    public int getBrandImageVertical() {
        return brandImageVertical;
    }

    public void setBrandImageVertical(int brandImageVertical) {
        this.brandImageVertical = brandImageVertical;
    }

    public int getScreenBackground() {
        return screenBackground;
    }

    public void setScreenBackground(int screenBackground) {
        this.screenBackground = screenBackground;
    }

    public int getBrandImageBackgroundColor() {
        return brandImageBackgroundColor;
    }

    public void setBrandImageBackgroundColor(int brandImageBackgroundColor) {
        this.brandImageBackgroundColor = brandImageBackgroundColor;
    }

    public int getModeButtonName() {
        return modeButtonName;
    }

    public void setModeButtonName(int modeButtonName) {
        this.modeButtonName = modeButtonName;
    }

    public int getListenModeLedColorName() {
        return listenModeLedColorName;
    }

    public void setListenModeLedColorName(int listenModeLedColorName) {
        this.listenModeLedColorName = listenModeLedColorName;
    }

    public int getNetworkNamePrefix() {
        return networkNamePrefix;
    }

    public void setNetworkNamePrefix(int networkNamePrefix) {
        this.networkNamePrefix = networkNamePrefix;
    }

    public int getTermsOfServiceUri() {
        return termsOfServiceUri;
    }

    public void setTermsOfServiceUri(int termsOfServiceUri) {
        this.termsOfServiceUri = termsOfServiceUri;
    }

    public int getPrivacyPolicyUri() {
        return privacyPolicyUri;
    }

    public void setPrivacyPolicyUri(int privacyPolicyUri) {
        this.privacyPolicyUri = privacyPolicyUri;
    }

    public int getForgotPasswordUri() {
        return forgotPasswordUri;
    }

    public void setForgotPasswordUri(int forgotPasswordUri) {
        this.forgotPasswordUri = forgotPasswordUri;
    }

    public int getTroubleshootingUri() {
        return troubleshootingUri;
    }

    public void setTroubleshootingUri(int troubleshootingUri) {
        this.troubleshootingUri = troubleshootingUri;
    }

    public int getTermsOfServiceHtmlFile() {
        return termsOfServiceHtmlFile;
    }

    public void setTermsOfServiceHtmlFile(int termsOfServiceHtmlFile) {
        this.termsOfServiceHtmlFile = termsOfServiceHtmlFile;
    }

    public int getPrivacyPOlicyHtmlFile() {
        return privacyPOlicyHtmlFile;
    }

    public void setPrivacyPOlicyHtmlFile(int privacyPOlicyHtmlFile) {
        this.privacyPOlicyHtmlFile = privacyPOlicyHtmlFile;
    }

    public int getGetForgotPasswordHtmlFile() {
        return getForgotPasswordHtmlFile;
    }

    public void setGetForgotPasswordHtmlFile(int getForgotPasswordHtmlFile) {
        this.getForgotPasswordHtmlFile = getForgotPasswordHtmlFile;
    }

    public int getTroubleshootingHtmlFile() {
        return troubleshootingHtmlFile;
    }

    public void setTroubleshootingHtmlFile(int troubleshootingHtmlFile) {
        this.troubleshootingHtmlFile = troubleshootingHtmlFile;
    }

    public int getShowSignUpPageFinePrint() {
        return showSignUpPageFinePrint;
    }

    public void setShowSignUpPageFinePrint(int showSignUpPageFinePrint) {
        this.showSignUpPageFinePrint = showSignUpPageFinePrint;
    }

    public int getPageBackgroundColor() {
        return pageBackgroundColor;
    }

    public void setPageBackgroundColor(int pageBackgroundColor) {
        this.pageBackgroundColor = pageBackgroundColor;
    }

    public int getFormFieldBackgroundColor() {
        return formFieldBackgroundColor;
    }

    public void setFormFieldBackgroundColor(int formFieldBackgroundColor) {
        this.formFieldBackgroundColor = formFieldBackgroundColor;
    }

    public int getNormalTextColor() {
        return normalTextColor;
    }

    public void setNormalTextColor(int normalTextColor) {
        this.normalTextColor = normalTextColor;
    }

    public int getLinkTextColor() {
        return linkTextColor;
    }

    public void setLinkTextColor(int linkTextColor) {
        this.linkTextColor = linkTextColor;
    }

    public int getLinkTextBackground() {
        return linkTextBackground;
    }

    public void setLinkTextBackground(int linkTextBackground) {
        this.linkTextBackground = linkTextBackground;
    }

    public int getErrorTextColor() {
        return errorTextColor;
    }

    public void setErrorTextColor(int errorTextColor) {
        this.errorTextColor = errorTextColor;
    }

    public int getElementBackgroundColor() {
        return elementBackgroundColor;
    }

    public void setElementBackgroundColor(int elementBackgroundColor) {
        this.elementBackgroundColor = elementBackgroundColor;
    }

    public int getElementBackgroundColorDark() {
        return elementBackgroundColorDark;
    }

    public void setElementBackgroundColorDark(int elementBackgroundColorDark) {
        this.elementBackgroundColorDark = elementBackgroundColorDark;
    }

    public int getElementTextColor() {
        return elementTextColor;
    }

    public void setElementTextColor(int elementTextColor) {
        this.elementTextColor = elementTextColor;
    }

    public int getElementTextDisabledColor() {
        return elementTextDisabledColor;
    }

    public void setElementTextDisabledColor(int elementTextDisabledColor) {
        this.elementTextDisabledColor = elementTextDisabledColor;
    }

    public int getNormalTextFontName() {
        return normalTextFontName;
    }

    public void setNormalTextFontName(int normalTextFontName) {
        this.normalTextFontName = normalTextFontName;
    }

    public int getBoldTextFontName() {
        return boldTextFontName;
    }

    public void setBoldTextFontName(int boldTextFontName) {
        this.boldTextFontName = boldTextFontName;
    }

    public int getItalicTextFontName() {
        return italicTextFontName;
    }

    public void setItalicTextFontName(int italicTextFontName) {
        this.italicTextFontName = italicTextFontName;
    }

    public int getHeaderTextFontName() {
        return headerTextFontName;
    }

    public void setHeaderTextFontName(int headerTextFontName) {
        this.headerTextFontName = headerTextFontName;
    }

    public int getOrganization() {
        return organization;
    }

    public void setOrganization(int organization) {
        this.organization = organization;
    }

    public int getOrganizationSlug() {
        return organizationSlug;
    }

    public void setOrganizationSlug(int organizationSlug) {
        this.organizationSlug = organizationSlug;
    }

    public int getProductSlug() {
        return productSlug;
    }

    public void setProductSlug(int productSlug) {
        this.productSlug = productSlug;
    }

    public int getIndicatorLight() {
        return indicatorLight;
    }

    public void setIndicatorLight(int indicatorLight) {
        this.indicatorLight = indicatorLight;
    }

    public int getPowerOnAction() {
        return powerOnAction;
    }

    public void setPowerOnAction(int powerOnAction) {
        this.powerOnAction = powerOnAction;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(int primaryColor) {
        this.primaryColor = primaryColor;
    }

    public int getPrimaryColorDark() {
        return primaryColorDark;
    }

    public void setPrimaryColorDark(int primaryColorDark) {
        this.primaryColorDark = primaryColorDark;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
    }

    public int getSystemBarBackground() {
        return systemBarBackground;
    }

    public void setSystemBarBackground(int systemBarBackground) {
        this.systemBarBackground = systemBarBackground;
    }

    public int getEdittextFromElementGap() {
        return edittextFromElementGap;
    }

    public void setEdittextFromElementGap(int edittextFromElementGap) {
        this.edittextFromElementGap = edittextFromElementGap;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //Parcelable:

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.brandName);
        dest.writeInt(this.appName);
        dest.writeInt(this.deviceName);
        dest.writeInt(this.deviceImage);
        dest.writeInt(this.deviceImageSmall);
        dest.writeInt(this.brandImageHorizontal);
        dest.writeInt(this.brandImageVertical);
        dest.writeInt(this.screenBackground);
        dest.writeInt(this.brandImageBackgroundColor);
        dest.writeInt(this.modeButtonName);
        dest.writeInt(this.listenModeLedColorName);
        dest.writeInt(this.networkNamePrefix);
        dest.writeInt(this.termsOfServiceUri);
        dest.writeInt(this.privacyPolicyUri);
        dest.writeInt(this.forgotPasswordUri);
        dest.writeInt(this.troubleshootingUri);
        dest.writeInt(this.termsOfServiceHtmlFile);
        dest.writeInt(this.privacyPOlicyHtmlFile);
        dest.writeInt(this.getForgotPasswordHtmlFile);
        dest.writeInt(this.troubleshootingHtmlFile);
        dest.writeInt(this.showSignUpPageFinePrint);
        dest.writeInt(this.pageBackgroundColor);
        dest.writeInt(this.formFieldBackgroundColor);
        dest.writeInt(this.normalTextColor);
        dest.writeInt(this.linkTextColor);
        dest.writeInt(this.linkTextBackground);
        dest.writeInt(this.errorTextColor);
        dest.writeInt(this.elementBackgroundColor);
        dest.writeInt(this.elementBackgroundColorDark);
        dest.writeInt(this.elementTextColor);
        dest.writeInt(this.elementTextDisabledColor);
        dest.writeInt(this.normalTextFontName);
        dest.writeInt(this.boldTextFontName);
        dest.writeInt(this.italicTextFontName);
        dest.writeInt(this.headerTextFontName);
        dest.writeInt(this.organization);
        dest.writeInt(this.organizationSlug);
        dest.writeInt(this.productSlug);
        dest.writeInt(this.indicatorLight);
        dest.writeInt(this.powerOnAction);
        dest.writeInt(this.primaryColor);
        dest.writeInt(this.primaryColorDark);
        dest.writeInt(this.accentColor);
        dest.writeInt(this.systemBarBackground);
        dest.writeInt(this.edittextFromElementGap);
    }

    public DeviceCustomization() {
    }

    protected DeviceCustomization(Parcel in) {
        this.brandName = in.readInt();
        this.appName = in.readInt();
        this.deviceName = in.readInt();
        this.deviceImage = in.readInt();
        this.deviceImageSmall = in.readInt();
        this.brandImageHorizontal = in.readInt();
        this.brandImageVertical = in.readInt();
        this.screenBackground = in.readInt();
        this.brandImageBackgroundColor = in.readInt();
        this.modeButtonName = in.readInt();
        this.listenModeLedColorName = in.readInt();
        this.networkNamePrefix = in.readInt();
        this.termsOfServiceUri = in.readInt();
        this.privacyPolicyUri = in.readInt();
        this.forgotPasswordUri = in.readInt();
        this.troubleshootingUri = in.readInt();
        this.termsOfServiceHtmlFile = in.readInt();
        this.privacyPOlicyHtmlFile = in.readInt();
        this.getForgotPasswordHtmlFile = in.readInt();
        this.troubleshootingHtmlFile = in.readInt();
        this.showSignUpPageFinePrint = in.readInt();
        this.pageBackgroundColor = in.readInt();
        this.formFieldBackgroundColor = in.readInt();
        this.normalTextColor = in.readInt();
        this.linkTextColor = in.readInt();
        this.linkTextBackground = in.readInt();
        this.errorTextColor = in.readInt();
        this.elementBackgroundColor = in.readInt();
        this.elementBackgroundColorDark = in.readInt();
        this.elementTextColor = in.readInt();
        this.elementTextDisabledColor = in.readInt();
        this.normalTextFontName = in.readInt();
        this.boldTextFontName = in.readInt();
        this.italicTextFontName = in.readInt();
        this.headerTextFontName = in.readInt();
        this.organization = in.readInt();
        this.organizationSlug = in.readInt();
        this.productSlug = in.readInt();
        this.indicatorLight = in.readInt();
        this.powerOnAction = in.readInt();
        this.primaryColor = in.readInt();
        this.primaryColorDark = in.readInt();
        this.accentColor = in.readInt();
        this.systemBarBackground = in.readInt();
        this.edittextFromElementGap = in.readInt();
    }

    public static final Parcelable.Creator<DeviceCustomization> CREATOR = new Parcelable.Creator<DeviceCustomization>() {
        @Override
        public DeviceCustomization createFromParcel(Parcel source) {
            return new DeviceCustomization(source);
        }

        @Override
        public DeviceCustomization[] newArray(int size) {
            return new DeviceCustomization[size];
        }
    };

    public static DeviceCustomization fromIntent(Intent intent) {
        DeviceCustomization customization = null;

        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null)
                customization = extras.getParcelable(ParticleSetupConstants.CUSTOMIZATION_TAG);
        }

        if (customization == null)
            customization = new DeviceCustomization();
        return customization;
    }
}
