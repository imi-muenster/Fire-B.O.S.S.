package de.uni_muenster.imi.fhirFacade.basex

import de.uni_muenster.imi.fhirFacade.exception.BaseXDatabaseNotFoundException
import de.uni_muenster.imi.fhirFacade.fhir.encodeFromResource
import de.uni_muenster.imi.fhirFacade.utils.Properties
import org.basex.api.client.ClientSession
import org.basex.core.BaseXException
import org.basex.util.DateTime
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Patient
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*


class BaseX(SETTINGS: Properties) {
    private lateinit var session: ClientSession
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        try {
            session = ClientSession(
                SETTINGS["basex.host", true],
                SETTINGS["basex.port", true].toInt(),
                SETTINGS["basex.username", true],
                SETTINGS["basex.password", true]
            )
        } catch (e: Exception) {
            log.error("Could not establish connection: ${e.message}")
        }
    }

    fun postResourceToBaseX(resource: IBaseResource) {
        val type = resource.fhirType()
        val id = resource.idElement.idPart

        if (!resource.idElement.hasVersionIdPart()) {
            resource.setId(IdType(type, id, "1"))
            resource.meta.lastUpdated = Date(System.currentTimeMillis())
        }
        val version = resource.idElement.versionIdPart

        encodeFromResource(resource)?.let {
            try {
                session.execute("open $type")
            } catch (e: BaseXException) {
                log.info("Database was not found. Creating it.")
                session.execute("create db $type")
                session.execute("open $type")
            }

            session.add("${id}_$version", it.byteInputStream())
        }
    }

    fun updateResourceInBaseX(resource: IBaseResource) {
        val type = resource.fhirType()
        val id = resource.idElement.idPart

        try {
            session.execute("open $type")
        } catch (e: Exception) {
            throw BaseXDatabaseNotFoundException()
        }

    }

    fun executeXQuery(xquery: String): String {
        return session.query(xquery).execute()
    }

    fun close() {
        session.close()
    }
}