package de.uni_muenster.imi.fhirFacade.fhir

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import de.uni_muenster.imi.fhirFacade.utils.UUIDHelper
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.formats.ParserType
import org.hl7.fhir.r4.model.IdType
import org.joda.time.DateTime
import java.util.*

private val fhirContext = FhirContext.forR4()
private val xmlParser = fhirContext.newXmlParser()
private val jsonParser = fhirContext.newJsonParser()


fun encodeFromResource(resource: IBaseResource, parserType: ParserType = ParserType.XML): String? {
    return when (parserType) {
        ParserType.XML -> stripNamespaceFromXML(xmlParser.encodeResourceToString(resource))
        ParserType.JSON -> jsonParser.encodeResourceToString(resource)
        else -> null
    }
}


fun decodeFromString(resourceString: String): IBaseResource? {
    return try {
        if (resourceString.startsWith("<")) {
            decodeFromString(resourceString, ParserType.XML)
        } else {
            decodeFromString(resourceString, ParserType.JSON)
        }
    } catch(e: Exception) {
        null
    }
}

private fun decodeFromString(resourceString: String, parserType: ParserType): IBaseResource? {
    return when (parserType) {
        ParserType.XML -> xmlParser.parseResource(resourceString)
        ParserType.JSON -> jsonParser.parseResource(resourceString)
        else -> null
    }
}

fun decodeQueryResults(resultString: String): List<IBaseResource> {
    val result = mutableListOf<IBaseResource>()
    splitSearchResults(resultString).forEach {
        result.add(decodeFromString(it.trim())!!)
    }
    return result
}

fun getNewestVersionFromBundle(resources: List<IBaseResource>): IBaseResource? {
    return resources.sortedWith(compareBy {it.idElement.versionIdPart.toInt()}).lastOrNull()
}

fun splitSearchResults(result: String): List<String> {
    return try {
        StringUtils.substringsBetween(result, "<result>", "</result>").asList()
    } catch(e: Exception) {
        listOf()
    }
}

fun stripNamespaceFromXML(resource: String): String {
    return resource.replace("xmlns=\"http://hl7.org/fhir\"", "")
}

fun IBaseResource.incrementVersion() {
    if (this.idElement.hasVersionIdPart()) {
        this.setId(
            IdType(
                this.fhirType(),
                this.idElement.idPart,
                "${this.idElement.versionIdPart.toInt() + 1}"
            )
        )
        this.meta.lastUpdated = Date(System.currentTimeMillis())
    } else {
        this.addVersion()
    }
}

fun IBaseResource.addVersion() {
    this.setId(
        IdType(
            this.fhirType(),
            this.idElement.idPart,
            "1"
        )
    )
    this.meta.lastUpdated = Date(System.currentTimeMillis())
}

fun IBaseResource.setNewVersion(newVersion: String) {
    this.setId(IdType(this.fhirType(), this.idElement.idPart, newVersion))
    this.meta.versionId = newVersion
    this.meta.lastUpdated = Date(System.currentTimeMillis())
}

fun IBaseResource.generateAndSetNewId() {
    this.setId(IdType(this.fhirType(), UUIDHelper.getUID(), "1"))
    this.meta.versionId = "1"
    this.meta.lastUpdated = Date(System.currentTimeMillis())
}

fun IBaseResource.hasVersionIdPart(): Boolean {
    return this.idElement!!.hasVersionIdPart()
}

fun DateRangeParam.completeInformation() {
    this.fillInRange()
    this.convertToDateTime()
}

fun DateRangeParam.fillInRange() {
    if (this.lowerBound == null) {
        this.lowerBound = DateParam("gt1970-01-01T00:00")
    }
    if (this.upperBound == null) {
        this.upperBound = DateParam("le${Date(System.currentTimeMillis()).toInstant()}")
    }
}

fun DateRangeParam.convertToDateTime() {
    this.lowerBound = DateParam(
        this.lowerBound.prefix.value +
                DateTime(this.lowerBound.value).toString()
        )
    this.upperBound = DateParam(
        this.upperBound.prefix.value +
                DateTime(this.upperBound.value).toString()
    )
}

