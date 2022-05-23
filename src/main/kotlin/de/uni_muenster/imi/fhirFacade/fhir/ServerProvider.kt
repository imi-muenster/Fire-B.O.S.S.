package de.uni_muenster.imi.fhirFacade.fhir

import ca.uhn.fhir.rest.annotation.Transaction
import ca.uhn.fhir.rest.annotation.TransactionParam
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.HTTPVerb

class ServerProvider {

    @Transaction
    fun transaction(@TransactionParam theInput: Bundle): Bundle {
        if (theInput.type == Bundle.BundleType.TRANSACTION) {
            for (nextEntry in theInput.entry) {

                when (nextEntry.request.method) {
                    HTTPVerb.GET -> {}
                    HTTPVerb.POST -> {}
                    HTTPVerb.PUT -> {}
                    HTTPVerb.DELETE -> {}
                    HTTPVerb.PATCH -> {}
                }

            }
        }
        return theInput
    }

}