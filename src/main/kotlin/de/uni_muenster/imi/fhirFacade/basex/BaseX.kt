package de.uni_muenster.imi.fhirFacade.basex

import de.uni_muenster.imi.fhirFacade.fhir.addVersion
import de.uni_muenster.imi.fhirFacade.fhir.helper.encodeFromResource
import de.uni_muenster.imi.fhirFacade.utils.Properties
import org.basex.api.client.ClientSession
import org.basex.core.BaseXException
import org.hl7.fhir.instance.model.api.IBaseResource
import org.slf4j.LoggerFactory


class BaseX(SETTINGS: Properties) {
    private lateinit var session: ClientSession
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        try {
            session = ClientSession(
                SETTINGS["basex.host", true],
                SETTINGS["basex.port", true].toInt(),
                SETTINGS["basex.username", true],
                SETTINGS["basex.password", true]
            )
        } catch (e: Exception) {
            logger.error("Could not establish connection: ${e.message}")
        }
    }

    fun postResourceToBaseX(resource: IBaseResource, type: String = resource.fhirType()) {
        val id = resource.idElement.idPart

        if (!resource.idElement.hasVersionIdPart()) {
            resource.addVersion()
        }
        val version = resource.idElement.versionIdPart

        encodeFromResource(resource)?.let {
            try {
                session.execute("open $type")
            } catch (e: BaseXException) {
                logger.info("Database was not found. Creating it.")
                session.execute("create db $type")
                session.execute("open $type")
            }

            session.add("${id}_$version", it.byteInputStream())
        }
    }

    fun postResourceToHistory(resource: IBaseResource) {
        postResourceToBaseX(resource, "${resource.fhirType()}_history")
    }

    fun executeXQuery(xquery: String): String {
        try {
            return session.query(xquery).execute()
        } catch (e: Exception) {
            logger.info("Could not execute Query: $e")
        }
        return ""
    }

    fun createDbIfNotExist(dbName: String) {
        try {
            session.execute("open $dbName")
        } catch (e: BaseXException) {
            logger.info("Database was not found. Creating it.")
            session.execute("create db $dbName")
        }
    }

    fun close() {
        session.close()
    }
}