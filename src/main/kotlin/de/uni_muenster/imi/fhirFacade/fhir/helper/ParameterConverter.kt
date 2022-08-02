package de.uni_muenster.imi.fhirFacade.fhir.helper

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap
import ca.uhn.fhir.rest.param.*
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.IdType

object ParameterConverter {

    fun getSearchParameterMapForTypeHistory(theAt: DateRangeParam?, theSince: DateTimeType?) : SearchParameterMap {
        val searchParameterMap = SearchParameterMap()

        if (theAt != null) {
            searchParameterMap.add("_at", convertAtToDateAndListParam(theAt))
        }

        if (theSince != null) {
            searchParameterMap.add("_since", convertSinceToDateAndListParam(theSince))
        }


        return searchParameterMap
    }

    fun getSearchParameterMapForInstanceHistory(
        theId: IdType,
        theAt: DateRangeParam?,
        theSince: DateTimeType?
    ): SearchParameterMap {
        val searchParameterMap = SearchParameterMap()

        if (theAt != null) {
            searchParameterMap.add("_at", convertAtToDateAndListParam(theAt))
        }

        if (theSince != null) {
            searchParameterMap.add("_since", convertSinceToDateAndListParam(theSince))
        }

        searchParameterMap.add("_id", convertIdTypeToStringAndListParam(theId))

        return searchParameterMap
    }

    private fun convertAtToDateAndListParam(theAt: DateRangeParam): DateAndListParam? {
        return DateAndListParam().addAnd(
            DateOrListParam().add(theAt.lowerBound)
        )
    }

    private fun convertSinceToDateAndListParam(theSince: DateTimeType): DateAndListParam? {
        val date = DateParam("ge${theSince.valueAsString}")
        return DateAndListParam().addAnd(
            DateOrListParam().add(date)
        )
    }

    private fun convertIdTypeToStringAndListParam(theId: IdType): StringAndListParam {
        return StringAndListParam().addAnd(
            StringOrListParam().add(StringParam(theId.idPart))
        )
    }
}