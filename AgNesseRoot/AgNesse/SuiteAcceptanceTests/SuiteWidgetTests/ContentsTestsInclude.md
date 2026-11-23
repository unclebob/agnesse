{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
**Next create a child**

| script | |
| --- | --- |
| start | Page Builder |
| line | I came first! |
| attributes | Suite=true,Test=true,Suites=FC1 |
| page | `ParentPage.FirstChild` |

**Next create a sibling**

| script | |
| --- | --- |
| start | Page Builder |
| line | I'm younger, but wiser |
| attributes | \`WikiImport\`=true,Prune=true,Suites=FC2 |
| page | `ParentPage.SecondChild` |

| script | page driver |
| --- | --- |
| check | request page | ParentPage.FirstChild | 200 |
| ensure | content contains | I came first! |
| check | page has attribute; | ParentPage.FirstChild | Suite | true |
| check | page has attribute; | ParentPage.SecondChild | Prune | true |
