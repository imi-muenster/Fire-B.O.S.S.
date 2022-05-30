package de.uni_muenster.imi.fhirFacade.basex

import ca.uhn.fhir.i18n.Msg
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.rest.param.*
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException
import de.uni_muenster.imi.fhirFacade.fhir.getSearchParameterMap
import de.uni_muenster.imi.fhirFacade.fhir.helper.DateUtil
import de.uni_muenster.imi.fhirFacade.fhir.helper.getApproximationRange
import de.uni_muenster.imi.fhirFacade.fhir.helper.getSignificantRange

class QueryGenerator {

    fun generateQuery(parameterMap: SearchParameterMap, pathMap: HashMap<String, String>): List<String> {
        //TODO: Process constants

        //TODO: Process _has and _id

        //TODO: Process different optional ParamTypes and create Queries from Snippets accordingly
        val searchParameterPart = handleSearchParameterMap(parameterMap.getSearchParameterMap(), pathMap)
        //TODO: Process result Parameters
        return listOf("")
    }

    private fun handleSearchParameterMap(theMap: HashMap<String, List<List<IQueryParameterType>>>,
                                         pathMap: HashMap<String, String>
    ): String {
        var result = ""
        for (entry in theMap) {
            for (list in entry.value) {
                val path = mapPathToXPath(pathMap[entry.key]!!)
                result += handleListEntry(entry.key, path, list)
            }
        }
        return result
    }

    private fun handleListEntry(paramName: String, paramPath: String, theList: List<IQueryParameterType>): String {
        var result = ""
        for (theParam in theList) {
            result += "${handleBaseParameter(theParam, paramName)} or "
        }

        result.substring(0, result.lastIndexOf(" or "))

        return QuerySnippets.parameterTemplate(paramName, paramPath).replace(
            "#SNIPPETS", result
        )
    }

    private fun handleBaseParameter(theParam: IQueryParameterType, paramName: String): String {
        return when (theParam::class.java) {
            NumberParam::class.java -> {
                handleNumberParam(theParam as NumberParam, paramName)
            }
            DateParam::class.java -> {
                ""
            }
            DateRangeParam::class.java -> {
                "" //TODO: I dont think this will ever appear here
            }
            StringParam::class.java -> {
                handleStringParam(theParam as StringParam, paramName)
            }
            TokenParam::class.java -> {
                ""
            }
            ReferenceParam::class.java -> {
                ""
            }
            CompositeParam::class.java -> {
                ""
            }
            QuantityParam::class.java -> {
                ""
            }
            UriParam::class.java -> {
                ""
            }
            SpecialParam::class.java -> {
                ""
            }
            else -> {
                ""
            }
        }
    }

    private fun handleStringParam(theParam: StringParam, paramName: String): String {
        return if (theParam.isContains) {
                QuerySnippets.StringSnippets.stringContains(paramName, theParam.value)
            } else if (theParam.isExact) {
                QuerySnippets.StringSnippets.stringExact(paramName, theParam.value)
            } else {
                QuerySnippets.StringSnippets.stringStart(paramName, theParam.value)
            }
    }

    private fun handleNumberParam(theParam: NumberParam, paramName: String): String {
        when (theParam.prefix) {
            ParamPrefixEnum.APPROXIMATE -> {
                val range = theParam.getApproximationRange()
                return QuerySnippets.NumberSnippets.APPROXIMATE(paramName, range)
            }
            ParamPrefixEnum.GREATERTHAN -> {
                return QuerySnippets.NumberSnippets.GREATERTHAN(paramName, theParam)
            }
            ParamPrefixEnum.GREATERTHAN_OR_EQUALS -> {
                return QuerySnippets.NumberSnippets.GREATERTHAN_OR_EQUALS(paramName, theParam)
            }
            ParamPrefixEnum.LESSTHAN -> {
                return QuerySnippets.NumberSnippets.LESSTHAN(paramName, theParam)
            }
            ParamPrefixEnum.LESSTHAN_OR_EQUALS -> {
                return QuerySnippets.NumberSnippets.LESSTHAN_OR_EQUALS(paramName, theParam)
            }
            ParamPrefixEnum.EQUAL, null /* e.g. ?[parameter]=100 */ -> {
                val range = theParam.getSignificantRange()
                return QuerySnippets.NumberSnippets.EQUAL(paramName, range)
            }
            ParamPrefixEnum.NOT_EQUAL -> {
                val range = theParam.getSignificantRange()
                return QuerySnippets.NumberSnippets.NOT_EQUAL(paramName, range)
            }
            else -> {
                throw InvalidRequestException(Msg.code(1235) + theParam.toString())
            }
        }
    }

    private fun handleDateParam(theParam: DateParam, paramName: String) {
        val util = DateUtil(theParam)

        when (theParam.prefix) {
            ParamPrefixEnum.APPROXIMATE -> {

            }
            ParamPrefixEnum.GREATERTHAN -> {

            }
            ParamPrefixEnum.GREATERTHAN_OR_EQUALS -> {

            }
            ParamPrefixEnum.LESSTHAN -> {

            }
            ParamPrefixEnum.LESSTHAN_OR_EQUALS -> {

            }
            ParamPrefixEnum.EQUAL, null /* e.g. ?[parameter]=2022-01-01 */ -> {
                val range = util.dateRange

            }
            ParamPrefixEnum.NOT_EQUAL -> {

            }
            ParamPrefixEnum.STARTS_AFTER -> {

            }
            ParamPrefixEnum.ENDS_BEFORE -> {

            }
        }
    }



    fun mapPathToXPath(path: String): String {
        if (true) { //TODO: Add correct condition
            //TODO: Special path handling (or, and, where, exists, etc.)
            return ""
        } else {
            //Normal Path handling (. separated)
            return path.split(".").joinToString("/")
        }
    }

}