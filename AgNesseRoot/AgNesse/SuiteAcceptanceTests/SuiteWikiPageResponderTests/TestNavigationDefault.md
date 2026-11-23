{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A page that is neither a test nor a suite still offers its variables, and does not offer Test or Suite.

| script | | | |
| --- | --- |
| given page | NavPlain | with content | nav body |
| check | request page | NavPlain | 200 |
| ensure | content contains | href="NavPlain?variables" |
| reject | content contains | NavPlain?test |
| reject | content contains | NavPlain?suite |
