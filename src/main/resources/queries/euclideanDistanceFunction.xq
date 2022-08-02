declare function local:euclideanDistance($resLatitude as xs:string?,
                                        $resLongitude as xs:string?,
                                        $searchLatitude as xs:string?,
                                        $searchLongitude as xs:string?) as xs:double
{
	let $result := math:pow(( xs:double($resLatitude) - xs:double($searchLatitude) ), 2) +
	                math:pow( (xs:double($resLongitude) - xs:double($searchLongitude) ), 2)

    return $result
};
