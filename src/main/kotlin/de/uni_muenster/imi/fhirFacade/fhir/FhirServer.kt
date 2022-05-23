package de.uni_muenster.imi.fhirFacade.fhir

import de.uni_muenster.imi.fhirFacade.basex.BaseX
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.server.RestfulServer
import de.uni_muenster.imi.fhirFacade.utils.Properties
import mu.KotlinLogging
import javax.servlet.ServletException
import javax.servlet.annotation.WebServlet

private lateinit var settings: Properties
lateinit var baseX: BaseX

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

        registerProvider(ServerProvider())

        for (resourceProvider in getAllResourceProviders()!!) {
            registerProvider(resourceProvider!!.getConstructor().newInstance())
        }

        log.info("FHIR Server started")
    }
}
