declare function local:searchInList($resId as xs:string?, $resType as xs:string?, $listId as xs:string?) as xs:boolean?
{
    let $refString := fn:concat($resType, '/', $resId)
    let $result :=
	<results>
    {
        for $x in db:open("List")//id[contains(@value, $listId)]/../..

        for $entry in $x//entry

        where ($entry//reference[contains(@value, $refString)])

        return element result {$x}

    }
    </results>

    return fn:has-children($result)
};
