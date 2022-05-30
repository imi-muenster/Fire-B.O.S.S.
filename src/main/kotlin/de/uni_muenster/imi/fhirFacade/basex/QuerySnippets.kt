package de.uni_muenster.imi.fhirFacade.basex

import ca.uhn.fhir.rest.param.NumberParam
import de.uni_muenster.imi.fhirFacade.fhir.helper.DateRange
import org.apache.commons.lang.math.DoubleRange

object QuerySnippets {

    /**
     * Some paths return sets if they exist multiple times.
     * That's why each searchParameter has to be processed in a for loop
     */
    fun parameterTemplate(searchParameterName: String, path: String) =
        "let \$${searchParameterName}_set := \$x/$path/@value \n" +
                "for \$$searchParameterName in \$${searchParameterName}_set \n" +
                "where #SNIPPETS"

    /** STRING SNIPPETS: https://www.hl7.org/fhir/search.html#string **/
    object StringSnippets {
        fun stringStart(searchParameterName: String, value: String) =
            "(starts-with(\$$searchParameterName, \"$value\"))"

        fun stringContains(searchParameterName: String, value: String) =
            "(contains(\$$searchParameterName, \"$value\"))"

        fun stringExact(searchParameterName: String, value: String) =
            "(\$$searchParameterName = \"$value\")"
    }

    /** NUMBER SNIPPETS: https://www.hl7.org/fhir/search.html#number  **/
    object NumberSnippets {
        fun EQUAL(searchParameterName: String, value: DoubleRange) =
            "(\$$searchParameterName >= ${value.minimumDouble} and \$$searchParameterName < ${value.maximumDouble})"

        fun NOT_EQUAL(searchParameterName: String, value: DoubleRange) =
            "(\$$searchParameterName < ${value.minimumDouble} and \$$searchParameterName >= ${value.maximumDouble})"

        fun GREATERTHAN(searchParameterName: String, value: NumberParam) =
            "(${value.value.toDouble()} > \$$searchParameterName)"

        fun LESSTHAN(searchParameterName: String, value: NumberParam) =
            "(${value.value.toDouble()} < \$$searchParameterName)"

        fun GREATERTHAN_OR_EQUALS(searchParameterName: String, value: NumberParam) =
            "(${value.value.toDouble()} >= \$$searchParameterName)"

        fun LESSTHAN_OR_EQUALS(searchParameterName: String, value: NumberParam) =
            "(${value.value.toDouble()} <= \$$searchParameterName)"

        fun STARTS_AFTER(searchParameterName: String, value: NumberParam) =
            GREATERTHAN(searchParameterName, value)

        fun ENDS_BEFORE(searchParameterName: String, value: NumberParam) =
            LESSTHAN(searchParameterName, value)

        fun APPROXIMATE(searchParameterName: String, value: DoubleRange) =
            "(\$$searchParameterName >= ${value.minimumDouble} and \$$searchParameterName < ${value.maximumDouble})"
    }

    object DateSnippets {
        /**
         *TODO:
         * Problem: A date e.g. 2013-01-01 actually means a dateTime Range 2013-01-01T00:00:00.000 - 2013-01-01T23:59:59:999
         * Currently the date is always transformed to 2013-01-01T00:00:00.000.
         * Thus a query searching for 2013-01-01T12:00:00 does not match with fields with 2013-01-01, but they should
         * Possible Solution:
         * Always create a set of two dateTimes in Xquery
         * if a dateTime is successfully casted those are identical (resulting in redundant conditions)
         * if a date is casted those create the range as described above
         */
        fun catchDateProperties(searchParameterName: String): String {
            return "let \$${searchParameterName}_conv :=  \n" +
                    "  try {\n" +
                    "      \$$searchParameterName cast as xs:dateTime\n" +
                    "      \n" +
                    "  } catch * {\n" +
                    "  \ttry {\n" +
                    "  \t\t(\$$searchParameterName cast as xs:date) cast as xs:dateTime\n" +
                    "    } catch * {\n" +
                    "    \t\$err:code || \$err:description\n" +
                    "    }\n" +
                    "  }"
        }
        //TODO: Adjust to Query with set
        fun EQUAL(searchParameterName: String, range: DateRange) =
            "(" +
                "( xs:dateTime(\"${range.min}\") <= \$${searchParameterName}_conv ) and " +
                "( xs:dateTime(\"${range.max}\" >= \$${searchParameterName}_conv" +
            ")"

    }

}