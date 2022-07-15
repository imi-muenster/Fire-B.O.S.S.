package de.uni_muenster.imi.fhirFacade.basex.generator

import ca.uhn.fhir.i18n.Msg
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.rest.param.*
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException
import de.uni_muenster.imi.fhirFacade.basex.BaseXQueries
import de.uni_muenster.imi.fhirFacade.basex.generator.XPathMapper.mapPathToXPath
import de.uni_muenster.imi.fhirFacade.fhir.getSearchParameterMap
import de.uni_muenster.imi.fhirFacade.fhir.helper.SignificanceHelper
import org.apache.commons.lang.StringUtils
import javax.management.Query
import kotlin.collections.HashMap

class QueryGenerator {

    fun generateQuery(parameterMap: SearchParameterMap, pathMap: HashMap<String, String>, fhirType: String): String {
        //TODO: Process constants

        //TODO: Process _has and _id

        //TODO: Process different optional ParamTypes and create Queries from Snippets accordingly
        val searchParameterPart = handleSearchParameterMap(parameterMap.getSearchParameterMap(), pathMap)
        //TODO: Process result Parameters
        val testQuery = BaseXQueries.performSearch(fhirType, "", searchParameterPart)

        return testQuery
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

        theList.forEachIndexed { index, theParam ->
            if (index == 0) {
                result += handleBaseParameter(theParam, paramName)
            } else {
                result += " or ${handleBaseParameter(theParam, paramName)}"
            }
        }
        result = "where ($result)"

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
                handleTokenParam(theParam as TokenParam, paramName)
            }
            ReferenceParam::class.java -> {
                ""
            }
            CompositeParam::class.java -> {
                ""
            }
            QuantityParam::class.java -> {
                handleQuantityParam(theParam as QuantityParam, paramName)
            }
            UriParam::class.java -> {
                handleURIParam(theParam as UriParam, paramName)
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
                val range = SignificanceHelper.getSignificantRange(theParam)
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
                val range = SignificanceHelper.getSignificantRange(theParam)
                return QuerySnippets.NumberSnippets.EQUAL(paramName, range)
            }
            ParamPrefixEnum.NOT_EQUAL -> {
                val range = SignificanceHelper.getSignificantRange(theParam)
                return QuerySnippets.NumberSnippets.NOT_EQUAL(paramName, range)
            }
            else -> {
                throw InvalidRequestException(Msg.code(1235) + theParam.toString())
            }
        }
    }

    private fun handleDateParam(theParam: DateParam, paramName: String): String {
        //TODO: Time Zones?
        when (theParam.prefix) {
            ParamPrefixEnum.GREATERTHAN -> {
                return QuerySnippets.DateSnippets.GREATERTHAN(paramName, theParam.valueAsString)
            }
            ParamPrefixEnum.GREATERTHAN_OR_EQUALS -> {
                return QuerySnippets.DateSnippets.GREATERTHAN_OR_EQUALS(paramName, theParam.valueAsString)
            }
            ParamPrefixEnum.LESSTHAN -> {
                return QuerySnippets.DateSnippets.LESSTHAN(paramName, theParam.valueAsString)
            }
            ParamPrefixEnum.LESSTHAN_OR_EQUALS -> {
                return QuerySnippets.DateSnippets.LESSTHAN_OR_EQUALS(paramName, theParam.valueAsString)
            }
            ParamPrefixEnum.EQUAL, null /* e.g. ?[parameter]=2022-01-01 */ -> {
                //TODO: Equal prefix creates two lists (possible error in HAPI?)
                return QuerySnippets.DateSnippets.EQUAL(paramName, theParam.valueAsString)
            }
            ParamPrefixEnum.NOT_EQUAL -> {
                return QuerySnippets.DateSnippets.NOT_EQUAL(paramName, theParam.valueAsString)
            }
            else -> {
                throw InvalidRequestException(Msg.code(1235) + theParam.toString())
            }
        }
    }

    private fun handleTokenParam(theParam: TokenParam, paramName: String): String {
        var queryModifier = "#REPLACE"
        //Handle modifiers separately
        if (theParam.modifier != null) {
            when (theParam.modifier.name) {
                "TEXT" -> { return QuerySnippets.TokenSnippets.textSearch(paramName, theParam.value)}
                "NOT" -> {queryModifier = "(fn:not(#REPLACE))"}
                "OF_TYPE" -> {
                    //All three have to be present
                    if (theParam.system == null || !theParam.value.contains("|")) {
                        throw InvalidRequestException(Msg.code(1235) + theParam.toString())
                    }
                    val system = theParam.system
                    val (code, value) = theParam.value.split("|")
                    return QuerySnippets.TokenSnippets.searchOfType(paramName, system, code, value)
                }
                else -> {throw NotImplementedOperationException(Msg.code(501) + theParam.modifier.value)}
            }
        }

        val query: String
        // [parameter]=[code]
        if (theParam.system == null && theParam.value != null) {
            query = QuerySnippets.TokenSnippets.searchForCode(paramName, theParam.value)
        }
        else if (theParam.system != null && theParam.value != null) {
            // [parameter]=[system]|[code]
            if (StringUtils.isNotBlank(theParam.system) && StringUtils.isNotBlank(theParam.value)) {
                query = QuerySnippets.TokenSnippets.searchForCodeAndSystem(paramName, theParam.system, theParam.value)
            }
            // [parameter]=|[code]
            else if (StringUtils.isBlank(theParam.system) && StringUtils.isNotBlank(theParam.value)) {
                query = QuerySnippets.TokenSnippets.searchForCodeWithoutSystem(paramName, theParam.value)
            }
            // [parameter]=[system]|
            else if (StringUtils.isNotBlank(theParam.system) && StringUtils.isBlank(theParam.value)) {
                query = QuerySnippets.TokenSnippets.searchForSystem(paramName, theParam.system)
            }
            else {
                throw InvalidRequestException(Msg.code(1235) + theParam.toString())
            }
        }
        else {
            throw InvalidRequestException(Msg.code(1235) + theParam.toString())
        }
        return queryModifier.replace("#REPLACE", query)
    }

    private fun handleQuantityParam(theParam: QuantityParam, paramName: String): String {
        //TODO: Conversion of units in the future?
        if (theParam.value == null) {
            //TODO: Error
        }
        var query = ""

        when(theParam.prefix) {
            ParamPrefixEnum.EQUAL, null -> {
                val range = SignificanceHelper.getSignificantRange(theParam)
                query += QuerySnippets.QuantitySnippets.EQUAL(paramName, range)
            }
            ParamPrefixEnum.NOT_EQUAL -> {
                val range = SignificanceHelper.getSignificantRange(theParam)
                query += QuerySnippets.QuantitySnippets.NOT_EQUAL(paramName, range)
            }
            ParamPrefixEnum.GREATERTHAN -> {
                query += QuerySnippets.QuantitySnippets.GREATERTHAN(paramName, theParam)
            }
            ParamPrefixEnum.GREATERTHAN_OR_EQUALS -> {
                query += QuerySnippets.QuantitySnippets.GREATERTHAN_OR_EQUALS(paramName, theParam)
            }
            ParamPrefixEnum.LESSTHAN -> {
                query += QuerySnippets.QuantitySnippets.LESSTHAN(paramName, theParam)
            }
            ParamPrefixEnum.LESSTHAN_OR_EQUALS -> {
                query += QuerySnippets.QuantitySnippets.LESSTHAN_OR_EQUALS(paramName, theParam)
            }
            ParamPrefixEnum.APPROXIMATE -> {
                val range = SignificanceHelper.getSignificantRange(theParam)
                query += QuerySnippets.QuantitySnippets.APPROXIMATE(paramName, range)
            }
            ParamPrefixEnum.STARTS_AFTER -> {
                query += QuerySnippets.QuantitySnippets.STARTS_AFTER(paramName, theParam)
            }
            ParamPrefixEnum.ENDS_BEFORE -> {
                query += QuerySnippets.QuantitySnippets.ENDS_BEFORE(paramName, theParam)
            }
            else -> {
                //TODO: Error code
            }
        }
        if (theParam.system != null && theParam.units != null) {
            query += " and ${QuerySnippets.QuantitySnippets.searchForCodeAndSystem(paramName, theParam.units, theParam.system)}"
        }
        if (theParam.units != null && theParam.system == null) {
            query += " and ${QuerySnippets.QuantitySnippets.searchForCodeWithoutSystem(paramName, theParam.units)}"
        }
        return "($query)"
    }

    private fun handleURIParam(theParam: UriParam, paramName: String): String {
        //TODO: :urn ?
        return if (theParam.qualifier != null) {
            when (theParam.qualifier) {
                UriParamQualifierEnum.ABOVE -> {
                    QuerySnippets.UriSnippets.searchForUriAbove(paramName, theParam.value)
                }
                UriParamQualifierEnum.BELOW -> {
                    QuerySnippets.UriSnippets.searchForUriBelow(paramName, theParam.value)
                }
            }
        } else {
            QuerySnippets.UriSnippets.searchForUriExact(paramName, theParam.value)
        }
    }
}