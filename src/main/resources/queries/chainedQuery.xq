declare function local:performChainedQuery($ref as xs:string?, $cSP as xs:string?, $value as xs:string?) as xs:boolean?
{
	let $split := fn:tokenize($ref, "[/]")
    let $id := $split[last()]
    let $type := $split[position() = last() - 1]

    let $result :=
	<results>
    {
      for $x in db:open($type)//id[contains(@value, $id)]/../..


    let $chainedParamSet := $x//*[contains(fn:lower-case(local-name()), fn:lower-case($cSP))]//@value
    for $chainedParam in $chainedParamSet

    where ( ($x//id[contains(@value, $id)]) and (contains($chainedParam, $value)) )

    return element result {$x}
    }
    </results>

    return fn:has-children($result)
};
