<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:wd="urn:com.workday/bsvc"
                exclude-result-prefixes="wd">

    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <xsl:template match="/wd:Report_Data">
        <CanonicalHRData>
            <xsl:for-each select="wd:Report_Entry[wd:Employee_Status = 'Active' or wd:Employee_Status = 'Hire']">
                <EmployeeEvent>
                    <EventID><xsl:value-of select="generate-id()"/></EventID>
                    <Timestamp><xsl:value-of select="current-dateTime()"/></Timestamp>
                    <GlobalWorkerID><xsl:value-of select="wd:Worker/wd:Employee_ID"/></GlobalWorkerID>
                    <FirstName><xsl:value-of select="wd:Worker/wd:Personal_Data/wd:First_Name"/></FirstName>
                    <LastName><xsl:value-of select="wd:Worker/wd:Personal_Data/wd:Last_Name"/></LastName>
                    <Email><xsl:value-of select="wd:Worker/wd:Personal_Data/wd:Primary_Work_Email"/></Email>
                    <CostCenter><xsl:value-of select="wd:Worker/wd:Position_Data/wd:Cost_Center_Reference_ID"/></CostCenter>
                    <CompanyCode><xsl:value-of select="wd:Worker/wd:Position_Data/wd:Company_Reference_ID"/></CompanyCode>
                    <BaseSalary><xsl:value-of select="wd:Worker/wd:Compensation_Data/wd:Base_Pay_Amount"/></BaseSalary>
                    <Currency><xsl:value-of select="wd:Worker/wd:Compensation_Data/wd:Currency"/></Currency>
                    <ActionType><xsl:value-of select="wd:Employee_Status"/></ActionType>
                </EmployeeEvent>
            </xsl:for-each>
        </CanonicalHRData>
    </xsl:template>

    <xsl:template match="text()"/>
</xsl:stylesheet>
