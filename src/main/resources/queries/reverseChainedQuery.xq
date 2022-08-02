declare function local:performReverseChainedQuery(
    $refPath as xs:string?,
    $refId as xs:string?,
    $parameterPath as xs:string?,
    $parameterValue as xs:string?,
    $targetResource as xs:string?
) as xs:boolean?
{
    let $result :=
	<results>
    {
        for $x in db:open($targetResource)

        let $parameter_set := xquery:eval($parameterPath, map {"": $x})
        for $parameter in $parameter_set
        for $possibleMatches in $parameter//@value

        where (
                (contains($possibleMatches, $parameterValue)) and
                (xquery:eval($refPath, map {"": $x})[contains(@value, $refId)])
        )

        return element result {$x}


    }
    </results>

    return fn:has-children($result)
};
