package com.stla.services;

import com.stla.domain.models.CountryPhone;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads country phone data from JSON resource and provides auto-detection helpers.
 */
public class CountryPhoneLoader {

    private static List<CountryPhone> countries;

    public static List<CountryPhone> loadCountries() {
        if (countries != null) return countries;
        countries = new ArrayList<>();

        try (InputStream is = CountryPhoneLoader.class.getResourceAsStream("/com/stla/data/countries_phone_codes.json")) {
            if (is == null) {
                System.err.println("countries_phone_codes.json not found!");
                return countries;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Pattern handles curly braces inside quoted strings (e.g. regex: "^[0-9]{10,11}$")
            Pattern objPattern = Pattern.compile("\\{(?:[^{}\"]*|\"(?:[^\"\\\\]|\\\\.)*\")*\\}");
            Matcher m = objPattern.matcher(json);
            while (m.find()) {
                String obj = m.group();
                CountryPhone cp = new CountryPhone();
                cp.setName(extractField(obj, "name"));
                cp.setIsoCode(extractField(obj, "isoCode"));
                cp.setDialCode(extractField(obj, "dialCode"));
                cp.setPhoneFormat(extractField(obj, "phoneFormat"));
                cp.setRegex(extractField(obj, "regex"));
                cp.setFlag(extractField(obj, "flag"));
                countries.add(cp);
            }
        } catch (Exception e) {
            System.err.println("Error loading countries: " + e.getMessage());
        }

        return countries;
    }

    private static String extractField(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    /**
     * Validate a phone number against a country's regex pattern.
     * Strips spaces, dashes, parentheses, dots before validation.
     */
    public static boolean validatePhone(CountryPhone country, String phoneNumber) {
        if (country == null || phoneNumber == null) return false;
        String cleaned = phoneNumber.replaceAll("[\\s\\-().]", "");
        if (cleaned.isEmpty()) return false;
        try {
            return cleaned.matches(country.getRegex());
        } catch (Exception e) {
            return cleaned.matches("^[0-9]{6,15}$");
        }
    }

    /**
     * Auto-detect default country from system timezone.
     */
    public static CountryPhone detectDefaultCountry() {
        List<CountryPhone> all = loadCountries();
        String tzId = TimeZone.getDefault().getID();
        String isoGuess = guessIsoFromTimezone(tzId);
        for (CountryPhone cp : all) {
            if (cp.getIsoCode().equalsIgnoreCase(isoGuess)) return cp;
        }
        for (CountryPhone cp : all) {
            if ("US".equals(cp.getIsoCode())) return cp;
        }
        return all.isEmpty() ? null : all.get(0);
    }

    private static String guessIsoFromTimezone(String tzId) {
        if (tzId == null) return "US";
        String lower = tzId.toLowerCase();
        if (lower.contains("riyadh") || lower.contains("arabia")) return "SA";
        if (lower.contains("cairo")) return "EG";
        if (lower.contains("dubai") || lower.contains("muscat")) return "AE";
        if (lower.contains("amman")) return "JO";
        if (lower.contains("baghdad")) return "IQ";
        if (lower.contains("beirut")) return "LB";
        if (lower.contains("london")) return "GB";
        if (lower.contains("paris")) return "FR";
        if (lower.contains("berlin")) return "DE";
        if (lower.contains("istanbul")) return "TR";
        if (lower.contains("tokyo")) return "JP";
        if (lower.contains("shanghai") || lower.contains("hong_kong")) return "CN";
        if (lower.contains("kolkata") || lower.contains("calcutta")) return "IN";
        if (lower.contains("sydney") || lower.contains("melbourne")) return "AU";
        if (lower.contains("sao_paulo")) return "BR";
        if (lower.contains("new_york") || lower.contains("chicago") || lower.contains("los_angeles")) return "US";
        if (lower.contains("toronto")) return "CA";
        if (lower.contains("moscow")) return "RU";
        if (lower.startsWith("asia/")) return "SA";
        if (lower.startsWith("europe/")) return "GB";
        if (lower.startsWith("america/")) return "US";
        if (lower.startsWith("africa/")) return "EG";
        return "US";
    }
}
