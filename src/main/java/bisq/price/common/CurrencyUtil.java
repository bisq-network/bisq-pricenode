/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.price.common;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class CurrencyUtil {
    public static final Set<String> ALL_CRYPTO_CURRENCIES = Set.of(
            "ACM", "ADE", "AEON", "AMIT", "ANI", "ARQ", "ASK", "AEUR", "AUS", "BSQ",
            "BEAM", "DARX", "BTM", "BZC", "BLUR", "BLK-BURNT", "CRCL", "CTSC", "CASH2", "CHA",
            "CLOAK", "XCP", "CRDS", "CROAT", "DST", "DAI", "D4RK", "DASH", "DCR", "ONION",
            "DXO", "DOGE", "DOI", "DONU", "DRGL", "EMC", "ERG", "ETH", "ETC", "ETHS",
            "FAIR", "FRTY", "FJC", "GALI", "GMCN", "GENX", "GRIN", "HATCH", "HLM", "ZEN",
            "IDA", "IRD", "KEK", "KYDC", "KORE", "ZOD", "LBC", "L-BTC", "LTC", "LCP",
            "LTZ", "LYTX", "MSR", "MASK", "MILE", "MQX", "MOX", "MBGL", "XMR", "MUE",
            "YCE", "NMC", "NAV", "NOR", "NTBC", "PENG", "PIVX", "PZDC", "PARS", "PART",
            "PRSN", "PINK", "PLE", "QMCoin", "QBS", "QWC", "R-BTC", "RADS", "RMX", "RYO",
            "SUB1X", "SCP", "SF", "SIL", "XSL", "SPACE", "XSPEC", "USDH", "USDT-E", "TUSD",
            "TEO", "TRTL", "USDC", "UCC", "UNO", "VARIUS", "VXV", "VEIL", "VTC", "WORX",
            "WEB", "WRKZ", "XDR0", "XRC", "ZEC", "XZC", "ZEL", "ZER", "ZERC", "XND", "UPX"
    );

    public static final Set<String> ALL_FIAT_CURRENCIES = Set.of(
            "AFN", "ALL", "DZD", "AOA", "ARS", "AMD", "AWG", "AUD", "AZN", "BSD",
            "BHD", "BDT", "BBD", "BYN", "BZD", "BMD", "BTN", "BOB", "BAM", "BWP",
            "BRL", "GBP", "BND", "BGN", "BIF", "XPF", "KHR", "CAD", "CVE", "KYD",
            "XAF", "CLP", "CNY", "COP", "KMF", "CDF", "CRC", "CUP", "CZK", "DKK",
            "DJF", "DOP", "XCD", "EGP", "ERN", "ETB", "EUR", "FKP", "FJD", "GMD",
            "GEL", "GHS", "GIP", "GTQ", "GNF", "GYD", "HTG", "HNL", "HKD", "HUF",
            "ISK", "INR", "IDR", "IRR", "IQD", "ILS", "JMD", "JPY", "JOD", "KZT",
            "KES", "KWD", "KGS", "LAK", "LBP", "LSL", "LRD", "LYD", "MOP", "MKD",
            "MGA", "MWK", "MYR", "MVR", "MRU", "MUR", "MXN", "MDL", "MNT", "MAD",
            "MZN", "MMK", "NAD", "NPR", "ANG", "TWD", "NZD", "NIO", "NGN", "KPW",
            "NOK", "OMR", "PKR", "PAB", "PGK", "PYG", "PEN", "PHP", "PLN", "QAR",
            "RON", "RUB", "RWF", "SVC", "WST", "SAR", "RSD", "SCR", "SLE", "SGD",
            "SBD", "SOS", "ZAR", "KRW", "SSP", "LKR", "SHP", "SDG", "SRD", "SZL",
            "SEK", "CHF", "SYP", "STN", "TJS", "TZS", "THB", "TOP", "TTD", "TND",
            "TRY", "TMT", "USD", "UGX", "UAH", "AED", "UYU", "UZS", "VUV", "VES",
            "VND", "XOF", "YER", "ZMW", "ZWL"
    );

    public static boolean isCryptoCurrency(String code) {
        return ALL_CRYPTO_CURRENCIES.contains(code);
    }

    public static boolean isFiatCurrency(String code) {
        return ALL_FIAT_CURRENCIES.contains(code);
    }
}
