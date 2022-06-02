let $result :=
<results>
{
  for $x in db:open("#TYPE")
  #CONSTANT_CONDITIONS

  #OPTIONAL_SEARCHPARAMETERS

  return $x
}
</results>

return $result