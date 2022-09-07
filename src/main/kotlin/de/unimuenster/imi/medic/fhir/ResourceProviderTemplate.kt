package de.unimuenster.imi.medic.fhir

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.i18n.Msg
import ca.uhn.fhir.jpa.util.jsonpatch.JsonPatchUtils
import ca.uhn.fhir.jpa.util.xmlpatch.XmlPatchUtils
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.PatchTypeEnum
import ca.uhn.fhir.rest.param.*
import ca.uhn.fhir.rest.server.IResourceProvider
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException
import de.unimuenster.imi.medic.basex.BaseX
import de.unimuenster.imi.medic.basex.BaseXQueries
import de.unimuenster.imi.medic.basex.generator.QueryGenerator
import de.unimuenster.imi.medic.fhir.FhirServer.Companion.baseX
import de.unimuenster.imi.medic.fhir.helper.ParameterConverter
import de.unimuenster.imi.medic.fhir.helper.decodeFromString
import de.unimuenster.imi.medic.fhir.helper.decodeQueryResults
import de.unimuenster.imi.medic.fhir.helper.getNewestVersionFromBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.OperationOutcome

abstract class ResourceProviderTemplate<T : IBaseResource>(private val resourceType: T): IResourceProvider {

    private val fhirType = resourceType.fhirType()
    override fun getResourceType(): Class<out IBaseResource> {
        return resourceType::class.java
    }

    fun getBaseXInstance(): BaseX {
        return baseX
    }



    @Read(version = true)
    fun getResourceById(@IdParam theId: IdType): IBaseResource? {
        return if(theId.hasVersionIdPart()) {
            getResourceForIdWithVersion(theId)
        } else {
            getResourceForId(theId)
        }
    }

    //TODO: Return Resource on update
    //TODO: Implement Conditional Updates
    @Update
    fun update(@IdParam theId: IdType, @ResourceParam theResource: String): MethodOutcome {
        val availableResource = getResourceById(theId)

        if (availableResource != null) {
            val decodedResource = decodeFromString(theResource)
            if (decodedResource != null) {
                val urlId = theId.idPart
                val resourceId = decodedResource.idElement.idPart

                if (!urlId.equals(resourceId)) {
                    throw ResourceVersionConflictException("${Msg.code(632)} Expected version $resourceId")
                }

                val newVersionNumber =
                    "${availableResource.idElement.versionIdPart.toInt() + 1}"
                decodedResource.setNewVersion(newVersionNumber)

                delete(IdType(availableResource.idElement.idPart))
                baseX.postResourceToBaseX(decodedResource)

                return MethodOutcome().apply {
                    id = decodedResource.idElement
                    created = true
                }
            } else {
                return MethodOutcome().apply {
                    created = false
                }
            }
        } else {
            return create(theResource)
        }
    }

    @Create
    fun create(@ResourceParam theResource: String): MethodOutcome {
        val decodedResource = decodeFromString(theResource)
        return if (decodedResource != null) {
            decodedResource.generateAndSetNewId()
            baseX.postResourceToBaseX(decodedResource)

            MethodOutcome().apply {
                id = decodedResource.idElement
                created = true
            }
        } else {
            MethodOutcome().apply {
                created = false
            }
        }
    }

    @Delete
    fun delete(@IdParam theId: IdType) {
        if (!theId.hasVersionIdPart()) {
            val availableResource = getResourceById(theId)

            if (availableResource != null) {
                baseX.postResourceToHistory(availableResource)
                baseX.executeXQuery(
                    BaseXQueries.deleteById(this.fhirType, theId.idPart)
                )
            } else {
                throw ResourceNotFoundException(Msg.code(634) + "No Resource for ID available.")
            }
        } else {
            throw InvalidRequestException("Please do not use Version ID for delete Operation")
        }
    }

    @Patch
    fun patch(@IdParam theId: IdType, thePatchType: PatchTypeEnum, @ResourceParam theBody: String): OperationOutcome {
        val resourceToPatch = getResourceById(theId)
        if (resourceToPatch != null) {

            if (thePatchType == PatchTypeEnum.JSON_PATCH) {
                val updatedResource = JsonPatchUtils.apply(FhirContext.forR4(), resourceToPatch, theBody)
                update(theId, updatedResource.asJSON()) //TODO: Redundant Conversion but update needs string (Write separate method?)
            }
            else if (thePatchType == PatchTypeEnum.XML_PATCH) {
                val updatedResource = XmlPatchUtils.apply(FhirContext.forR4(), resourceToPatch, theBody)
                update(theId, updatedResource.asXML())
            }

            return OperationOutcome().apply {
                text.divAsString = "<div>OK</div>"
            }
        } else {
            return OperationOutcome().apply {
                text.divAsString = "<div>Bad Request</div>"
            }
        }
    }


    //TODO: Save request History for modified files and present them here
    @History
    fun getInstanceHistory(@IdParam theId: IdType,
                           @At theAt: DateRangeParam?,
                           @Since theSince: DateTimeType?
    ): List<IBaseResource> {
        val paramMap = ParameterConverter.getSearchParameterMapForInstanceHistory(theId, theAt, theSince)
        val pathMap: HashMap<String, String> = HashMap<String, String>().apply {
            put("_id", "${getResourceName()}.id")
            put("_since", "${getResourceName()}.meta.lastUpdated")
            put("_at", "${getResourceName()}.meta.lastUpdated")
        }

        val gen = QueryGenerator()
        val search = gen.getHistoryForType(paramMap, pathMap, this.getResourceName()) +
                        gen.getHistoryForType(paramMap, pathMap, "${this.getResourceName()}_history")

        return decodeQueryResults(
            baseX.executeXQuery(BaseXQueries.getServerHistory(search))
        )
    }

    @History
    fun getTypeHistory(@At theAt: DateRangeParam?, @Since theSince: DateTimeType?): List<IBaseResource> {
        val paramMap = ParameterConverter.getSearchParameterMapForTypeHistory(theAt, theSince)
        val pathMap: HashMap<String, String> = HashMap<String, String>().apply {
            put("_since", "${getResourceName()}.meta.lastUpdated")
            put("_at", "${getResourceName()}.meta.lastUpdated")
        }

        val gen = QueryGenerator()
        val search = gen.getHistoryForType(paramMap, pathMap, this.getResourceName()) +
                        gen.getHistoryForType(paramMap, pathMap, "${this.getResourceName()}_history")

        return decodeQueryResults(
            baseX.executeXQuery(BaseXQueries.getServerHistory(search))
        )
    }

    fun getResourceName(): String {
        return this.fhirType
    }

    private fun getResourceForIdWithVersion(theId: IdType): IBaseResource? {
        val result = decodeQueryResults(
            baseX.executeXQuery(
                BaseXQueries.getByIdAndVersion(this.fhirType, theId.idPart, theId.versionIdPart)
            )
        )
        return if (result.isNotEmpty()) {
            //List should always contain only one element. Just to be safe
            getNewestVersionFromBundle(result)
        } else {
            null
        }
    }

    private fun getResourceForId(theId: IdType): IBaseResource? {
        val result = decodeQueryResults(
            baseX.executeXQuery(
                BaseXQueries.getById(this.fhirType, theId.idPart)
            )
        )
        return if (result.isNotEmpty()) {
            //List should always contain only one element. Just to be safe
            getNewestVersionFromBundle(result)
        } else {
            null
        }
    }
}