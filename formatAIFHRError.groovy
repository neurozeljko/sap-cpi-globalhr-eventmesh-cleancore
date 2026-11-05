import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import java.time.Instant

def Message processData(Message message) {
    def properties = message.getProperties()
    def ex = properties.get("CamelExceptionCaught")
    
    String rawError = ex ? ex.getMessage() : "Unknown HR Master Data Replication Exception"
    String originalPayload = properties.get("Original_PECI_Payload") ?: "N/A"

    def aifPayload = [
        "AIF_Header": [
            "Namespace"        : "/HR_PAYROLL/",
            "InterfaceName"    : "WORKDAY_PECI_INB",
            "InterfaceVersion" : "1",
            "MessageGuid"      : UUID.randomUUID().toString(),
            "Timestamp"        : Instant.now().toString(),
            "SourceSystem"     : "WORKDAY_PROD",
            "TargetSystems"    : ["S4HANA_BP", "SAP_CONCUR"]
        ],
        "AIF_ErrorContext": [
            "Category"         : "EMPLOYEE_REPLICATION_FAULT",
            "RawSystemMessage" : rawError,
            "RemediationAction": "Verify Cost Center validity in S/4HANA or Concur User Licensing",
            "Reprocessable"    : true
        ],
        "OriginalPayload"      : originalPayload
    ]

    message.setBody(JsonOutput.toJson(aifPayload))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("AIF_Interface_Status", "ERROR_PENDING_HR_REPROCESS")
    return message
}
