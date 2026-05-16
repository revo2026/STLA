package com.stla.domain.models;

/**
 * Country phone code model for registration country/phone selector.
 * The flag field stores the ISO country code (e.g. "EG"),
 * and getFlagEmoji() converts it to a Unicode Regional Indicator flag emoji.
 */
public class CountryPhone {
    private String name;
    private String isoCode;
    private String dialCode;
    private String phoneFormat;
    private String regex;
    private String flag; // Stores ISO code like "EG", "US"

    public CountryPhone() {}

    public CountryPhone(String name, String isoCode, String dialCode, String phoneFormat, String regex, String flag) {
        this.name = name;
        this.isoCode = isoCode;
        this.dialCode = dialCode;
        this.phoneFormat = phoneFormat;
        this.regex = regex;
        this.flag = flag;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIsoCode() { return isoCode; }
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }

    public String getDialCode() { return dialCode; }
    public void setDialCode(String dialCode) { this.dialCode = dialCode; }

    public String getPhoneFormat() { return phoneFormat; }
    public void setPhoneFormat(String phoneFormat) { this.phoneFormat = phoneFormat; }

    public String getRegex() { return regex; }
    public void setRegex(String regex) { this.regex = regex; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }

    /**
     * Convert 2-letter ISO country code to Unicode Regional Indicator flag emoji.
     * "EG" → 🇪🇬, "US" → 🇺🇸, "SA" → 🇸🇦
     * Works by converting each letter to a Regional Indicator Symbol (U+1F1E6..U+1F1FF).
     */
    public String getFlagEmoji() {
        String code = (flag != null && flag.length() == 2) ? flag.toUpperCase() : isoCode.toUpperCase();
        if (code == null || code.length() != 2) return "🏳";
        int first = 0x1F1E6 + (code.charAt(0) - 'A');
        int second = 0x1F1E6 + (code.charAt(1) - 'A');
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }

    /** Display label for ComboBox: 🇪🇬 Egypt (+20) */
    @Override
    public String toString() {
        return getFlagEmoji() + "  " + name + "  (" + dialCode + ")";
    }
}
