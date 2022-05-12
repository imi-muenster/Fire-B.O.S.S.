package de.uni_muenster.imi.fhirFacade.basex

object QuerySnippets {

    fun eqDateTime(path: String, dateTime: String) =
        "(xs:dateTime(\$x/../$path) = xs:dateTime('$dateTime'))"

    fun eqDate(path: String, date: String, pathIsDateTime: Boolean = true) =
        if (pathIsDateTime) {
            "(xs:date(substring-before(\$x/../$path), 'T') = xs:date('$date'))"
        } else {
            "(xs:date(\$x/../$path) = xs:date('$date'))"
        }

    fun neDateTime(path: String, dateTime: String) =
        "(xs:dateTime(\$x/../$path) > xs:dateTime('$dateTime') or " +
        "xs:dateTime(\$x/../$path) < xs:dateTime('$dateTime'))"

}