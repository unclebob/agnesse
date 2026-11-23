{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A passing check inside a scenario counts as right, even when another scenario check on the same page is wrong. The run summary counts those assertions, not the page as a single wrong.

| script | page driver |
| --- | --- |
| given page | ScenarioCounts.Colored | with content | <br>\| scenario \| passes \|<br>\| --- \| --- \|<br>\| check \| echo \| ok \| ok \|<br><br>\| scenario \| fails \|<br>\| --- \| --- \|<br>\| check \| echo \| ok \| no \|<br><br>\| script \| agnesse.fixtures.EchoFixture \|<br>\| --- \| --- \|<br>\| passes \|<br>\| fails \| |
| make | ScenarioCounts.Colored | a test page |
| check | request page | ScenarioCounts.Colored?test | 200 |
| ensure | content contains | "summary":"1 right, 1 wrong, 0 ignored, 0 exceptions" |
| reject | content contains | "summary":"0 right, 1 wrong, 0 ignored, 0 exceptions" |
| check | request page | ScenarioCounts.Colored?test&format=json | 200 |
| ensure | content contains | "right":1 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "status":"fail" |
