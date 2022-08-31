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
import de.uni_muenster.imi.fhirFacade.fhir.helper.PathMapUtil
import de.uni_muenster.imi.fhirFacade.fhir.helper.SignificanceHelper
import org.apache.commons.lang.StringUtils
import kotlin.collections.HashMap

class QueryGenerator {
    //TODO: Create Error code register

    /* TODO:
        Improve chaining: Currently it just uses the parameterName from the query.
        However the path is often different from the parameter name. (e.g. Patient?organization -> Patient.managingOrganization)
        The Path from paramPath could be used (see _has Query). However the Resource type is only known at Query Runtime.
        Another way could be to keep track of all possible references for a field and pass every possible path to the
        chained Query
     */


    fun generateQuery(parameterMap: SearchParameterMap, pathMap: HashMap<String, String>, fhirType: String): String {
        val searchParameterPart = handleSearchParameterMap(parameterMap.getSearchParameterMap(), pathMap)
        //TODO: Process search result parameters
        val query = BaseXQueries.performSearch(fhirType, searchParameterPart)

        return query
    }

    fun getHistoryForType(parameterMap: SearchParameterMap, pathMap: HashMap<String, String>, fhirType: String): String {
        val searchParameterMap = handleSearchParameterMap(parameterMap.getSearchParameterMap(), pathMap)

        return QuerySnippets.serverHistoryResourcePart(fhirType)
            .replace("#OPTIONALSEARCHPARAMETERS", searchParameterMap)
    }

    private fun handleSearchParameterMap(theMap: HashMap<String, List<List<IQueryParameterType>>>,
                                         pathMap: HashMap<String, String>
    ): String {
        return buildString {
            for ((searchParam, orList) in theMap) {
                for(andList in orList) {
                    val path = mapPathToXPath(pathMap[searchParam]!!)
                    append(handleListEntry(searchParam, path, andList))
                }
            }
        }
    }

    private fun handleListEntry(paramName: String, paramPath: String, theList: List<IQueryParameterType>): String {
            val result = theList.joinToString(" or ", "where(", ")") {
                handleParameter(it, paramName)
            }

            return QuerySnippets.parameterTemplate(paramName, paramPath).replace(
                "#SNIPPETS", result
            )
    }

