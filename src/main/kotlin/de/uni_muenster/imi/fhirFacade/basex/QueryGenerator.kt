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
import java.util.*
import kotlin.collections.HashMap

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

        result = result.substring(0, result.lastIndexOf(" or "))

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
                handleDateParam(theParam as DateParam, paramName)
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

    private fun handleDateParam(theParam: DateParam, paramName: String): String {
        val util = DateUtil(theParam)

        when (theParam.prefix) {
            ParamPrefixEnum.APPROXIMATE -> {
                return ""
            }
            ParamPrefixEnum.GREATERTHAN -> {
                return ""

            }
            ParamPrefixEnum.GREATERTHAN_OR_EQUALS -> {
                return ""

            }
            ParamPrefixEnum.LESSTHAN -> {
                return ""
            }
            ParamPrefixEnum.LESSTHAN_OR_EQUALS -> {
                return ""
            }
            ParamPrefixEnum.EQUAL, null /* e.g. ?[parameter]=2022-01-01 */ -> {
                //TODO: Equal prefix creates two lists (possible error in HAPI?)
                val range = util.dateRange
                return QuerySnippets.DateSnippets.catchDateProperties(paramName) +
                        QuerySnippets.DateSnippets.EQUAL(paramName, range)
            }
            ParamPrefixEnum.NOT_EQUAL -> {
                return ""
            }
            ParamPrefixEnum.STARTS_AFTER -> {
                return ""
            }
            ParamPrefixEnum.ENDS_BEFORE -> {
                return ""
            }
            else -> {
                throw InvalidRequestException(Msg.code(1235) + theParam.toString())
            }
        }
    }



    fun mapPathToXPath(path: String): String {
        //TODO: First check for "|" or "and", split and handle parts recursively (?), connect with "where ..."
        //Special path handling
        return if (path.contains("where")) {
            handlePathWithWhere(path)
        } else if (path.contains(" as ") || path.contains(".as(")) {
            handlePathWithAs(path)
        } else { //Normal Path handling (. separated)
            path.split(".").joinToString("/")
        }
    }

    private fun handlePathWithWhere(path: String): String {
        val partBefore = path.substringBefore(".where")
        val partWhere = path.substringAfter("$partBefore.")

        if (partWhere.contains("resolve()")) {
            val resolveType = partWhere.substringAfter("resolve() is ")
            //TODO: Evaluate: with or without /.. at the end. Should be without as the reference is the important bit
            return "${partBefore.split(".").joinToString("/")}/reference[contains(@value, \"$resolveType\")]"
        } else {
            val partAfter = partWhere.substringAfter(".", "")

            //looks like this: where(property='value')
            val property = partWhere
                .substringAfter("(")
                .substringBefore("=")
            val value = partWhere
                .substringAfter("='")
                .substringBefore("')")

            return "${partBefore.split(".").joinToString("/")}/$property[contains(@value, \"$value\")]/.." +
                    if (partAfter.isNotEmpty()) "/$partAfter" else ""
        }
    }

    private fun handlePathWithAs(path: String): String {
        val partBefore: String
        val partAfter: String
        if (path.contains(" as ")) {
            partBefore = path.substringBefore(" as ")
            partAfter = path
                .substringAfter(" as ")
                .capitalize()
        } else {
            partBefore = path.substringBefore(".as(")
            partAfter = path
                .substringAfter(".as(")
                .substringBefore(")")
                .capitalize()
        }
        return "${partBefore.split(".").joinToString("/")}$partAfter"
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault())
            else it.toString()
        }
    }
}