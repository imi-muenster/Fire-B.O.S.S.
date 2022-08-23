package de.uni_muenster.imi.fhirFacade.basex.generator

import ca.uhn.fhir.rest.param.NumberParam
import ca.uhn.fhir.rest.param.QuantityParam
import org.apache.commons.lang.math.DoubleRange

object QuerySnippets {

    /**
     * Some paths return sets if they exist multiple times (0..*).
     * That's why each searchParameter has to be processed in a for loop
     */
    fun parameterTemplate(searchParameterName: String, path: String) =
        "let \$${searchParameterName}_set := \$x/$path \n" +
                "for \$$searchParameterName in \$${searchParameterName}_set \n" +
                "#SNIPPETS \n"

    fun serverHistoryResourcePart(type: String) =
        "{\n" +
            "for \$x in db:open(\"$type\")\n" +
            "#OPTIONALSEARCHPARAMETERS\n" +
            "\n" +
            "return element result {\$x}\n" +
        "}\n"

    /* https://www.hl7.org/fhir/search.html#string */
    object StringSnippets {
        fun stringStart(searchParameterName: String, value: String) =
            "(starts-with(\$$searchParameterName/@value, \"$value\"))"

        fun stringContains(searchParameterName: String, value: String) =
            "(contains(\$$searchParameterName/@value, \"$value\"))"

        fun stringExact(searchParameterName: String, value: String) =
            "(\$$searchParameterName/@value = \"$value\")"

        fun contentParam(searchParameterName: String, value: String) =
            "(contains(\$$searchParameterName, \"$value\"))"

        fun listSearch(searchParameterName: String, value: String) =
            "( local:searchInList(\$$searchParameterName/id/@value, \$x/*/name(), \"$value\") )"

        fun textSearch(searchParameterName: String, value: String) =
            "( \$$searchParameterName contains text \"$value\" )"
    }

    /** https://www.hl7.org/fhir/search.html#number  **/
    object NumberSnippets {
        fun EQUAL(searchParameterName: String, value: DoubleRange) =
            "(\$$searchParameterName/@value >= ${value.minimumDouble} and " +
                    "\$$searchParameterName/@value < ${value.maximumDouble})"

        fun NOT_EQUAL(searchParameterName: String, value: DoubleRange) =
            "(\$$searchParameterName/@value < ${value.minimumDouble} or " +
                    "\$$searchParameterName/@value >= ${value.maximumDouble})"

        fun GREATERTHAN(searchParameterName: String, value: NumberParam) =
            "(${value.value.toDouble()} > \$$searchParameterName/@value)"

        fun LESSTHAN(searchParameterName: String, value: NumberParam) =
            "(${value.value.toDouble()} < \$$searchParameterName/@value)"

        fun GREATERTHAN_OR_EQUALS(searchParameterName: String, value: NumberParam) =
            "(${value.value.toDouble()} >= \$$searchParameterName/@value)"

        fun LESSTHAN_OR_EQUALS(searchParameterName: String, value: NumberParam) =
            "(${value.value.toDouble()} <= \$$searchParameterName/@value)"

        fun STARTS_AFTER(searchParameterName: String, value: NumberParam) =
            GREATERTHAN(searchParameterName, value)

        fun ENDS_BEFORE(searchParameterName: String, value: NumberParam) =
            LESSTHAN(searchParameterName, value)

        fun APPROXIMATE(searchParameterName: String, value: DoubleRange) =
            "(\$$searchParameterName/@value >= ${value.minimumDouble} and " +
                    "\$$searchParameterName/@value < ${value.maximumDouble})"
    }

    /* https://www.hl7.org/fhir/search.html#date */
    object DateSnippets {


        // Checks if sets do not overlap
        // (s_min < p_min and s_max < p_min) or (s_min > p_max and s_max > p_max)
        fun NOT_EQUAL(searchParameterName: String, value: String) =
            "(" +
                "(" +
                    "( local:getLowerDateRange(\"$value\") < local:getLowerDateRange(\$$searchParameterName/@value) ) and " +
                    "( local:getUpperDateRange(\"$value\") < local:getLowerDateRange(\$$searchParameterName/@value) )" +
                ") or" +
                "(" +
                    "( local:getLowerDateRange(\"$value\") > local:getUpperDateRange(\$$searchParameterName/@value) ) and " +
                    "( local:getUpperDateRange(\"$value\") > local:getUpperDateRange(\$$searchParameterName/@value) )" +
                ")" +
            ")"
        // Checks if sets overlap
        // (s_min >= p_min or s_max >= p_min) and (s_min <= p_max or s_max <= p_max)
        fun EQUAL(searchParameterName: String, value: String) =
            "(" +
                "(" +
                    "( local:getLowerDateRange(\"$value\") >= local:getLowerDateRange(\$$searchParameterName/@value) ) or " +
                    "( local:getUpperDateRange(\"$value\") >= local:getLowerDateRange(\$$searchParameterName/@value) )" +
                ") and" +
                "(" +
                    "( local:getLowerDateRange(\"$value\") <= local:getUpperDateRange(\$$searchParameterName/@value) ) or " +
                    "( local:getUpperDateRange(\"$value\") <= local:getUpperDateRange(\$$searchParameterName/@value) )" +
                ")" +
            ")"

