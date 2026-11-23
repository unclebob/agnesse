{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A page shows the header and footer inherited from the nearest ancestor. A nearer header replaces the one above it.

| script | | | |
| --- | --- |
| given page | HeaderNest.PageHeader | with content | nest-header |
| given page | HeaderNest.PageFooter | with content | nest-footer |
| given page | HeaderNest.Body | with content | nest-body |
| check | request page | HeaderNest.Body | 200 |
| ensure | content contains | nest-header |
| ensure | content contains | nest-body |
| ensure | content contains | nest-footer |
| given page | HeaderNest.Body.Kid | with content | nest-kid |
| check | request page | HeaderNest.Body.Kid | 200 |
| ensure | content contains | nest-header |
| ensure | content contains | nest-kid |
| given page | HeaderNest.Body.PageHeader | with content | kid-header |
| check | request page | HeaderNest.Body.Kid | 200 |
| ensure | content contains | kid-header |
| reject | content contains | nest-header |
