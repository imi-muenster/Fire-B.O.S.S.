package de.uni_muenster.imi.fhirFacade.fhir

import ca.uhn.fhir.i18n.Msg
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.server.IResourceProvider
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException
import de.uni_muenster.imi.fhirFacade.basex.BaseXQueries
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.IdType

abstract class PlainProvider<T : IBaseResource>(private val resourceType: T): IResourceProvider {

    private val fhirType = resourceType::class.simpleName!!

    override fun getResourceType(): Class<out IBaseResource> {
        return resourceType::class.java
    }

    //TODO: Read FHIR Documentation on how these functions should behave
    //TODO: Conditionals?
    //TODO: Patching? No logic support in HAPI. Has to be implemented.
    //TODO: Validation has to be made on Resource level
    //TODO: Transactional not implemented in HAPI.

    @Read(version = true)
    fun getResourceById(@IdParam theId: IdType): IBaseResource? {
        return if(theId.hasVersionIdPart()) {
            getResourceForIdWithVersion(theId)
        } else {
            getNewestVersionFromBundle(
                getResourcesForId(theId)
            )
        }
    }

    //TODO: Return Resource on update?
    @Update
    fun update(@IdParam theId: IdType, @ResourceParam theResource: String): MethodOutcome {
        val availableResources = getResourcesForId(theId)

        if (availableResources.isNotEmpty()) {
            val decodedResource = decodeFromString(theResource)
            if (decodedResource != null) {
                val urlId = theId.idPart
                val resourceId = decodedResource.idElement.idPart

                if (!urlId.equals(resourceId)) {
                    throw ResourceVersionConflictException("${Msg.code(632)} Expected version $resourceId")
                }

                val newVersionNumber =
                    "${getNewestVersionFromBundle(availableResources)!!.idElement.versionIdPart.toInt() + 1}"
                decodedResource.setNewVersion(newVersionNumber)

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
            //TODO: Should Update include create?
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
            val availableResources = getResourcesForId(theId)

            if (availableResources.isNotEmpty()) {
                baseX.executeXQuery(
                    BaseXQueries.deleteById(this.fhirType, theId.idPart)
                )
            } else {
                throw ResourceNotFoundException(Msg.code(634) + "Unknown version")
            }
        } else {
            baseX.executeXQuery(
                BaseXQueries.deleteByIdAndVersion(this.fhirType, theId.idPart, theId.versionIdPart)
            )
        }
    }

    //TODO: Redo later with Query Generator
    @History()
    fun getResourceHistory(@IdParam theId: IdType, @At theAt: DateRangeParam): List<IBaseResource> {
        theAt.completeInformation()
        return decodeQueryResults(
            baseX.executeXQuery(
                BaseXQueries.getHistoryInRange(
                    this.fhirType,
                    theId.idPart,
                    theAt.lowerBound.valueAsString,
                    theAt.upperBound.valueAsString
                )
            )
        )
    }

    @History
    fun getResourcesSince(@IdParam theId: IdType, @Since theSince: DateParam) {
        //TODO: Implement
    }

    private fun getResourceForIdWithVersion(theId: IdType): IBaseResource? {
        return decodeFromString(
            baseX.executeXQuery(
                BaseXQueries.getByIdAndVersion(this.fhirType, theId.idPart, theId.versionIdPart)
            )
        )
    }

    private fun getResourcesForId(theId: IdType): List<IBaseResource> {
        return decodeQueryResults(
            baseX.executeXQuery(
                BaseXQueries.getById(this.fhirType, theId.idPart)
            )
        )
    }
}