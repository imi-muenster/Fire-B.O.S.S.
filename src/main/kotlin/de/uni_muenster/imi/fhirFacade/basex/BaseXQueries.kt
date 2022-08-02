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


    fun getDBNames() = "for \$x in db:list()\nreturn \$x"

    fun performSearch(type: String, optionalSearchparameters: String)
        = getDateFunctions() +
        getListQueryFunction() +
        getChainedQueryFunction() +
        getReverseChainedQueryFunction() +
        readFile("searchTemplate.xq")
            .replace("#TYPE", type)
            .replace("#OPTIONAL_SEARCHPARAMETERS", optionalSearchparameters)

    fun getServerHistory(resourceParts: String) =
        getDateFunctions() +
        getListQueryFunction() +
        readFile("serverHistoryTemplate.xq")
            .replace("#RESOURCEPARTS", resourceParts)


    private fun getDateFunctions() = readFile("dateFunctions.xq")

    private fun getChainedQueryFunction() = readFile("chainedQuery.xq")

    private fun getListQueryFunction() = readFile("listQuery.xq")

    private fun getReverseChainedQueryFunction() = readFile("reverseChainedQuery.xq")


    private fun readFile(filename: String): String {
        return javaClass.classLoader
            .getResourceAsStream("queries/$filename")!!
            .readBytes()
            .toString(Charsets.UTF_8)
    }
}