        // (s_min < p_max or s_max < p_max)
        fun GREATERTHAN(searchParameterName: String, value: String) =
            "(" +
                "( local:getLowerDateRange(\"$value\") < local:getUpperDateRange(\$$searchParameterName/@value) ) or " +
                "( local:getUpperDateRange(\"$value\") < local:getUpperDateRange(\$$searchParameterName/@value) )" +
            ")"

        // (s_min > p_min or s_max > p_min)
        fun LESSTHAN(searchParameterName: String, value: String) =
            "(" +
                "( local:getLowerDateRange(\"$value\") > local:getLowerDateRange(\$$searchParameterName/@value) ) or " +
                "( local:getUpperDateRange(\"$value\") > local:getLowerDateRange(\$$searchParameterName/@value) )" +
            ")"

        // checks if sets overlap or is greater than (combination of conditions above)
        // ( (s_min >= p_min or s_max >= p_min) and (s_min <= p_max or s_max <= p_max) ) or ( s_min < p_max or s_max < p_max )
        fun GREATERTHAN_OR_EQUALS(searchParameterName: String, value: String) =
            "( ${EQUAL(searchParameterName, value)} or ${GREATERTHAN(searchParameterName, value)} )"

        // checks if sets overlap or is less than (combination of conditions above)
        // ( (s_min >= p_min or s_max >= p_min) and (s_min <= p_max or s_max <= p_max) ) or ( s_min > p_min or s_max > p_min )
        fun LESSTHAN_OR_EQUALS(searchParameterName: String, value: String) =
            "( ${EQUAL(searchParameterName, value)} or ${LESSTHAN(searchParameterName, value)}"

    }

    /* https://www.hl7.org/fhir/search.html#token */
    object TokenSnippets {
        fun textSearch(searchParameterName: String, text: String) =
            "(" +
                "( \$$searchParameterName//text contains text \"$text\" ) or " +
                "( \$$searchParameterName//display contains text \"$text\" )" +
            ")"
        fun searchForCode(searchParameterName: String, code: String) =
            "(" +
                "( \$$searchParameterName//code[fn:matches(@value, \"$code\")] ) or " +
                "( \$$searchParameterName//value[fn:matches(@value, \"$code\")] ) or " +
                "( \$$searchParameterName[fn:matches(@value, \"$code\", \"i\")] )" +
            ")"

        fun searchForSystem(searchParameterName: String, system: String) =
            "(" +
                "( \$$searchParameterName//system[fn:matches(@value, \"$system\")] )" +
            ")"

        fun searchForCodeAndSystem(searchParameterName: String, system: String, code: String) =
            "(" +
                "(" +
                    "( \$$searchParameterName//code[fn:matches(@value, \"$code\")] ) or " +
                    "( \$$searchParameterName//value[fn:matches(@value, \"$code\")] ) or " +
                    "( \$$searchParameterName[fn:matches(@value, \"$code\", \"i\")] )" +
                ") and " +
                "(" +
                    "( \$$searchParameterName//system[fn:matches(@value, \"$system\")] )" +
                ")" +
            ")"

        fun searchForCodeWithoutSystem(searchParameterName: String, code: String) =
            "(" +
                "(" +
                    "( \$$searchParameterName//code[fn:matches(@value, \"$code\")] ) or " +
                    "( \$$searchParameterName//value[fn:matches(@value, \"$code\")] )" +
                ") and " +
                "(" +
                    "( fn:not(exists(\$$searchParameterName//system)) )" +
                ")" +
            ") "

        fun searchOfType( searchParameterName: String, system: String, code: String, value: String) =
            "(" +
                "(" +
                    "( \$$searchParameterName//code[fn:matches(@value, \"$code\")] ) and " +
                    "( \$$searchParameterName//system[fn:matches(@value, \"$code\")] ) and " +
                    "( \$$searchParameterName//value[fn:matches(@value, \"$code\")] ) "

    }

    object QuantitySnippets {
        fun EQUAL(searchParameterName: String, value: DoubleRange) =
            "(\$$searchParameterName//value/@value >= ${value.minimumDouble} and " +
                    "\$$searchParameterName//value/@value < ${value.maximumDouble})"

        fun NOT_EQUAL(searchParameterName: String, value: DoubleRange) =
            "(\$$searchParameterName//value/@value < ${value.minimumDouble} or " +
                    "\$$searchParameterName//value/@value >= ${value.maximumDouble})"

