package de.uni_muenster.imi.fhirFacade.fhir

import de.uni_muenster.imi.fhirFacade.basex.BaseX
import de.uni_muenster.imi.fhirFacade.basex.BaseXQueries
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.i18n.Msg
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.server.IResourceProvider
import ca.uhn.fhir.rest.server.RestfulServer
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException
import de.uni_muenster.imi.fhirFacade.utils.Properties
import mu.KotlinLogging
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Patient
import javax.servlet.ServletException
import javax.servlet.annotation.WebServlet

private lateinit var settings: Properties
private lateinit var baseX: BaseX



/**
 * Main Servlet. Main Context is initialized here.
 */
@WebServlet(urlPatterns = ["/fhir/*"], displayName = "FHIR Server")
class FhirServer: RestfulServer() {

    private val log = KotlinLogging.logger {  }
    private val serialVersionUID = 1L


    @Throws(ServletException::class)
    override fun initialize() {
        settings = Properties(servletContext, "WEB-INF/classes/settings/default.properties")
        baseX = BaseX(settings)

        fhirContext = FhirContext.forR4()

        registerProviders(mutableListOf(
            PlainProvider(Patient())
        ))

        log.info("FHIR Server started")
    }
}

class PlainProvider<T : IBaseResource>(val resourceType: T): IResourceProvider {

    override fun getResourceType(): Class<out IBaseResource> {
        return resourceType::class.java
    }

    //TODO: Read FHIR Documentation on how these functions should behave

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
                //TODO: Delete by id
            } else {
                throw ResourceNotFoundException(Msg.code(634) + "Unknown version")
            }
        } else {
            //TODO: Delete with version
        }
    }

    private fun getResourceForIdWithVersion(theId: IdType): IBaseResource? {
        return decodeFromString(
            baseX.executeXQuery(
                BaseXQueries.getByIdAndVersion("${resourceType::class.simpleName}", theId.idPart, theId.versionIdPart)
            )
        )
    }

    private fun getResourcesForId(theId: IdType): List<IBaseResource> {
        return decodeQueryResults(
            baseX.executeXQuery(
                BaseXQueries.getById("${resourceType::class.simpleName}", theId.idPart)
            )
        )
    }
}
