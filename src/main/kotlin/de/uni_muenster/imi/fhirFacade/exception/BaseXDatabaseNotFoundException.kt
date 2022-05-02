package de.uni_muenster.imi.fhirFacade.exception

import java.lang.Exception

class BaseXDatabaseNotFoundException:
    Exception("The Database could not be found on the given BaseX Server.")