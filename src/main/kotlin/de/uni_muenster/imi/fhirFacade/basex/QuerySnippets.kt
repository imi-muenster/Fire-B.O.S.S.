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
        "let \$${searchParameterName}_set := \$x/$path \n" +
                "for \$$searchParameterName in \$${searchParameterName}_set \n" +
                "#SNIPPETS"

    /** STRING SNIPPETS: https://www.hl7.org/fhir/search.html#string **/
    object StringSnippets {
        fun stringStart(searchParameterName: String, value: String) =
            "where (starts-with(\$$searchParameterName/@value, \"$value\"))"

        fun stringContains(searchParameterName: String, value: String) =
            "where (contains(\$$searchParameterName/@value, \"$value\"))"

        fun stringExact(searchParameterName: String, value: String) =
            "where (\$$searchParameterName/@value = \"$value\")"
    }

    /** NUMBER SNIPPETS: https://www.hl7.org/fhir/search.html#number  **/
    object NumberSnippets {
        fun EQUAL(searchParameterName: String, value: DoubleRange) =
            "where (\$$searchParameterName/@value >= ${value.minimumDouble} and " +
                    "\$$searchParameterName/@value < ${value.maximumDouble})"

        fun NOT_EQUAL(searchParameterName: String, value: DoubleRange) =
            "where (\$$searchParameterName/@value < ${value.minimumDouble} and " +
                    "\$$searchParameterName/@value >= ${value.maximumDouble})"

        fun GREATERTHAN(searchParameterName: String, value: NumberParam) =
            "where (${value.value.toDouble()} > \$$searchParameterName/@value)"

        fun LESSTHAN(searchParameterName: String, value: NumberParam) =
            "where (${value.value.toDouble()} < \$$searchParameterName/@value)"

        fun GREATERTHAN_OR_EQUALS(searchParameterName: String, value: NumberParam) =
            "where (${value.value.toDouble()} >= \$$searchParameterName/@value)"

        fun LESSTHAN_OR_EQUALS(searchParameterName: String, value: NumberParam) =
            "where (${value.value.toDouble()} <= \$$searchParameterName/@value)"

        fun STARTS_AFTER(searchParameterName: String, value: NumberParam) =
            GREATERTHAN(searchParameterName, value)

        fun ENDS_BEFORE(searchParameterName: String, value: NumberParam) =
            LESSTHAN(searchParameterName, value)

        fun APPROXIMATE(searchParameterName: String, value: DoubleRange) =
            "where (\$$searchParameterName/@value >= ${value.minimumDouble} and " +
                    "\$$searchParameterName/@value < ${value.maximumDouble})"
    }

    //DATE SNIPPETS: https://www.hl7.org/fhir/search.html#date
    object DateSnippets {
        /**
         * Problem: A date e.g. 2013-01-01 actually means a dateTime Range 2013-01-01T00:00:00.000 - 2013-01-01T23:59:59:999
         * This range has to be considered, otherwise a query searching for 2013-01-01T12:00:00 does not match with
         * fields with 2013-01-01, but it should
         * Solution:
         * Always create a set of two dateTimes in Xquery
         * if a dateTime is successfully casted those are identical (resulting in redundant conditions, which is acceptable)
         * if a date is casted those create the range as described above
         */
        fun catchDateProperties(searchParameterName: String): String {
            return "let \$${searchParameterName}_conv :=\n" +
                    "  try {\n" +
                    "      [\$$searchParameterName/@value cast as xs:dateTime,\n" +
                    "      \$$searchParameterName/@value cast as xs:dateTime]\n" +
                    "\n" +
                    "  } catch * {\n" +
                    "  \ttry {\n" +
                    "  \t\t[fn:dateTime((\$$searchParameterName/@value cast as xs:date), xs:time(\"00:00:00\")),\n" +
                    "        fn:dateTime((\$$searchParameterName/@value cast as xs:date), xs:time(\"23:59:59.999\"))]\n" +
                    "    } catch * {\n" +
                    "    \t\$err:code || \$err:description\n" +
                    "    }\n" +
                    "  }\n"
        }

        // Checks if sets overlap
        // (s_min >= p_min or s_max >= p_min) and (s_max <= p_max or s_max <= p_max)
        fun EQUAL(searchParameterName: String, range: DateRange) =
            "where (\n" +
                "\t(\n" +
                    "\t\t( xs:dateTime(\"${range.min}\") >= array:get(\$${searchParameterName}_conv, 1) ) or \n" +
                    "\t\t( xs:dateTime(\"${range.max}\") >= array:get(\$${searchParameterName}_conv, 1) )\n" +
                "\t) and " +
                "\t( " +
                    "\t\t( xs:dateTime(\"${range.min}\") <= array:get(\$${searchParameterName}_conv, 2) ) or \n" +
                    "\t\t( xs:dateTime(\"${range.max}\") <= array:get(\$${searchParameterName}_conv, 2) )\n" +
                "\t)\n" +
            ")\n"

    }

}