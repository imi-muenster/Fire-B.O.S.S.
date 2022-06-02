package de.uni_muenster.imi.fhirFacade.basex

object BaseXQueries {

    fun getById(type: String, id: String) = readFile("getById.xq")
        .replace("#TYPE", type)
        .replace("#ID", id)

    fun getByIdAndVersion(type: String, id: String, version: String) = readFile("getByIdAndVersion.xq")
        .replace("#TYPE", type)
        .replace("#ID", id)
        .replace("#VERSION", version)

    fun deleteById(type: String, id: String) = readFile("deleteById.xq")
        .replace("#TYPE", type)
        .replace("#ID", id)

    fun deleteByIdAndVersion(type: String, id: String, version: String) = readFile("deleteByIdAndVersion.xq")
        .replace("#TYPE", type)
        .replace("#ID", id)
        .replace("#VERSION", version)

    fun getHistoryInRange(type: String, id: String, startDateTime: String, endDateTime: String)
        = readFile("getHistoryInRange.xq")
        .replace("#TYPE", type)
        .replace("#ID", id)
        .replace("#START_DATETIME", startDateTime)
        .replace("#END_DATETIME", endDateTime)

    fun performSearch(type: String, constantConditions: String, optionalSearchparameters: String)
        = readFile("searchTemplate.xq")
        .replace("#TYPE", type)
        .replace("#CONSTANT_CONDITIONS", constantConditions)
        .replace("#OPTIONAL_SEARCHPARAMETERS", optionalSearchparameters)

    private fun readFile(filename: String): String {
        val query = javaClass.classLoader
            .getResourceAsStream("queries/$filename")!!
            .readBytes()
            .toString(Charsets.UTF_8)
        return query
    }
}