        fun GREATERTHAN(searchParameterName: String, value: QuantityParam) =
            "(${value.value.toDouble()} > \$$searchParameterName//value/@value)"

        fun LESSTHAN(searchParameterName: String, value: QuantityParam) =
            "(${value.value.toDouble()} < \$$searchParameterName//value/@value)"

        fun GREATERTHAN_OR_EQUALS(searchParameterName: String, value: QuantityParam) =
            "(${value.value.toDouble()} >= \$$searchParameterName//value/@value)"

        fun LESSTHAN_OR_EQUALS(searchParameterName: String, value: QuantityParam) =
            "(${value.value.toDouble()} <= \$$searchParameterName//value/@value)"

        fun STARTS_AFTER(searchParameterName: String, value: QuantityParam) =
            GREATERTHAN(searchParameterName, value)

        fun ENDS_BEFORE(searchParameterName: String, value: QuantityParam) =
            LESSTHAN(searchParameterName, value)

        fun APPROXIMATE(searchParameterName: String, value: DoubleRange) =
            "(\$$searchParameterName//value/@value >= ${value.minimumDouble} and " +
                    "\$$searchParameterName//value/@value < ${value.maximumDouble})"

        fun searchForCodeAndSystem(searchParameterName: String, code: String, system: String) =
            "(" +
                "( \$$searchParameterName//code[fn:matches(@value, \"$code\")] ) and " +
                "( \$$searchParameterName//system[fn:matches(@value, \"$system\")] )" +
            ")"

        fun searchForCodeWithoutSystem(searchParameterName: String, code: String) =
            "(" +
                "( \$$searchParameterName//code[fn:matches(@value, \"$code\")] ) or " +
                "( \$$searchParameterName//unit[fn:matches(@value, \"$code\")] )" +
            ")"
    }

    object UriSnippets {
        fun searchForUriExact(searchParameterName: String, uri: String) =
            "(" +
                "( \$$searchParameterName[fn:matches(@value, \"$uri\")] )" +
            ")"

        fun searchForUriAbove(searchParameterName: String, uri: String) =
            "(" +
                "( \$$searchParameterName[contains(\"$uri\", @value)] )" +
            ")"

        fun searchForUriBelow(searchParameterName: String, uri: String) =
            "(" +
                "( \$$searchParameterName[starts-with(@value, \"$uri\")] )" +
            ")"
    }

    object ReferenceSnippets {

        fun searchForId(searchParameterName: String, idValue: String) =
            "(" +
                "( \$$searchParameterName//reference[contains(@value, \"$idValue\")] )" +
            ")"

        fun searchForVersionedId(searchParameterName: String, idValue: String, versionId: String) =
            "(" +
                "( \$$searchParameterName//reference[contains(@value, \"$idValue/_history/$versionId\")] )" +
            ")"

        fun searchForIdAndType(searchParameterName: String, idValue: String, type: String) =
            "(" +
                "( \$$searchParameterName//reference[contains(@value, \"$type/$idValue\")] )" +
            ")"

        fun searchForVersionedIdAndType(searchParameterName: String, idValue: String, type: String, versionId: String) =
            "(" +
                "( \$$searchParameterName//reference[contains(@value, \"$type/$idValue/_history/$versionId\")] )" +
            ")"
        fun chainedQuery(searchParameterName: String, chainedParameterName: String, chainedValue: String) =
            "(" +
                "( local:performChainedQuery(\$$searchParameterName//@value, \"$chainedParameterName\", \"$chainedValue\") )" +
            ")"
    }

    object SpecialQueries {

        fun locationNearWithDistance(
            searchParameterName: String,
            latitude: String,
            longitude: String,
            distance: String
        ) =
            "(" +
                "(local:euclideanDistance(" +
                    "\$$searchParameterName//latitude/@value, " +
                    "\$$searchParameterName//longitude/@value, " +
                    "\"$latitude\", " +
                    "\"$longitude\"" +
                ") < xs:double($distance))" +
            ")"

        fun locationNearWithoutDistance(
            searchParameterName: String,
            latitude: String,
            longitude: String
        ) =
            "(" +
                "(local:euclideanDistance(" +
                    "\$$searchParameterName//latitude/@value, " +
                    "\$$searchParameterName//longitude/@value, " +
                    "\"$latitude\", " +
                    "\"$longitude\"" +
                ") < xs:double(200))" +
            ")"
    }

    object HasSnippets {
        fun hasQuery(
                referencePath: String,
                parameterPath: String,
                parameterValue: String,
                targetResourceType: String,
                searchParameterName: String
        ) =
            "(" +
                "( local:performReverseChainedQuery(" +
                    "\'$referencePath\', " +
                    "\$$searchParameterName/id/@value, " +
                    "\'$parameterPath\'," +
                    "\'$parameterValue\'," +
                    "\'$targetResourceType\'" +
                ") )" +
            ")"
    }

}