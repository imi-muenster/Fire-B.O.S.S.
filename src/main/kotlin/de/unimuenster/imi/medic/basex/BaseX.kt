package de.unimuenster.imi.medic.basex

import de.unimuenster.imi.medic.fhir.addVersion
import de.unimuenster.imi.medic.fhir.helper.encodeFromResource
import de.unimuenster.imi.medic.utils.Properties
import mu.KotlinLogging
import org.basex.api.client.ClientSession
import org.basex.core.BaseXException
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.IdType


class BaseX(SETTINGS: Properties) {
    private lateinit var session: ClientSession
    private val logger = KotlinLogging.logger {  }

    init {
        try {
            session = ClientSession(
                SETTINGS["basex.host", true],
                SETTINGS["basex.port", true].toInt(),
                SETTINGS["basex.username", true],
                SETTINGS["basex.password", true]
            )
        } catch (e: Exception) {
            logger.error(e) {"Could not establish connection to ${SETTINGS["basex.host"]}:${SETTINGS["basex.port"]}: ${e.message}"}
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

    fun postStringToBaseX(resource: String, type: String, id: IdType)  {
        val idPart = id.idPart
        val version = id.versionIdPart ?: "1"

        try {
            session.execute("open $type")
        } catch (e: BaseXException) {
            logger.info("Database was not found. Creating it.")
            session.execute("create db $type")
            session.execute("open $type")
        }

        session.add("${idPart}_$version", resource.byteInputStream())
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