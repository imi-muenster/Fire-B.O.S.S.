import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.Search
import de.uni_muenster.imi.fhirFacade.fhir.*
import de.uni_muenster.imi.fhirFacade.fhir.helper.decodeFromString
import de.uni_muenster.imi.fhirFacade.fhir.helper.decodeQueryResults
import de.uni_muenster.imi.fhirFacade.fhir.helper.encodeFromResource
import org.basex.api.client.ClientSession
import org.basex.core.BaseXException
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.*
import java.io.File
import java.math.BigDecimal


private val ctx = FhirContext.forR4()
private val parser = ctx.newXmlParser().setPrettyPrint(true)


fun main() {

}


@Search
fun resourceInclusion(): List<IBaseResource> {
    val patient = Patient().apply {
        id = "Patient/1333"
        addIdentifier().apply {
            system = "urn:mrns"
            value = "253345"
        }
        managingOrganization.resource = Organization().apply {
            id = "Organization/65546"
            name = "Test Organization"
        }
    }

    return listOf(patient)
}

fun serializationExample() {
    val obs = Observation().apply {
        status = Observation.ObservationStatus.AMENDED
        code.addCoding().apply {
            code = "29463-7"
            system = "http://loinc.org"
            display = "Body Weight"
        }
        value = Quantity().apply {
            value = BigDecimal.valueOf(83.9)
            system = "http://unitsofmeasure.org"
            code = "kg"
        }
        referenceRangeFirstRep.low = SimpleQuantity().apply {
            value = BigDecimal.valueOf(45)
            system = "http://unitsofmeasure.org"
            code = "kg"
        }
        referenceRangeFirstRep.high = SimpleQuantity().apply {
            value = BigDecimal.valueOf(90)
            system = "http://unitsofmeasure.org"
            code = "kg"
        }
    }


    val serialized = parser.encodeResourceToString(obs)

    println(serialized)
}

object TestBasex {
    val session = ClientSession(
        "localhost",
        1984,
        "admin",
        "admin"
    )

    fun addPatient() {
        val text = File("src/main/resources/fhirResources/patient01").readText()
        val resource = decodeFromString(text)
        this.postResourceToBaseX(resource!!)
    }

    fun postResourceToBaseX(resource: IBaseResource) {
        val type = resource.fhirType()
        val id = resource.idElement.idPart

        if (!resource.idElement.hasVersionIdPart()) {
            resource.incrementVersion()
        }
        val version = resource.idElement.versionIdPart

        encodeFromResource(resource)?.let {
            try {
                session.execute("open $type")
            } catch (e: BaseXException) {
                session.execute("create db $type")
                session.execute("open $type")
            }

            session.add("${id}_$version", it.byteInputStream())
        }
    }
}


private fun printAsXml(resource: IBaseResource) {
    println(parser.encodeResourceToString(resource))
}

