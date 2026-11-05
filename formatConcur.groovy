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
    String email      = emp.Email.text() ?: "${workerId}@company.com"
    String costCenter = emp.CostCenter.text() ?: "CC_GLOBAL_01"

    def concurUserPayload = [
        "schemas" : ["urn:ietf:params:scim:schemas:core:2.0:User"],
        "userName": email,
        "name"    : [
            "givenName" : firstName,
            "familyName": lastName
        ],
        "emails"  : [
            ["value": email, "type": "work", "primary": true]
        ],
        "active"  : true,
        "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": [
            "employeeNumber": workerId,
            "costCenter"    : costCenter
        ]
    ]

    message.setBody(JsonOutput.toJson(concurUserPayload))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("Accept", "application/json")
    return message
}
