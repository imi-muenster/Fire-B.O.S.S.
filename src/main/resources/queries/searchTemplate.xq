let $result :=
<results>
{
  for $x in db:open("#TYPE")

  #OPTIONAL_SEARCHPARAMETERS

  return element result {$x}
}
</results>

return $result