fun testQueryDecoding() {
    val test = "<results>\n" +
            "  <result>\n" +
            "    <Patient>\n" +
            "      <id value=\"pat2\"/>\n" +
            "      <meta>\n" +
            "        <versionId value=\"1\"/>\n" +
            "        <lastUpdated value=\"2022-05-02T13:35:36.152+02:00\"/>\n" +
            "      </meta>\n" +
            "      <text>\n" +
            "        <status value=\"generated\"/>\n" +
            "        <div xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "          <p>Patient Donald D DUCK @ Acme Healthcare, Inc. MR = 123456</p>\n" +
            "        </div>\n" +
            "      </text>\n" +
            "      <identifier>\n" +
            "        <use value=\"usual\"/>\n" +
            "        <type>\n" +
            "          <coding>\n" +
            "            <system value=\"http://hl7.org/fhir/v2/0203\"/>\n" +
            "            <code value=\"MR\"/>\n" +
            "          </coding>\n" +
            "        </type>\n" +
            "        <system value=\"urn:oid:0.1.2.3.4.5.6.7\"/>\n" +
            "        <value value=\"123456\"/>\n" +
            "      </identifier>\n" +
            "      <active value=\"true\"/>\n" +
            "      <name>\n" +
            "        <use value=\"official\"/>\n" +
            "        <family value=\"Donald\"/>\n" +
            "        <given value=\"Duck\"/>\n" +
            "        <given value=\"D\"/>\n" +
            "      </name>\n" +
            "      <gender value=\"other\">\n" +
            "        <extension url=\"http://example.org/Profile/administrative-status\">\n" +
            "          <valueCodeableConcept>\n" +
            "            <coding>\n" +
            "              <system value=\"http://hl7.org/fhir/v2/0001\"/>\n" +
            "              <code value=\"A\"/>\n" +
            "              <display value=\"Ambiguous\"/>\n" +
            "            </coding>\n" +
            "          </valueCodeableConcept>\n" +
            "        </extension>\n" +
            "      </gender>\n" +
            "      <photo>\n" +
            "        <contentType value=\"image/gif\"/>\n" +
            "        <data value=\"R0lGODlhEwARAPcAAAAAAAAA/+9aAO+1AP/WAP/eAP/eCP/eEP/eGP/nAP/nCP/nEP/nIf/nKf/nUv/nWv/vAP/vCP/vEP/vGP/vIf/vKf/vMf/vOf/vWv/vY//va//vjP/3c//3lP/3nP//tf//vf///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////yH5BAEAAAEALAAAAAATABEAAAi+AAMIDDCgYMGBCBMSvMCQ4QCFCQcwDBGCA4cLDyEGECDxAoAQHjxwyKhQAMeGIUOSJJjRpIAGDS5wCDly4AALFlYOgHlBwwOSNydM0AmzwYGjBi8IHWoTgQYORg8QIGDAwAKhESI8HIDgwQaRDI1WXXAhK9MBBzZ8/XDxQoUFZC9IiCBh6wEHGz6IbNuwQoSpWxEgyLCXL8O/gAnylNlW6AUEBRIL7Og3KwQIiCXb9HsZQoIEUzUjNEiaNMKAAAA7\"/>\n" +
            "      </photo>\n" +
            "      <managingOrganization>\n" +
            "        <reference value=\"Organization/1\"/>\n" +
            "        <display value=\"ACME Healthcare, Inc\"/>\n" +
            "      </managingOrganization>\n" +
            "      <link>\n" +
            "        <other>\n" +
            "          <reference value=\"Patient/pat1\"/>\n" +
            "        </other>\n" +
            "        <type value=\"seealso\"/>\n" +
            "      </link>\n" +
            "    </Patient>\n" +
            "  </result>\n" +
            "  <result>\n" +
            "    <Patient>\n" +
            "      <id value=\"pat2\"/>\n" +
            "      <meta>\n" +
            "        <versionId value=\"2\"/>\n" +
            "        <lastUpdated value=\"2022-05-02T13:35:36.152+02:00\"/>\n" +
            "      </meta>\n" +
            "      <text>\n" +
            "        <status value=\"generated\"/>\n" +
            "        <div xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "          <p>Patient Donald D DUCK @ Acme Healthcare, Inc. MR = 123456</p>\n" +
            "        </div>\n" +
            "      </text>\n" +
            "      <identifier>\n" +
            "        <use value=\"usual\"/>\n" +
            "        <type>\n" +
            "          <coding>\n" +
            "            <system value=\"http://hl7.org/fhir/v2/0203\"/>\n" +
            "            <code value=\"MR\"/>\n" +
            "          </coding>\n" +
            "        </type>\n" +
            "        <system value=\"urn:oid:0.1.2.3.4.5.6.7\"/>\n" +
            "        <value value=\"123456\"/>\n" +
            "      </identifier>\n" +
            "      <active value=\"true\"/>\n" +
            "      <name>\n" +
            "        <use value=\"official\"/>\n" +
            "        <family value=\"Donald\"/>\n" +
            "        <given value=\"Duck\"/>\n" +
            "        <given value=\"D\"/>\n" +
            "      </name>\n" +
            "      <gender value=\"other\">\n" +
            "        <extension url=\"http://example.org/Profile/administrative-status\">\n" +
            "          <valueCodeableConcept>\n" +
            "            <coding>\n" +
            "              <system value=\"http://hl7.org/fhir/v2/0001\"/>\n" +
            "              <code value=\"A\"/>\n" +
            "              <display value=\"Ambiguous\"/>\n" +
            "            </coding>\n" +
            "          </valueCodeableConcept>\n" +
            "        </extension>\n" +
            "      </gender>\n" +
            "      <photo>\n" +
            "        <contentType value=\"image/gif\"/>\n" +
            "        <data value=\"R0lGODlhEwARAPcAAAAAAAAA/+9aAO+1AP/WAP/eAP/eCP/eEP/eGP/nAP/nCP/nEP/nIf/nKf/nUv/nWv/vAP/vCP/vEP/vGP/vIf/vKf/vMf/vOf/vWv/vY//va//vjP/3c//3lP/3nP//tf//vf///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////yH5BAEAAAEALAAAAAATABEAAAi+AAMIDDCgYMGBCBMSvMCQ4QCFCQcwDBGCA4cLDyEGECDxAoAQHjxwyKhQAMeGIUOSJJjRpIAGDS5wCDly4AALFlYOgHlBwwOSNydM0AmzwYGjBi8IHWoTgQYORg8QIGDAwAKhESI8HIDgwQaRDI1WXXAhK9MBBzZ8/XDxQoUFZC9IiCBh6wEHGz6IbNuwQoSpWxEgyLCXL8O/gAnylNlW6AUEBRIL7Og3KwQIiCXb9HsZQoIEUzUjNEiaNMKAAAA7\"/>\n" +
            "      </photo>\n" +
            "      <managingOrganization>\n" +
            "        <reference value=\"Organization/1\"/>\n" +
            "        <display value=\"ACME Healthcare, Inc\"/>\n" +
            "      </managingOrganization>\n" +
            "      <link>\n" +
            "        <other>\n" +
            "          <reference value=\"Patient/pat1\"/>\n" +
            "        </other>\n" +
            "        <type value=\"seealso\"/>\n" +
            "      </link>\n" +
            "    </Patient>\n" +
            "  </result>\n" +
            "</results>"
    val results = decodeQueryResults(test)
}
