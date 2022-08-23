package de.uni_muenster.imi.fhirFacade.fhir

import ca.uhn.fhir.interceptor.api.Hook
import ca.uhn.fhir.interceptor.api.Interceptor
import ca.uhn.fhir.interceptor.api.Pointcut
import de.uni_muenster.imi.fhirFacade.fhir.helper.PathMapUtil
import org.hl7.fhir.instance.model.api.IBaseConformance
import org.hl7.fhir.r4.model.CapabilityStatement
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Enumerations

@Interceptor
class CapabilityStatementCustomizer {

    @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
    fun customize(theCapabilityStatement: IBaseConformance) {
        val cs = theCapabilityStatement as CapabilityStatement

        cs.apply {
            software.name = "BaseX FHIR Facade"
            software.version = "0.8"
            software.releaseDateElement = DateTimeType("2022-09-04")
            publisher = "IMI - Westfälische Wilhelms-Universität Münster"

            fhirVersion = Enumerations.FHIRVersion._4_0_0

            rest = listOf(
                CapabilityStatement.CapabilityStatementRestComponent().apply {
                    mode = CapabilityStatement.RestfulCapabilityMode.SERVER
                    val resourceList = mutableListOf<CapabilityStatement.CapabilityStatementRestResourceComponent>()
                    for (resource in PathMapUtil.getResources()!!) {
                        val res = CapabilityStatement.CapabilityStatementRestResourceComponent().apply {
                            type = resource.name
                            profile = ""
                            interaction =  mutableListOf(
                                CapabilityStatement.ResourceInteractionComponent().apply {code = CapabilityStatement.TypeRestfulInteraction.CREATE},
                                CapabilityStatement.ResourceInteractionComponent().apply {code = CapabilityStatement.TypeRestfulInteraction.DELETE},
                                CapabilityStatement.ResourceInteractionComponent().apply {code = CapabilityStatement.TypeRestfulInteraction.HISTORYINSTANCE},
                                CapabilityStatement.ResourceInteractionComponent().apply {code = CapabilityStatement.TypeRestfulInteraction.HISTORYTYPE},
                                CapabilityStatement.ResourceInteractionComponent().apply {code = CapabilityStatement.TypeRestfulInteraction.PATCH},
                                CapabilityStatement.ResourceInteractionComponent().apply {code = CapabilityStatement.TypeRestfulInteraction.VREAD},
                                CapabilityStatement.ResourceInteractionComponent().apply {code = CapabilityStatement.TypeRestfulInteraction.READ},
                                CapabilityStatement.ResourceInteractionComponent().apply {code = CapabilityStatement.TypeRestfulInteraction.UPDATE},
                                CapabilityStatement.ResourceInteractionComponent().apply {code = CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE}
                            )
                            versioning = CapabilityStatement.ResourceVersionPolicy.VERSIONEDUPDATE
                            conditionalCreate = true
                            val searchParametersForResource = mutableListOf<CapabilityStatementRestResourceSearchParamComponent>()
                            for (sp in resource.searchParameters) {
                                searchParametersForResource.add(
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
                                )
                            }
                            searchParam = searchParametersForResource
                            searchInclude = listOf()
                            searchRevInclude = listOf()
                            operation = listOf()
                        }
                        resourceList.add(res)
                    }
                    resource = resourceList
                }
            )
        }
    }
}