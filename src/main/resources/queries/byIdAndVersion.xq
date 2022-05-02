let $result :=
<results>
{
  for $x in db:open("#TYPE")/Patient/id[contains(@value, "#ID")]
  where $x/../meta/versionId/@value="#VERSION"

  let $result2 := $x/../..

  return element result {$result2}
}
</results>

return $result