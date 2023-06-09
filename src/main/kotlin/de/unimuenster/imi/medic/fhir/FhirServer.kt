package de.unimuenster.imi.medic.fhir

import de.unimuenster.imi.medic.basex.BaseX
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor
import ca.uhn.fhir.rest.server.RestfulServer
import de.unimuenster.imi.medic.fhir.helper.getAllResourceProviders
import de.unimuenster.imi.medic.utils.Properties
import mu.KotlinLogging
import javax.servlet.ServletException
import javax.servlet.annotation.WebServlet


/**
 * Main Servlet. Main Context is initialized here.
 */
@WebServlet(urlPatterns = ["/fhir/*"], displayName = "FHIR Server")
class FhirServer: RestfulServer() {

    companion object {
        lateinit var baseX: BaseX
        lateinit var settings: Properties
    }

    private val log = KotlinLogging.logger {  }
    private val serialVersionUID = 1L


    @Throws(ServletException::class)
    override fun initialize() {
        settings = Properties(servletContext, "WEB-INF/classes/settings/default.properties")
        baseX = BaseX(settings)

        fhirContext = FhirContext.forR4()

        registerProvider(ServerProvider())

        for (resourceProvider in getAllResourceProviders()!!) {
            registerProvider(resourceProvider!!.getConstructor().newInstance())
        }

        registerInterceptor(OpenApiInterceptor())

        log.info("FHIR Server started")
    }
}
