package net.corda.samples.duediligence;

import net.corda.core.crypto.SecureHash;

import java.util.Arrays;
import java.util.List;

public interface Constants {
    //Whitelisted Corporate Auditors
    String CORPORATE_JAR_PATH = "../contracts/src/main/resources/corporateAuditors.jar";
    SecureHash CORPORATE_JAR_HASH = SecureHash.parse("8DF3275D80B26B9A45AB022F2FDA4A2ED996449B425F8F2245FA5BCF7D1AC587");
    String CORPORATE_ATTACTMENT_FILE_NAME = "whitelistedCorporateAuditors.txt";
    List<String> CORPORATE_ATTACHMENT_EXPECTED_CONTENTS = Arrays.asList(
            "Crossland Savings",
            "Trusted Auditor"
    );

    //Whitelisted Finance Auditors
    String FINANCIAL_JAR_PATH = "../contracts/src/main/resources/financialAuditors.jar";
    SecureHash FINANCIAL_JAR_HASH = SecureHash.parse("DE7635E2AD626BC57D811D065F7841FC35839FA2D0CF095857CACE1579756A1C");
    String FINANCIAL_ATTACTMENT_FILE_NAME = "whitelistedFinancialAuditors.txt";
    List<String> FINANCIAL_ATTACHMENT_EXPECTED_CONTENTS = Arrays.asList(
            "Detroit Partners Group",
            "Tifton Banking Company"
    );
}