    private fun handleParameter(theParam: IQueryParameterType, paramName: String): String {
        return when (paramName) {
            //handle special parameters
            "_content" -> {
                handleContentParam(theParam as StringParam, paramName)
            }
            "_filter" -> {
                handleFilterParam(theParam as StringParam)
            }
            "_list" -> {
                handleListParam(theParam as StringParam, paramName)
            }
            "_has" -> {
                handleHasParam(theParam as HasParam, paramName)
            }
            "_text" -> {
                handleTextParam(theParam as StringParam, paramName)
            }
            //handle base parameters
            else -> {
                handleBaseParameter(theParam, paramName)
            }
        }
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
                "" //Does not appear in any ResourceProvider
            }
            StringParam::class.java -> {
                handleStringParam(theParam as StringParam, paramName)
            }
            TokenParam::class.java -> {
                handleTokenParam(theParam as TokenParam, paramName)
            }
            ReferenceParam::class.java -> {
                handleReferenceParam(theParam as ReferenceParam, paramName)
            }
            CompositeParam::class.java -> {
                handleCompositeParam(theParam as CompositeParam<IQueryParameterType, IQueryParameterType>, paramName)
            }
            QuantityParam::class.java -> {
                handleQuantityParam(theParam as QuantityParam, paramName)
            }
            UriParam::class.java -> {
                handleURIParam(theParam as UriParam, paramName)
            }
            SpecialParam::class.java -> {
                handleSpecialParam(theParam as SpecialParam, paramName)
            }
            else -> {
                throw InvalidRequestException(Msg.code(1235) + theParam.toString())
            }
        }
    }

    /** TODO: Implement this logic
     * This is a simple implementation of the Filter Parameter.
     * For now, it allows to compose more complex Queries with "x (and | or) (+ not) x" on the search Parameters of a resource.
     * The logic creates Parameters by dissecting the Query String and matching it to the search parameters.
     * The normal Query logic is then applied.
     *
     * Operators are limited for each search Parameter. Unsupported Operators throw an exception.
     * Additional Parameters are not implemented (https://www.hl7.org/fhir/search_filter.html#params).
     * Concept Operators (ss, sb, in, ni) are not supported as this logic is not implemented by the server.
     */

    private fun handleFilterParam(theParam: StringParam): String {
        throw NotImplementedOperationException(Msg.code(501) + theParam.toString() +
                ". Please use the standard search syntax.")
    }

    private fun handleContentParam(theParam: StringParam, paramName: String): String {
        return QuerySnippets.StringSnippets.contentParam(paramName, theParam.value)
    }

    private fun handleListParam(theParam: StringParam, paramName: String): String {
        return QuerySnippets.StringSnippets.listSearch(paramName, theParam.value)
    }

    private fun handleHasParam(theParam: HasParam, paramName: String): String {
        if (theParam.parameterName == null ||
            theParam.parameterValue == null ||
            theParam.referenceFieldName == null ||
            theParam.targetResourceType == null) {
            throw InvalidRequestException(Msg.code(1235) + "Incomplete _has Parameter. Please correct it. $theParam")
        }
        if (theParam.parameterName.contains("_has")) {
            //TODO: Make _has chaining work (HAPI does not support it yet)
            throw InvalidRequestException(Msg.code(1235) + "_has chaining is not supported yet. $theParam")
        }

        val pathMap = PathMapUtil.getPathMap()
        val referencePath = mapPathToXPath(pathMap[theParam.targetResourceType]!![theParam.referenceFieldName]!!)
        val parameterPath = mapPathToXPath(pathMap[theParam.targetResourceType]!![theParam.parameterName]!!)

        return QuerySnippets.HasSnippets.hasQuery(
            referencePath = referencePath,
            parameterPath = parameterPath,
            parameterValue = theParam.parameterValue,
            targetResourceType = theParam.targetResourceType,
            searchParameterName = paramName
        )

    }

    private fun handleTextParam(theParam: StringParam, paramName: String): String {
        return QuerySnippets.StringSnippets.textSearch(paramName, theParam.value)
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
        //TODO: Add logic for Time Zones
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
                //Equal prefix creates two lists (possible error in HAPI?); Can be ignored as this just creates redundant conditions
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
        //TODO: Conversion of units in the future (e.g. kg -> mg)
        if (theParam.value == null) {
            throw InvalidRequestException(Msg.code(1235) + theParam.toString())
        }

        val query = buildString {
            when (theParam.prefix) {
                ParamPrefixEnum.EQUAL, null -> {
                    val range = SignificanceHelper.getSignificantRange(theParam)
                    append(QuerySnippets.QuantitySnippets.EQUAL(paramName, range))
                }
                ParamPrefixEnum.NOT_EQUAL -> {
                    val range = SignificanceHelper.getSignificantRange(theParam)
                    append(QuerySnippets.QuantitySnippets.NOT_EQUAL(paramName, range))
                }
                ParamPrefixEnum.GREATERTHAN -> {
                    append(QuerySnippets.QuantitySnippets.GREATERTHAN(paramName, theParam))
                }
                ParamPrefixEnum.GREATERTHAN_OR_EQUALS -> {
                    append(QuerySnippets.QuantitySnippets.GREATERTHAN_OR_EQUALS(paramName, theParam))
                }
                ParamPrefixEnum.LESSTHAN -> {
                    append(QuerySnippets.QuantitySnippets.LESSTHAN(paramName, theParam))
                }
                ParamPrefixEnum.LESSTHAN_OR_EQUALS -> {
                    append(QuerySnippets.QuantitySnippets.LESSTHAN_OR_EQUALS(paramName, theParam))
                }
                ParamPrefixEnum.APPROXIMATE -> {
                    val range = SignificanceHelper.getSignificantRange(theParam)
                    append(QuerySnippets.QuantitySnippets.APPROXIMATE(paramName, range))
                }
                ParamPrefixEnum.STARTS_AFTER -> {
                    append(QuerySnippets.QuantitySnippets.STARTS_AFTER(paramName, theParam))
                }
                ParamPrefixEnum.ENDS_BEFORE -> {
                    append(QuerySnippets.QuantitySnippets.ENDS_BEFORE(paramName, theParam))
                }
                else -> {
                    throw InvalidRequestException(Msg.code(1235) + theParam.toString())
                }
            }
            if (theParam.system != null && theParam.units != null) {
                append(" and ${
                    QuerySnippets.QuantitySnippets.searchForCodeAndSystem(
                        paramName,
                        theParam.units,
                        theParam.system
                    )
                }")
            }
            if (theParam.units != null && theParam.system == null) {
                append(" and ${QuerySnippets.QuantitySnippets.searchForCodeWithoutSystem(paramName, theParam.units)}")
            }
        }
        return "($query)"
    }

    private fun handleURIParam(theParam: UriParam, paramName: String): String {
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

    private fun handleReferenceParam(theParam: ReferenceParam, paramName: String): String {
        //TODO: Resource Hierarchy (:above, :below) not supported by HAPI Server: https://www.hl7.org/fhir/references.html#circular
        //TODO: Improve canonical reference logic (http://abc.cde/fgh|1.234 should match on http://abc.cde/fgh)
        if (theParam.chain != null) {
            //TODO: Chained Query on versioned references not supported yet
            return QuerySnippets.ReferenceSnippets.chainedQuery(paramName, theParam.chain, theParam.value)
        }

        var versionId: String? = null

        if (theParam.value.contains("_history")) {
            versionId = theParam.value.substringAfter("_history/")
        }

        //Search just for ID, every Type
        return if (theParam.idPart != null && theParam.resourceType == null) {
            if (versionId != null) {
                QuerySnippets.ReferenceSnippets.searchForVersionedId(paramName, theParam.idPart, versionId)
            } else {
                QuerySnippets.ReferenceSnippets.searchForId(paramName, theParam.idPart)
            }

        //Search for ID with specific Type
        } else if (theParam.idPart != null && theParam.resourceType != null) {
            if (versionId != null) {
                QuerySnippets.ReferenceSnippets.searchForVersionedIdAndType(
                    paramName, theParam.idPart, theParam.resourceType, versionId
                )
            } else {
                QuerySnippets.ReferenceSnippets.searchForIdAndType(
                    paramName, theParam.idPart, theParam.resourceType
                )
            }
        } else {
            throw InvalidRequestException(Msg.code(1235) + theParam.toString())
        }
    }

    private fun handleCompositeParam(theParam: CompositeParam<IQueryParameterType, IQueryParameterType>, paramName: String): String {
        return "( ${handleParameter(theParam.leftValue, paramName)} and ${handleParameter(theParam.rightValue, paramName)} )"
    }

    private fun handleSpecialParam(theParam: SpecialParam, paramName: String): String {
        //Only special Param in R4 is "near" for Location
        if (paramName == "near") {
            val coords = theParam.value.split("|")
            if (coords.size < 4) {
                throw InvalidRequestException(Msg.code(1235) + theParam.toString())
            }
            //latitude|longitude|distance|unit -> unit is not important
            return if (coords[2].isNotEmpty()) {
                QuerySnippets.SpecialQueries.locationNearWithDistance(paramName, coords[0], coords[1], coords[2])
            } else {
                QuerySnippets.SpecialQueries.locationNearWithoutDistance(paramName, coords[0], coords[1])
            }
        } else {
            throw NotImplementedOperationException(Msg.code(501) + theParam.toString())
        }
    }


}