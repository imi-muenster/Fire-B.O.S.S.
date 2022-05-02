let $result := 
<results>
{
  for $x in db:open("#TYPE")/Patient/id[contains(@value, "#ID")]
	
  let $result2 := $x/../..
  
  return element result {$result2}
}
</results>

return $result