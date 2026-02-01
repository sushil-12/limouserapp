package com.example.limouserapp.domain.validation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phone number validation service
 * Handles validation of phone numbers with country-specific rules
 */
@Singleton
class PhoneValidationService @Inject constructor() {
    
    /**
     * Validate phone number based on country code
     * Returns smart, contextual error messages
     */
    fun validatePhoneNumber(phoneNumber: String, countryCode: CountryCode): ValidationResult {
        // Clean the phone number - remove all non-digit characters
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        
        return when {
            cleanNumber.isEmpty() -> ValidationResult.Error("Please enter your phone number")
            
            // Check for non-digit characters (shouldn't happen after cleaning, but double-check)
            !cleanNumber.all { it.isDigit() } -> ValidationResult.Error("Phone number should only contain numbers")
            
            // Check length - provide helpful messages based on how close they are
            cleanNumber.length < countryCode.phoneLength -> {
                val digitsNeeded = countryCode.phoneLength - cleanNumber.length
                ValidationResult.Error(
                    if (digitsNeeded == 1) {
                        "Please add 1 more digit"
                    } else {
                        "Please add $digitsNeeded more digits"
                    }
                )
            }
            
            cleanNumber.length > countryCode.phoneLength -> ValidationResult.Error(
                "Phone number should be ${countryCode.phoneLength} digits. Remove ${cleanNumber.length - countryCode.phoneLength} digit${if (cleanNumber.length - countryCode.phoneLength > 1) "s" else ""}"
            )
            
            // Check format validity
            !isValidPhoneFormat(cleanNumber, countryCode) -> {
                when (countryCode) {
                    CountryCode.US, CountryCode.CA -> ValidationResult.Error(
                        "Invalid phone number format. US/Canada numbers should start with 2-9"
                    )
                    CountryCode.UK -> ValidationResult.Error(
                        "Invalid phone number format. UK numbers should start with 1-9"
                    )
                    else -> ValidationResult.Error("Invalid phone number format")
                }
            }
            
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Format phone number for display
     */
    fun formatPhoneNumber(phoneNumber: String, countryCode: CountryCode): String {
        // Clean the phone number - remove all non-digit characters
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        
        return when (countryCode) {
            CountryCode.US -> {
                when {
                    cleanNumber.length == 10 -> "(${cleanNumber.substring(0, 3)}) ${cleanNumber.substring(3, 6)}-${cleanNumber.substring(6)}"
                    cleanNumber.length > 10 -> cleanNumber.substring(0, 10).let { 
                        "(${it.substring(0, 3)}) ${it.substring(3, 6)}-${it.substring(6)}"
                    }
                    else -> cleanNumber
                }
            }
            CountryCode.UK -> {
                when {
                    cleanNumber.length == 10 -> "${cleanNumber.substring(0, 4)} ${cleanNumber.substring(4, 7)} ${cleanNumber.substring(7)}"
                    cleanNumber.length > 10 -> cleanNumber.substring(0, 10).let {
                        "${it.substring(0, 4)} ${it.substring(4, 7)} ${it.substring(7)}"
                    }
                    else -> cleanNumber
                }
            }
            else -> cleanNumber
        }
    }
    
    /**
     * Check if phone number format is valid for the country
     */
    private fun isValidPhoneFormat(phoneNumber: String, countryCode: CountryCode): Boolean {
        return when (countryCode) {
            CountryCode.US -> phoneNumber.matches(Regex("^[2-9]\\d{2}[2-9]\\d{2}\\d{4}$"))
            CountryCode.UK -> phoneNumber.matches(Regex("^[1-9]\\d{9}$"))
            CountryCode.CA -> phoneNumber.matches(Regex("^[2-9]\\d{2}[2-9]\\d{2}\\d{4}$"))
            else -> true // For other countries, basic length check is sufficient
        }
    }
}

/**
 * Country code enumeration with phone number specifications
 * Expanded to include all countries from Countries.list for proper country selection
 */
enum class CountryCode(
    val code: String,
    val shortCode: String,
    val displayName: String,
    val phoneLength: Int
) {
    US("+1", "us", "United States", 10),
    AF("+93", "af", "Afghanistan", 9),
    AX("+358", "ax", "Åland Islands", 10),
    AL("+355", "al", "Albania", 9),
    DZ("+213", "dz", "Algeria", 9),
    AS("+1", "as", "American Samoa", 7),
    AD("+376", "ad", "Andorra", 6),
    AO("+244", "ao", "Angola", 9),
    AI("+1", "ai", "Anguilla", 10),
    AQ("+672", "aq", "Antarctica", 6),
    AG("+1", "ag", "Antigua and Barbuda", 10),
    AR("+54", "ar", "Argentina", 10),
    AM("+374", "am", "Armenia", 8),
    AW("+297", "aw", "Aruba", 7),
    AU("+61", "au", "Australia", 9),
    AT("+43", "at", "Austria", 10),
    AZ("+994", "az", "Azerbaijan", 9),
    BS("+1", "bs", "Bahamas", 10),
    BH("+973", "bh", "Bahrain", 8),
    BD("+880", "bd", "Bangladesh", 10),
    BB("+1", "bb", "Barbados", 10),
    BY("+375", "by", "Belarus", 9),
    BE("+32", "be", "Belgium", 9),
    BZ("+501", "bz", "Belize", 7),
    BJ("+229", "bj", "Benin", 8),
    BM("+1", "bm", "Bermuda", 10),
    BT("+975", "bt", "Bhutan", 8),
    BO("+591", "bo", "Bolivia", 8),
    BA("+387", "ba", "Bosnia and Herzegovina", 8),
    BW("+267", "bw", "Botswana", 7),
    BR("+55", "br", "Brazil", 10),
    VG("+1", "vg", "British Virgin Islands", 10),
    BN("+673", "bn", "Brunei Darussalam", 7),
    BG("+359", "bg", "Bulgaria", 9),
    BF("+226", "bf", "Burkina Faso", 8),
    BI("+257", "bi", "Burundi", 8),
    KH("+855", "kh", "Cambodia", 9),
    CM("+237", "cm", "Cameroon", 9),
    CA("+1", "ca", "Canada", 10),
    CV("+238", "cv", "Cape Verde", 7),
    KY("+1", "ky", "Cayman Islands", 10),
    CF("+236", "cf", "Central African Republic", 8),
    TD("+235", "td", "Chad", 8),
    CL("+56", "cl", "Chile", 9),
    CN("+86", "cn", "China", 11),
    CX("+61", "cx", "Christmas Island", 9),
    CC("+61", "cc", "Cocos (Keeling) Islands", 9),
    CO("+57", "co", "Colombia", 10),
    KM("+269", "km", "Comoros", 7),
    CG("+242", "cg", "Congo", 9),
    CD("+243", "cd", "Congo, Democratic Republic of the", 9),
    CK("+682", "ck", "Cook Islands", 5),
    CR("+506", "cr", "Costa Rica", 8),
    CI("+225", "ci", "Côte d'Ivoire", 8),
    HR("+385", "hr", "Croatia", 9),
    CU("+53", "cu", "Cuba", 8),
    CW("+599", "cw", "Curaçao", 7),
    CY("+357", "cy", "Cyprus", 8),
    CZ("+420", "cz", "Czech Republic", 9),
    DK("+45", "dk", "Denmark", 8),
    DJ("+253", "dj", "Djibouti", 8),
    DM("+1", "dm", "Dominica", 10),
    DO("+1", "do", "Dominican Republic", 10),
    EC("+593", "ec", "Ecuador", 9),
    EG("+20", "eg", "Egypt", 10),
    SV("+503", "sv", "El Salvador", 8),
    GQ("+240", "gq", "Equatorial Guinea", 9),
    ER("+291", "er", "Eritrea", 7),
    EE("+372", "ee", "Estonia", 8),
    SZ("+268", "sz", "Eswatini", 8),
    ET("+251", "et", "Ethiopia", 9),
    FK("+500", "fk", "Falkland Islands", 5),
    FO("+298", "fo", "Faroe Islands", 6),
    FJ("+679", "fj", "Fiji", 7),
    FI("+358", "fi", "Finland", 10),
    FR("+33", "fr", "France", 9),
    GF("+594", "gf", "French Guiana", 9),
    PF("+689", "pf", "French Polynesia", 8),
    GA("+241", "ga", "Gabon", 8),
    GM("+220", "gm", "Gambia", 7),
    GE("+995", "ge", "Georgia", 9),
    DE("+49", "de", "Germany", 11),
    GH("+233", "gh", "Ghana", 9),
    GI("+350", "gi", "Gibraltar", 8),
    GR("+30", "gr", "Greece", 10),
    GL("+299", "gl", "Greenland", 6),
    GD("+1", "gd", "Grenada", 10),
    GP("+590", "gp", "Guadeloupe", 9),
    GU("+1", "gu", "Guam", 10),
    GT("+502", "gt", "Guatemala", 8),
    GG("+44", "gg", "Guernsey", 10),
    GN("+224", "gn", "Guinea", 9),
    GW("+245", "gw", "Guinea-Bissau", 7),
    GY("+592", "gy", "Guyana", 7),
    HT("+509", "ht", "Haiti", 8),
    HN("+504", "hn", "Honduras", 8),
    HK("+852", "hk", "Hong Kong", 8),
    HU("+36", "hu", "Hungary", 9),
    IS("+354", "is", "Iceland", 7),
    IN("+91", "in", "India", 10),
    ID("+62", "id", "Indonesia", 10),
    IR("+98", "ir", "Iran", 10),
    IQ("+964", "iq", "Iraq", 10),
    IE("+353", "ie", "Ireland", 9),
    IM("+44", "im", "Isle of Man", 10),
    IL("+972", "il", "Israel", 9),
    IT("+39", "it", "Italy", 10),
    JM("+1", "jm", "Jamaica", 10),
    JP("+81", "jp", "Japan", 10),
    JE("+44", "je", "Jersey", 10),
    JO("+962", "jo", "Jordan", 9),
    KZ("+7", "kz", "Kazakhstan", 10),
    KE("+254", "ke", "Kenya", 10),
    KI("+686", "ki", "Kiribati", 8),
    KW("+965", "kw", "Kuwait", 8),
    KG("+996", "kg", "Kyrgyzstan", 9),
    LA("+856", "la", "Laos", 10),
    LV("+371", "lv", "Latvia", 8),
    LB("+961", "lb", "Lebanon", 8),
    LS("+266", "ls", "Lesotho", 8),
    LR("+231", "lr", "Liberia", 8),
    LY("+218", "ly", "Libya", 9),
    LI("+423", "li", "Liechtenstein", 7),
    LT("+370", "lt", "Lithuania", 8),
    LU("+352", "lu", "Luxembourg", 9),
    MO("+853", "mo", "Macao", 8),
    MK("+389", "mk", "North Macedonia", 8),
    MG("+261", "mg", "Madagascar", 9),
    MW("+265", "mw", "Malawi", 9),
    MY("+60", "my", "Malaysia", 9),
    MV("+960", "mv", "Maldives", 7),
    ML("+223", "ml", "Mali", 8),
    MT("+356", "mt", "Malta", 8),
    MH("+692", "mh", "Marshall Islands", 7),
    MQ("+596", "mq", "Martinique", 9),
    MR("+222", "mr", "Mauritania", 8),
    MU("+230", "mu", "Mauritius", 8),
    YT("+262", "yt", "Mayotte", 9),
    MX("+52", "mx", "Mexico", 10),
    FM("+691", "fm", "Micronesia", 7),
    MD("+373", "md", "Moldova", 8),
    MC("+377", "mc", "Monaco", 8),
    MN("+976", "mn", "Mongolia", 8),
    ME("+382", "me", "Montenegro", 8),
    MS("+1", "ms", "Montserrat", 10),
    MA("+212", "ma", "Morocco", 9),
    MZ("+258", "mz", "Mozambique", 9),
    MM("+95", "mm", "Myanmar", 9),
    NA("+264", "na", "Namibia", 9),
    NR("+674", "nr", "Nauru", 7),
    NP("+977", "np", "Nepal", 10),
    NL("+31", "nl", "Netherlands", 9),
    NC("+687", "nc", "New Caledonia", 6),
    NZ("+64", "nz", "New Zealand", 9),
    NI("+505", "ni", "Nicaragua", 8),
    NE("+227", "ne", "Niger", 8),
    NG("+234", "ng", "Nigeria", 10),
    NU("+683", "nu", "Niue", 4),
    NF("+672", "nf", "Norfolk Island", 6),
    KP("+850", "kp", "North Korea", 9),
    MP("+1", "mp", "Northern Mariana Islands", 10),
    NO("+47", "no", "Norway", 8),
    OM("+968", "om", "Oman", 8),
    PK("+92", "pk", "Pakistan", 10),
    PW("+680", "pw", "Palau", 7),
    PS("+970", "ps", "Palestine", 9),
    PA("+507", "pa", "Panama", 8),
    PG("+675", "pg", "Papua New Guinea", 8),
    PY("+595", "py", "Paraguay", 9),
    PE("+51", "pe", "Peru", 9),
    PH("+63", "ph", "Philippines", 10),
    PL("+48", "pl", "Poland", 9),
    PT("+351", "pt", "Portugal", 9),
    PR("+1", "pr", "Puerto Rico", 10),
    QA("+974", "qa", "Qatar", 8),
    RE("+262", "re", "Réunion", 9),
    RO("+40", "ro", "Romania", 10),
    RU("+7", "ru", "Russia", 10),
    RW("+250", "rw", "Rwanda", 9),
    BL("+590", "bl", "Saint Barthélemy", 9),
    SH("+290", "sh", "Saint Helena", 4),
    KN("+1", "kn", "Saint Kitts and Nevis", 10),
    LC("+1", "lc", "Saint Lucia", 10),
    MF("+590", "mf", "Saint Martin (French part)", 9),
    PM("+508", "pm", "Saint Pierre and Miquelon", 6),
    VC("+1", "vc", "Saint Vincent and the Grenadines", 10),
    WS("+685", "ws", "Samoa", 7),
    SM("+378", "sm", "San Marino", 8),
    ST("+239", "st", "Sao Tome and Principe", 7),
    SA("+966", "sa", "Saudi Arabia", 9),
    SN("+221", "sn", "Senegal", 9),
    RS("+381", "rs", "Serbia", 9),
    SC("+248", "sc", "Seychelles", 7),
    SL("+232", "sl", "Sierra Leone", 8),
    SG("+65", "sg", "Singapore", 8),
    SX("+1", "sx", "Sint Maarten", 10),
    SK("+421", "sk", "Slovakia", 9),
    SI("+386", "si", "Slovenia", 8),
    SB("+677", "sb", "Solomon Islands", 7),
    SO("+252", "so", "Somalia", 8),
    ZA("+27", "za", "South Africa", 9),
    KR("+82", "kr", "South Korea", 10),
    SS("+211", "ss", "South Sudan", 9),
    ES("+34", "es", "Spain", 9),
    LK("+94", "lk", "Sri Lanka", 9),
    SD("+249", "sd", "Sudan", 9),
    SR("+597", "sr", "Suriname", 7),
    SE("+46", "se", "Sweden", 9),
    CH("+41", "ch", "Switzerland", 9),
    SY("+963", "sy", "Syria", 9),
    TW("+886", "tw", "Taiwan", 9),
    TJ("+992", "tj", "Tajikistan", 9),
    TZ("+255", "tz", "Tanzania", 9),
    TH("+66", "th", "Thailand", 9),
    TL("+670", "tl", "Timor-Leste", 7),
    TG("+228", "tg", "Togo", 8),
    TK("+690", "tk", "Tokelau", 4),
    TO("+676", "to", "Tonga", 5),
    TT("+1", "tt", "Trinidad and Tobago", 10),
    TN("+216", "tn", "Tunisia", 8),
    TR("+90", "tr", "Turkey", 10),
    TM("+993", "tm", "Turkmenistan", 8),
    TC("+1", "tc", "Turks and Caicos Islands", 10),
    TV("+688", "tv", "Tuvalu", 5),
    UG("+256", "ug", "Uganda", 9),
    UA("+380", "ua", "Ukraine", 9),
    AE("+971", "ae", "United Arab Emirates", 9),
    GB("+44", "gb", "United Kingdom", 10),
    UK("+44", "uk", "United Kingdom", 10), // Alias for GB
    UM("+1", "um", "United States Minor Outlying Islands", 10),
    UY("+598", "uy", "Uruguay", 9),
    UZ("+998", "uz", "Uzbekistan", 9),
    VU("+678", "vu", "Vanuatu", 7),
    VA("+379", "va", "Vatican City", 6),
    VE("+58", "ve", "Venezuela", 10),
    VN("+84", "vn", "Vietnam", 9),
    VI("+1", "vi", "U.S. Virgin Islands", 10),
    WF("+681", "wf", "Wallis and Futuna", 6),
    YE("+967", "ye", "Yemen", 9),
    ZM("+260", "zm", "Zambia", 9),
    ZW("+263", "zw", "Zimbabwe", 9)
}
