{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A test page offers Test in the menu.

| script | | | |
| --- | --- |
| given page | NavTest | with content | a test |
| make | NavTest | a test page |
| check | request page | NavTest | 200 |
| ensure | content contains | href="NavTest?test" |
| ensure | content contains | >Test</a> |
| reject | content contains | NavTest?suite |
