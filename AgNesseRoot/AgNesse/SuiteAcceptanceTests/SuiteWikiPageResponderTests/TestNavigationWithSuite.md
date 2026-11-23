{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A suite page offers Suite in the menu.

| script | | | |
| --- | --- |
| given page | NavSuite | with content | a suite |
| make | NavSuite | a suite page |
| check | request page | NavSuite | 200 |
| ensure | content contains | href="NavSuite?suite" |
| ensure | content contains | >Suite</a> |
| reject | content contains | NavSuite?test |
