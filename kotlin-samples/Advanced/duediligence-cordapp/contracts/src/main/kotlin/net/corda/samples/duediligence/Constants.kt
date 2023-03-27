package net.corda.samples.duediligence

import net.corda.core.crypto.SecureHash
import java.util.*

//Whitelisted Corporate Auditors
var CORPORATE_JAR_PATH = "../contracts/src/main/resources/corporateAuditors.jar"
var CORPORATE_JAR_HASH: SecureHash = SecureHash.parse("8DF3275D80B26B9A45AB022F2FDA4A2ED996449B425F8F2245FA5BCF7D1AC587")
var CORPORATE_ATTACTMENT_FILE_NAME = "whitelistedCorporateAuditors.txt"
var CORPORATE_ATTACHMENT_EXPECTED_CONTENTS = Arrays.asList(
        "Crossland Savings",
        "Trusted Auditor"
)

//Whitelisted Finance Auditors
var FINANCIAL_JAR_PATH = "../contracts/src/main/resources/financialAuditors.jar"
var FINANCIAL_JAR_HASH: SecureHash = SecureHash.parse("DE7635E2AD626BC57D811D065F7841FC35839FA2D0CF095857CACE1579756A1C")
var FINANCIAL_ATTACTMENT_FILE_NAME = "whitelistedFinancialAuditors.txt"
var FINANCIAL_ATTACHMENT_EXPECTED_CONTENTS = Arrays.asList(
        "Detroit Partners Group",
        "Tifton Banking Company"
)