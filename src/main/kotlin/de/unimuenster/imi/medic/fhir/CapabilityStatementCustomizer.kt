package de.unimuenster.imi.medic.fhir

import ca.uhn.fhir.interceptor.api.Hook
import ca.uhn.fhir.interceptor.api.Interceptor
import ca.uhn.fhir.interceptor.api.Pointcut
import de.unimuenster.imi.medic.fhir.helper.PathMapUtil
import org.hl7.fhir.instance.model.api.IBaseConformance
import org.hl7.fhir.r4.model.CapabilityStatement
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction.*
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Enumerations


@Interceptor
class CapabilityStatementCustomizer {

    @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
    fun customize(theCapabilityStatement: IBaseConformance) {
        val cs = theCapabilityStatement as CapabilityStatement

        cs.apply {
            software.name = "FHIR B.O.S.S."
            software.version = "0.8"
            software.releaseDateElement = DateTimeType("2022-09-04")
            publisher = "IMI - Universität Münster"

            fhirVersion = Enumerations.FHIRVersion._4_0_0

            rest = listOf(
                CapabilityStatement.CapabilityStatementRestComponent().apply {
                    mode = CapabilityStatement.RestfulCapabilityMode.SERVER
                    resource = PathMapUtil.getResources()!!.map { resource ->
                        CapabilityStatement.CapabilityStatementRestResourceComponent().apply {
                            type = resource.name
                            profile = ""
                            interaction = listOf(CREATE, DELETE, HISTORYINSTANCE, HISTORYTYPE, PATCH, VREAD, READ, UPDATE, SEARCHTYPE)
                                .map { CapabilityStatement.ResourceInteractionComponent().apply { code = it } }
                            versioning = CapabilityStatement.ResourceVersionPolicy.VERSIONEDUPDATE
                            conditionalCreate = true
                            searchParam = resource.searchParameters.map { sp ->
                                CapabilityStatementRestResourceSearchParamComponent().apply {
                                    name = sp.name
                                    type = when (sp.type) {
                                        "token" -> Enumerations.SearchParamType.TOKEN
                                        "string" -> Enumerations.SearchParamType.STRING
                                        "composite" -> Enumerations.SearchParamType.COMPOSITE
                                        "number" -> Enumerations.SearchParamType.NUMBER
                                        "date" -> Enumerations.SearchParamType.DATE
                                        "reference" -> Enumerations.SearchParamType.REFERENCE
                                        "quantity" -> Enumerations.SearchParamType.QUANTITY
                                        "uri" -> Enumerations.SearchParamType.URI
                                        "special" -> Enumerations.SearchParamType.SPECIAL
                                        else -> Enumerations.SearchParamType.NULL
                                    }
                                    documentation = sp.description
                                }
                            }
                            searchInclude = listOf()
                            searchRevInclude = listOf()
                            operation = listOf()
                        }
                    }
                }
            )
        }
    }
}