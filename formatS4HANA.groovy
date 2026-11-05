import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.XmlSlurper
import groovy.json.JsonOutput
import java.io.Reader

def Message processData(Message message) {
    Reader reader = message.getBody(java.io.Reader)
    def hrData = new XmlSlurper().parse(reader)
    def emp = hrData.EmployeeEvent[0]

    String workerId   = emp.GlobalWorkerID.text() ?: "EMP-9901"
    String firstName  = emp.FirstName.text() ?: "Unknown"
    String lastName   = emp.LastName.text() ?: "Worker"
    String costCenter = emp.CostCenter.text() ?: "CC_GLOBAL_01"
    String compCode   = emp.CompanyCode.text() ?: "1010"

    def s4HanaPayload = [
        "BusinessPartner"         : workerId,
        "BusinessPartnerCategory" : "1",
        "FirstName"               : firstName,
        "LastName"                : lastName,
        "SearchTerm1"             : "HR_WORKDAY",
        "to_Employment"           : [
            [
                "CompanyCode"      : compCode,
                "CostCenter"       : costCenter,
                "EmploymentStatus" : "3"
            ]
        ]
    ]

    message.setBody(JsonOutput.toJson(s4HanaPayload))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("Accept", "application/json")
    return message
}
