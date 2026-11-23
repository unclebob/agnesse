{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A title search returns the pages whose names contain the text, as JSON.

| Library |
| --- |
| echo fixture |
| page driver |

| script | | | |
| --- | --- | --- | --- |
| given page | PageAbc | with content | This page has ABCs |
| check | request page | search?responder=search&type=title&q=PageAbc | 200 |
| ensure | content contains | PageAbc |
| check | request page | search?responder=search&type=title&q=no-such-title-zz | 200 |
| reject | content contains | PageAbc |
