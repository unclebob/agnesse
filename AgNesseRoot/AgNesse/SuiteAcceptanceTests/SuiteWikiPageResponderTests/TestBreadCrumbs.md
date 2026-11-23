{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
The current page is the heading. Ancestor pages are the links above it.

| script | | | |
| --- | --- |
| given page | CrumbTop | with content | top level page |
| check | request page | CrumbTop | 200 |
| ensure | content contains | <h1>CrumbTop</h1> |
| reject | content contains | <a href="/CrumbTop">CrumbTop</a> |
| given page | CrumbTop.CrumbChild | with content | second level |
| check | request page | CrumbTop.CrumbChild | 200 |
| ensure | content contains | <a href="/CrumbTop">CrumbTop</a> |
| ensure | content contains | <h1>CrumbChild</h1> |
