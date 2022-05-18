package de.uni_muenster.imi.fhirFacade.basex

object QuerySnippets {

    /** PREFIXES **/

    fun prefixEq(searchParameter: String, value: String) =
        "($searchParameter = $value)"

    fun prefixNe(searchParameter: String, value: String) =
        "($searchParameter != $value)"

    fun prefixGt(searchParameter: String, value: String) =
        "($value > $searchParameter)"

    fun prefixLt(searchParameter: String, value: String) =
        "($value < $searchParameter)"

    fun prefixGe(searchParameter: String, value: String) =
        "($value >= $searchParameter)"

    fun prefixLe(searchParameter: String, value: String) =
        "($value <= $searchParameter)"

    fun prefixSa(searchParameter: String, value: String) =
        prefixGt(searchParameter, value)

    fun prefixEb(searchParameter: String, value: String) =
        prefixLt(searchParameter, value)

}