import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.Search
import de.uni_muenster.imi.fhirFacade.fhir.decodeFromString
import de.uni_muenster.imi.fhirFacade.fhir.encodeFromResource
import org.basex.api.client.ClientSession
import org.basex.core.BaseXException
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.formats.ParserType
import org.hl7.fhir.r4.model.*
import java.io.File
import java.math.BigDecimal
import java.util.*


private val ctx = FhirContext.forR4()
private val parser = ctx.newXmlParser().setPrettyPrint(true)


fun main() {
    //println("${Patient::class.simpleName}")
    TestBasex.addPatient()
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
        val resource = decodeFromString(text, ParserType.JSON)
        this.postResourceToBaseX(resource!!)
    }

    fun postResourceToBaseX(resource: IBaseResource) {
        val type = resource.fhirType()
        val id = resource.idElement.idPart

        if (!resource.idElement.hasVersionIdPart()) {
            resource.meta.versionId = "2"
            resource.meta.lastUpdated = Date(System.currentTimeMillis())
        }
        val version = resource.meta.versionId

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
