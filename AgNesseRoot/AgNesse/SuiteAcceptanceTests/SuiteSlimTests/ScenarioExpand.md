{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A scenario call on the page, before the test is run, collapses to the steps in its definition. The toggle is the same one a result page uses. Nothing has passed or failed yet, so the call is not colored. `@name` stays as written.

| scenario | greet | name |
| --- | --- | --- |
| check | echo | hello @name | hello @name |


| scenario | nest |
| --- |
| greet | world |


| scenario | make greeting | name |
| --- | --- | --- |
| check | echo | @name | @name |


| script | echo fixture |
| --- |
| nest |
| from the library |


| make greeting |
| --- |
| name |
| world |


| script | page driver |
| --- |
| ensure | bundled html contains; | AgNesse.SuiteAcceptanceTests.SuiteSlimTests.ScenarioExpand | class="scenario-toggle" |
| ensure | bundled html contains; | AgNesse.SuiteAcceptanceTests.SuiteSlimTests.ScenarioExpand | class="when-closed">▸ |
| ensure | bundled html contains; | AgNesse.SuiteAcceptanceTests.SuiteSlimTests.ScenarioExpand | class="scenario-body" |
| ensure | bundled html contains; | AgNesse.SuiteAcceptanceTests.SuiteSlimTests.ScenarioExpand | <details class="scenario-call"> |
| ensure | bundled html contains; | AgNesse.SuiteAcceptanceTests.SuiteSlimTests.ScenarioExpand | >library< |
| ensure | bundled html contains; | AgNesse.SuiteAcceptanceTests.SuiteSlimTests.ScenarioExpand | hello @name |
