package de.uni_muenster.imi.fhirFacade.basex

object BaseXQueries {

    fun getById(type: String, id: String) = readFile("byId.xq")
        .replace("#TYPE", type)
        .replace("#ID", id)

    fun getByIdAndVersion(type: String, id: String, version: String) = readFile("byIdAndVersion.xq")
        .replace("#TYPE", type)
        .replace("#ID", id)
        .replace("#VERSION", version)

    private fun readFile(filename: String): String {
        val query = javaClass.classLoader
            .getResourceAsStream("queries/$filename")!!
            .readBytes()
            .toString(Charsets.UTF_8)
        return query
    }
}