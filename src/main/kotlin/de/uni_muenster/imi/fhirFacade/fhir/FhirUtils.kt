package de.uni_muenster.imi.fhirFacade.fhir

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.formats.ParserType

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

fun decodeFromString(resourceString: String, parserType: ParserType = ParserType.XML): IBaseResource? {
    return when (parserType) {
        ParserType.XML -> xmlParser.parseResource(resourceString)
        ParserType.JSON -> jsonParser.parseResource(resourceString)
        else -> null
    }
}

fun stripNamespaceFromXML(resource: String): String {
    return resource.replace("xmlns=\"http://hl7.org/fhir\"", "")
}