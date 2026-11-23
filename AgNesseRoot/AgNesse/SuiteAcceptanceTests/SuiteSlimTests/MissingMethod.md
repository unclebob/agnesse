{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A Slim call that throws is an exception on the cell. The page is not a pass, and the exception is in the totals, even when another check on the page passed.

| script | page driver |
| --- | --- |
| given page | MissingMethod.Throws | with content | <br>\| script \| agnesse.fixtures.EchoFixture \|<br>\| --- \| --- \|<br>\| check \| echo \| ok \| ok \|<br>\| check \| noSuchMethod; \| x \| true \| |
| make | MissingMethod.Throws | a test page |
| check | request page | MissingMethod.Throws?test&format=json | 200 |
| ensure | content contains | "kind":"MISSING_METHOD" |
| ensure | content contains | "status":"error" |
| ensure | content contains | "exceptions":1 |
| ensure | content contains | "right":1 |
| reject | content contains | "status":"pass","counts" |
| check | request page | MissingMethod.Throws?test | 200 |
| ensure | content contains | Exit-Code: 1 |
